package com.pengcheng.realty.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 项目佣金规则 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCommissionRuleDTO {

    /** 项目ID（必填） */
    private Long projectId;

    /** 基础佣金比例 */
    private BigDecimal baseRate;

    /** 跳点规则JSON */
    private String jumpPointRules;

    /** 现金奖 */
    private BigDecimal cashReward;

    /** 开单奖 */
    private BigDecimal firstDealReward;

    /** 平台奖励 */
    private BigDecimal platformReward;
}
