package com.pengcheng.admin.controller.project;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.project.entity.PmTask;
import com.pengcheng.system.project.entity.PmTaskDependency;
import com.pengcheng.system.project.service.PmTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目任务：CRUD、状态/指派人/进度、依赖、看板/甘特/日历数据
 */
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class PmTaskController {

    private final PmTaskService taskService;

    @GetMapping("/{projectId}/tasks")
    public Result<IPage<PmTask>> taskPage(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer priority) {
        return Result.ok(taskService.page(new Page<>(page, size), projectId, parentId, assigneeId, status, priority));
    }

    @GetMapping("/{projectId}/tasks/tree")
    public Result<List<PmTask>> taskTree(@PathVariable Long projectId) {
        return Result.ok(taskService.listTree(projectId));
    }

    @GetMapping("/task/{taskId}")
    public Result<PmTask> getTask(@PathVariable Long taskId) {
        return Result.ok(taskService.getById(taskId));
    }

    @PostMapping("/{projectId}/tasks")
    public Result<Long> createTask(@PathVariable Long projectId, @RequestBody PmTask task) {
        task.setProjectId(projectId);
        Long userId = null;
        try { userId = StpUtil.getLoginIdAsLong(); } catch (Exception ignored) {}
        return Result.ok(taskService.create(task, userId));
    }

    @PutMapping("/task/{taskId}")
    public Result<Void> updateTask(@PathVariable Long taskId, @RequestBody PmTask task) {
        task.setId(taskId);
        taskService.update(task);
        return Result.ok();
    }

    @DeleteMapping("/task/{taskId}")
    public Result<Void> deleteTask(@PathVariable Long taskId) {
        taskService.delete(taskId);
        return Result.ok();
    }

    @PutMapping("/task/{taskId}/status")
    public Result<Void> updateTaskStatus(@PathVariable Long taskId, @RequestParam String status) {
        taskService.updateStatus(taskId, status);
        return Result.ok();
    }

    @PutMapping("/task/{taskId}/assignee")
    public Result<Void> updateTaskAssignee(@PathVariable Long taskId, @RequestParam Long assigneeId) {
        taskService.updateAssignee(taskId, assigneeId);
        return Result.ok();
    }

    @PutMapping("/task/{taskId}/progress")
    public Result<Void> updateTaskProgress(@PathVariable Long taskId, @RequestParam Integer progress) {
        taskService.updateProgress(taskId, progress);
        return Result.ok();
    }

    @GetMapping("/task/{taskId}/dependencies")
    public Result<List<PmTaskDependency>> listDependencies(@PathVariable Long taskId) {
        return Result.ok(taskService.listDependencies(taskId));
    }

    @PostMapping("/task/{taskId}/dependencies")
    public Result<Long> addDependency(@PathVariable Long taskId, @RequestParam Long dependsOnTaskId, @RequestParam(required = false) String type) {
        return Result.ok(taskService.addDependency(taskId, dependsOnTaskId, type));
    }

    @DeleteMapping("/task/{taskId}/dependencies/{depId}")
    public Result<Void> removeDependency(@PathVariable Long taskId, @PathVariable Long depId) {
        taskService.removeDependency(depId);
        return Result.ok();
    }

    @GetMapping("/{projectId}/board")
    public Result<Map<String, List<PmTask>>> board(@PathVariable Long projectId) {
        return Result.ok(taskService.getBoardData(projectId));
    }

    @GetMapping("/{projectId}/gantt")
    public Result<List<Map<String, Object>>> gantt(@PathVariable Long projectId) {
        return Result.ok(taskService.getGanttData(projectId));
    }

    @GetMapping("/{projectId}/calendar")
    public Result<List<Map<String, Object>>> calendar(@PathVariable Long projectId) {
        return Result.ok(taskService.getCalendarData(projectId));
    }
}
