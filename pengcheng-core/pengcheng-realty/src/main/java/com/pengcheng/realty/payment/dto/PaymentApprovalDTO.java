package com.pengcheng.realty.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 付款审批 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApprovalDTO {

    /** 付款申请ID（必填） */
    private Long requestId;

    /** 审批人ID（必填） */
    private Long approverId;

    /** 是否通过（必填） */
    private Boolean approved;

    /** 审批备注（驳回时必填） */
    private String remark;
}
