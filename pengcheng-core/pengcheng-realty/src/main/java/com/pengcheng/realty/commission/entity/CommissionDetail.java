package com.pengcheng.realty.commission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 佣金明细实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("commission_detail")
public class CommissionDetail extends BaseEntity {

    /**
     * 佣金ID
     */
    private Long commissionId;

    /**
     * 基础佣金
     */
    private BigDecimal baseCommission;

    /**
     * 跳点佣金
     */
    private BigDecimal jumpPointCommission;

    /**
     * 现金奖
     */
    private BigDecimal cashReward;

    /**
     * 开单奖
     */
    private BigDecimal firstDealReward;

    /**
     * 平台奖励
     */
    private BigDecimal platformReward;
}
