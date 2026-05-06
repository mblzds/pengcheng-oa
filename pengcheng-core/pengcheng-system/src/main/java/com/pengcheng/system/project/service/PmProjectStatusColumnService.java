package com.pengcheng.system.project.service;

import com.pengcheng.system.project.entity.PmProjectStatusColumn;

import java.util.List;

/**
 * 项目看板状态列配置服务（V24）
 */
public interface PmProjectStatusColumnService {

    /** 按项目查询看板列（按 sort_order 排序） */
    List<PmProjectStatusColumn> listByProjectId(Long projectId);

    /** 新增看板列 */
    Long create(PmProjectStatusColumn column);

    /** 更新看板列 */
    void update(PmProjectStatusColumn column);

    /** 删除看板列 */
    void delete(Long id);

    /** 批量更新排序（按 id 列表顺序作为新 sort_order） */
    void updateOrder(Long projectId, List<Long> columnIdsInOrder);
}
