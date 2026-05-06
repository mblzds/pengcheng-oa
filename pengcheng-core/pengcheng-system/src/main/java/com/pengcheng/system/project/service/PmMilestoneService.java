package com.pengcheng.system.project.service;

import com.pengcheng.system.project.entity.PmMilestone;

import java.util.List;

/**
 * 里程碑服务
 */
public interface PmMilestoneService {

    PmMilestone getById(Long id);

    List<PmMilestone> listByProject(Long projectId);

    Long create(PmMilestone milestone);

    void update(PmMilestone milestone);

    void delete(Long id);

    void setComplete(Long id, boolean complete);
}
