package com.pengcheng.realty.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 付款申请 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {

    /** 申请人ID */
    private Long applicantId;

    /** 申请类型：1-费用报销 2-垫佣 3-预付佣 */
    private Integer requestType;

    /** 报销类型：1-交通费 2-餐饮费 3-住宿费 4-办公用品 5-其他（费用报销时必填） */
    private Integer expenseType;

    /** 金额（必填） */
    private BigDecimal amount;

    /** 说明 */
    private String description;

    /** 关联成交记录ID（垫佣/预付佣时必填） */
    private Long relatedDealId;

    /** 关联联盟商ID（预付佣时必填） */
    private Long relatedAllianceId;

    /** 附件路径JSON */
    private String attachments;

    /** 费用发生时间（费用报销时填写） */
    private LocalDateTime occurTime;
}
