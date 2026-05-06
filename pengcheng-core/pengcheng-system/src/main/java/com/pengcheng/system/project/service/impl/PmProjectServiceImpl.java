package com.pengcheng.system.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.project.entity.PmProject;
import com.pengcheng.system.project.entity.PmProjectMember;
import com.pengcheng.system.project.mapper.PmProjectMapper;
import com.pengcheng.system.project.mapper.PmProjectMemberMapper;
import com.pengcheng.system.project.mapper.PmTaskMapper;
import com.pengcheng.system.project.service.PmProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PmProjectServiceImpl implements PmProjectService {

    private final PmProjectMapper projectMapper;
    private final PmProjectMemberMapper memberMapper;
    private final PmTaskMapper taskMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PmProject getById(Long id) {
        return projectMapper.selectById(id);
    }

    @Override
    public IPage<PmProject> page(Page<PmProject> page, Long currentUserId, String scope, Integer status) {
        LambdaQueryWrapper<PmProject> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(PmProject::getStatus, status);
        if (currentUserId != null && scope != null) {
            if ("my_created".equals(scope)) {
                wrapper.eq(PmProject::getOwnerId, currentUserId);
            } else if ("my_joined".equals(scope)) {
                List<Long> projectIds = getMemberProjectIds(currentUserId);
                if (projectIds.isEmpty()) wrapper.eq(PmProject::getId, -1L);
                else wrapper.in(PmProject::getId, projectIds);
            } else if ("all".equals(scope)) {
                applyVisibilityFilter(wrapper, currentUserId);
            }
        } else if (currentUserId != null) {
            applyVisibilityFilter(wrapper, currentUserId);
        }
        wrapper.orderByDesc(PmProject::getCreateTime);
        return projectMapper.selectPage(page, wrapper);
    }

    /**
     * 可见性过滤：用户可见 visibility=all 的项目 + 同部门 visibility=dept 的项目 + 自己参与/创建的 private 项目
     */
    private void applyVisibilityFilter(LambdaQueryWrapper<PmProject> wrapper, Long userId) {
        List<Long> memberProjectIds = getMemberProjectIds(userId);
        Long userDeptId = getUserDeptId(userId);

        wrapper.and(w -> {
            w.eq(PmProject::getVisibility, "all");
            if (userDeptId != null) {
                List<Long> deptOwnerIds = getDeptUserIds(userDeptId);
                if (!deptOwnerIds.isEmpty()) {
                    w.or(inner -> inner.eq(PmProject::getVisibility, "dept")
                            .in(PmProject::getOwnerId, deptOwnerIds));
                }
            }
            if (!memberProjectIds.isEmpty()) {
                w.or(inner -> inner.in(PmProject::getId, memberProjectIds));
            }
            w.or(inner -> inner.eq(PmProject::getOwnerId, userId));
        });
    }

    private List<Long> getMemberProjectIds(Long userId) {
        return memberMapper.selectList(
                        new LambdaQueryWrapper<PmProjectMember>().eq(PmProjectMember::getUserId, userId))
                .stream().map(PmProjectMember::getProjectId).distinct().collect(Collectors.toList());
    }

    private Long getUserDeptId(Long userId) {
        try {
            return jdbcTemplate.queryForObject("SELECT dept_id FROM sys_user WHERE user_id = ?", Long.class, userId);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Long> getDeptUserIds(Long deptId) {
        try {
            return jdbcTemplate.queryForList("SELECT user_id FROM sys_user WHERE dept_id = ? AND status = '0'", Long.class, deptId);
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 检查用户是否为项目成员或有权限访问 */
    public boolean hasAccess(Long projectId, Long userId) {
        PmProject project = projectMapper.selectById(projectId);
        if (project == null) return false;
        if ("all".equals(project.getVisibility())) return true;
        if (Objects.equals(project.getOwnerId(), userId)) return true;
        long memberCount = memberMapper.selectCount(
                new LambdaQueryWrapper<PmProjectMember>()
                        .eq(PmProjectMember::getProjectId, projectId)
                        .eq(PmProjectMember::getUserId, userId));
        if (memberCount > 0) return true;
        if ("dept".equals(project.getVisibility())) {
            Long ownerDeptId = getUserDeptId(project.getOwnerId());
            Long userDeptId = getUserDeptId(userId);
            return ownerDeptId != null && ownerDeptId.equals(userDeptId);
        }
        return false;
    }

    /** 检查用户是否拥有指定角色或更高权限 */
    public boolean hasMemberRole(Long projectId, Long userId, String requiredRole) {
        PmProjectMember m = memberMapper.selectOne(
                new LambdaQueryWrapper<PmProjectMember>()
                        .eq(PmProjectMember::getProjectId, projectId)
                        .eq(PmProjectMember::getUserId, userId));
        if (m == null) return false;
        int required = roleLevel(requiredRole);
        int actual = roleLevel(m.getRole());
        return actual >= required;
    }

    private int roleLevel(String role) {
        if (role == null) return 0;
        return switch (role) {
            case "owner" -> 3;
            case "admin" -> 2;
            case "member" -> 1;
            default -> 0;
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PmProject project, Long currentUserId) {
        if (project.getStatus() == null) project.setStatus(1);
        if (project.getVisibility() == null) project.setVisibility("private");
        project.setOwnerId(currentUserId != null ? currentUserId : project.getOwnerId());
        project.setCreateBy(currentUserId);
        projectMapper.insert(project);
        if (currentUserId != null) {
            PmProjectMember owner = new PmProjectMember();
            owner.setProjectId(project.getId());
            owner.setUserId(currentUserId);
            owner.setRole("owner");
            memberMapper.insert(owner);
        }
        return project.getId();
    }

    @Override
    public void update(PmProject project) {
        projectMapper.updateById(project);
    }

    @Override
    public void delete(Long id) {
        projectMapper.deleteById(id);
    }

    @Override
    public List<PmProjectMember> listMembers(Long projectId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<PmProjectMember>().eq(PmProjectMember::getProjectId, projectId));
    }

    @Override
    public void addMember(Long projectId, Long userId, String role) {
        long c = memberMapper.selectCount(
                new LambdaQueryWrapper<PmProjectMember>()
                        .eq(PmProjectMember::getProjectId, projectId)
                        .eq(PmProjectMember::getUserId, userId));
        if (c > 0) return;
        PmProjectMember m = new PmProjectMember();
        m.setProjectId(projectId);
        m.setUserId(userId);
        m.setRole(role != null ? role : "member");
        memberMapper.insert(m);
    }

    @Override
    public void updateMemberRole(Long projectId, Long userId, String role) {
        PmProjectMember m = memberMapper.selectOne(
                new LambdaQueryWrapper<PmProjectMember>()
                        .eq(PmProjectMember::getProjectId, projectId)
                        .eq(PmProjectMember::getUserId, userId));
        if (m != null) {
            m.setRole(role);
            memberMapper.updateById(m);
        }
    }

    @Override
    public void removeMember(Long projectId, Long userId) {
        memberMapper.delete(
                new LambdaQueryWrapper<PmProjectMember>()
                        .eq(PmProjectMember::getProjectId, projectId)
                        .eq(PmProjectMember::getUserId, userId));
    }

    @Override
    public Map<String, Object> getStats(Long projectId) {
        var taskWrapper = new LambdaQueryWrapper<com.pengcheng.system.project.entity.PmTask>()
                .eq(com.pengcheng.system.project.entity.PmTask::getProjectId, projectId);
        long total = taskMapper.selectCount(taskWrapper);
        long completed = taskMapper.selectCount(taskWrapper.eq(com.pengcheng.system.project.entity.PmTask::getStatus, "已完成"));
        long overdue = taskMapper.selectCount(
                new LambdaQueryWrapper<com.pengcheng.system.project.entity.PmTask>()
                        .eq(com.pengcheng.system.project.entity.PmTask::getProjectId, projectId)
                        .ne(com.pengcheng.system.project.entity.PmTask::getStatus, "已完成")
                        .lt(com.pengcheng.system.project.entity.PmTask::getDueDate, LocalDate.now()));
        Map<String, Object> map = new HashMap<>();
        map.put("totalTasks", total);
        map.put("completedTasks", completed);
        map.put("overdueTasks", overdue);
        map.put("completionRate", total == 0 ? 0 : (completed * 100.0 / total));
        return map;
    }
}
