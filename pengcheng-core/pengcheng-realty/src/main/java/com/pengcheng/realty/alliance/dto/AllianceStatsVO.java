package com.pengcheng.realty.alliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 联盟商运营数据统计 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllianceStatsVO {

    /** 联盟商ID */
    private Long allianceId;

    /** 联盟公司名称 */
    private String companyName;

    /** 上客数量（报备客户总数） */
    private Long customerCount;

    /** 成交数量 */
    private Long dealCount;

    /** 成交业绩（成交金额合计） */
    private BigDecimal dealAmount;

    /** 已结佣金额 */
    private BigDecimal settledCommission;

    /** 待结佣金额 */
    private BigDecimal pendingCommission;

    /** 派车费用 */
    private BigDecimal transportExpense;

    /** 推广费用 */
    private BigDecimal promotionExpense;

    /** 宴请费用 */
    private BigDecimal entertainmentExpense;

    /** 渠道走访次数 */
    private Long channelVisitCount;
}
