package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批详情响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDetailVO {

    /** 业务ID */
    private Long id;

    /** 审批类型：leave/compensate/expense/advance/prepay/commission */
    private String type;

    /** 申请人姓名 */
    private String applicantName;

    /** 摘要描述 */
    private String summary;

    /** 金额 */
    private BigDecimal amount;

    /** 审批状态：1-待审批 2-审批中 3-已通过 4-已驳回 */
    private Integer status;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审批流转历史时间线 */
    private List<ApprovalHistory> histories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalHistory {
        /** 审批人姓名 */
        private String approverName;
        /** 审批结果：1-通过 2-驳回 */
        private Integer result;
        /** 审批备注 */
        private String remark;
        /** 审批时间 */
        private LocalDateTime approvalTime;
    }
}
