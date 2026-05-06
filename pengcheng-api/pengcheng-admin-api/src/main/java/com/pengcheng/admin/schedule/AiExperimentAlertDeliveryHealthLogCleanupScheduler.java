package com.pengcheng.admin.schedule;

import com.pengcheng.admin.service.ai.AiExperimentAlertDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI 实验告警投递健康巡检日志清理任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiExperimentAlertDeliveryHealthLogCleanupScheduler {

    private final AiExperimentAlertDeliveryService deliveryService;

    @Scheduled(initialDelay = 120000, fixedDelay = 3600000)
    public void cleanupExpiredHealthLogs() {
        AiExperimentAlertDeliveryService.DeliveryHealthLogCleanupResult result = deliveryService.cleanupHealthLogs(
                AiExperimentAlertDeliveryService.HEALTH_LOG_RETENTION_DAYS_DEFAULT,
                AiExperimentAlertDeliveryService.HEALTH_LOG_CLEANUP_BATCH_DEFAULT,
                false
        );
        if (result.deletedCount() > 0) {
            log.info("AI实验投递健康巡检日志自动清理: retainDays={}, limit={}, deleted={}",
                    result.retainDays(), result.limit(), result.deletedCount());
        }
    }
}
