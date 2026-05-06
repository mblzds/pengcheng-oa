package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户详情聚合响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetailVO {

    /** 报备信息 */
    private ReportInfo reportInfo;

    /** 到访记录列表 */
    private List<VisitRecord> visits;

    /** 成交信息（可为null） */
    private DealInfo deal;

    /** 跟进历史时间线 */
    private List<TimelineItem> timeline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportInfo {
        private Long id;
        private String reportNo;
        private String customerName;
        private String phoneMasked;
        private Integer visitCount;
        private LocalDateTime visitTime;
        private String allianceName;
        private String agentName;
        private String agentPhone;
        private Integer status;
        private LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitRecord {
        private Long id;
        private LocalDateTime actualVisitTime;
        private Integer actualVisitCount;
        private String receptionist;
        private String remark;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DealInfo {
        private Long id;
        private String roomNo;
        private BigDecimal dealAmount;
        private LocalDateTime dealTime;
        private Integer signStatus;
        private Integer subscribeType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineItem {
        /** 事件类型：report/visit/deal/follow */
        private String eventType;
        /** 事件描述 */
        private String description;
        /** 事件时间 */
        private LocalDateTime eventTime;
    }
}
