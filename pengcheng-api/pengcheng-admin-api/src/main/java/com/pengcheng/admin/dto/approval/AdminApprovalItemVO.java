package com.pengcheng.admin.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台「我的待审批」表格行 VO
 *
 * 通用字段（所有类型都有）：id / type / applicantName / applyTime / currentNodeName / summary
 * 假期专属字段（leave / compensate 才有值）：leaveTypeLabel / dateRange / days / reason
 * 付款专属字段（expense / advance / prepay / commission 才有值）：amount
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApprovalItemVO {

    private Long id;
    private String type;
    private String applicantName;
    private String summary;
    private BigDecimal amount;
    private LocalDateTime applyTime;
    private String currentNodeName;

    /** 假期类型显示名（病假/事假/年假/调休...）；非假期类型为 null */
    private String leaveTypeLabel;
    /** 起止/日期文案（请假: "5/9 09:00~5/11 18:00"；调休: "5/9"）；非假期类型为 null */
    private String dateRange;
    /** 天数；非假期类型为 null */
    private Double days;
    /** 申请原因；付款类摘要里已包含金额，仅假期类填此字段 */
    private String reason;
}
