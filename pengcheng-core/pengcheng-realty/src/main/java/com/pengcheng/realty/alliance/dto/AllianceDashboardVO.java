package com.pengcheng.realty.alliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 联盟商业务概览 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllianceDashboardVO {

    /** 本月报备数 */
    private Long monthlyReportCount;

    /** 本月到访数 */
    private Long monthlyVisitCount;

    /** 本月成交数 */
    private Long monthlyDealCount;

    /** 待结佣金额 */
    private BigDecimal pendingCommissionAmount;
}
