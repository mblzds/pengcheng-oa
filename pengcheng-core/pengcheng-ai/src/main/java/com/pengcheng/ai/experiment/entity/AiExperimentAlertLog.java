package com.pengcheng.ai.experiment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 实验异常告警日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_experiment_alert_log")
public class AiExperimentAlertLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String alertType;

    private String experimentType;

    private String triggerSource;

    private String title;

    private String content;

    private String dedupeKey;

    private Integer suppressed;

    private Long suppressedUntilEpochMs;

    private String metadataJson;

    private LocalDateTime createTime;
}
