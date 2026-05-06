package com.pengcheng.realty.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 报备-到访-成交转化漏斗 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunnelVO {

    /** 报备数 */
    private Long reportCount;

    /** 到访数 */
    private Long visitCount;

    /** 成交数 */
    private Long dealCount;

    /** 报备→到访转化率 */
    private BigDecimal reportToVisitRate;

    /** 到访→成交转化率 */
    private BigDecimal visitToDealRate;

    /** 报备→成交转化率 */
    private BigDecimal reportToDealRate;
}
