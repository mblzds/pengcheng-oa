package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.automation.entity.AutomationLog;
import com.pengcheng.system.automation.entity.AutomationRule;
import com.pengcheng.system.automation.service.AutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自动化规则管理接口
 */
@RestController
@RequestMapping("/automation")
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationService automationService;

    @GetMapping("/rules")
    @SaCheckPermission("system:automation:list")
    public Result<List<AutomationRule>> listRules() {
        return Result.ok(automationService.getAllRules());
    }

    @PostMapping("/rule")
    @SaCheckPermission("system:automation:add")
    public Result<AutomationRule> createRule(@RequestBody AutomationRule rule) {
        rule.setCreatedBy(StpUtil.getLoginIdAsLong());
        return Result.ok(automationService.createRule(rule));
    }

    @PutMapping("/rule")
    @SaCheckPermission("system:automation:edit")
    public Result<Void> updateRule(@RequestBody AutomationRule rule) {
        automationService.updateRule(rule);
        return Result.ok();
    }

    @PostMapping("/rule/{id}/toggle")
    @SaCheckPermission("system:automation:edit")
    public Result<Void> toggleRule(@PathVariable Long id, @RequestParam boolean enabled) {
        automationService.toggleRule(id, enabled);
        return Result.ok();
    }

    @DeleteMapping("/rule/{id}")
    @SaCheckPermission("system:automation:delete")
    public Result<Void> deleteRule(@PathVariable Long id) {
        automationService.deleteRule(id);
        return Result.ok();
    }

    @GetMapping("/rule/{id}/logs")
    @SaCheckPermission("system:automation:list")
    public Result<List<AutomationLog>> getRuleLogs(@PathVariable Long id, @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(automationService.getRuleLogs(id, limit));
    }

    /**
     * 手动触发规则执行（调试用）
     */
    @PostMapping("/execute")
    @SaCheckPermission("system:automation:execute")
    public Result<Void> manualExecute() {
        automationService.executeTimeBasedRules();
        return Result.ok();
    }
}
