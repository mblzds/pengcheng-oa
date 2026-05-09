package com.pengcheng.admin.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.admin.dto.roster.RosterImportResultVO;
import com.pengcheng.admin.dto.roster.RosterPreviewVO;
import com.pengcheng.admin.dto.roster.RosterRowDTO;
import com.pengcheng.admin.service.RosterImportService;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.hr.employee.entity.EmployeeProfile;
import com.pengcheng.hr.employee.mapper.EmployeeProfileMapper;
import com.pengcheng.system.entity.SysDept;
import com.pengcheng.system.entity.SysRole;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.entity.SysUserRole;
import com.pengcheng.system.mapper.SysDeptMapper;
import com.pengcheng.system.mapper.SysRoleMapper;
import com.pengcheng.system.mapper.SysUserMapper;
import com.pengcheng.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 花名册 CSV 导入服务
 *
 * 导入流程（commit）：
 *   Pass 1: 部门按 deptPath 深度排序，建/补 sys_dept（带 ancestors）
 *   Pass 2: 按工号 upsert sys_user + hr_employee_profile（密码默认 BCrypt(Pc@123456)）
 *   Pass 3: 回填 sys_dept.leader_id + sys_user.leader_id
 *   Pass 4: 重建 sys_user_role（按角色映射规则补 flow_*）
 *
 * 全程 @Transactional，任何一行入库失败就整个回滚（preview 阶段已经把校验失败的行剔除）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RosterImportServiceImpl implements RosterImportService {

    private static final String DEFAULT_PASSWORD = "Pc@123456";

    /** CSV 表头（必须按此顺序） */
    private static final String[] HEADERS = {
            "工号", "姓名", "登录名", "手机", "邮箱", "部门路径",
            "本部门负责人", "直接上级姓名", "角色", "性别", "入职日期", "状态"
    };

    /** 中文角色名 → role.code（多个通过 ; 分隔）。审批流标签 flow_* 由系统自动补 */
    private static final Map<String, String> ROLE_NAME_TO_CODE = Map.of(
            "超级管理员", "admin",
            "部门经理", "manager",
            "HR", "hr",
            "人事", "hr",
            "财务", "user",     // 财务没有独立业务角色，用 user 打底，flow_finance 由规则补
            "普通员工", "user"
    );

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final EmployeeProfileMapper employeeProfileMapper;

    @Override
    public RosterPreviewVO preview(MultipartFile file) throws IOException {
        List<RosterRowDTO> rows = parseCsv(file);
        return buildPreview(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RosterImportResultVO importRoster(MultipartFile file) throws IOException {
        List<RosterRowDTO> rows = parseCsv(file);
        RosterPreviewVO preview = buildPreview(rows);

        // 校验失败的行直接剔除，不进入入库流程；errors 一并返回给 HR
        Set<Integer> errorLines = new HashSet<>();
        for (RosterPreviewVO.RowError e : preview.getErrors()) {
            errorLines.add(e.getLineNo());
        }
        List<RosterRowDTO> validRows = new ArrayList<>();
        for (RosterRowDTO r : rows) {
            if (!errorLines.contains(r.getLineNo())) validRows.add(r);
        }

        // ===== Pass 1: 部门 =====
        Map<String, Long> deptPathToId = ensureDepts(validRows);

        // ===== Pass 2: 用户 upsert =====
        int created = 0, updated = 0, deactivated = 0;
        Map<String, Long> employeeNoToUserId = loadEmployeeNoIndex();
        Map<String, Long> nameToUserId = new HashMap<>();  // 同一批新建用户的姓名 → id 缓存（用于直接上级解析）

        for (RosterRowDTO r : validRows) {
            Long deptId = deptPathToId.get(r.getDeptPath());
            Long existingId = employeeNoToUserId.get(r.getEmployeeNo());

            if ("离职".equals(r.getStatus())) {
                if (existingId != null) {
                    SysUser u = new SysUser();
                    u.setId(existingId);
                    u.setDeleted(1);
                    userMapper.updateById(u);
                    deactivated++;
                }
                // 离职行不参与后续 leader 解析，跳过
                continue;
            }

            SysUser user = new SysUser();
            user.setNickname(r.getName());
            user.setUsername(blankToNull(r.getUsername(), r.getEmployeeNo()));
            user.setPhone(r.getPhone());
            user.setEmail(blankToNull(r.getEmail()));
            user.setDeptId(deptId);
            user.setGender(parseGender(r.getGender()));
            user.setStatus(1);

            if (existingId == null) {
                user.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD));
                userMapper.insert(user);
                upsertEmployeeProfile(user.getId(), r);
                employeeNoToUserId.put(r.getEmployeeNo(), user.getId());
                created++;
            } else {
                user.setId(existingId);
                userMapper.updateById(user);
                upsertEmployeeProfile(existingId, r);
                updated++;
            }
            nameToUserId.put(r.getName(), employeeNoToUserId.get(r.getEmployeeNo()));
        }

        // ===== Pass 3: 回填 leader_id（user.leader_id + dept.leader_id） =====
        for (RosterRowDTO r : validRows) {
            if ("离职".equals(r.getStatus())) continue;
            Long userId = employeeNoToUserId.get(r.getEmployeeNo());
            if (userId == null) continue;

            // user.leader_id：直接上级姓名 → 同批 nameToUserId 或 DB 已有
            String leaderName = r.getLeaderName();
            Long leaderId = null;
            if (leaderName != null && !leaderName.isBlank()) {
                leaderId = nameToUserId.get(leaderName);
                if (leaderId == null) {
                    leaderId = findUserIdByNickname(leaderName);
                }
            }
            if (leaderId != null && !leaderId.equals(userId)) {
                SysUser u = new SysUser();
                u.setId(userId);
                u.setLeaderId(leaderId);
                userMapper.updateById(u);
            }

            // dept.leader_id：本部门负责人=Y 时，把该用户写入对应部门
            if (r.isDeptLeader()) {
                Long deptId = deptPathToId.get(r.getDeptPath());
                if (deptId != null) {
                    SysDept d = new SysDept();
                    d.setId(deptId);
                    d.setLeaderId(userId);
                    deptMapper.updateById(d);
                }
            }
        }

        // ===== Pass 4: 角色重建 =====
        Map<String, Long> roleCodeToId = loadRoleCodeIndex();
        for (RosterRowDTO r : validRows) {
            if ("离职".equals(r.getStatus())) continue;
            Long userId = employeeNoToUserId.get(r.getEmployeeNo());
            if (userId == null) continue;
            Set<Long> targetRoleIds = resolveRoleIds(r, roleCodeToId, deptPathToId);
            // 删旧
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
            // 加新
            for (Long rid : targetRoleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(rid);
                userRoleMapper.insert(ur);
            }
        }

        RosterImportResultVO result = new RosterImportResultVO();
        result.setDeptsCreated(preview.getDeptsToCreate());
        result.setUsersCreated(created);
        result.setUsersUpdated(updated);
        result.setUsersDeactivated(deactivated);
        result.setErrorRows(preview.getErrors().size());
        result.setErrors(preview.getErrors());
        result.setDefaultPassword(DEFAULT_PASSWORD);
        return result;
    }

    @Override
    public String template() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        sb.append("A001,周强,gm_zhou,13800001001,zhou@pc.com,朋诚科技,Y,,部门经理,M,2020-01-01,在职\n");
        sb.append("A002,王军,mgr_wang,13800001002,wang@pc.com,朋诚科技/技术部,Y,周强,部门经理,M,2021-03-15,在职\n");
        sb.append("A003,李一,dev_li,13800001003,li@pc.com,朋诚科技/技术部/后端组,N,王军,普通员工,F,2024-06-01,在职\n");
        sb.append("A004,孙HR,hr_sun,13800001004,hr@pc.com,朋诚科技/HR部,Y,周强,HR,F,2022-08-10,在职\n");
        return sb.toString();
    }

    // ============================== CSV 解析 ==============================

    private List<RosterRowDTO> parseCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        List<RosterRowDTO> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new BusinessException("CSV 没有表头");
            // 容错 BOM
            if (headerLine.startsWith("﻿")) headerLine = headerLine.substring(1);
            String[] headers = splitCsv(headerLine);
            if (!Arrays.equals(headers, HEADERS)) {
                throw new BusinessException("CSV 表头与模板不一致，应为：" + String.join(",", HEADERS));
            }

            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                String[] cols = splitCsv(line);
                if (cols.length < HEADERS.length) {
                    cols = Arrays.copyOf(cols, HEADERS.length);
                    for (int i = 0; i < cols.length; i++) if (cols[i] == null) cols[i] = "";
                }
                RosterRowDTO r = new RosterRowDTO();
                r.setLineNo(lineNo);
                r.setEmployeeNo(trim(cols[0]));
                r.setName(trim(cols[1]));
                r.setUsername(trim(cols[2]));
                r.setPhone(trim(cols[3]));
                r.setEmail(trim(cols[4]));
                r.setDeptPath(trim(cols[5]));
                r.setDeptLeader("Y".equalsIgnoreCase(trim(cols[6])));
                r.setLeaderName(trim(cols[7]));
                r.setRoleNames(trim(cols[8]));
                r.setGender(trim(cols[9]));
                r.setHireDate(trim(cols[10]));
                r.setStatus(blankToDefault(trim(cols[11]), "在职"));
                rows.add(r);
            }
        }
        return rows;
    }

    /** 简易 CSV 切分，支持 "..." 包裹（含逗号），不支持转义双引号——HR 表足够用 */
    private static String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    // ============================== 校验 + Diff ==============================

    private RosterPreviewVO buildPreview(List<RosterRowDTO> rows) {
        RosterPreviewVO vo = new RosterPreviewVO();
        vo.setTotalRows(rows.size());
        List<RosterPreviewVO.RowError> errors = new ArrayList<>();

        Set<String> seenEmployeeNos = new HashSet<>();
        Set<String> seenPhones = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        Set<String> seenUsernames = new HashSet<>();

        Map<String, Long> existingEmployeeNos = loadEmployeeNoIndex();
        Map<String, Long> existingDeptPaths = loadDeptPathIndex();
        Set<String> existingPhones = loadAllPhones();
        Set<String> existingEmails = loadAllEmails();
        Set<String> existingUsernames = loadAllUsernames();

        Set<String> deptPathsToCreate = new java.util.LinkedHashSet<>();
        int usersToCreate = 0, usersToUpdate = 0, usersToDeactivate = 0;
        Set<String> validNames = new HashSet<>();  // 文件内已校验通过的姓名（供 leaderName 引用校验）

        // 第一遍：单行校验 + 文件内 dup 检查
        for (RosterRowDTO r : rows) {
            String err = validateRow(r);
            if (err != null) { errors.add(new RosterPreviewVO.RowError(r.getLineNo(), r.getEmployeeNo(), r.getName(), err)); continue; }

            // 文件内重复
            if (!seenEmployeeNos.add(r.getEmployeeNo())) {
                errors.add(new RosterPreviewVO.RowError(r.getLineNo(), r.getEmployeeNo(), r.getName(), "工号在文件内重复"));
                continue;
            }
            if (r.getPhone() != null && !r.getPhone().isBlank() && !seenPhones.add(r.getPhone())) {
                errors.add(new RosterPreviewVO.RowError(r.getLineNo(), r.getEmployeeNo(), r.getName(), "手机号在文件内重复: " + r.getPhone()));
                continue;
            }
            if (r.getEmail() != null && !r.getEmail().isBlank() && !seenEmails.add(r.getEmail())) {
                errors.add(new RosterPreviewVO.RowError(r.getLineNo(), r.getEmployeeNo(), r.getName(), "邮箱在文件内重复: " + r.getEmail()));
                continue;
            }
            String username = blankToNull(r.getUsername(), r.getEmployeeNo());
            if (!seenUsernames.add(username)) {
                errors.add(new RosterPreviewVO.RowError(r.getLineNo(), r.getEmployeeNo(), r.getName(), "登录名在文件内重复: " + username));
                continue;
            }

            // 与 DB 撞库（除了同工号 upsert 时允许）
            Long existingByEmpNo = existingEmployeeNos.get(r.getEmployeeNo());
            if (r.getPhone() != null && !r.getPhone().isBlank()
                    && existingPhones.contains(r.getPhone())
                    && !samePhoneAsExisting(existingByEmpNo, r.getPhone())) {
                errors.add(new RosterPreviewVO.RowError(r.getLineNo(), r.getEmployeeNo(), r.getName(), "手机号与系统已有用户冲突: " + r.getPhone()));
                continue;
            }
            if (r.getEmail() != null && !r.getEmail().isBlank()
                    && existingEmails.contains(r.getEmail())
                    && !sameEmailAsExisting(existingByEmpNo, r.getEmail())) {
                errors.add(new RosterPreviewVO.RowError(r.getLineNo(), r.getEmployeeNo(), r.getName(), "邮箱与系统已有用户冲突: " + r.getEmail()));
                continue;
            }
            if (existingUsernames.contains(username) && !sameUsernameAsExisting(existingByEmpNo, username)) {
                errors.add(new RosterPreviewVO.RowError(r.getLineNo(), r.getEmployeeNo(), r.getName(), "登录名与系统已有用户冲突: " + username));
                continue;
            }

            // diff 统计
            if ("离职".equals(r.getStatus())) {
                if (existingByEmpNo != null) usersToDeactivate++;
                continue;
            }
            if (existingByEmpNo == null) usersToCreate++;
            else usersToUpdate++;

            // 部门路径 diff
            collectDeptPathsToCreate(r.getDeptPath(), existingDeptPaths, deptPathsToCreate);
            validNames.add(r.getName());
        }

        // 第二遍：直接上级姓名引用校验（必须在文件中或 DB 已有）
        for (RosterRowDTO r : rows) {
            if (r.getLeaderName() == null || r.getLeaderName().isBlank()) continue;
            // 已经被第一遍标错的跳过
            boolean alreadyError = errors.stream().anyMatch(e -> e.getLineNo() == r.getLineNo());
            if (alreadyError) continue;
            if (validNames.contains(r.getLeaderName())) continue;
            // DB 里查
            if (findUserIdByNickname(r.getLeaderName()) != null) continue;
            errors.add(new RosterPreviewVO.RowError(r.getLineNo(), r.getEmployeeNo(), r.getName(),
                    "直接上级姓名 [" + r.getLeaderName() + "] 在文件中和系统中均未找到"));
        }

        vo.setErrorRows(errors.size());
        vo.setValidRows(rows.size() - errors.size());
        vo.setDeptsToCreate(deptPathsToCreate.size());
        vo.setDeptsToCreateList(new ArrayList<>(deptPathsToCreate));
        vo.setUsersToCreate(usersToCreate);
        vo.setUsersToUpdate(usersToUpdate);
        vo.setUsersToDeactivate(usersToDeactivate);
        vo.setErrors(errors);
        return vo;
    }

    /** 校验单行，返回 null 表示通过 */
    private String validateRow(RosterRowDTO r) {
        if (r.getEmployeeNo() == null || r.getEmployeeNo().isBlank()) return "工号必填";
        if (r.getName() == null || r.getName().isBlank()) return "姓名必填";
        if (r.getDeptPath() == null || r.getDeptPath().isBlank()) return "部门路径必填";
        if (r.getStatus() != null && !r.getStatus().equals("在职") && !r.getStatus().equals("离职")) {
            return "状态只能是 在职 / 离职";
        }
        // 手机：在职必填，离职可空
        if ("在职".equals(r.getStatus())) {
            if (r.getPhone() == null || r.getPhone().isBlank()) return "在职员工手机号必填";
            if (!r.getPhone().matches("\\d{11}")) return "手机号格式错误：" + r.getPhone();
        }
        if (r.getEmail() != null && !r.getEmail().isBlank() && !r.getEmail().contains("@")) {
            return "邮箱格式错误：" + r.getEmail();
        }
        if (r.getRoleNames() == null || r.getRoleNames().isBlank()) {
            // 默认按普通员工
        } else {
            for (String name : r.getRoleNames().split(";")) {
                String n = name.trim();
                if (n.isEmpty()) continue;
                if (!ROLE_NAME_TO_CODE.containsKey(n)) {
                    return "未知角色：" + n + "（合法值：" + String.join("/", ROLE_NAME_TO_CODE.keySet()) + "）";
                }
            }
        }
        if (r.getHireDate() != null && !r.getHireDate().isBlank()) {
            try { LocalDate.parse(r.getHireDate()); }
            catch (DateTimeParseException e) { return "入职日期格式错误（应为 YYYY-MM-DD）：" + r.getHireDate(); }
        }
        return null;
    }

    // ============================== 部门处理 ==============================

    /** 把所有 deptPath 拆解，保证从根到叶按深度顺序建出来；返回 path → id 映射 */
    private Map<String, Long> ensureDepts(List<RosterRowDTO> validRows) {
        Map<String, Long> existing = loadDeptPathIndex();
        Map<String, Long> result = new HashMap<>(existing);

        // 收集所有需要的路径（包括所有祖先路径）
        Set<String> allPaths = new java.util.LinkedHashSet<>();
        for (RosterRowDTO r : validRows) {
            String[] segs = r.getDeptPath().split("/");
            StringBuilder p = new StringBuilder();
            for (String s : segs) {
                if (p.length() > 0) p.append("/");
                p.append(s);
                allPaths.add(p.toString());
            }
        }

        // 按路径深度排序（短的先建）
        List<String> sorted = new ArrayList<>(allPaths);
        sorted.sort((a, b) -> Integer.compare(a.split("/").length, b.split("/").length));

        for (String path : sorted) {
            if (result.containsKey(path)) continue;
            String[] segs = path.split("/");
            String name = segs[segs.length - 1];
            Long parentId = 0L;
            String ancestors = "0";
            if (segs.length > 1) {
                String parentPath = path.substring(0, path.lastIndexOf('/'));
                parentId = result.get(parentPath);
                if (parentId == null) {
                    throw new BusinessException("内部错误：父部门未建：" + parentPath);
                }
                SysDept parent = deptMapper.selectById(parentId);
                ancestors = parent.getAncestors() + "," + parentId;
            }
            SysDept d = new SysDept();
            d.setDeptName(name);
            d.setParentId(parentId);
            d.setAncestors(ancestors);
            d.setStatus(1);
            d.setSort(0);
            deptMapper.insert(d);
            result.put(path, d.getId());
        }
        return result;
    }

    private Map<String, Long> loadDeptPathIndex() {
        // 把 sys_dept 全部读出来，构造 path → id 映射
        List<SysDept> all = deptMapper.selectList(new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeleted, 0));
        Map<Long, SysDept> byId = new HashMap<>();
        for (SysDept d : all) byId.put(d.getId(), d);
        Map<String, Long> pathToId = new HashMap<>();
        for (SysDept d : all) {
            pathToId.put(buildPath(d, byId), d.getId());
        }
        return pathToId;
    }

    private String buildPath(SysDept d, Map<Long, SysDept> byId) {
        List<String> parts = new ArrayList<>();
        SysDept cur = d;
        while (cur != null && cur.getParentId() != null && cur.getParentId() != 0) {
            parts.add(0, cur.getDeptName());
            cur = byId.get(cur.getParentId());
        }
        if (cur != null) parts.add(0, cur.getDeptName());
        return String.join("/", parts);
    }

    private void collectDeptPathsToCreate(String path, Map<String, Long> existing, Set<String> out) {
        String[] segs = path.split("/");
        StringBuilder p = new StringBuilder();
        for (String s : segs) {
            if (p.length() > 0) p.append("/");
            p.append(s);
            String cur = p.toString();
            if (!existing.containsKey(cur)) out.add(cur);
        }
    }

    // ============================== 用户辅助 ==============================

    private Map<String, Long> loadEmployeeNoIndex() {
        List<EmployeeProfile> profiles = employeeProfileMapper.selectList(
                new LambdaQueryWrapper<EmployeeProfile>().isNotNull(EmployeeProfile::getEmployeeNo));
        Map<String, Long> map = new HashMap<>();
        for (EmployeeProfile p : profiles) {
            if (p.getEmployeeNo() != null) map.put(p.getEmployeeNo(), p.getUserId());
        }
        return map;
    }

    private Set<String> loadAllPhones() {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().select(SysUser::getId, SysUser::getPhone));
        Set<String> out = new HashSet<>();
        for (SysUser u : users) if (u.getPhone() != null) out.add(u.getPhone());
        return out;
    }

    private Set<String> loadAllEmails() {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().select(SysUser::getId, SysUser::getEmail));
        Set<String> out = new HashSet<>();
        for (SysUser u : users) if (u.getEmail() != null) out.add(u.getEmail());
        return out;
    }

    private Set<String> loadAllUsernames() {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().select(SysUser::getId, SysUser::getUsername));
        Set<String> out = new HashSet<>();
        for (SysUser u : users) if (u.getUsername() != null) out.add(u.getUsername());
        return out;
    }

    private boolean samePhoneAsExisting(Long userId, String phone) {
        if (userId == null) return false;
        SysUser u = userMapper.selectById(userId);
        return u != null && phone.equals(u.getPhone());
    }

    private boolean sameEmailAsExisting(Long userId, String email) {
        if (userId == null) return false;
        SysUser u = userMapper.selectById(userId);
        return u != null && email.equals(u.getEmail());
    }

    private boolean sameUsernameAsExisting(Long userId, String username) {
        if (userId == null) return false;
        SysUser u = userMapper.selectById(userId);
        return u != null && username.equals(u.getUsername());
    }

    private Long findUserIdByNickname(String nickname) {
        SysUser u = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getNickname, nickname).last("LIMIT 1"));
        return u == null ? null : u.getId();
    }

    private void upsertEmployeeProfile(Long userId, RosterRowDTO r) {
        EmployeeProfile existing = employeeProfileMapper.selectOne(
                new LambdaQueryWrapper<EmployeeProfile>().eq(EmployeeProfile::getUserId, userId).last("LIMIT 1"));
        LocalDate joinDate = null;
        if (r.getHireDate() != null && !r.getHireDate().isBlank()) {
            try { joinDate = LocalDate.parse(r.getHireDate()); } catch (Exception ignored) {}
        }
        if (existing == null) {
            EmployeeProfile p = new EmployeeProfile();
            p.setUserId(userId);
            p.setEmployeeNo(r.getEmployeeNo());
            p.setJoinDate(joinDate);
            employeeProfileMapper.insert(p);
        } else {
            existing.setEmployeeNo(r.getEmployeeNo());
            if (joinDate != null) existing.setJoinDate(joinDate);
            employeeProfileMapper.updateById(existing);
        }
    }

    // ============================== 角色映射 ==============================

    private Map<String, Long> loadRoleCodeIndex() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().eq(SysRole::getDeleted, 0));
        Map<String, Long> map = new HashMap<>();
        for (SysRole r : roles) map.put(r.getCode(), r.getId());
        return map;
    }

    /**
     * 解析行的角色 → role_id 集合，并按规则补审批流标记：
     *   - 部门经理 + 本部门负责人 → +flow_dept_mgr
     *   - 部门经理 + 在根部门当负责人 → +flow_gm
     *   - HR/人事 → +flow_hr
     *   - 财务 → +flow_finance
     */
    private Set<Long> resolveRoleIds(RosterRowDTO r, Map<String, Long> roleCodeToId, Map<String, Long> deptPathToId) {
        Set<String> codes = new HashSet<>();
        String roleNames = r.getRoleNames();
        if (roleNames == null || roleNames.isBlank()) {
            codes.add("user");
        } else {
            for (String name : roleNames.split(";")) {
                String n = name.trim();
                if (n.isEmpty()) continue;
                String code = ROLE_NAME_TO_CODE.get(n);
                if (code != null) codes.add(code);
                if ("HR".equals(n) || "人事".equals(n)) codes.add("flow_hr");
                if ("财务".equals(n)) codes.add("flow_finance");
            }
        }
        // 部门负责人补 flow_dept_mgr
        if (r.isDeptLeader() && codes.contains("manager")) {
            codes.add("flow_dept_mgr");
            // 根部门负责人 = 总经理
            if (!r.getDeptPath().contains("/")) {
                codes.add("flow_gm");
            }
        }
        Set<Long> ids = new HashSet<>();
        for (String c : codes) {
            Long id = roleCodeToId.get(c);
            if (id != null) ids.add(id);
        }
        return ids;
    }

    // ============================== 杂项 ==============================

    private Integer parseGender(String g) {
        if (g == null) return 0;
        String s = g.trim().toUpperCase();
        return switch (s) {
            case "M", "男", "1" -> 1;
            case "F", "女", "2" -> 2;
            default -> 0;
        };
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
    private static String blankToNull(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) return primary;
        return (fallback == null || fallback.isBlank()) ? null : fallback;
    }
    private static String blankToDefault(String s, String def) { return (s == null || s.isBlank()) ? def : s; }
}
