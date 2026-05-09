package com.pengcheng.admin.controller.hr;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.entity.SysDept;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.mapper.SysDeptMapper;
import com.pengcheng.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 考勤数据可见性范围决策
 * 三档：
 *   - HR / 管理员（全公司）→ 不限
 *   - 部门主管（担任至少一个 dept.leader_id 的用户）→ 本部门 + 所有下级部门成员
 *   - 普通员工 → 仅自己
 *
 * 不依赖 sys_role.data_scope，直接看「角色编码 + 是否担任部门负责人」，
 * 与该项目当前的角色体系一致（admin / flow_hr 等），后期若需要可改造。
 */
@Component
@RequiredArgsConstructor
public class AttendanceScopeHelper {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /** 视为"全公司可见"的角色编码 */
    private static final Set<String> UNRESTRICTED_ROLES = Set.of(
            "admin", "super_admin", "HR", "hr", "flow_hr"
    );

    /**
     * 当前登录用户在考勤数据上的可见 userId 集合
     * @return null 表示不限；非空集合表示限定为这些 userId
     */
    public Set<Long> visibleUserIds() {
        Long currentUid = StpUtil.getLoginIdAsLong();
        List<String> roles = StpUtil.getRoleList();
        for (String code : roles) {
            if (UNRESTRICTED_ROLES.contains(code)) {
                return null;
            }
        }
        // 担任了任意部门的 leader_id ?
        List<SysDept> ledDepts = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getLeaderId, currentUid));
        if (ledDepts.isEmpty()) {
            return Set.of(currentUid);  // 普通员工
        }
        // 部门主管：本部门 + 所有后代部门
        Set<Long> deptIds = new HashSet<>();
        for (SysDept d : ledDepts) {
            deptIds.add(d.getId());
            // ancestors 形如 "0,1,200"，找包含 d.id 的所有部门
            List<SysDept> descendants = deptMapper.selectList(
                    new LambdaQueryWrapper<SysDept>()
                            .apply("FIND_IN_SET({0}, ancestors)", d.getId().toString()));
            for (SysDept desc : descendants) {
                deptIds.add(desc.getId());
            }
        }
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().in(SysUser::getDeptId, deptIds));
        Set<Long> result = users.stream().map(SysUser::getId).collect(Collectors.toSet());
        result.add(currentUid);  // 主管自己也包含
        return result;
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
}
