package com.pengcheng.realty.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 仪表盘核心指标概览 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewVO {

    /** 报备数 */
    private Long reportCount;

    /** 到访数 */
    private Long visitCount;

    /** 成交数 */
    private Long dealCount;

    /** 成交金额 */
    private BigDecimal dealAmount;

    /** 应收佣金 */
    private BigDecimal receivableCommission;

    /** 已结佣金 */
    private BigDecimal settledCommission;

    /** 今日成交数 */
    private Integer todayDealCount;

    /** 总成交金额（当月） */
    private BigDecimal totalDealAmount;

    /** 待跟进客户数 */
    private Integer pendingFollowUp;

    /** 待审批数 */
    private Integer pendingApproval;

    /** 成交数环比趋势（百分比） */
    private Integer dealCountTrend;

    /** 成交金额环比趋势（百分比） */
    private Integer dealAmountTrend;
}
