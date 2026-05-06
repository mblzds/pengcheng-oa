package com.pengcheng.system.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.project.entity.PmProject;
import com.pengcheng.system.project.entity.PmProjectMember;

import java.util.List;
import java.util.Map;

/**
 * 项目服务
 */
public interface PmProjectService {

    PmProject getById(Long id);

    IPage<PmProject> page(Page<PmProject> page, Long currentUserId, String scope, Integer status);

    Long create(PmProject project, Long currentUserId);

    void update(PmProject project);

    void delete(Long id);

    List<PmProjectMember> listMembers(Long projectId);

    void addMember(Long projectId, Long userId, String role);

    void updateMemberRole(Long projectId, Long userId, String role);

    void removeMember(Long projectId, Long userId);

    Map<String, Object> getStats(Long projectId);
}
