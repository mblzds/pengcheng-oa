package com.pengcheng.system.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.project.entity.PmProjectStatusColumn;
import com.pengcheng.system.project.entity.PmTask;
import com.pengcheng.system.project.entity.PmTaskDependency;
import com.pengcheng.system.project.mapper.PmTaskDependencyMapper;
import com.pengcheng.system.project.mapper.PmTaskMapper;
import com.pengcheng.system.project.service.PmProjectStatusColumnService;
import com.pengcheng.system.project.service.PmTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PmTaskServiceImpl implements PmTaskService {

    private final PmTaskMapper taskMapper;
    private final PmTaskDependencyMapper dependencyMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PmProjectStatusColumnService statusColumnService;

    @Override
    public PmTask getById(Long taskId) {
        return taskMapper.selectById(taskId);
    }

    @Override
    public IPage<PmTask> page(Page<PmTask> page, Long projectId, Long parentId, Long assigneeId, String status, Integer priority) {
        LambdaQueryWrapper<PmTask> w = new LambdaQueryWrapper<PmTask>().eq(PmTask::getProjectId, projectId);
        if (parentId != null) w.eq(PmTask::getParentId, parentId);
        if (assigneeId != null) w.eq(PmTask::getAssigneeId, assigneeId);
        if (status != null && !status.isEmpty()) w.eq(PmTask::getStatus, status);
        if (priority != null) w.eq(PmTask::getPriority, priority);
        w.orderByAsc(PmTask::getSortOrder).orderByAsc(PmTask::getId);
        return taskMapper.selectPage(page, w);
    }

    @Override
    public List<PmTask> listTree(Long projectId) {
        List<PmTask> all = taskMapper.selectList(
                new LambdaQueryWrapper<PmTask>().eq(PmTask::getProjectId, projectId).orderByAsc(PmTask::getSortOrder));
        return buildTree(all, 0L);
    }

    private List<PmTask> buildTree(List<PmTask> all, Long parentId) {
        return all.stream()
                .filter(t -> Objects.equals(t.getParentId() != null ? t.getParentId() : 0L, parentId))
                .peek(t -> t.setChildren(buildTree(all, t.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PmTask task, Long currentUserId) {
        if (task.getParentId() == null) task.setParentId(0L);
        if (task.getStatus() == null || task.getStatus().isEmpty()) task.setStatus("待办");
        if (task.getPriority() == null) task.setPriority(1);
        if (task.getProgress() == null) task.setProgress(0);
        task.setCreateBy(currentUserId);
        taskMapper.insert(task);
        syncTaskToTodo(task);
        return task.getId();
    }

    @Override
    public void update(PmTask task) {
        taskMapper.updateById(task);
    }

    @Override
    public void delete(Long taskId) {
        taskMapper.deleteById(taskId);
    }

    @Override
    public void updateStatus(Long taskId, String status) {
        PmTask t = taskMapper.selectById(taskId);
        if (t != null) { t.setStatus(status); taskMapper.updateById(t); }
    }

    @Override
    public void updateAssignee(Long taskId, Long assigneeId) {
        PmTask t = taskMapper.selectById(taskId);
        if (t != null) {
            t.setAssigneeId(assigneeId);
            taskMapper.updateById(t);
            syncTaskToTodo(t);
        }
    }

    private void syncTaskToTodo(PmTask task) {
        if (task.getAssigneeId() == null) return;
        try {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_todo WHERE source_type = 'project_task' AND source_id = ? AND user_id = ? AND status IN (0, 1)",
                    Integer.class, task.getId(), task.getAssigneeId());
            if (exists != null && exists > 0) return;
            jdbcTemplate.update(
                    "INSERT INTO sys_todo (user_id, title, description, source_type, source_id, priority, status, due_date) VALUES (?, ?, ?, 'project_task', ?, ?, 0, ?)",
                    task.getAssigneeId(),
                    "[项目任务] " + task.getTitle(),
                    "来自项目管理的指派任务",
                    task.getId(),
                    task.getPriority() != null ? task.getPriority() : 0,
                    task.getDueDate());
            log.info("[PmTask] 任务 {} 已同步到用户 {} 的待办", task.getId(), task.getAssigneeId());
        } catch (Exception e) {
            log.warn("[PmTask] 同步待办失败: {}", e.getMessage());
        }
    }

    @Override
    public void updateProgress(Long taskId, Integer progress) {
        PmTask t = taskMapper.selectById(taskId);
        if (t != null) { t.setProgress(progress); taskMapper.updateById(t); }
    }

    @Override
    public List<PmTaskDependency> listDependencies(Long taskId) {
        List<PmTaskDependency> out = dependencyMapper.selectList(
                new LambdaQueryWrapper<PmTaskDependency>().eq(PmTaskDependency::getTaskId, taskId));
        List<PmTaskDependency> in = dependencyMapper.selectList(
                new LambdaQueryWrapper<PmTaskDependency>().eq(PmTaskDependency::getDependsOnTaskId, taskId));
        out.addAll(in);
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addDependency(Long taskId, Long dependsOnTaskId, String type) {
        if (taskId.equals(dependsOnTaskId)) throw new IllegalArgumentException("不能依赖自身");
        if (hasCycle(taskId, dependsOnTaskId)) throw new IllegalArgumentException("存在循环依赖");
        PmTaskDependency d = new PmTaskDependency();
        d.setTaskId(taskId);
        d.setDependsOnTaskId(dependsOnTaskId);
        d.setType(type != null ? type : "fs");
        dependencyMapper.insert(d);
        return d.getId();
    }

    private boolean hasCycle(Long taskId, Long dependsOnTaskId) {
        Set<Long> visited = new HashSet<>();
        return dfsCycle(dependsOnTaskId, taskId, visited);
    }

    private boolean dfsCycle(Long current, Long target, Set<Long> visited) {
        if (current.equals(target)) return true;
        if (visited.contains(current)) return false;
        visited.add(current);
        List<PmTaskDependency> deps = dependencyMapper.selectList(
                new LambdaQueryWrapper<PmTaskDependency>().eq(PmTaskDependency::getTaskId, current));
        for (PmTaskDependency d : deps) {
            if (dfsCycle(d.getDependsOnTaskId(), target, visited)) return true;
        }
        return false;
    }

    @Override
    public void removeDependency(Long depId) {
        dependencyMapper.deleteById(depId);
    }

    @Override
    public Map<String, List<PmTask>> getBoardData(Long projectId) {
        List<PmTask> all = taskMapper.selectList(
                new LambdaQueryWrapper<PmTask>().eq(PmTask::getProjectId, projectId).eq(PmTask::getParentId, 0));
        Map<String, List<PmTask>> byStatus = all.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus() != null ? t.getStatus() : "待办"));

        List<PmProjectStatusColumn> columns = statusColumnService.listByProjectId(projectId);
        if (!columns.isEmpty()) {
            LinkedHashMap<String, List<PmTask>> ordered = new LinkedHashMap<>();
            for (PmProjectStatusColumn col : columns) {
                String sv = col.getStatusValue() != null ? col.getStatusValue() : "";
                ordered.put(sv, byStatus.getOrDefault(sv, List.of()));
            }
            return ordered;
        }
        // 默认三列顺序
        return Stream.of("待办", "进行中", "已完成")
                .collect(Collectors.toMap(s -> s, s -> byStatus.getOrDefault(s, List.of()), (a, b) -> a, LinkedHashMap::new));
    }

    @Override
    public List<Map<String, Object>> getGanttData(Long projectId) {
        List<PmTask> list = taskMapper.selectList(
                new LambdaQueryWrapper<PmTask>().eq(PmTask::getProjectId, projectId));
        return list.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("startDate", t.getStartDate());
            m.put("dueDate", t.getDueDate());
            m.put("progress", t.getProgress());
            m.put("assigneeId", t.getAssigneeId());
            m.put("status", t.getStatus());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getCalendarData(Long projectId) {
        List<PmTask> list = taskMapper.selectList(
                new LambdaQueryWrapper<PmTask>().eq(PmTask::getProjectId, projectId));
        return list.stream()
                .filter(t -> t.getDueDate() != null)
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", "task-" + t.getId());
                    m.put("type", "task");
                    m.put("title", t.getTitle());
                    m.put("dueDate", t.getDueDate());
                    m.put("taskId", t.getId());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getMyTasks(Long userId, int limit) {
        String sql = """
            SELECT t.id, t.title, t.status, t.priority, t.due_date AS dueDate,
                   t.project_id AS projectId, p.name AS projectName
            FROM pm_task t LEFT JOIN pm_project p ON t.project_id = p.id
            WHERE t.deleted = 0 AND t.assignee_id = ? AND t.status != 'done'
            ORDER BY FIELD(t.priority, 2, 1, 0) DESC, t.due_date ASC
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, userId, limit);
    }
}
