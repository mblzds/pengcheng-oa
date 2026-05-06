package com.pengcheng.realty.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 项目佣金规则实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("project_commission_rule")
public class ProjectCommissionRule extends BaseEntity {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 基础佣金比例
     */
    private BigDecimal baseRate;

    /**
     * 跳点规则JSON
     */
    private String jumpPointRules;

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

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 状态：1-生效 2-待审批 3-已失效
     */
    private Integer status;
}
