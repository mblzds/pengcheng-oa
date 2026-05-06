package com.pengcheng.admin.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.job.entity.SysJob;
import com.pengcheng.job.entity.SysJobLog;
import com.pengcheng.job.service.SysJobLogService;
import com.pengcheng.job.service.SysJobService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务控制器
 */
@RestController
@RequestMapping("/monitor/job")
@RequiredArgsConstructor
public class SysJobController {

    private final SysJobService jobService;
    private final SysJobLogService jobLogService;

    /**
     * 分页查询任务
     */
    @GetMapping("/page")
    @SaCheckPermission("monitor:job:list")
    public Result<PageResult<SysJob>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String jobGroup,
            @RequestParam(required = false) Integer status) {
        return Result.ok(jobService.page(page, pageSize, jobName, jobGroup, status));
    }

    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("monitor:job:list")
    public Result<SysJob> detail(@PathVariable Long id) {
        return Result.ok(jobService.getById(id));
    }

    /**
     * 创建
     */
    @PostMapping
    @SaCheckPermission("monitor:job:add")
    public Result<Void> create(@RequestBody SysJob job) {
        jobService.create(job);
        return Result.ok();
    }

    /**
     * 更新
     */
    @PutMapping
    @SaCheckPermission("monitor:job:edit")
    public Result<Void> update(@RequestBody SysJob job) {
        jobService.update(job);
        return Result.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("monitor:job:delete")
    public Result<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return Result.ok();
    }

    /**
     * 修改状态
     */
    @PutMapping("/changeStatus")
    @SaCheckPermission("monitor:job:edit")
    public Result<Void> changeStatus(@RequestBody StatusRequest request) {
        jobService.changeStatus(request.getId(), request.getStatus());
        return Result.ok();
    }

    /**
     * 立即执行
     */
    @PostMapping("/run/{id}")
    @SaCheckPermission("monitor:job:edit")
    public Result<Void> run(@PathVariable Long id) {
        jobService.run(id);
        return Result.ok();
    }

    /**
     * 分页查询任务日志
     */
    @GetMapping("/log/page")
    @SaCheckPermission("monitor:job:list")
    public Result<PageResult<SysJobLog>> logPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String jobGroup,
            @RequestParam(required = false) Integer status) {
        return Result.ok(jobLogService.page(page, pageSize, jobName, jobGroup, status));
    }

    /**
     * 清空日志
     */
    @DeleteMapping("/log/clean")
    @SaCheckPermission("monitor:job:delete")
    public Result<Void> cleanLog() {
        jobLogService.clean();
        return Result.ok();
    }

    @Data
    public static class StatusRequest {
        private Long id;
        private Integer status;
    }
}
