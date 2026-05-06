package com.pengcheng.realty.receivable.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 实际到账流水（对应 receivable_plan 的一期可有多笔）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("receivable_record")
public class ReceivableRecord extends BaseEntity {

    /** 计划分期 ID */
    private Long planId;

    /** 本次到账金额 */
    private BigDecimal amount;

    /** 实际到账日期 */
    private LocalDate paidDate;

    /** 回款方式：1-银行转账 2-支票 3-现金 4-承兑 5-其他 */
    private Integer payWay;

    /** 付款方名称 */
    private String payer;

    /** 凭证号/流水号 */
    private String voucherNo;

    /** 凭证附件 URL */
    private String attachmentUrl;

    /** 备注 */
    private String remark;
}
