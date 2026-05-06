package com.pengcheng.realty.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 佣金录入 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionCreateDTO {

    /** 关联成交记录ID（必填） */
    private Long dealId;

    /** 项目ID（必填） */
    private Long projectId;

    /** 联盟商ID（必填） */
    private Long allianceId;

    /** 应收佣金（必填） */
    private BigDecimal receivableAmount;

    /** 应结佣金（必填） */
    private BigDecimal payableAmount;

    /** 公司平台费（必填） */
    private BigDecimal platformFee;

    /** 佣金明细 */
    private CommissionDetailDTO detail;
}
