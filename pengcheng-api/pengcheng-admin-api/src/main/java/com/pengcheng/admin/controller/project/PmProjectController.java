package com.pengcheng.admin.controller.project;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.project.entity.PmProject;
import com.pengcheng.system.project.entity.PmProjectMember;
import com.pengcheng.system.project.entity.PmProjectStatusColumn;
import com.pengcheng.system.project.service.PmProjectService;
import com.pengcheng.system.project.service.PmProjectStatusColumnService;
import com.pengcheng.system.project.service.PmTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目管理：项目 CRUD、成员、统计
 */
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class PmProjectController {

    private final PmProjectService projectService;
    private final PmTaskService taskService;
    private final PmProjectStatusColumnService statusColumnService;

    @GetMapping("/list")
    public Result<IPage<PmProject>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Integer status) {
        Long userId = null;
        try { userId = StpUtil.getLoginIdAsLong(); } catch (Exception ignored) {}
        return Result.ok(projectService.page(new Page<>(page, size), userId, scope, status));
    }

    /** 获取当前用户被指派的任务（跨项目，供工作台使用）。必须放在 /{id} 之前，避免 "my-tasks" 被当作 id 解析 */
    @GetMapping("/my-tasks")
    public Result<List<Map<String, Object>>> myTasks(@RequestParam(defaultValue = "5") int limit) {
        Long userId = null;
        try { userId = StpUtil.getLoginIdAsLong(); } catch (Exception ignored) {}
        if (userId == null) return Result.ok(List.of());
        return Result.ok(taskService.getMyTasks(userId, limit));
    }

    @GetMapping("/{id}")
    public Result<PmProject> get(@PathVariable Long id) {
        return Result.ok(projectService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody PmProject project) {
        Long userId = null;
        try { userId = StpUtil.getLoginIdAsLong(); } catch (Exception ignored) {}
        return Result.ok(projectService.create(project, userId));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody PmProject project) {
        project.setId(id);
        projectService.update(project);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/members")
    public Result<List<PmProjectMember>> members(@PathVariable Long id) {
        return Result.ok(projectService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    public Result<Void> addMember(@PathVariable Long id, @RequestParam Long userId, @RequestParam(defaultValue = "member") String role) {
        projectService.addMember(id, userId, role);
        return Result.ok();
    }

    @PutMapping("/{id}/members/{userId}")
    public Result<Void> updateMemberRole(@PathVariable Long id, @PathVariable Long userId, @RequestParam String role) {
        projectService.updateMemberRole(id, userId, role);
        return Result.ok();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return Result.ok();
    }

    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        return Result.ok(projectService.getStats(id));
    }

    // ---------- V24 看板状态列配置 ----------

    @GetMapping("/{id}/status-columns")
    public Result<List<PmProjectStatusColumn>> listStatusColumns(@PathVariable Long id) {
        return Result.ok(statusColumnService.listByProjectId(id));
    }

    @PostMapping("/{id}/status-columns")
    public Result<Long> createStatusColumn(@PathVariable Long id, @RequestBody PmProjectStatusColumn column) {
        column.setProjectId(id);
        return Result.ok(statusColumnService.create(column));
    }

    @PutMapping("/status-columns/{columnId}")
    public Result<Void> updateStatusColumn(@PathVariable Long columnId, @RequestBody PmProjectStatusColumn column) {
        column.setId(columnId);
        statusColumnService.update(column);
        return Result.ok();
    }

    @DeleteMapping("/status-columns/{columnId}")
    public Result<Void> deleteStatusColumn(@PathVariable Long columnId) {
        statusColumnService.delete(columnId);
        return Result.ok();
    }

    @PutMapping("/{id}/status-columns/order")
    public Result<Void> updateStatusColumnsOrder(@PathVariable Long id, @RequestBody List<Long> columnIdsInOrder) {
        statusColumnService.updateOrder(id, columnIdsInOrder);
        return Result.ok();
    }
}
