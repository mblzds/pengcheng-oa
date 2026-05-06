package com.pengcheng.admin.controller.monitor;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.heartbeat.entity.HeartbeatLog;
import com.pengcheng.system.heartbeat.service.HeartbeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 巡检告警管理
 */
@RestController
@RequestMapping("/sys/heartbeat")
@RequiredArgsConstructor
public class HeartbeatController {

    private final HeartbeatService heartbeatService;

    /** 获取当前用户的告警列表 */
    @GetMapping("/list")
    public Result<Page<HeartbeatLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean handled) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(heartbeatService.getUserAlerts(userId, severity, handled, page, pageSize));
    }

    /** 获取告警统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(heartbeatService.getAlertStats(userId));
    }

    /** 标记告警为已处理 */
    @PostMapping("/handle/{id}")
    public Result<Void> handle(@PathVariable Long id) {
        heartbeatService.markHandled(id);
        return Result.ok();
    }

    /** 批量标记已处理 */
    @PostMapping("/batch-handle")
    public Result<Void> batchHandle(@RequestBody List<Long> ids) {
        heartbeatService.batchMarkHandled(ids);
        return Result.ok();
    }

    /** 手动触发巡检（仅管理员） */
    @PostMapping("/run")
    public Result<Integer> run() {
        int count = heartbeatService.runFullCheck();
        return Result.ok(count);
    }
}
