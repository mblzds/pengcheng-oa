package com.pengcheng.admin.controller.ai;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.ai.audit.service.AiExperimentMetricsService;
import com.pengcheng.ai.experiment.AiExperimentAlertService;
import com.pengcheng.ai.experiment.AiExperimentGuardService;
import com.pengcheng.admin.service.ai.AiExperimentAlertDeliveryService;
import com.pengcheng.admin.service.ai.AiExperimentConfigService;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * AI 实验观测控制器
 */
@RestController
@RequestMapping("/admin/ai/experiment")
@RequiredArgsConstructor
public class AiExperimentController {

    private final AiExperimentMetricsService experimentMetricsService;
    private final AiExperimentGuardService experimentGuardService;
    private final AiExperimentAlertService experimentAlertService;
    private final AiExperimentAlertDeliveryService alertDeliveryService;
    private final AiExperimentConfigService experimentConfigService;
    private final SysUserService userService;

    /**
     * 路由实验聚合统计（按日/场景/实验组）
     */
    @GetMapping("/route-stats")
    public Result<AiExperimentMetricsService.RouteExperimentStatsResponse> routeStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String scene) {
        return Result.ok(experimentMetricsService.queryRouteStats(startDate, endDate, scene));
    }

    /**
     * 提示词实验聚合统计（按日/场景/实验组/版本）
     */
    @GetMapping("/prompt-stats")
    public Result<AiExperimentMetricsService.PromptExperimentStatsResponse> promptStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String scene) {
        return Result.ok(experimentMetricsService.queryPromptStats(startDate, endDate, scene));
    }

    /**
     * 实验保护开关状态
     */
    @GetMapping("/status")
    public Result<AiExperimentGuardService.ExperimentGuardStatus> status() {
        return Result.ok(experimentGuardService.currentStatus());
    }

    /**
     * 实验异常告警日志（最近 N 条）
     */
    @GetMapping("/alerts")
    public Result<java.util.List<AiExperimentAlertService.AlertLogItem>> alerts(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Boolean includeSuppressed) {
        return Result.ok(experimentAlertService.listRecentAlerts(limit, includeSuppressed));
    }

    /**
     * 重置告警抑制状态（立即恢复告警触发）
     */
    @PostMapping("/alerts/suppression/reset")
    public Result<AiExperimentAlertService.AlertSuppressionState> resetAlertSuppression() {
        return Result.ok(experimentAlertService.resetSuppressionState());
    }

    /**
     * 告警投递日志（最近 N 条）
     */
    @GetMapping("/alerts/deliveries")
    public Result<java.util.List<AiExperimentAlertDeliveryService.DeliveryLogItem>> deliveries(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status) {
        return Result.ok(alertDeliveryService.listRecentDeliveries(limit, status));
    }

    /**
     * 手动触发告警投递重试
     */
    @PostMapping("/alerts/deliveries/retry")
    public Result<AiExperimentAlertDeliveryService.RetrySummary> retryDeliveries(
            @RequestParam(required = false) Integer limit) {
        return Result.ok(alertDeliveryService.retryDueDeliveries(limit, true));
    }

    /**
     * 死信队列（最近 N 条）
     */
    @GetMapping("/alerts/deliveries/dead")
    public Result<java.util.List<AiExperimentAlertDeliveryService.DeliveryLogItem>> deadDeliveries(
            @RequestParam(required = false) Integer limit) {
        return Result.ok(alertDeliveryService.listDeadLetters(limit));
    }

    /**
     * 重放指定失败/死信投递记录
     */
    @PostMapping("/alerts/deliveries/replay")
    public Result<AiExperimentAlertDeliveryService.DeliveryReplayResult> replayDelivery(
            @RequestParam Long deliveryId) {
        try {
            return Result.ok(alertDeliveryService.replayDelivery(deliveryId));
        } catch (IllegalArgumentException ex) {
            return Result.fail(400, ex.getMessage());
        } catch (Exception ex) {
            return Result.fail("重放投递记录失败: " + ex.getMessage());
        }
    }

    /**
     * 批量重放失败/死信投递记录
     */
    @PostMapping("/alerts/deliveries/replay/batch")
    public Result<AiExperimentAlertDeliveryService.DeliveryBatchReplayResult> replayDeliveriesBatch(
            @RequestBody DeliveryBatchReplayRequest request) {
        List<Long> deliveryIds = request == null ? List.of() : request.deliveryIds();
        return Result.ok(alertDeliveryService.replayDeliveries(deliveryIds));
    }

    /**
     * 关闭死信记录（不再重试）
     */
    @PostMapping("/alerts/deliveries/dead/close")
    public Result<AiExperimentAlertDeliveryService.DeliveryCloseResult> closeDeadDelivery(
            @RequestParam Long deliveryId,
            @RequestParam(required = false) String reason) {
        try {
            return Result.ok(alertDeliveryService.closeDeadLetter(deliveryId, reason));
        } catch (IllegalArgumentException ex) {
            return Result.fail(400, ex.getMessage());
        } catch (Exception ex) {
            return Result.fail("关闭死信失败: " + ex.getMessage());
        }
    }

    /**
     * 批量关闭死信记录（不再重试）
     */
    @PostMapping("/alerts/deliveries/dead/close/batch")
    public Result<AiExperimentAlertDeliveryService.DeliveryBatchCloseResult> closeDeadDeliveriesBatch(
            @RequestBody DeliveryBatchCloseRequest request) {
        List<Long> deliveryIds = request == null ? List.of() : request.deliveryIds();
        String reason = request == null ? null : request.reason();
        return Result.ok(alertDeliveryService.closeDeadLetters(deliveryIds, reason));
    }

    /**
     * 投递汇总（按天）
     */
    @GetMapping("/alerts/deliveries/summary")
    public Result<AiExperimentAlertDeliveryService.DeliverySummary> deliverySummary(
            @RequestParam(required = false) Integer days) {
        return Result.ok(alertDeliveryService.summarizeDeliveries(days));
    }

    /**
     * 投递健康度评估（按天 + 阈值）
     */
    @GetMapping("/alerts/deliveries/health")
    public Result<AiExperimentAlertDeliveryService.DeliveryHealthSummary> deliveryHealth(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Double deadRateThreshold,
            @RequestParam(required = false) Double pendingRateThreshold) {
        return Result.ok(alertDeliveryService.summarizeDeliveryHealth(days, deadRateThreshold, pendingRateThreshold));
    }

    /**
     * 投递健康巡检历史（最近 N 条）
     */
    @GetMapping("/alerts/deliveries/health/logs")
    public Result<List<AiExperimentAlertDeliveryService.DeliveryHealthLogItem>> deliveryHealthLogs(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.ok(alertDeliveryService.listHealthChecks(limit, level, date));
    }

    /**
     * 投递健康巡检趋势（按天窗口）
     */
    @GetMapping("/alerts/deliveries/health/trend")
    public Result<AiExperimentAlertDeliveryService.DeliveryHealthTrendSummary> deliveryHealthTrend(
            @RequestParam(required = false) Integer days) {
        return Result.ok(alertDeliveryService.summarizeHealthTrend(days));
    }

    /**
     * 投递健康巡检按日趋势
     */
    @GetMapping("/alerts/deliveries/health/trend/daily")
    public Result<List<AiExperimentAlertDeliveryService.DeliveryHealthDailyTrendItem>> deliveryHealthDailyTrend(
            @RequestParam(required = false) Integer days) {
        return Result.ok(alertDeliveryService.listHealthDailyTrend(days));
    }

    /**
     * 投递健康高风险日期（TopN）
     */
    @GetMapping("/alerts/deliveries/health/trend/risk-days")
    public Result<List<AiExperimentAlertDeliveryService.DeliveryHealthRiskDayItem>> deliveryHealthRiskDays(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(alertDeliveryService.listHealthRiskDays(days, limit));
    }

    /**
     * 投递健康风险分布汇总
     */
    @GetMapping("/alerts/deliveries/health/trend/risk-summary")
    public Result<AiExperimentAlertDeliveryService.DeliveryHealthRiskSummary> deliveryHealthRiskSummary(
            @RequestParam(required = false) Integer days) {
        return Result.ok(alertDeliveryService.summarizeHealthRiskDays(days));
    }

    /**
     * 投递健康治理建议
     */
    @GetMapping("/alerts/deliveries/health/governance-advice")
    public Result<AiExperimentAlertDeliveryService.DeliveryHealthGovernanceAdvice> deliveryHealthGovernanceAdvice(
            @RequestParam(required = false) Integer days) {
        return Result.ok(alertDeliveryService.generateHealthGovernanceAdvice(days));
    }

    /**
     * 投递健康治理报告（文本）
     */
    @GetMapping("/alerts/deliveries/health/governance-report")
    public Result<AiExperimentAlertDeliveryService.DeliveryHealthGovernanceReport> deliveryHealthGovernanceReport(
            @RequestParam(required = false) Integer days) {
        return Result.ok(alertDeliveryService.generateHealthGovernanceReport(days));
    }

    /**
     * 投递健康阈值建议（基于历史巡检样本）
     */
    @GetMapping("/alerts/deliveries/health/threshold-suggestion")
    public Result<AiExperimentAlertDeliveryService.DeliveryHealthThresholdSuggestion> deliveryHealthThresholdSuggestion(
            @RequestParam(required = false) Integer days) {
        return Result.ok(alertDeliveryService.suggestHealthThresholds(days));
    }

    /**
     * 投递健康巡检日志存储概览（总量/过期量）
     */
    @GetMapping("/alerts/deliveries/health/logs/storage")
    public Result<AiExperimentAlertDeliveryService.DeliveryHealthLogStorageSummary> deliveryHealthLogStorage(
            @RequestParam(required = false) Integer retainDays) {
        return Result.ok(alertDeliveryService.summarizeHealthLogStorage(retainDays));
    }

    /**
     * 清理过期的投递健康巡检日志
     */
    @PostMapping("/alerts/deliveries/health/logs/cleanup")
    public Result<AiExperimentAlertDeliveryService.DeliveryHealthLogCleanupResult> cleanupDeliveryHealthLogs(
            @RequestParam(required = false) Integer retainDays,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(alertDeliveryService.cleanupHealthLogs(retainDays, limit, true));
    }

    /**
     * 手动执行投递健康度巡检并按需升级告警
     */
    @PostMapping("/alerts/deliveries/health/check")
    public Result<AiExperimentAlertDeliveryService.DeliveryHealthCheckResult> checkDeliveryHealth(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Double deadRateThreshold,
            @RequestParam(required = false) Double pendingRateThreshold) {
        return Result.ok(alertDeliveryService.checkAndEscalateDeliveryHealth(
                days, deadRateThreshold, pendingRateThreshold, true
        ));
    }

    /**
     * 实验配置快照（当前运行参数）
     */
    @GetMapping("/config")
    public Result<AiExperimentConfigService.ExperimentConfigSnapshot> config() {
        return Result.ok(experimentConfigService.currentSnapshot());
    }

    /**
     * 更新实验配置（写入系统配置中心并即时生效）
     */
    @PostMapping("/config")
    public Result<AiExperimentConfigService.ExperimentConfigSnapshot> updateConfig(
            @RequestBody AiExperimentConfigService.ExperimentConfigUpdateRequest request) {
        try {
            return Result.ok(experimentConfigService.updateAndPersist(request, currentOperator()));
        } catch (IllegalArgumentException ex) {
            return Result.fail(400, ex.getMessage());
        } catch (Exception ex) {
            return Result.fail("更新实验配置失败: " + ex.getMessage());
        }
    }

    /**
     * 从系统配置中心重载实验配置（不中断服务）
     */
    @PostMapping("/config/reload")
    public Result<AiExperimentConfigService.ExperimentConfigSnapshot> reloadConfig() {
        return Result.ok(experimentConfigService.reloadFromSystemConfig());
    }

    /**
     * 配置变更审计列表（最近 N 条）
     */
    @GetMapping("/config/audits")
    public Result<java.util.List<AiExperimentConfigService.ConfigAuditItem>> configAudits(
            @RequestParam(required = false) Integer limit) {
        return Result.ok(experimentConfigService.listRecentAudits(limit));
    }

    /**
     * 回滚到指定审计版本
     */
    @PostMapping("/config/rollback")
    public Result<AiExperimentConfigService.ExperimentConfigSnapshot> rollbackConfig(
            @RequestParam Long auditId) {
        try {
            return Result.ok(experimentConfigService.rollbackTo(auditId, currentOperator()));
        } catch (IllegalArgumentException ex) {
            return Result.fail(400, ex.getMessage());
        } catch (Exception ex) {
            return Result.fail("回滚实验配置失败: " + ex.getMessage());
        }
    }

    /**
     * 手动重置实验保护状态（清空失败计数+解除冷却）
     */
    @PostMapping("/guard/reset")
    public Result<AiExperimentGuardService.ExperimentGuardStatus> resetGuard(
            @RequestParam(defaultValue = "all") String type) {
        String normalized = normalizeType(type);
        if (normalized == null) {
            return Result.fail("type 仅支持 route/prompt/all");
        }
        switch (normalized) {
            case "route" -> experimentGuardService.resetRouteGuard();
            case "prompt" -> experimentGuardService.resetPromptGuard();
            default -> experimentGuardService.resetAll();
        }
        return Result.ok(experimentGuardService.currentStatus());
    }

    /**
     * 手动封禁实验分支（用于线上止血）
     */
    @PostMapping("/guard/block")
    public Result<AiExperimentGuardService.ExperimentGuardStatus> blockGuard(
            @RequestParam String type,
            @RequestParam(required = false) Integer cooldownSeconds) {
        String normalized = normalizeType(type);
        if (normalized == null || "all".equals(normalized)) {
            return Result.fail("type 仅支持 route/prompt");
        }
        switch (normalized) {
            case "route" -> experimentGuardService.blockRouteExperiment(cooldownSeconds);
            case "prompt" -> experimentGuardService.blockPromptExperiment(cooldownSeconds);
            default -> {
                return Result.fail("type 仅支持 route/prompt");
            }
        }
        return Result.ok(experimentGuardService.currentStatus());
    }

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if ("route".equals(normalized) || "prompt".equals(normalized) || "all".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private AiExperimentConfigService.OperatorInfo currentOperator() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            SysUser user = userService.getById(userId);
            String operatorName = null;
            if (user != null) {
                operatorName = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
            }
            if (!StringUtils.hasText(operatorName)) {
                operatorName = String.valueOf(userId);
            }
            return new AiExperimentConfigService.OperatorInfo(userId, operatorName);
        } catch (Exception ex) {
            return new AiExperimentConfigService.OperatorInfo(0L, "system");
        }
    }

    public record DeliveryBatchReplayRequest(List<Long> deliveryIds) {}

    public record DeliveryBatchCloseRequest(List<Long> deliveryIds, String reason) {}
}
