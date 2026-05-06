package com.pengcheng.system.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.project.entity.PmMilestone;
import com.pengcheng.system.project.mapper.PmMilestoneMapper;
import com.pengcheng.system.project.service.PmMilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 里程碑服务实现
 */
@Service
@RequiredArgsConstructor
public class PmMilestoneServiceImpl implements PmMilestoneService {

    private final PmMilestoneMapper milestoneMapper;

    @Override
    public PmMilestone getById(Long id) {
        return milestoneMapper.selectById(id);
    }

    @Override
    public List<PmMilestone> listByProject(Long projectId) {
        return milestoneMapper.selectList(
                new LambdaQueryWrapper<PmMilestone>()
                        .eq(PmMilestone::getProjectId, projectId)
                        .orderByAsc(PmMilestone::getSortOrder)
                        .orderByAsc(PmMilestone::getDueDate));
    }

    @Override
    public Long create(PmMilestone milestone) {
        if (milestone.getStatus() == null) milestone.setStatus(0);
        milestoneMapper.insert(milestone);
        return milestone.getId();
    }

    @Override
    public void update(PmMilestone milestone) {
        milestoneMapper.updateById(milestone);
    }

    @Override
    public void delete(Long id) {
        milestoneMapper.deleteById(id);
    }

    @Override
    public void setComplete(Long id, boolean complete) {
        PmMilestone m = milestoneMapper.selectById(id);
        if (m != null) {
            m.setStatus(complete ? 1 : 0);
            milestoneMapper.updateById(m);
        }
    }
}
