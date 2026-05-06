package com.pengcheng.admin.schedule;

import com.pengcheng.admin.service.ai.AiExperimentAlertDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI 实验告警投递重试任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiExperimentAlertDeliveryRetryScheduler {

    private final AiExperimentAlertDeliveryService deliveryService;

    @Scheduled(initialDelay = 20000, fixedDelay = 30000)
    public void retryDueDeliveries() {
        AiExperimentAlertDeliveryService.RetrySummary summary = deliveryService.retryDueDeliveries(20, false);
        if (summary.pickedCount() > 0) {
            log.info("AI实验告警投递重试: picked={}, success={}, dead={}, pending={}",
                    summary.pickedCount(), summary.successCount(), summary.deadCount(), summary.pendingCount());
        }
    }
}
