package com.pengcheng.admin.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 实验告警投递健康巡检日志实体。
 */
@Data
@TableName("ai_experiment_alert_delivery_health_log")
public class AiExperimentAlertDeliveryHealthLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer rangeDays;

    private String healthLevel;

    private String previousHealthLevel;

    private Integer levelChanged;

    private Integer escalated;

    private Integer suppressed;

    private Integer warningNotified;

    private Integer recoveryNotified;

    private String reason;

    private BigDecimal deadRate;

    private BigDecimal pendingRate;

    private BigDecimal deadRateThreshold;

    private BigDecimal pendingRateThreshold;

    private Long totalCount;

    private Long successCount;

    private Long pendingCount;

    private Long deadCount;

    private Long closedCount;

    private Long failedCount;

    private String suggestion;

    private String checkSource;

    private LocalDateTime checkTime;

    private LocalDateTime createTime;
}
