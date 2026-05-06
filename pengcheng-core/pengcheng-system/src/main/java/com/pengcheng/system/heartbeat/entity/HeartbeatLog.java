package com.pengcheng.system.heartbeat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 巡检记录
 */
@Data
@TableName("sys_ai_heartbeat_log")
public class HeartbeatLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 巡检类型：customer_followup / commission / contract / overdue */
    private String checkType;

    private Long userId;

    private Long targetId;

    /** 业务类型：customer / commission / contract */
    private String targetType;

    /** 严重程度：info / warn / critical */
    private String severity;

    private String title;

    private String content;

    private String suggestion;

    private Boolean handled;

    private LocalDateTime handledAt;

    private LocalDateTime createdAt;
}
