package com.pengcheng.ai.experiment;

import com.pengcheng.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A/B 实验保护开关（轻量内存态）：
 * 当实验分支连续失败达到阈值时，自动进入冷却期并回落到控制组。
 */
@Slf4j
@Service
public class AiExperimentGuardService {

    private final AiProperties aiProperties;
    private final AiExperimentAlertService alertService;

    private final AtomicInteger routeFailureCount = new AtomicInteger();
    private final AtomicInteger promptFailureCount = new AtomicInteger();
    private final AtomicLong routeBlockedUntilEpochMs = new AtomicLong(0);
    private final AtomicLong promptBlockedUntilEpochMs = new AtomicLong(0);

    public AiExperimentGuardService(AiProperties aiProperties) {
        this(aiProperties, null);
    }

    @Autowired
    public AiExperimentGuardService(AiProperties aiProperties, AiExperimentAlertService alertService) {
        this.aiProperties = aiProperties;
        this.alertService = alertService;
    }

    public boolean allowRouteExperiment() {
        return allow(routeBlockedUntilEpochMs);
    }

    public boolean allowPromptExperiment() {
        return allow(promptBlockedUntilEpochMs);
    }

    public void onRouteExperimentSuccess() {
        routeFailureCount.set(0);
    }

    public void onPromptExperimentSuccess() {
        promptFailureCount.set(0);
    }

    public void onRouteExperimentFailure() {
        onFailure("route", routeFailureCount, routeBlockedUntilEpochMs);
    }

    public void onPromptExperimentFailure() {
        onFailure("prompt", promptFailureCount, promptBlockedUntilEpochMs);
    }

    public ExperimentGuardStatus currentStatus() {
        return new ExperimentGuardStatus(
                allowRouteExperiment(),
                routeBlockedUntilEpochMs.get(),
                routeFailureCount.get(),
                allowPromptExperiment(),
                promptBlockedUntilEpochMs.get(),
                promptFailureCount.get()
        );
    }

    public void resetRouteGuard() {
        reset(routeFailureCount, routeBlockedUntilEpochMs);
    }

    public void resetPromptGuard() {
        reset(promptFailureCount, promptBlockedUntilEpochMs);
    }

    public void resetAll() {
        resetRouteGuard();
        resetPromptGuard();
    }

    public void blockRouteExperiment(Integer cooldownSeconds) {
        block("route", routeBlockedUntilEpochMs, cooldownSeconds);
    }

    public void blockPromptExperiment(Integer cooldownSeconds) {
        block("prompt", promptBlockedUntilEpochMs, cooldownSeconds);
    }

    private boolean allow(AtomicLong blockedUntilEpochMs) {
        long blockedUntil = blockedUntilEpochMs.get();
        return blockedUntil <= System.currentTimeMillis();
    }

    private void reset(AtomicInteger failureCount, AtomicLong blockedUntilEpochMs) {
        failureCount.set(0);
        blockedUntilEpochMs.set(0);
    }

    private void block(String experimentType, AtomicLong blockedUntilEpochMs, Integer cooldownSeconds) {
        long cooldownMillis = resolveCooldownMillis(cooldownSeconds);
        int cooldownSecondsValue = (int) (cooldownMillis / 1000L);
        long blockedUntil = System.currentTimeMillis() + cooldownMillis;
        blockedUntilEpochMs.set(blockedUntil);
        log.warn("A/B 实验手动封禁: type={}, blockedUntil={}, cooldownSeconds={}",
                experimentType, Instant.ofEpochMilli(blockedUntil), cooldownSecondsValue);
        if (alertService != null) {
            alertService.notifyManualBlock(experimentType, cooldownSecondsValue, blockedUntil);
        }
    }

    private long resolveCooldownMillis(Integer cooldownSeconds) {
        int seconds = cooldownSeconds != null && cooldownSeconds > 0
                ? cooldownSeconds
                : Math.max(1, aiProperties.getExperimentCooldownSeconds());
        return seconds * 1000L;
    }

    private void onFailure(String experimentType, AtomicInteger failureCount, AtomicLong blockedUntilEpochMs) {
        if (!aiProperties.isExperimentAutoRollbackEnabled()) {
            return;
        }
        int threshold = Math.max(1, aiProperties.getExperimentFailureThreshold());
        int failures = failureCount.incrementAndGet();
        if (failures < threshold) {
            return;
        }
        long cooldownMillis = Math.max(1, aiProperties.getExperimentCooldownSeconds()) * 1000L;
        int cooldownSeconds = (int) (cooldownMillis / 1000L);
        long blockedUntil = System.currentTimeMillis() + cooldownMillis;
        blockedUntilEpochMs.set(blockedUntil);
        failureCount.set(0);
        log.warn("A/B 实验自动回滚触发: type={}, blockedUntil={}, cooldownSeconds={}",
                experimentType, Instant.ofEpochMilli(blockedUntil), cooldownSeconds);
        if (alertService != null) {
            alertService.notifyAutoRollback(experimentType, threshold, cooldownSeconds, blockedUntil);
        }
    }

    public record ExperimentGuardStatus(
            boolean routeExperimentAllowed,
            long routeBlockedUntilEpochMs,
            int routeFailureCount,
            boolean promptExperimentAllowed,
            long promptBlockedUntilEpochMs,
            int promptFailureCount
    ) {}
}
