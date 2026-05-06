package com.pengcheng.system.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.project.entity.PmTask;
import com.pengcheng.system.project.entity.PmTaskDependency;

import java.util.List;
import java.util.Map;

/**
 * 任务服务
 */
public interface PmTaskService {

    PmTask getById(Long taskId);

    IPage<PmTask> page(Page<PmTask> page, Long projectId, Long parentId, Long assigneeId, String status, Integer priority);

    List<PmTask> listTree(Long projectId);

    Long create(PmTask task, Long currentUserId);

    void update(PmTask task);

    void delete(Long taskId);

    void updateStatus(Long taskId, String status);

    void updateAssignee(Long taskId, Long assigneeId);

    void updateProgress(Long taskId, Integer progress);

    List<PmTaskDependency> listDependencies(Long taskId);

    Long addDependency(Long taskId, Long dependsOnTaskId, String type);

    void removeDependency(Long depId);

    /** 看板数据：按状态分列 */
    Map<String, List<PmTask>> getBoardData(Long projectId);

    /** 甘特/日历用：任务列表含起止日期 */
    List<Map<String, Object>> getGanttData(Long projectId);

    List<Map<String, Object>> getCalendarData(Long projectId);

    /** 获取指定用户被指派的任务（跨项目） */
    List<Map<String, Object>> getMyTasks(Long userId, int limit);
}
