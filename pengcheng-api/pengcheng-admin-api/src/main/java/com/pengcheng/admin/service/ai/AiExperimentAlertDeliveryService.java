package com.pengcheng.admin.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.admin.entity.ai.AiExperimentAlertDeliveryHealthLog;
import com.pengcheng.admin.entity.ai.AiExperimentAlertDeliveryLog;
import com.pengcheng.admin.mapper.ai.AiExperimentAlertDeliveryHealthLogMapper;
import com.pengcheng.admin.websocket.MessageWebSocketHandler;
import com.pengcheng.admin.mapper.ai.AiExperimentAlertDeliveryLogMapper;
import com.pengcheng.mail.EmailService;
import com.pengcheng.message.entity.SysNotice;
import com.pengcheng.message.entity.SysUserNotice;
import com.pengcheng.message.mapper.SysNoticeMapper;
import com.pengcheng.message.mapper.SysUserNoticeMapper;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;

/**
 * AI 实验告警投递服务（落库 + 重试）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiExperimentAlertDeliveryService {

    public static final int HEALTH_LOG_RETENTION_DAYS_DEFAULT = 90;
    public static final int HEALTH_LOG_CLEANUP_BATCH_DEFAULT = 500;
    private static final HttpClient WEBHOOK_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final String CHANNEL_EMAIL = "email";
    private static final String CHANNEL_WEBHOOK = "webhook";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_DEAD = "dead";
    private static final String STATUS_CLOSED = "closed";
    private static final String HEALTH_LEVEL_HEALTHY = "HEALTHY";
    private static final String HEALTH_LEVEL_WARNING = "WARNING";
    private static final String HEALTH_LEVEL_CRITICAL = "CRITICAL";

    private final AiExperimentAlertDeliveryHealthLogMapper healthLogMapper;
    private final AiExperimentAlertDeliveryLogMapper deliveryLogMapper;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final SysNoticeMapper noticeMapper;
    private final SysUserMapper userMapper;
    private final SysUserNoticeMapper userNoticeMapper;
    private final MessageWebSocketHandler webSocketHandler;
    private volatile long lastHealthEscalationEpochMs = 0L;
    private volatile String lastObservedHealthLevel = HEALTH_LEVEL_HEALTHY;

    public void dispatchEmail(Long alertLogId,
                              String alertType,
                              String alertLevel,
                              String recipient,
                              String subject,
                              String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subject", subject);
        payload.put("content", content);
        AiExperimentAlertDeliveryLog log = createPendingLog(alertLogId, alertType, alertLevel, CHANNEL_EMAIL, recipient, toJson(payload), null);
        executeDelivery(log);
    }

    public void dispatchWebhook(Long alertLogId,
                                String alertType,
                                String alertLevel,
                                String url,
                                String requestBody,
                                int timeoutSeconds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestBody", requestBody);
        payload.put("timeoutSeconds", timeoutSeconds);
        AiExperimentAlertDeliveryLog log = createPendingLog(alertLogId, alertType, alertLevel, CHANNEL_WEBHOOK, url, toJson(payload), null);
        executeDelivery(log);
    }

    public RetrySummary retryDueDeliveries(Integer limit, boolean manualTrigger) {
        int safeLimit = normalizeRetryBatchSize(limit);
        if (!manualTrigger && !aiProperties.isExperimentAlertDeliveryRetryEnabled()) {
            return new RetrySummary(0, 0, 0, 0);
        }
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<AiExperimentAlertDeliveryLog> wrapper = new QueryWrapper<>();
        wrapper.eq("status", STATUS_PENDING)
                .apply("attempt_count < max_attempts");
        if (!manualTrigger) {
            wrapper.le("next_retry_time", now);
        }
        wrapper.orderByAsc("next_retry_time")
                .orderByAsc("id")
                .last("limit " + safeLimit);
        List<AiExperimentAlertDeliveryLog> candidates = deliveryLogMapper.selectList(wrapper);
        int success = 0;
        int dead = 0;
        int pending = 0;
        for (AiExperimentAlertDeliveryLog candidate : candidates) {
            String status = executeDelivery(candidate);
            if (STATUS_SUCCESS.equals(status)) {
                success++;
            } else if (STATUS_DEAD.equals(status)) {
                dead++;
            } else {
                pending++;
            }
        }
        return new RetrySummary(candidates.size(), success, dead, pending);
    }

    public List<DeliveryLogItem> listRecentDeliveries(Integer limit, String status) {
        int safeLimit = normalizeListLimit(limit);
        QueryWrapper<AiExperimentAlertDeliveryLog> wrapper = new QueryWrapper<>();
        String normalizedStatus = normalizeStatus(status);
        if (normalizedStatus != null) {
            wrapper.eq("status", normalizedStatus);
        }
        wrapper.orderByDesc("id")
                .last("limit " + safeLimit);
        return deliveryLogMapper.selectList(wrapper).stream()
                .map(this::toDeliveryLogItem)
                .toList();
    }

    public List<DeliveryLogItem> listDeadLetters(Integer limit) {
        int safeLimit = normalizeListLimit(limit);
        QueryWrapper<AiExperimentAlertDeliveryLog> wrapper = new QueryWrapper<>();
        wrapper.eq("status", STATUS_DEAD)
                .orderByDesc("id")
                .last("limit " + safeLimit);
        return deliveryLogMapper.selectList(wrapper).stream()
                .map(this::toDeliveryLogItem)
                .toList();
    }

    public DeliverySummary summarizeDeliveries(Integer days) {
        int safeDays = normalizeDays(days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(safeDays);
        long total = countByStatus(null, since);
        long success = countByStatus(STATUS_SUCCESS, since);
        long pending = countByStatus(STATUS_PENDING, since);
        long dead = countByStatus(STATUS_DEAD, since);
        long closed = countByStatus(STATUS_CLOSED, since);
        long failed = countByStatus(STATUS_FAILED, since);
        return new DeliverySummary(
                safeDays,
                since,
                now,
                total,
                success,
                pending,
                dead,
                closed,
                failed
        );
    }

    public DeliveryHealthSummary summarizeDeliveryHealth(Integer days,
                                                         Double deadRateThreshold,
                                                         Double pendingRateThreshold) {
        DeliverySummary summary = summarizeDeliveries(days);
        double deadThreshold = normalizeRateThreshold(deadRateThreshold, 0.05d);
        double pendingThreshold = normalizeRateThreshold(pendingRateThreshold, 0.10d);
        double total = Math.max(1d, summary.totalCount());
        double deadRate = summary.deadCount() / total;
        double pendingRate = summary.pendingCount() / total;
        String level;
        String suggestion;
        if (summary.totalCount() == 0) {
            level = HEALTH_LEVEL_HEALTHY;
            suggestion = "暂无投递数据";
        } else if (deadRate >= deadThreshold * 2 || pendingRate >= pendingThreshold * 2) {
            level = HEALTH_LEVEL_CRITICAL;
            suggestion = "请立即处理死信并检查告警渠道可用性";
        } else if (deadRate >= deadThreshold || pendingRate >= pendingThreshold) {
            level = HEALTH_LEVEL_WARNING;
            suggestion = "建议优先重放或关闭死信，避免堆积";
        } else {
            level = HEALTH_LEVEL_HEALTHY;
            suggestion = "投递链路整体健康";
        }
        return new DeliveryHealthSummary(
                summary.rangeDays(),
                summary.startTime(),
                summary.endTime(),
                summary.totalCount(),
                summary.successCount(),
                summary.pendingCount(),
                summary.deadCount(),
                summary.closedCount(),
                summary.failedCount(),
                deadRate,
                pendingRate,
                deadThreshold,
                pendingThreshold,
                level,
                suggestion
        );
    }

    public List<DeliveryHealthLogItem> listHealthChecks(Integer limit, String level) {
        return listHealthChecks(limit, level, null);
    }

    public List<DeliveryHealthLogItem> listHealthChecks(Integer limit, String level, LocalDate date) {
        int safeLimit = normalizeListLimit(limit);
        QueryWrapper<AiExperimentAlertDeliveryHealthLog> wrapper = new QueryWrapper<>();
        String normalizedLevel = normalizeHealthLevel(level);
        if (normalizedLevel != null) {
            wrapper.eq("health_level", normalizedLevel);
        }
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            wrapper.ge("check_time", start)
                    .lt("check_time", end);
        }
        wrapper.orderByDesc("id")
                .last("limit " + safeLimit);
        return healthLogMapper.selectList(wrapper).stream()
                .map(this::toHealthLogItem)
                .toList();
    }

    public DeliveryHealthTrendSummary summarizeHealthTrend(Integer days) {
        int safeDays = normalizeDays(days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(safeDays);
        QueryWrapper<AiExperimentAlertDeliveryHealthLog> wrapper = new QueryWrapper<>();
        wrapper.ge("check_time", since)
                .orderByDesc("id")
                .last("limit 5000");
        List<AiExperimentAlertDeliveryHealthLog> logs = healthLogMapper.selectList(wrapper);
        long total = logs.size();
        long healthy = logs.stream().filter(item -> HEALTH_LEVEL_HEALTHY.equals(item.getHealthLevel())).count();
        long warning = logs.stream().filter(item -> HEALTH_LEVEL_WARNING.equals(item.getHealthLevel())).count();
        long critical = logs.stream().filter(item -> HEALTH_LEVEL_CRITICAL.equals(item.getHealthLevel())).count();
        long changed = logs.stream().filter(item -> toBool(item.getLevelChanged())).count();
        long escalated = logs.stream().filter(item -> toBool(item.getEscalated())).count();
        long warningNotified = logs.stream().filter(item -> toBool(item.getWarningNotified())).count();
        long recoveryNotified = logs.stream().filter(item -> toBool(item.getRecoveryNotified())).count();
        DeliveryHealthLogItem latest = logs.isEmpty() ? null : toHealthLogItem(logs.get(0));
        return new DeliveryHealthTrendSummary(
                safeDays,
                since,
                now,
                total,
                healthy,
                warning,
                critical,
                changed,
                escalated,
                warningNotified,
                recoveryNotified,
                latest
        );
    }

    public List<DeliveryHealthDailyTrendItem> listHealthDailyTrend(Integer days) {
        int safeDays = normalizeDays(days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(safeDays);
        QueryWrapper<AiExperimentAlertDeliveryHealthLog> wrapper = new QueryWrapper<>();
        wrapper.ge("check_time", since)
                .orderByAsc("check_time")
                .last("limit 5000");
        List<AiExperimentAlertDeliveryHealthLog> logs = healthLogMapper.selectList(wrapper);
        Map<LocalDate, List<AiExperimentAlertDeliveryHealthLog>> grouped = new LinkedHashMap<>();
        for (AiExperimentAlertDeliveryHealthLog item : logs) {
            if (item.getCheckTime() == null) {
                continue;
            }
            LocalDate date = item.getCheckTime().toLocalDate();
            grouped.computeIfAbsent(date, key -> new ArrayList<>()).add(item);
        }
        List<DeliveryHealthDailyTrendItem> items = new ArrayList<>();
        for (Map.Entry<LocalDate, List<AiExperimentAlertDeliveryHealthLog>> entry : grouped.entrySet()) {
            List<AiExperimentAlertDeliveryHealthLog> dayItems = entry.getValue();
            long total = dayItems.size();
            long healthy = dayItems.stream().filter(item -> HEALTH_LEVEL_HEALTHY.equals(item.getHealthLevel())).count();
            long warning = dayItems.stream().filter(item -> HEALTH_LEVEL_WARNING.equals(item.getHealthLevel())).count();
            long critical = dayItems.stream().filter(item -> HEALTH_LEVEL_CRITICAL.equals(item.getHealthLevel())).count();
            long escalated = dayItems.stream().filter(item -> toBool(item.getEscalated())).count();
            double avgDeadRate = dayItems.stream().map(AiExperimentAlertDeliveryHealthLog::getDeadRate)
                    .filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0d);
            double avgPendingRate = dayItems.stream().map(AiExperimentAlertDeliveryHealthLog::getPendingRate)
                    .filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0d);
            double maxDeadRate = dayItems.stream().map(AiExperimentAlertDeliveryHealthLog::getDeadRate)
                    .filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).max().orElse(0d);
            double maxPendingRate = dayItems.stream().map(AiExperimentAlertDeliveryHealthLog::getPendingRate)
                    .filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).max().orElse(0d);
            items.add(new DeliveryHealthDailyTrendItem(
                    entry.getKey().toString(),
                    total,
                    healthy,
                    warning,
                    critical,
                    escalated,
                    avgDeadRate,
                    avgPendingRate,
                    maxDeadRate,
                    maxPendingRate
            ));
        }
        return items;
    }

    public List<DeliveryHealthRiskDayItem> listHealthRiskDays(Integer days, Integer limit) {
        int safeLimit = normalizeListLimit(limit);
        List<DeliveryHealthDailyTrendItem> dailyItems = listHealthDailyTrend(days);
        return dailyItems.stream()
                .map(item -> {
                    double riskScore = calculateRiskScore(item);
                    String riskLevel = resolveRiskLevel(riskScore);
                    String insight = buildRiskInsight(item, riskScore, riskLevel);
                    return new DeliveryHealthRiskDayItem(
                            item.date(),
                            riskScore,
                            riskLevel,
                            insight,
                            item.totalChecks(),
                            item.healthyCount(),
                            item.warningCount(),
                            item.criticalCount(),
                            item.escalatedCount(),
                            item.avgDeadRate(),
                            item.avgPendingRate(),
                            item.maxDeadRate(),
                            item.maxPendingRate()
                    );
                })
                .sorted(Comparator.comparingDouble(DeliveryHealthRiskDayItem::riskScore).reversed()
                        .thenComparing(DeliveryHealthRiskDayItem::date, Comparator.reverseOrder()))
                .limit(safeLimit)
                .toList();
    }

    public DeliveryHealthRiskSummary summarizeHealthRiskDays(Integer days) {
        int safeDays = normalizeDays(days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(safeDays);
        List<DeliveryHealthRiskDayItem> riskDays = listHealthRiskDays(safeDays, 3650);
        long highCount = riskDays.stream().filter(item -> "HIGH".equals(item.riskLevel())).count();
        long mediumCount = riskDays.stream().filter(item -> "MEDIUM".equals(item.riskLevel())).count();
        long lowCount = riskDays.stream().filter(item -> "LOW".equals(item.riskLevel())).count();
        DeliveryHealthRiskDayItem maxRiskDay = riskDays.stream()
                .max(Comparator.comparingDouble(DeliveryHealthRiskDayItem::riskScore))
                .orElse(null);
        List<String> topHighRiskDates = riskDays.stream()
                .filter(item -> "HIGH".equals(item.riskLevel()))
                .limit(10)
                .map(DeliveryHealthRiskDayItem::date)
                .toList();
        return new DeliveryHealthRiskSummary(
                safeDays,
                since,
                now,
                riskDays.size(),
                highCount,
                mediumCount,
                lowCount,
                maxRiskDay == null ? 0d : maxRiskDay.riskScore(),
                maxRiskDay == null ? null : maxRiskDay.date(),
                topHighRiskDates
        );
    }

    public DeliveryHealthGovernanceAdvice generateHealthGovernanceAdvice(Integer days) {
        int safeDays = normalizeDays(days);
        DeliveryHealthSummary healthSummary = summarizeDeliveryHealth(safeDays, null, null);
        DeliveryHealthRiskSummary riskSummary = summarizeHealthRiskDays(safeDays);
        DeliveryHealthThresholdSuggestion thresholdSuggestion = suggestHealthThresholds(safeDays);
        String overallRiskLevel;
        if (HEALTH_LEVEL_CRITICAL.equals(healthSummary.healthLevel()) || riskSummary.highRiskDays() >= 2) {
            overallRiskLevel = "HIGH";
        } else if (HEALTH_LEVEL_WARNING.equals(healthSummary.healthLevel()) || riskSummary.mediumRiskDays() >= 2) {
            overallRiskLevel = "MEDIUM";
        } else {
            overallRiskLevel = "LOW";
        }
        List<String> keyFindings = new ArrayList<>();
        keyFindings.add("健康级别=" + healthSummary.healthLevel()
                + ", deadRate=" + String.format(Locale.ROOT, "%.2f%%", healthSummary.deadRate() * 100d)
                + ", pendingRate=" + String.format(Locale.ROOT, "%.2f%%", healthSummary.pendingRate() * 100d));
        keyFindings.add("风险分布: high=" + riskSummary.highRiskDays()
                + ", medium=" + riskSummary.mediumRiskDays()
                + ", low=" + riskSummary.lowRiskDays()
                + ", maxRiskDate=" + normalizeBlank(riskSummary.maxRiskDate(), "-"));
        keyFindings.add("阈值建议: dead<="
                + String.format(Locale.ROOT, "%.2f%%", thresholdSuggestion.recommendedDeadRateThreshold() * 100d)
                + ", pending<="
                + String.format(Locale.ROOT, "%.2f%%", thresholdSuggestion.recommendedPendingRateThreshold() * 100d)
                + ", confidence=" + thresholdSuggestion.confidenceLevel());

        List<String> recommendedActions = new ArrayList<>();
        if ("HIGH".equals(overallRiskLevel)) {
            recommendedActions.add("立即清理/重放死信并确认告警通道可用，优先处理高风险日期样本。");
            recommendedActions.add("临时收紧巡检窗口：提升巡检频率并降低升级冷却时间，缩短问题发现间隔。");
        } else if ("MEDIUM".equals(overallRiskLevel)) {
            recommendedActions.add("按风险 Top 日期逐日排查（点击风险日下钻日志），处理 warning/critical 高发时段。");
        } else {
            recommendedActions.add("保持现有策略，持续观察趋势并按周复盘风险分布。");
        }
        double deadDiff = Math.abs(thresholdSuggestion.recommendedDeadRateThreshold() - thresholdSuggestion.currentDeadRateThreshold());
        double pendingDiff = Math.abs(thresholdSuggestion.recommendedPendingRateThreshold() - thresholdSuggestion.currentPendingRateThreshold());
        if (deadDiff >= 0.01d || pendingDiff >= 0.01d) {
            recommendedActions.add("建议评估并应用新阈值（可在控制台一键应用），降低误报或漏报概率。");
        }
        if (riskSummary.highRiskDays() > 0) {
            recommendedActions.add("对高风险日期进行专项复盘，记录原因并形成告警处置 SOP。");
        }
        return new DeliveryHealthGovernanceAdvice(
                safeDays,
                thresholdSuggestion.startTime(),
                thresholdSuggestion.endTime(),
                overallRiskLevel,
                keyFindings,
                recommendedActions,
                LocalDateTime.now()
        );
    }

    public DeliveryHealthGovernanceReport generateHealthGovernanceReport(Integer days) {
        int safeDays = normalizeDays(days);
        DeliveryHealthSummary healthSummary = summarizeDeliveryHealth(safeDays, null, null);
        DeliveryHealthRiskSummary riskSummary = summarizeHealthRiskDays(safeDays);
        DeliveryHealthThresholdSuggestion thresholdSuggestion = suggestHealthThresholds(safeDays);
        DeliveryHealthGovernanceAdvice advice = generateHealthGovernanceAdvice(safeDays);
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("AI 告警投递健康治理报告").append('\n');
        contentBuilder.append("统计窗口: 最近 ").append(safeDays).append(" 天").append('\n');
        contentBuilder.append("生成时间: ").append(LocalDateTime.now()).append('\n');
        contentBuilder.append('\n');
        contentBuilder.append("一、健康现状").append('\n');
        contentBuilder.append("- 健康级别: ").append(healthSummary.healthLevel()).append('\n');
        contentBuilder.append("- 死信率: ").append(String.format(Locale.ROOT, "%.2f%%", healthSummary.deadRate() * 100d))
                .append(" (阈值 ").append(String.format(Locale.ROOT, "%.2f%%", healthSummary.deadRateThreshold() * 100d)).append(')').append('\n');
        contentBuilder.append("- 待重试率: ").append(String.format(Locale.ROOT, "%.2f%%", healthSummary.pendingRate() * 100d))
                .append(" (阈值 ").append(String.format(Locale.ROOT, "%.2f%%", healthSummary.pendingRateThreshold() * 100d)).append(')').append('\n');
        contentBuilder.append('\n');
        contentBuilder.append("二、风险分布").append('\n');
        contentBuilder.append("- 高风险天数: ").append(riskSummary.highRiskDays()).append('\n');
        contentBuilder.append("- 中风险天数: ").append(riskSummary.mediumRiskDays()).append('\n');
        contentBuilder.append("- 低风险天数: ").append(riskSummary.lowRiskDays()).append('\n');
        contentBuilder.append("- 最高风险日: ").append(normalizeBlank(riskSummary.maxRiskDate(), "-"))
                .append(" (").append(String.format(Locale.ROOT, "%.1f", riskSummary.maxRiskScore())).append(")").append('\n');
        contentBuilder.append('\n');
        contentBuilder.append("三、阈值建议").append('\n');
        contentBuilder.append("- deadRate 建议阈值: ").append(String.format(Locale.ROOT, "%.2f%%", thresholdSuggestion.recommendedDeadRateThreshold() * 100d)).append('\n');
        contentBuilder.append("- pendingRate 建议阈值: ").append(String.format(Locale.ROOT, "%.2f%%", thresholdSuggestion.recommendedPendingRateThreshold() * 100d)).append('\n');
        contentBuilder.append("- 置信度: ").append(thresholdSuggestion.confidenceLevel()).append('\n');
        contentBuilder.append('\n');
        contentBuilder.append("四、关键发现").append('\n');
        for (String finding : advice.keyFindings()) {
            contentBuilder.append("- ").append(finding).append('\n');
        }
        contentBuilder.append('\n');
        contentBuilder.append("五、建议动作").append('\n');
        for (int i = 0; i < advice.recommendedActions().size(); i++) {
            contentBuilder.append(i + 1).append(". ").append(advice.recommendedActions().get(i)).append('\n');
        }
        return new DeliveryHealthGovernanceReport(
                safeDays,
                advice.generatedAt(),
                advice.overallRiskLevel(),
                contentBuilder.toString().trim()
        );
    }

    public DeliveryHealthThresholdSuggestion suggestHealthThresholds(Integer days) {
        int safeDays = normalizeDays(days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(safeDays);
        QueryWrapper<AiExperimentAlertDeliveryHealthLog> wrapper = new QueryWrapper<>();
        wrapper.ge("check_time", since)
                .orderByDesc("id")
                .last("limit 5000");
        List<AiExperimentAlertDeliveryHealthLog> logs = healthLogMapper.selectList(wrapper);

        double currentDeadThreshold = normalizeRateThreshold(
                aiProperties.getExperimentAlertDeliveryHealthDeadRateThreshold(), 0.05d
        );
        double currentPendingThreshold = normalizeRateThreshold(
                aiProperties.getExperimentAlertDeliveryHealthPendingRateThreshold(), 0.10d
        );
        if (logs.isEmpty()) {
            return new DeliveryHealthThresholdSuggestion(
                    safeDays,
                    since,
                    now,
                    0L,
                    0d,
                    0d,
                    0d,
                    0d,
                    currentDeadThreshold,
                    currentPendingThreshold,
                    currentDeadThreshold,
                    currentPendingThreshold,
                    "巡检样本不足，建议继续观察后再调整阈值",
                    "LOW"
            );
        }
        List<Double> deadRates = logs.stream()
                .map(item -> toDouble(item.getDeadRate()))
                .filter(rate -> rate >= 0d)
                .toList();
        List<Double> pendingRates = logs.stream()
                .map(item -> toDouble(item.getPendingRate()))
                .filter(rate -> rate >= 0d)
                .toList();
        double avgDeadRate = deadRates.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
        double avgPendingRate = pendingRates.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
        double p90DeadRate = percentile(deadRates, 0.90d);
        double p90PendingRate = percentile(pendingRates, 0.90d);
        double recommendedDeadThreshold = clampRate(Math.max(currentDeadThreshold * 0.8d, p90DeadRate * 1.15d));
        double recommendedPendingThreshold = clampRate(Math.max(currentPendingThreshold * 0.8d, p90PendingRate * 1.15d));
        String confidence = logs.size() >= 100 ? "HIGH" : (logs.size() >= 30 ? "MEDIUM" : "LOW");
        String suggestion = "建议将阈值调整为 deadRate<="
                + String.format(Locale.ROOT, "%.4f", recommendedDeadThreshold)
                + "、pendingRate<="
                + String.format(Locale.ROOT, "%.4f", recommendedPendingThreshold)
                + "（基于近 " + safeDays + " 天 p90 * 1.15）";
        return new DeliveryHealthThresholdSuggestion(
                safeDays,
                since,
                now,
                logs.size(),
                avgDeadRate,
                avgPendingRate,
                p90DeadRate,
                p90PendingRate,
                currentDeadThreshold,
                currentPendingThreshold,
                recommendedDeadThreshold,
                recommendedPendingThreshold,
                suggestion,
                confidence
        );
    }

    public DeliveryHealthLogStorageSummary summarizeHealthLogStorage(Integer retainDays) {
        int safeRetainDays = normalizeHealthLogRetentionDays(retainDays);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(safeRetainDays);
        QueryWrapper<AiExperimentAlertDeliveryHealthLog> totalWrapper = new QueryWrapper<>();
        Long total = healthLogMapper.selectCount(totalWrapper);
        QueryWrapper<AiExperimentAlertDeliveryHealthLog> expiredWrapper = new QueryWrapper<>();
        expiredWrapper.lt("check_time", cutoff);
        Long expired = healthLogMapper.selectCount(expiredWrapper);
        return new DeliveryHealthLogStorageSummary(
                safeRetainDays,
                cutoff,
                total == null ? 0L : total,
                expired == null ? 0L : expired
        );
    }

    public DeliveryHealthLogCleanupResult cleanupHealthLogs(Integer retainDays, Integer limit, boolean manualTrigger) {
        int safeRetainDays = normalizeHealthLogRetentionDays(retainDays);
        int safeLimit = normalizeCleanupLimit(limit);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(safeRetainDays);
        QueryWrapper<AiExperimentAlertDeliveryHealthLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.lt("check_time", cutoff)
                .orderByAsc("id")
                .last("limit " + safeLimit);
        List<AiExperimentAlertDeliveryHealthLog> expired = healthLogMapper.selectList(queryWrapper);
        List<Long> deletedIds = expired.stream()
                .map(AiExperimentAlertDeliveryHealthLog::getId)
                .filter(Objects::nonNull)
                .toList();
        int deletedCount = deletedIds.isEmpty() ? 0 : healthLogMapper.deleteBatchIds(deletedIds);
        if (deletedCount > 0) {
            log.info("AI实验投递健康巡检日志清理完成: trigger={}, retainDays={}, limit={}, deleted={}",
                    manualTrigger ? "manual" : "scheduler",
                    safeRetainDays,
                    safeLimit,
                    deletedCount);
        }
        return new DeliveryHealthLogCleanupResult(
                safeRetainDays,
                safeLimit,
                deletedCount,
                cutoff,
                manualTrigger ? "manual" : "scheduler",
                now
        );
    }

    public DeliveryHealthCheckResult checkAndEscalateDeliveryHealth(Integer days,
                                                                    Double deadRateThreshold,
                                                                    Double pendingRateThreshold,
                                                                    boolean manualTrigger) {
        DeliveryHealthSummary summary = summarizeDeliveryHealth(days, deadRateThreshold, pendingRateThreshold);
        HealthTransition transition = updateHealthTransition(summary.healthLevel());
        boolean warningNotified = false;
        boolean recoveryNotified = false;
        if (transition.levelChanged()) {
            if (HEALTH_LEVEL_WARNING.equals(summary.healthLevel())) {
                warningNotified = raiseDeliveryHealthWarning(summary, transition.previousLevel(), manualTrigger);
            } else if (HEALTH_LEVEL_HEALTHY.equals(summary.healthLevel())
                    && !HEALTH_LEVEL_HEALTHY.equals(transition.previousLevel())) {
                recoveryNotified = raiseDeliveryHealthRecovery(summary, transition.previousLevel(), manualTrigger);
            }
        }
        DeliveryHealthCheckResult result;
        if (!HEALTH_LEVEL_CRITICAL.equals(summary.healthLevel())) {
            result = new DeliveryHealthCheckResult(
                    summary,
                    false,
                    false,
                    warningNotified,
                    recoveryNotified,
                    transition.previousLevel(),
                    transition.currentLevel(),
                    transition.levelChanged(),
                    LocalDateTime.now(),
                    "not_critical"
            );
            persistHealthCheck(result, manualTrigger);
            return result;
        }
        if (isHealthEscalationSuppressed(manualTrigger)) {
            result = new DeliveryHealthCheckResult(
                    summary,
                    false,
                    true,
                    warningNotified,
                    recoveryNotified,
                    transition.previousLevel(),
                    transition.currentLevel(),
                    transition.levelChanged(),
                    LocalDateTime.now(),
                    "cooldown_suppressed"
            );
            persistHealthCheck(result, manualTrigger);
            return result;
        }
        boolean escalated = raiseDeliveryHealthEscalation(summary, manualTrigger);
        result = new DeliveryHealthCheckResult(
                summary,
                escalated,
                false,
                warningNotified,
                recoveryNotified,
                transition.previousLevel(),
                transition.currentLevel(),
                transition.levelChanged(),
                LocalDateTime.now(),
                escalated ? "escalated" : "escalation_failed"
        );
        persistHealthCheck(result, manualTrigger);
        return result;
    }

    public DeliveryReplayResult replayDelivery(Long deliveryId) {
        if (deliveryId == null || deliveryId < 1) {
            throw new IllegalArgumentException("deliveryId 必须大于 0");
        }
        AiExperimentAlertDeliveryLog source = deliveryLogMapper.selectById(deliveryId);
        if (source == null) {
            throw new IllegalArgumentException("未找到投递记录");
        }
        if (!isReplayableStatus(source.getStatus())) {
            throw new IllegalArgumentException("仅支持重放 failed/dead 状态记录");
        }
        if (!CHANNEL_EMAIL.equals(source.getChannel()) && !CHANNEL_WEBHOOK.equals(source.getChannel())) {
            throw new IllegalArgumentException("仅支持重放 email/webhook 渠道");
        }
        String replayPayload = withReplayMetadata(source.getPayloadJson(), deliveryId);
        AiExperimentAlertDeliveryLog replay = createPendingLog(
                source.getAlertLogId(),
                source.getAlertType(),
                source.getAlertLevel(),
                source.getChannel(),
                source.getTargetValue(),
                replayPayload,
                source.getMaxAttempts()
        );
        String status = executeDelivery(replay);
        return new DeliveryReplayResult(source.getId(), replay.getId(), status);
    }

    public DeliveryBatchReplayResult replayDeliveries(List<Long> deliveryIds) {
        if (deliveryIds == null || deliveryIds.isEmpty()) {
            return new DeliveryBatchReplayResult(0, 0, 0, 0, 0, 0, 0);
        }
        int requested = 0;
        int accepted = 0;
        int success = 0;
        int pending = 0;
        int dead = 0;
        int closed = 0;
        int skipped = 0;
        for (Long deliveryId : deliveryIds.stream().distinct().limit(200).toList()) {
            requested++;
            try {
                DeliveryReplayResult replayResult = replayDelivery(deliveryId);
                accepted++;
                if (STATUS_SUCCESS.equals(replayResult.replayStatus())) {
                    success++;
                } else if (STATUS_PENDING.equals(replayResult.replayStatus())) {
                    pending++;
                } else if (STATUS_DEAD.equals(replayResult.replayStatus())) {
                    dead++;
                } else if (STATUS_CLOSED.equals(replayResult.replayStatus())) {
                    closed++;
                }
            } catch (IllegalArgumentException ex) {
                skipped++;
            }
        }
        return new DeliveryBatchReplayResult(
                requested,
                accepted,
                success,
                pending,
                dead,
                closed,
                skipped
        );
    }

    public DeliveryCloseResult closeDeadLetter(Long deliveryId, String reason) {
        if (deliveryId == null || deliveryId < 1) {
            throw new IllegalArgumentException("deliveryId 必须大于 0");
        }
        AiExperimentAlertDeliveryLog source = deliveryLogMapper.selectById(deliveryId);
        if (source == null) {
            throw new IllegalArgumentException("未找到投递记录");
        }
        if (!STATUS_DEAD.equalsIgnoreCase(normalizeBlank(source.getStatus(), ""))) {
            throw new IllegalArgumentException("仅支持关闭 dead 状态记录");
        }
        LocalDateTime now = LocalDateTime.now();
        AiExperimentAlertDeliveryLog update = new AiExperimentAlertDeliveryLog();
        update.setId(source.getId());
        update.setStatus(STATUS_CLOSED);
        update.setNextRetryTime(null);
        update.setUpdateTime(now);
        update.setLastErrorMessage(mergeCloseReason(source.getLastErrorMessage(), reason, now));
        deliveryLogMapper.updateById(update);
        return new DeliveryCloseResult(source.getId(), STATUS_CLOSED, normalizeBlank(reason, "manual_close"), now);
    }

    public DeliveryBatchCloseResult closeDeadLetters(List<Long> deliveryIds, String reason) {
        if (deliveryIds == null || deliveryIds.isEmpty()) {
            return new DeliveryBatchCloseResult(0, 0, 0);
        }
        int requested = 0;
        int closed = 0;
        int skipped = 0;
        for (Long deliveryId : deliveryIds.stream().distinct().limit(200).toList()) {
            requested++;
            try {
                closeDeadLetter(deliveryId, reason);
                closed++;
            } catch (IllegalArgumentException ex) {
                skipped++;
            }
        }
        return new DeliveryBatchCloseResult(requested, closed, skipped);
    }

    private AiExperimentAlertDeliveryLog createPendingLog(Long alertLogId,
                                                          String alertType,
                                                          String alertLevel,
                                                          String channel,
                                                          String target,
                                                          String payloadJson,
                                                          Integer maxAttemptsOverride) {
        AiExperimentAlertDeliveryLog log = new AiExperimentAlertDeliveryLog();
        log.setAlertLogId(alertLogId != null ? alertLogId : 0L);
        log.setAlertType(normalizeBlank(alertType, "unknown"));
        log.setAlertLevel(normalizeBlank(alertLevel, "WARNING"));
        log.setChannel(channel);
        log.setTargetValue(normalizeBlank(target, "unknown"));
        log.setStatus(STATUS_PENDING);
        log.setAttemptCount(0);
        log.setMaxAttempts(normalizeMaxAttempts(maxAttemptsOverride != null
                ? maxAttemptsOverride
                : aiProperties.getExperimentAlertDeliveryMaxAttempts()));
        log.setPayloadJson(payloadJson);
        log.setCreateTime(LocalDateTime.now());
        log.setUpdateTime(LocalDateTime.now());
        deliveryLogMapper.insert(log);
        return log;
    }

    private String executeDelivery(AiExperimentAlertDeliveryLog item) {
        int currentAttempts = item.getAttemptCount() == null ? 0 : item.getAttemptCount();
        int attempt = currentAttempts + 1;
        int maxAttempts = item.getMaxAttempts() == null ? 1 : Math.max(1, item.getMaxAttempts());
        LocalDateTime now = LocalDateTime.now();
        AiExperimentAlertDeliveryLog update = new AiExperimentAlertDeliveryLog();
        update.setId(item.getId());
        update.setAttemptCount(attempt);
        update.setUpdateTime(now);
        try {
            int responseCode = doSend(item);
            update.setStatus(STATUS_SUCCESS);
            update.setLastResponseCode(responseCode);
            update.setLastErrorMessage(null);
            update.setNextRetryTime(null);
            deliveryLogMapper.updateById(update);
            return STATUS_SUCCESS;
        } catch (DeliveryException ex) {
            boolean canRetry = aiProperties.isExperimentAlertDeliveryRetryEnabled() && attempt < maxAttempts;
            update.setStatus(canRetry ? STATUS_PENDING : STATUS_DEAD);
            update.setLastResponseCode(ex.responseCode());
            update.setLastErrorMessage(truncate(ex.getMessage(), 1000));
            update.setNextRetryTime(canRetry ? now.plusSeconds(normalizeRetryDelaySeconds(aiProperties.getExperimentAlertDeliveryRetryDelaySeconds())) : null);
            deliveryLogMapper.updateById(update);
            if (!canRetry) {
                raiseDeadLetterEscalation(item, ex.getMessage());
            }
            return canRetry ? STATUS_PENDING : STATUS_DEAD;
        } catch (Exception ex) {
            boolean canRetry = aiProperties.isExperimentAlertDeliveryRetryEnabled() && attempt < maxAttempts;
            update.setStatus(canRetry ? STATUS_PENDING : STATUS_DEAD);
            update.setLastResponseCode(null);
            update.setLastErrorMessage(truncate(ex.getMessage(), 1000));
            update.setNextRetryTime(canRetry ? now.plusSeconds(normalizeRetryDelaySeconds(aiProperties.getExperimentAlertDeliveryRetryDelaySeconds())) : null);
            deliveryLogMapper.updateById(update);
            if (!canRetry) {
                raiseDeadLetterEscalation(item, ex.getMessage());
            }
            return canRetry ? STATUS_PENDING : STATUS_DEAD;
        }
    }

    private int doSend(AiExperimentAlertDeliveryLog item) {
        if (CHANNEL_EMAIL.equals(item.getChannel())) {
            return sendEmail(item);
        }
        if (CHANNEL_WEBHOOK.equals(item.getChannel())) {
            return sendWebhook(item);
        }
        throw new DeliveryException(null, "不支持的渠道: " + item.getChannel());
    }

    private int sendEmail(AiExperimentAlertDeliveryLog item) {
        JsonNode payload = readPayload(item.getPayloadJson());
        String subject = readPayloadText(payload, "subject", "[AI实验告警]");
        String content = readPayloadText(payload, "content", "");
        emailService.sendSimpleMail(item.getTargetValue(), subject, content);
        return 200;
    }

    private int sendWebhook(AiExperimentAlertDeliveryLog item) {
        JsonNode payload = readPayload(item.getPayloadJson());
        String requestBody = readPayloadText(payload, "requestBody", "{}");
        int timeoutSeconds = readPayloadInt(payload, "timeoutSeconds",
                Math.max(1, Math.min(aiProperties.getExperimentAlertWebhookTimeoutSeconds(), 30)));
        HttpRequest request = HttpRequest.newBuilder(URI.create(item.getTargetValue()))
                .timeout(Duration.ofSeconds(Math.max(1, Math.min(timeoutSeconds, 30))))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = WEBHOOK_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new DeliveryException(response.statusCode(), "Webhook响应异常: " + response.body());
            }
            return response.statusCode();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DeliveryException(null, "Webhook发送被中断");
        } catch (DeliveryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DeliveryException(null, "Webhook发送失败: " + ex.getMessage());
        }
    }

    private JsonNode readPayload(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(payloadJson);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private String readPayloadText(JsonNode payload, String key, String defaultValue) {
        JsonNode node = payload.get(key);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        String value = node.asText();
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private int readPayloadInt(JsonNode payload, String key, int defaultValue) {
        JsonNode node = payload.get(key);
        if (node == null || node.isNull() || !node.isNumber()) {
            return defaultValue;
        }
        return node.asInt();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private int normalizeRetryBatchSize(Integer limit) {
        if (limit == null) {
            return 20;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    private int normalizeListLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    private int normalizeCleanupLimit(Integer limit) {
        if (limit == null) {
            return HEALTH_LOG_CLEANUP_BATCH_DEFAULT;
        }
        return Math.max(1, Math.min(limit, 5000));
    }

    private int normalizeHealthLogRetentionDays(Integer days) {
        if (days == null) {
            return HEALTH_LOG_RETENTION_DAYS_DEFAULT;
        }
        return Math.max(1, Math.min(days, 3650));
    }

    private int normalizeMaxAttempts(int maxAttempts) {
        return Math.max(1, Math.min(maxAttempts, 10));
    }

    private int normalizeRetryDelaySeconds(int retryDelaySeconds) {
        return Math.max(5, Math.min(retryDelaySeconds, 3600));
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        if (STATUS_PENDING.equals(normalized)
                || STATUS_SUCCESS.equals(normalized)
                || STATUS_FAILED.equals(normalized)
                || STATUS_DEAD.equals(normalized)
                || STATUS_CLOSED.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeHealthLevel(String level) {
        if (!StringUtils.hasText(level)) {
            return null;
        }
        String normalized = level.trim().toUpperCase(Locale.ROOT);
        if (HEALTH_LEVEL_HEALTHY.equals(normalized)
                || HEALTH_LEVEL_WARNING.equals(normalized)
                || HEALTH_LEVEL_CRITICAL.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private boolean isReplayableStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return STATUS_DEAD.equals(normalized) || STATUS_FAILED.equals(normalized);
    }

    private String normalizeBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private int normalizeDays(Integer days) {
        if (days == null) {
            return 7;
        }
        return Math.max(1, Math.min(days, 90));
    }

    private int normalizeHealthEscalationCooldownSeconds(int value) {
        return Math.max(30, Math.min(value, 86400));
    }

    private double normalizeRateThreshold(Double value, double fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(0d, Math.min(1d, value));
    }

    private double clampRate(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    private double percentile(List<Double> values, double ratio) {
        if (values == null || values.isEmpty()) {
            return 0d;
        }
        double safeRatio = Math.max(0d, Math.min(1d, ratio));
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        double position = safeRatio * (sorted.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) {
            return sorted.get(lower);
        }
        double lowerValue = sorted.get(lower);
        double upperValue = sorted.get(upper);
        double weight = position - lower;
        return lowerValue + (upperValue - lowerValue) * weight;
    }

    private double calculateRiskScore(DeliveryHealthDailyTrendItem item) {
        double score = 0d;
        score += item.criticalCount() * 15d;
        score += item.warningCount() * 6d;
        score += item.escalatedCount() * 8d;
        score += item.avgDeadRate() * 100d * 1.5d;
        score += item.maxDeadRate() * 100d * 1.2d;
        score += item.avgPendingRate() * 100d * 0.9d;
        score += item.maxPendingRate() * 100d * 0.8d;
        return Math.min(100d, Math.max(0d, score));
    }

    private String resolveRiskLevel(double riskScore) {
        if (riskScore >= 70d) {
            return "HIGH";
        }
        if (riskScore >= 40d) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String buildRiskInsight(DeliveryHealthDailyTrendItem item, double riskScore, String riskLevel) {
        return "risk=" + String.format(Locale.ROOT, "%.1f", riskScore)
                + ", level=" + riskLevel
                + ", critical=" + item.criticalCount()
                + ", warning=" + item.warningCount()
                + ", escalated=" + item.escalatedCount()
                + ", maxDeadRate=" + String.format(Locale.ROOT, "%.2f%%", item.maxDeadRate() * 100d)
                + ", maxPendingRate=" + String.format(Locale.ROOT, "%.2f%%", item.maxPendingRate() * 100d);
    }

    private long countByStatus(String status, LocalDateTime since) {
        QueryWrapper<AiExperimentAlertDeliveryLog> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.ge("create_time", since);
        Long count = deliveryLogMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    private String mergeCloseReason(String previousError, String reason, LocalDateTime closeTime) {
        String normalizedReason = normalizeBlank(reason, "manual_close");
        String closeLine = "[closed@" + closeTime + "] " + normalizedReason;
        if (!StringUtils.hasText(previousError)) {
            return closeLine;
        }
        String merged = previousError + "\n" + closeLine;
        return truncate(merged, 1000);
    }

    private String withReplayMetadata(String payloadJson, Long sourceDeliveryId) {
        JsonNode payload = readPayload(payloadJson);
        ObjectNode objectNode;
        if (payload == null || !payload.isObject()) {
            objectNode = objectMapper.createObjectNode();
        } else {
            objectNode = (ObjectNode) payload.deepCopy();
        }
        objectNode.put("replayFromDeliveryId", sourceDeliveryId);
        objectNode.put("replayTime", LocalDateTime.now().toString());
        try {
            return objectMapper.writeValueAsString(objectNode);
        } catch (Exception ex) {
            return payloadJson;
        }
    }

    private void raiseDeadLetterEscalation(AiExperimentAlertDeliveryLog item, String errorMessage) {
        try {
            String level = normalizeBlank(item.getAlertLevel(), "WARNING");
            String title = "[AI投递死信告警] " + item.getChannel() + " - alertLogId=" + item.getAlertLogId();
            String content = "deliveryId=" + item.getId()
                    + "\nlevel=" + level
                    + "\nchannel=" + item.getChannel()
                    + "\ntarget=" + item.getTargetValue()
                    + "\nstatus=dead"
                    + "\nerror=" + normalizeBlank(truncate(errorMessage, 900), "unknown");
            SysNotice notice = new SysNotice();
            notice.setTitle(title);
            notice.setContent(content);
            notice.setNoticeType(1);
            notice.setStatus(1);
            notice.setCreateBy(0L);
            notice.setCreateName("ai-alert-delivery");
            notice.setCreateTime(LocalDateTime.now());
            notice.setDeleted(0);
            noticeMapper.insert(notice);
            List<SysUser> users = userMapper.selectList(null);
            for (SysUser user : users) {
                SysUserNotice link = new SysUserNotice();
                link.setUserId(user.getId());
                link.setNoticeId(notice.getId());
                link.setIsRead(0);
                userNoticeMapper.insert(link);
            }
            webSocketHandler.sendNotice(null, title, content);
            log.error("AI实验告警投递进入死信队列: deliveryId={}, alertLogId={}, channel={}, target={}",
                    item.getId(), item.getAlertLogId(), item.getChannel(), item.getTargetValue());
        } catch (Exception ex) {
            log.error("AI实验告警死信升级通知失败: deliveryId={}", item.getId(), ex);
        }
    }

    private boolean isHealthEscalationSuppressed(boolean manualTrigger) {
        long now = System.currentTimeMillis();
        int cooldownSeconds = normalizeHealthEscalationCooldownSeconds(
                aiProperties.getExperimentAlertDeliveryHealthEscalationCooldownSeconds()
        );
        long cooldownMillis = cooldownSeconds * 1000L;
        synchronized (this) {
            if (!manualTrigger && lastHealthEscalationEpochMs > 0 && now - lastHealthEscalationEpochMs < cooldownMillis) {
                return true;
            }
            lastHealthEscalationEpochMs = now;
            return false;
        }
    }

    private boolean raiseDeliveryHealthEscalation(DeliveryHealthSummary summary, boolean manualTrigger) {
        return raiseDeliveryHealthNotice(
                "[AI投递健康度告警] level=" + summary.healthLevel(),
                buildHealthNoticeContent(summary, null, manualTrigger),
                "ai-alert-delivery-health"
        );
    }

    private boolean raiseDeliveryHealthWarning(DeliveryHealthSummary summary, String previousLevel, boolean manualTrigger) {
        return raiseDeliveryHealthNotice(
                "[AI投递健康预警] level=" + summary.healthLevel(),
                buildHealthNoticeContent(summary, previousLevel, manualTrigger),
                "ai-alert-delivery-health-warning"
        );
    }

    private boolean raiseDeliveryHealthRecovery(DeliveryHealthSummary summary, String previousLevel, boolean manualTrigger) {
        return raiseDeliveryHealthNotice(
                "[AI投递健康恢复] " + previousLevel + " -> " + summary.healthLevel(),
                buildHealthNoticeContent(summary, previousLevel, manualTrigger),
                "ai-alert-delivery-health-recovery"
        );
    }

    private String buildHealthNoticeContent(DeliveryHealthSummary summary, String previousLevel, boolean manualTrigger) {
        return "rangeDays=" + summary.rangeDays()
                + "\nlevel=" + summary.healthLevel()
                + (StringUtils.hasText(previousLevel) ? "\npreviousLevel=" + previousLevel : "")
                + "\ntotal=" + summary.totalCount()
                + "\nsuccess=" + summary.successCount()
                + "\npending=" + summary.pendingCount()
                + "\ndead=" + summary.deadCount()
                + "\nclosed=" + summary.closedCount()
                + "\nfailed=" + summary.failedCount()
                + "\ndeadRate=" + String.format(Locale.ROOT, "%.4f", summary.deadRate())
                + "\npendingRate=" + String.format(Locale.ROOT, "%.4f", summary.pendingRate())
                + "\ndeadRateThreshold=" + String.format(Locale.ROOT, "%.4f", summary.deadRateThreshold())
                + "\npendingRateThreshold=" + String.format(Locale.ROOT, "%.4f", summary.pendingRateThreshold())
                + "\ntrigger=" + (manualTrigger ? "manual" : "scheduler")
                + "\nsuggestion=" + summary.suggestion();
    }

    private boolean raiseDeliveryHealthNotice(String title, String content, String createName) {
        try {
            SysNotice notice = new SysNotice();
            notice.setTitle(title);
            notice.setContent(content);
            notice.setNoticeType(1);
            notice.setStatus(1);
            notice.setCreateBy(0L);
            notice.setCreateName(createName);
            notice.setCreateTime(LocalDateTime.now());
            notice.setDeleted(0);
            noticeMapper.insert(notice);
            List<SysUser> users = userMapper.selectList(null);
            for (SysUser user : users) {
                SysUserNotice link = new SysUserNotice();
                link.setUserId(user.getId());
                link.setNoticeId(notice.getId());
                link.setIsRead(0);
                userNoticeMapper.insert(link);
            }
            webSocketHandler.sendNotice(null, title, content);
            log.warn("AI实验告警投递健康通知: title={}", title);
            return true;
        } catch (Exception ex) {
            log.error("AI实验告警投递健康通知失败: title={}", title, ex);
            return false;
        }
    }

    private HealthTransition updateHealthTransition(String currentLevel) {
        synchronized (this) {
            String previous = lastObservedHealthLevel;
            String current = normalizeHealthLevel(currentLevel);
            if (current == null) {
                current = HEALTH_LEVEL_HEALTHY;
            }
            boolean changed = !Objects.equals(previous, current);
            lastObservedHealthLevel = current;
            return new HealthTransition(previous, current, changed);
        }
    }

    private void persistHealthCheck(DeliveryHealthCheckResult result, boolean manualTrigger) {
        if (result == null || result.healthSummary() == null) {
            return;
        }
        try {
            DeliveryHealthSummary summary = result.healthSummary();
            AiExperimentAlertDeliveryHealthLog logItem = new AiExperimentAlertDeliveryHealthLog();
            logItem.setRangeDays(summary.rangeDays());
            logItem.setHealthLevel(normalizeBlank(normalizeHealthLevel(summary.healthLevel()), HEALTH_LEVEL_HEALTHY));
            logItem.setPreviousHealthLevel(normalizeHealthLevel(result.previousHealthLevel()));
            logItem.setLevelChanged(toInt(result.levelChanged()));
            logItem.setEscalated(toInt(result.escalated()));
            logItem.setSuppressed(toInt(result.suppressed()));
            logItem.setWarningNotified(toInt(result.warningNotified()));
            logItem.setRecoveryNotified(toInt(result.recoveryNotified()));
            logItem.setReason(truncate(normalizeBlank(result.reason(), "unknown"), 64));
            logItem.setDeadRate(toScaledDecimal(summary.deadRate()));
            logItem.setPendingRate(toScaledDecimal(summary.pendingRate()));
            logItem.setDeadRateThreshold(toScaledDecimal(summary.deadRateThreshold()));
            logItem.setPendingRateThreshold(toScaledDecimal(summary.pendingRateThreshold()));
            logItem.setTotalCount(summary.totalCount());
            logItem.setSuccessCount(summary.successCount());
            logItem.setPendingCount(summary.pendingCount());
            logItem.setDeadCount(summary.deadCount());
            logItem.setClosedCount(summary.closedCount());
            logItem.setFailedCount(summary.failedCount());
            logItem.setSuggestion(truncate(summary.suggestion(), 255));
            logItem.setCheckSource(manualTrigger ? "manual" : "scheduler");
            logItem.setCheckTime(result.checkTime() == null ? LocalDateTime.now() : result.checkTime());
            logItem.setCreateTime(LocalDateTime.now());
            healthLogMapper.insert(logItem);
        } catch (Exception ex) {
            log.error("AI实验告警投递健康巡检日志落库失败: reason={}", result.reason(), ex);
        }
    }

    private DeliveryHealthLogItem toHealthLogItem(AiExperimentAlertDeliveryHealthLog item) {
        return new DeliveryHealthLogItem(
                item.getId(),
                item.getRangeDays(),
                normalizeBlank(item.getHealthLevel(), HEALTH_LEVEL_HEALTHY),
                item.getPreviousHealthLevel(),
                toBool(item.getLevelChanged()),
                toBool(item.getEscalated()),
                toBool(item.getSuppressed()),
                toBool(item.getWarningNotified()),
                toBool(item.getRecoveryNotified()),
                item.getReason(),
                toDouble(item.getDeadRate()),
                toDouble(item.getPendingRate()),
                toDouble(item.getDeadRateThreshold()),
                toDouble(item.getPendingRateThreshold()),
                item.getTotalCount() == null ? 0L : item.getTotalCount(),
                item.getSuccessCount() == null ? 0L : item.getSuccessCount(),
                item.getPendingCount() == null ? 0L : item.getPendingCount(),
                item.getDeadCount() == null ? 0L : item.getDeadCount(),
                item.getClosedCount() == null ? 0L : item.getClosedCount(),
                item.getFailedCount() == null ? 0L : item.getFailedCount(),
                item.getSuggestion(),
                item.getCheckSource(),
                item.getCheckTime(),
                item.getCreateTime()
        );
    }

    private boolean toBool(Integer value) {
        return value != null && value == 1;
    }

    private int toInt(boolean value) {
        return value ? 1 : 0;
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0d : value.doubleValue();
    }

    private BigDecimal toScaledDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    private DeliveryLogItem toDeliveryLogItem(AiExperimentAlertDeliveryLog item) {
        return new DeliveryLogItem(
                item.getId(),
                item.getAlertLogId(),
                item.getAlertType(),
                item.getAlertLevel(),
                item.getChannel(),
                item.getTargetValue(),
                item.getStatus(),
                item.getAttemptCount(),
                item.getMaxAttempts(),
                item.getNextRetryTime(),
                item.getLastResponseCode(),
                item.getLastErrorMessage(),
                item.getCreateTime(),
                item.getUpdateTime()
        );
    }

    public record DeliveryLogItem(
            Long id,
            Long alertLogId,
            String alertType,
            String alertLevel,
            String channel,
            String targetValue,
            String status,
            Integer attemptCount,
            Integer maxAttempts,
            LocalDateTime nextRetryTime,
            Integer lastResponseCode,
            String lastErrorMessage,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {}

    public record RetrySummary(
            int pickedCount,
            int successCount,
            int deadCount,
            int pendingCount
    ) {}

    public record DeliveryReplayResult(
            Long sourceDeliveryId,
            Long replayDeliveryId,
            String replayStatus
    ) {}

    public record DeliveryBatchReplayResult(
            int requestedCount,
            int acceptedCount,
            int successCount,
            int pendingCount,
            int deadCount,
            int closedCount,
            int skippedCount
    ) {}

    public record DeliveryCloseResult(
            Long deliveryId,
            String status,
            String reason,
            LocalDateTime closeTime
    ) {}

    public record DeliveryBatchCloseResult(
            int requestedCount,
            int closedCount,
            int skippedCount
    ) {}

    public record DeliverySummary(
            int rangeDays,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long totalCount,
            long successCount,
            long pendingCount,
            long deadCount,
            long closedCount,
            long failedCount
    ) {}

    public record DeliveryHealthSummary(
            int rangeDays,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long totalCount,
            long successCount,
            long pendingCount,
            long deadCount,
            long closedCount,
            long failedCount,
            double deadRate,
            double pendingRate,
            double deadRateThreshold,
            double pendingRateThreshold,
            String healthLevel,
            String suggestion
    ) {}

    public record DeliveryHealthCheckResult(
            DeliveryHealthSummary healthSummary,
            boolean escalated,
            boolean suppressed,
            boolean warningNotified,
            boolean recoveryNotified,
            String previousHealthLevel,
            String currentHealthLevel,
            boolean levelChanged,
            LocalDateTime checkTime,
            String reason
    ) {}

    public record DeliveryHealthLogItem(
            Long id,
            Integer rangeDays,
            String healthLevel,
            String previousHealthLevel,
            boolean levelChanged,
            boolean escalated,
            boolean suppressed,
            boolean warningNotified,
            boolean recoveryNotified,
            String reason,
            double deadRate,
            double pendingRate,
            double deadRateThreshold,
            double pendingRateThreshold,
            long totalCount,
            long successCount,
            long pendingCount,
            long deadCount,
            long closedCount,
            long failedCount,
            String suggestion,
            String checkSource,
            LocalDateTime checkTime,
            LocalDateTime createTime
    ) {}

    public record DeliveryHealthTrendSummary(
            int rangeDays,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long totalChecks,
            long healthyCount,
            long warningCount,
            long criticalCount,
            long levelChangedCount,
            long escalatedCount,
            long warningNotifiedCount,
            long recoveryNotifiedCount,
            DeliveryHealthLogItem latest
    ) {}

    public record DeliveryHealthDailyTrendItem(
            String date,
            long totalChecks,
            long healthyCount,
            long warningCount,
            long criticalCount,
            long escalatedCount,
            double avgDeadRate,
            double avgPendingRate,
            double maxDeadRate,
            double maxPendingRate
    ) {}

    public record DeliveryHealthRiskDayItem(
            String date,
            double riskScore,
            String riskLevel,
            String insight,
            long totalChecks,
            long healthyCount,
            long warningCount,
            long criticalCount,
            long escalatedCount,
            double avgDeadRate,
            double avgPendingRate,
            double maxDeadRate,
            double maxPendingRate
    ) {}

    public record DeliveryHealthRiskSummary(
            int rangeDays,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int totalRiskDays,
            long highRiskDays,
            long mediumRiskDays,
            long lowRiskDays,
            double maxRiskScore,
            String maxRiskDate,
            List<String> topHighRiskDates
    ) {}

    public record DeliveryHealthGovernanceAdvice(
            int rangeDays,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String overallRiskLevel,
            List<String> keyFindings,
            List<String> recommendedActions,
            LocalDateTime generatedAt
    ) {}

    public record DeliveryHealthGovernanceReport(
            int rangeDays,
            LocalDateTime generatedAt,
            String overallRiskLevel,
            String content
    ) {}

    public record DeliveryHealthThresholdSuggestion(
            int rangeDays,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long sampleCount,
            double avgDeadRate,
            double avgPendingRate,
            double p90DeadRate,
            double p90PendingRate,
            double currentDeadRateThreshold,
            double currentPendingRateThreshold,
            double recommendedDeadRateThreshold,
            double recommendedPendingRateThreshold,
            String suggestion,
            String confidenceLevel
    ) {}

    public record DeliveryHealthLogStorageSummary(
            int retainDays,
            LocalDateTime cutoffTime,
            long totalCount,
            long expiredCount
    ) {}

    public record DeliveryHealthLogCleanupResult(
            int retainDays,
            int limit,
            int deletedCount,
            LocalDateTime cutoffTime,
            String triggerSource,
            LocalDateTime cleanupTime
    ) {}

    private record HealthTransition(
            String previousLevel,
            String currentLevel,
            boolean levelChanged
    ) {}

    private static class DeliveryException extends RuntimeException {
        private final Integer responseCode;

        private DeliveryException(Integer responseCode, String message) {
            super(message == null ? "delivery failed" : message);
            this.responseCode = responseCode;
        }

        private Integer responseCode() {
            return responseCode;
        }
    }
}
