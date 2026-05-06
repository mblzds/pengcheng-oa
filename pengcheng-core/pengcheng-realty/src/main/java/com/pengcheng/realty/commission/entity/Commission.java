package com.pengcheng.realty.commission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 佣金主表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("commission")
public class Commission extends BaseEntity {

    /**
     * 关联成交记录ID
     */
    private Long dealId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 联盟商ID
     */
    private Long allianceId;

    /**
     * 应收佣金
     */
    private BigDecimal receivableAmount;

    /**
     * 应结佣金
     */
    private BigDecimal payableAmount;

    /**
     * 公司平台费
     */
    private BigDecimal platformFee;

    /**
     * 审核状态：1-待审核 2-审核通过 3-审核驳回
     */
    private Integer auditStatus;

    /**
     * 审核备注
     */
    private String auditRemark;

    /**
     * 审核人ID
     */
    private Long auditorId;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;
}
