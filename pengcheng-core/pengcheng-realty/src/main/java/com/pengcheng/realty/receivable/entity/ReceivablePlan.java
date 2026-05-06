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
 * 回款计划分期（一条 customer_deal 可对应多期）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("receivable_plan")
public class ReceivablePlan extends BaseEntity {

    /** 成交记录 ID */
    private Long dealId;

    /** 期号（从 1 开始） */
    private Integer periodNo;

    /** 期名：首付 / 一期 / 尾款 等 */
    private String periodName;

    /** 应付日期 */
    private LocalDate dueDate;

    /** 应付金额 */
    private BigDecimal dueAmount;

    /** 已付金额（累计） */
    private BigDecimal paidAmount;

    /** 状态：0-未到期 1-待回款 2-部分回款 3-已回款 4-逾期 */
    private Integer status;

    /** 备注 */
    private String remark;

    public static final int STATUS_NOT_DUE = 0;
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_PARTIAL = 2;
    public static final int STATUS_PAID = 3;
    public static final int STATUS_OVERDUE = 4;
}
