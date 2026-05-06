package com.pengcheng.realty.payment.dto;

import com.pengcheng.realty.payment.entity.PaymentApproval;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 付款申请展示 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVO {

    private Long id;
    private Long applicantId;
    private String orderNo;
    private String payChannel;
    private String thirdTradeNo;
    private Integer payStatus;
    private LocalDateTime paidTime;

    /** 申请类型：1-费用报销 2-垫佣 3-预付佣 */
    private Integer requestType;

    /** 报销类型：1-交通费 2-餐饮费 3-住宿费 4-办公用品 5-其他 */
    private Integer expenseType;

    private BigDecimal amount;
    private String description;
    private Long relatedDealId;
    private Long relatedAllianceId;
    private String attachments;

    /** 审批状态：1-待审批 2-审批中 3-已通过 4-已驳回 */
    private Integer status;

    private LocalDateTime createTime;

    /** 审批记录列表 */
    private List<PaymentApproval> approvals;

    public static PaymentVO fromEntity(PaymentRequest entity) {
        if (entity == null) {
            return null;
        }
        return PaymentVO.builder()
                .id(entity.getId())
                .applicantId(entity.getApplicantId())
                .orderNo(entity.getOrderNo())
                .payChannel(entity.getPayChannel())
                .thirdTradeNo(entity.getThirdTradeNo())
                .payStatus(entity.getPayStatus())
                .paidTime(entity.getPaidTime())
                .requestType(entity.getRequestType())
                .expenseType(entity.getExpenseType())
                .amount(entity.getAmount())
                .description(entity.getDescription())
                .relatedDealId(entity.getRelatedDealId())
                .relatedAllianceId(entity.getRelatedAllianceId())
                .attachments(entity.getAttachments())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .build();
    }
}
