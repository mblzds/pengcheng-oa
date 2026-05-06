package com.pengcheng.realty.receivable.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 回款总览统计：应收 / 已收 / 未收 / 逾期
 */
@Data
@Builder
public class ReceivableStatsVO {

    /** 应收总额 */
    private BigDecimal totalDue;
    /** 已收总额 */
    private BigDecimal totalPaid;
    /** 未收总额 */
    private BigDecimal totalUnpaid;
    /** 逾期金额 */
    private BigDecimal totalOverdue;
    /** 逾期期数 */
    private Long overdueCount;
    /** 总期数 */
    private Long totalCount;
}
