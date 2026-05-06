package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.channel.entity.ChannelConfig;
import com.pengcheng.system.channel.entity.ChannelPushLog;
import com.pengcheng.system.channel.service.ChannelPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 多渠道推送管理接口
 */
@RestController
@RequestMapping("/channel")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelPushService channelPushService;

    @GetMapping("/list")
    public Result<List<ChannelConfig>> getAllChannels() {
        return Result.ok(channelPushService.getAllChannels());
    }

    @PostMapping("/save")
    public Result<Void> saveChannel(@RequestBody ChannelConfig config) {
        if (config.getCreatedBy() == null) {
            config.setCreatedBy(StpUtil.getLoginIdAsLong());
        }
        channelPushService.saveChannel(config);
        return Result.ok();
    }

    @PostMapping("/toggle/{id}")
    public Result<Void> toggleChannel(@PathVariable Long id, @RequestParam boolean enabled) {
        channelPushService.toggleChannel(id, enabled);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteChannel(@PathVariable Long id) {
        channelPushService.deleteChannel(id);
        return Result.ok();
    }

    @PostMapping("/test/{id}")
    public Result<Map<String, Object>> testChannel(@PathVariable Long id) {
        boolean success = channelPushService.testChannel(id);
        return Result.ok(Map.of("success", success));
    }

    @PostMapping("/broadcast")
    public Result<Void> broadcast(@RequestParam String title, @RequestParam String content,
                             @RequestParam(defaultValue = "system") String type) {
        channelPushService.broadcast(title, content, type);
        return Result.ok();
    }

    @GetMapping("/logs")
    public Result<List<ChannelPushLog>> getRecentLogs(@RequestParam(defaultValue = "50") int limit) {
        return Result.ok(channelPushService.getRecentLogs(limit));
    }
}
