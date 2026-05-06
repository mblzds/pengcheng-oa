package com.pengcheng.realty.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户公海池事件日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("customer_pool_event_log")
public class CustomerPoolEventLog {

    public static final String EVENT_TYPE_CLAIM = "claim";
    public static final String EVENT_TYPE_RECYCLE = "recycle";
    public static final String EVENT_SOURCE_MANUAL = "manual";
    public static final String EVENT_SOURCE_AUTO = "auto";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String eventType;

    private String eventSource;

    private Long operatorId;

    private LocalDateTime eventTime;

    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;
}
