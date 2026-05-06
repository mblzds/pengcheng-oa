package com.pengcheng.realty.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 付款申请实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("payment_request")
public class PaymentRequest extends BaseEntity {

    /**
     * 业务订单号（对外，传递给支付通道）
     */
    private String orderNo;

    /**
     * 支付渠道：alipay / wechat / offline
     */
    private String payChannel;

    /**
     * 第三方交易号
     */
    private String thirdTradeNo;

    /**
     * 支付状态：0-未付款 1-付款中 2-已付款 3-已退款 4-失败
     */
    private Integer payStatus;

    /**
     * 实际支付完成时间
     */
    private LocalDateTime paidTime;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请类型：1-费用报销 2-垫佣 3-预付佣
     */
    private Integer requestType;

    /**
     * 报销类型：1-交通费 2-餐饮费 3-住宿费 4-办公用品 5-其他
     */
    private Integer expenseType;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 说明
     */
    private String description;

    /**
     * 关联成交记录ID
     */
    private Long relatedDealId;

    /**
     * 关联联盟商ID
     */
    private Long relatedAllianceId;

    /**
     * 附件路径JSON
     */
    private String attachments;

    /**
     * 审批状态：1-待审批 2-审批中 3-已通过 4-已驳回
     */
    private Integer status;
}
