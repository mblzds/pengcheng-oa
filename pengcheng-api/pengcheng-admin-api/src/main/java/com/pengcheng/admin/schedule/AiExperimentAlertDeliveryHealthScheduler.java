package com.pengcheng.admin.schedule;

import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.admin.service.ai.AiExperimentAlertDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI 实验告警投递健康度巡检任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiExperimentAlertDeliveryHealthScheduler {

    private final AiExperimentAlertDeliveryService deliveryService;
    private final AiProperties aiProperties;

    @Scheduled(initialDelay = 45000, fixedDelay = 60000)
    public void inspectDeliveryHealth() {
        if (!aiProperties.isExperimentAlertDeliveryHealthCheckEnabled()) {
            return;
        }
        AiExperimentAlertDeliveryService.DeliveryHealthCheckResult result =
                deliveryService.checkAndEscalateDeliveryHealth(
                        aiProperties.getExperimentAlertDeliveryHealthCheckDays(),
                        aiProperties.getExperimentAlertDeliveryHealthDeadRateThreshold(),
                        aiProperties.getExperimentAlertDeliveryHealthPendingRateThreshold(),
                        false
                );
        AiExperimentAlertDeliveryService.DeliveryHealthSummary summary = result.healthSummary();
        if (result.escalated() || result.suppressed() || result.levelChanged()) {
            log.warn("AI实验告警投递健康巡检: prev={}, curr={}, changed={}, reason={}, escalated={}, suppressed={}, warnNotified={}, recoveryNotified={}, total={}, dead={}, pending={}",
                    result.previousHealthLevel(),
                    result.currentHealthLevel(),
                    result.levelChanged(),
                    result.reason(),
                    result.escalated(),
                    result.suppressed(),
                    result.warningNotified(),
                    result.recoveryNotified(),
                    summary.totalCount(),
                    summary.deadCount(),
                    summary.pendingCount());
        }
    }
}
