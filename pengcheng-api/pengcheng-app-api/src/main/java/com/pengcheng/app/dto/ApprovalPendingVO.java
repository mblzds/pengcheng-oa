package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 待审批列表聚合响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalPendingVO {

    /** 请假/调休审批项 */
    private List<ApprovalItem> leaveItems;

    /** 付款申请审批项 */
    private List<ApprovalItem> paymentItems;

    /** 佣金审核项 */
    private List<ApprovalItem> commissionItems;

    /** 待审批总数 */
    private Integer totalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalItem {
        /** 业务ID */
        private Long id;
        /** 审批类型：leave/compensate/expense/advance/prepay/commission */
        private String type;
        /** 申请人姓名 */
        private String applicantName;
        /** 摘要描述 */
        private String summary;
        /** 金额（付款/佣金类有值） */
        private BigDecimal amount;
        /** 申请时间 */
        private LocalDateTime applyTime;
    }
}
