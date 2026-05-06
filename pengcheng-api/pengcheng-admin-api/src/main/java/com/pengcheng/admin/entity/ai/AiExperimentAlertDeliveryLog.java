package com.pengcheng.admin.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 实验告警投递日志实体。
 */
@Data
@TableName("ai_experiment_alert_delivery_log")
public class AiExperimentAlertDeliveryLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long alertLogId;

    private String alertType;

    private String alertLevel;

    private String channel;

    private String targetValue;

    private String status;

    private Integer attemptCount;

    private Integer maxAttempts;

    private LocalDateTime nextRetryTime;

    private Integer lastResponseCode;

    private String lastErrorMessage;

    private String payloadJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
