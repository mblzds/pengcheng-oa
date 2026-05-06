package com.pengcheng.ai.experiment.event;

import java.time.LocalDateTime;

/**
 * AI 实验异常告警事件（仅在未被抑制时发布）。
 */
public record AiExperimentAlertEvent(
        Long alertLogId,
        String alertType,
        String experimentType,
        String triggerSource,
        String title,
        String content,
        String dedupeKey,
        String metadataJson,
        LocalDateTime createTime
) {
}
