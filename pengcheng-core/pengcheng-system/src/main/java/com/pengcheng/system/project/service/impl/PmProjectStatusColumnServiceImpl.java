package com.pengcheng.system.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.project.entity.PmProjectStatusColumn;
import com.pengcheng.system.project.mapper.PmProjectStatusColumnMapper;
import com.pengcheng.system.project.service.PmProjectStatusColumnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目看板状态列配置服务实现（V24）
 */
@Service
@RequiredArgsConstructor
public class PmProjectStatusColumnServiceImpl implements PmProjectStatusColumnService {

    private final PmProjectStatusColumnMapper columnMapper;

    @Override
    public List<PmProjectStatusColumn> listByProjectId(Long projectId) {
        return columnMapper.selectList(
                new LambdaQueryWrapper<PmProjectStatusColumn>()
                        .eq(PmProjectStatusColumn::getProjectId, projectId)
                        .orderByAsc(PmProjectStatusColumn::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PmProjectStatusColumn column) {
        if (column.getSortOrder() == null) {
            long maxOrder = listByProjectId(column.getProjectId()).stream()
                    .mapToInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0)
                    .max().orElse(0);
            column.setSortOrder((int) maxOrder + 1);
        }
        if (column.getIsDone() == null) column.setIsDone(0);
        columnMapper.insert(column);
        return column.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PmProjectStatusColumn column) {
        columnMapper.updateById(column);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        columnMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long projectId, List<Long> columnIdsInOrder) {
        for (int i = 0; i < columnIdsInOrder.size(); i++) {
            PmProjectStatusColumn c = new PmProjectStatusColumn();
            c.setId(columnIdsInOrder.get(i));
            c.setSortOrder(i);
            columnMapper.updateById(c);
        }
    }
}
