package com.pengcheng.realty.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 佣金审核 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionAuditDTO {

    /** 佣金ID（必填） */
    private Long commissionId;

    /** 是否通过（必填） */
    private Boolean approved;

    /** 审核备注（驳回时必填） */
    private String remark;

    /** 审核人ID */
    private Long auditorId;
}
