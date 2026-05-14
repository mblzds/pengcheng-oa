package com.pengcheng.admin.controller.hr;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.entity.SysDept;
import com.pengcheng.system.entity.SysRole;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.helper.SystemConfigHelper;
import com.pengcheng.system.mapper.SysDeptMapper;
import com.pengcheng.system.mapper.SysRoleMapper;
import com.pengcheng.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 考勤数据可见性范围决策
 * 三档：
 *   - 角色 code 命中考勤模块的"全员可见"白名单（默认 admin/chairman/general_manager/hr）→ 不限
 *   - 部门主管（角色 data_scope=4/3 或 dept.leader_id）→ 本部门 + 下级部门成员
 *   - 普通员工 → 仅自己
 *
 * 设计：每个业务模块自己定义"看全员"的角色清单，与通用 sys_role.data_scope 解耦——
 * data_scope=1 在 sys_role 表上表达"该角色在自己业务里看全员"（如 finance 在付款/佣金
 * 看全员），但跨到考勤模块未必合理。所以考勤模块走独立白名单：
 *   sys_config_group(attendance).fullScopeRoleCodes
 * 默认 "admin,chairman,general_manager,hr"，HR 在「系统配置 → 考勤设置」可改。
 * 部门级可见性（data_scope=3/4 / dept.leader_id）继续沿用，因为那是"角色 + 部门"
 * 的天然结构，未引发同款跨模块问题。
 */
@Component
@RequiredArgsConstructor
public class AttendanceScopeHelper {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SystemConfigHelper systemConfigHelper;

    /**
     * 当前登录用户在考勤数据上的可见 userId 集合
     * @return null 表示不限；非空集合表示限定为这些 userId
     */
    public Set<Long> visibleUserIds() {
        Long currentUid = StpUtil.getLoginIdAsLong();
        List<SysRole> userRoles = roleMapper.selectRolesByUserId(currentUid).stream()
                .filter(r -> r != null && Integer.valueOf(1).equals(r.getStatus()))
                .collect(Collectors.toList());

        // 任一启用角色的 code 命中"考勤看全员"白名单 → 不限
        // 白名单来自 sys_config_group(attendance).fullScopeRoleCodes，运行时可改
        Set<String> fullScopeRoleCodes = systemConfigHelper.getAttendanceFullScopeRoleCodes();
        for (SysRole r : userRoles) {
            if (r.getCode() != null && fullScopeRoleCodes.contains(r.getCode())) {
                return null;
            }
        }

        // 多角色取并集（按 data_scope 各自展开后合并）
        Set<Long> result = new HashSet<>();
        result.add(currentUid);  // 始终包含自己

        SysUser me = userMapper.selectById(currentUid);
        Long myDeptId = (me != null) ? me.getDeptId() : null;

        for (SysRole r : userRoles) {
            Integer ds = r.getDataScope();
            if (ds == null) continue;
            switch (ds) {
                case 4:  // 本部门及以下
                    if (myDeptId != null) {
                        result.addAll(usersInDeptAndDescendants(myDeptId));
                    }
                    break;
                case 3:  // 本部门
                    if (myDeptId != null) {
                        result.addAll(usersInDept(myDeptId));
                    }
                    break;
                case 2:  // 自定义（sys_role_dept 关联部门 + 下钻）
                    // 暂不实现：项目当前无角色走 data_scope=2，留 TODO
                    break;
                case 5:  // 仅本人
                default:
                    break;
            }
        }

        // 兼容：sys_dept.leader_id 历史兜底——即便角色没配 data_scope=4，
        // 只要被设为某部门 leader_id 也获得"本部门及以下"可见权
        List<SysDept> ledDepts = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getLeaderId, currentUid));
        for (SysDept d : ledDepts) {
            result.addAll(usersInDeptAndDescendants(d.getId()));
        }

        return result;
    }

    /** 拉指定部门下（不下钻）所有启用员工的 userId */
    private Set<Long> usersInDept(Long deptId) {
        return userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, deptId))
                .stream().map(SysUser::getId).collect(Collectors.toSet());
    }

    /** 拉指定部门 + 所有后代部门下的员工 userId */
    private Set<Long> usersInDeptAndDescendants(Long deptId) {
        Set<Long> deptIds = new HashSet<>();
        deptIds.add(deptId);
        List<SysDept> descendants = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .apply("FIND_IN_SET({0}, ancestors)", deptId.toString()));
        for (SysDept d : descendants) {
            deptIds.add(d.getId());
        }
        return userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().in(SysUser::getDeptId, deptIds))
                .stream().map(SysUser::getId).collect(Collectors.toSet());
    }

    /**
     * 当前登录用户能看到哪些用户（用于 dropdown）
     * 返回精简列表 [{id, nickname, deptId, deptName}]，不限时返回所有启用用户
     */
    public List<Map<String, Object>> visibleUserOptions() {
        Set<Long> allowed = visibleUserIds();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId);
        if (allowed != null) {
            if (allowed.isEmpty()) return Collections.emptyList();
            wrapper.in(SysUser::getId, allowed);
        }
        List<SysUser> users = userMapper.selectList(wrapper);

        // 拉一次部门用于回填 deptName，避免 N+1
        Map<Long, String> deptNameMap = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));

        return users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("deptId", u.getDeptId());
            m.put("deptName", u.getDeptId() != null ? deptNameMap.get(u.getDeptId()) : null);
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 校验请求方传入的 userId 是否在可见范围内；越权抛业务异常
     */
    public void assertCanView(Long userId) {
        if (userId == null) return;
        Set<Long> allowed = visibleUserIds();
        if (allowed != null && !allowed.contains(userId)) {
            throw new BusinessException("无权查看该用户的考勤数据");
        }
    }

    /**
     * 把 visibleUserIds 与"指定部门（含下钻）下的全部员工"做交集，用于月度汇总按部门聚焦。
     * @param visible visibleUserIds() 返回值；null = 全员
     * @param deptIds 前端筛选的部门 id 列表；null/空 = 不收紧
     * @return 与 visible 同语义：null = 不限；空集合 = 没人；非空 = 限定的 userId
     */
    public Set<Long> intersectWithDepts(Set<Long> visible, java.util.List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return visible;
        // 把每个 dept 下钻到所有后代部门
        Set<Long> allDeptIds = new HashSet<>();
        for (Long did : deptIds) {
            if (did == null) continue;
            allDeptIds.add(did);
            java.util.List<SysDept> descendants = deptMapper.selectList(
                    new LambdaQueryWrapper<SysDept>()
                            .apply("FIND_IN_SET({0}, ancestors)", did.toString()));
            for (SysDept d : descendants) allDeptIds.add(d.getId());
        }
        if (allDeptIds.isEmpty()) return visible == null ? Set.of() : visible;
        java.util.List<SysUser> deptUsers = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().in(SysUser::getDeptId, allDeptIds));
        Set<Long> deptUserIds = deptUsers.stream().map(SysUser::getId).collect(Collectors.toSet());
        if (visible == null) return deptUserIds;
        // visible 非 null 时取交集（防越权：经理筛上级部门时不应跨出本部门范围）
        deptUserIds.retainAll(visible);
        return deptUserIds;
    }
}
