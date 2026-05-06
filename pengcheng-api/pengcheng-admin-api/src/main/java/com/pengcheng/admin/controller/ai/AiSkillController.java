package com.pengcheng.admin.controller.ai;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.ai.orchestration.SkillEnableRegistry;
import com.pengcheng.ai.orchestration.tool.AiAgentTool;
import com.pengcheng.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Skill / Tool 管理接口
 * 查看已注册的 Agent 工具，支持启用/禁用；禁用后编排层不会调用该 Skill。
 */
@RestController
@RequestMapping("/ai/skills")
@RequiredArgsConstructor
public class AiSkillController {

    private final List<AiAgentTool> tools;
    private final SkillEnableRegistry skillEnableRegistry;

    /** 列出所有已注册的 Agent 工具 */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listSkills() {
        List<Map<String, Object>> skills = tools.stream().map(tool -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", tool.toolName());
            info.put("intent", tool.supportedIntent().name());
            info.put("className", tool.getClass().getSimpleName());
            info.put("enabled", !skillEnableRegistry.isDisabled(tool.toolName()));
            return info;
        }).collect(Collectors.toList());
        return Result.ok(skills);
    }

    /** 启用工具 */
    @PostMapping("/enable/{name}")
    @SaCheckPermission("ai:skill:toggle")
    public Result<Void> enableSkill(@PathVariable String name) {
        skillEnableRegistry.enable(name);
        return Result.ok();
    }

    /** 禁用工具 */
    @PostMapping("/disable/{name}")
    @SaCheckPermission("ai:skill:toggle")
    public Result<Void> disableSkill(@PathVariable String name) {
        skillEnableRegistry.disable(name);
        return Result.ok();
    }

    /** 获取已禁用的工具列表 */
    @GetMapping("/disabled")
    public Result<Set<String>> getDisabledSkills() {
        return Result.ok(skillEnableRegistry.getDisabledToolNames());
    }

    /** 获取统计信息 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Long> intentCounts = tools.stream()
                .collect(Collectors.groupingBy(t -> t.supportedIntent().name(), Collectors.counting()));

        int disabledCount = (int) tools.stream()
                .filter(t -> skillEnableRegistry.isDisabled(t.toolName()))
                .count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTools", tools.size());
        stats.put("enabledCount", tools.size() - disabledCount);
        stats.put("disabledCount", disabledCount);
        stats.put("intentDistribution", intentCounts);
        return Result.ok(stats);
    }
}
