package com.pengcheng.realty.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 佣金明细 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionDetailDTO {

    /** 基础佣金 */
    private BigDecimal baseCommission;

    /** 跳点佣金 */
    private BigDecimal jumpPointCommission;

    /** 现金奖 */
    private BigDecimal cashReward;

    /** 开单奖 */
    private BigDecimal firstDealReward;

    /** 平台奖励 */
    private BigDecimal platformReward;
}
