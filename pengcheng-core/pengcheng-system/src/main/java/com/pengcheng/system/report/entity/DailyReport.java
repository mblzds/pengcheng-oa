package com.pengcheng.system.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 日报实体
 */
@Data
@TableName("sys_daily_report")
public class DailyReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate reportDate;
    private String summary;
    private Integer customerFollowUp;
    private Integer newCustomers;
    private Integer dealCount;
    private BigDecimal dealAmount;
    private BigDecimal paymentReceived;
    private String attendanceStatus;
    private String chatSummary;
    private Integer todoCompleted;
    private Integer todoPending;
    private String aiSuggestions;
    private Integer pushed;
    private LocalDateTime createdAt;
}
