package com.pengcheng.ai.experiment;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.ai.experiment.event.AiExperimentAlertEvent;
import com.pengcheng.ai.experiment.entity.AiExperimentAlertLog;
import com.pengcheng.ai.experiment.mapper.AiExperimentAlertLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 实验异常告警服务（包含抑制策略）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiExperimentAlertService {

    private final AiProperties aiProperties;
    private final AiExperimentAlertLogMapper alertLogMapper;
    private final ObjectMapper objectMapper;
    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    /**
     * 同一去重键的最近一次“已发送告警”时间戳（毫秒）。
     */
    private final ConcurrentHashMap<String, Long> latestAlertEpochMsByKey = new ConcurrentHashMap<>();

    public void notifyAutoRollback(String experimentType,
                                   int failureThreshold,
                                   int cooldownSeconds,
                                   long blockedUntilEpochMs) {
        if (!aiProperties.isExperimentAlertEnabled()) {
            return;
        }
        String normalizedType = normalizeType(experimentType);
        String title = "AI实验自动回滚触发";
        String content = String.format(Locale.ROOT,
                "type=%s, threshold=%d, cooldownSeconds=%d, blockedUntilEpochMs=%d",
                normalizedType, failureThreshold, cooldownSeconds, blockedUntilEpochMs);
        dispatch(new AlertContext(
                "auto_rollback",
                normalizedType,
                "system",
                title,
                content,
                "auto_rollback:" + normalizedType,
                Map.of(
                        "failureThreshold", failureThreshold,
                        "cooldownSeconds", cooldownSeconds,
                        "blockedUntilEpochMs", blockedUntilEpochMs
                )
        ));
    }

    public void notifyManualBlock(String experimentType,
                                  int cooldownSeconds,
                                  long blockedUntilEpochMs) {
        if (!aiProperties.isExperimentAlertEnabled()) {
            return;
        }
        String normalizedType = normalizeType(experimentType);
        String title = "AI实验手动封禁";
        String content = String.format(Locale.ROOT,
                "type=%s, cooldownSeconds=%d, blockedUntilEpochMs=%d",
                normalizedType, cooldownSeconds, blockedUntilEpochMs);
        dispatch(new AlertContext(
                "manual_block",
                normalizedType,
                "manual",
                title,
                content,
                "manual_block:" + normalizedType,
                Map.of(
                        "cooldownSeconds", cooldownSeconds,
                        "blockedUntilEpochMs", blockedUntilEpochMs
                )
        ));
    }

    public List<AlertLogItem> listRecentAlerts(Integer limit, Boolean includeSuppressed) {
        int safeLimit = normalizeLimit(limit);
        QueryWrapper<AiExperimentAlertLog> wrapper = new QueryWrapper<>();
        if (Boolean.FALSE.equals(includeSuppressed)) {
            wrapper.eq("suppressed", 0);
        }
        wrapper.orderByDesc("id")
                .last("limit " + safeLimit);
        return alertLogMapper.selectList(wrapper).stream()
                .map(this::toAlertLogItem)
                .toList();
    }

    public AlertSuppressionState resetSuppressionState() {
        int keyCount = latestAlertEpochMsByKey.size();
        latestAlertEpochMsByKey.clear();
        return new AlertSuppressionState(keyCount, normalizeSuppressSeconds());
    }

    private void dispatch(AlertContext context) {
        long now = System.currentTimeMillis();
        long suppressMillis = normalizeSuppressSeconds() * 1000L;
        AtomicBoolean suppressed = new AtomicBoolean(false);
        AtomicLong suppressedUntilEpochMs = new AtomicLong(0L);

        if (suppressMillis > 0L) {
            latestAlertEpochMsByKey.compute(context.dedupeKey(), (key, previousEpochMs) -> {
                if (previousEpochMs != null && (now - previousEpochMs) < suppressMillis) {
                    suppressed.set(true);
                    suppressedUntilEpochMs.set(previousEpochMs + suppressMillis);
                    return previousEpochMs;
                }
                suppressedUntilEpochMs.set(now + suppressMillis);
                return now;
            });
        } else {
            latestAlertEpochMsByKey.put(context.dedupeKey(), now);
        }

        if (!suppressed.get()) {
            log.warn("AI实验异常告警: type={}, alertType={}, content={}",
                    context.experimentType(), context.alertType(), context.content());
        } else {
            log.info("AI实验告警已抑制: key={}, suppressUntilEpochMs={}",
                    context.dedupeKey(), suppressedUntilEpochMs.get());
        }

        AiExperimentAlertLog entity = new AiExperimentAlertLog();
        entity.setAlertType(context.alertType());
        entity.setExperimentType(context.experimentType());
        entity.setTriggerSource(context.triggerSource());
        entity.setTitle(context.title());
        entity.setContent(context.content());
        entity.setDedupeKey(context.dedupeKey());
        entity.setSuppressed(suppressed.get() ? 1 : 0);
        entity.setSuppressedUntilEpochMs(suppressedUntilEpochMs.get() > 0L ? suppressedUntilEpochMs.get() : null);
        entity.setMetadataJson(toJson(context.metadata()));
        entity.setCreateTime(LocalDateTime.now());
        alertLogMapper.insert(entity);
        if (!suppressed.get() && eventPublisher != null) {
            eventPublisher.publishEvent(new AiExperimentAlertEvent(
                    entity.getId(),
                    entity.getAlertType(),
                    entity.getExperimentType(),
                    entity.getTriggerSource(),
                    entity.getTitle(),
                    entity.getContent(),
                    entity.getDedupeKey(),
                    entity.getMetadataJson(),
                    entity.getCreateTime()
            ));
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    private int normalizeSuppressSeconds() {
        return Math.max(0, aiProperties.getExperimentAlertSuppressSeconds());
    }

    private String normalizeType(String experimentType) {
        if (!StringUtils.hasText(experimentType)) {
            return "unknown";
        }
        return experimentType.trim().toLowerCase(Locale.ROOT);
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private AlertLogItem toAlertLogItem(AiExperimentAlertLog item) {
        return new AlertLogItem(
                item.getId(),
                item.getAlertType(),
                item.getExperimentType(),
                item.getTriggerSource(),
                item.getTitle(),
                item.getContent(),
                item.getDedupeKey(),
                Integer.valueOf(1).equals(item.getSuppressed()),
                item.getSuppressedUntilEpochMs(),
                item.getMetadataJson(),
                item.getCreateTime()
        );
    }

    private record AlertContext(
            String alertType,
            String experimentType,
            String triggerSource,
            String title,
            String content,
            String dedupeKey,
            Map<String, Object> metadata
    ) {}

    public record AlertLogItem(
            Long id,
            String alertType,
            String experimentType,
            String triggerSource,
            String title,
            String content,
            String dedupeKey,
            boolean suppressed,
            Long suppressedUntilEpochMs,
            String metadataJson,
            LocalDateTime createTime
    ) {}

    public record AlertSuppressionState(
            int clearedKeyCount,
            int suppressSeconds
    ) {}
}
