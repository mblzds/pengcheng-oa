package com.pengcheng.realty.commission.dto;

import com.pengcheng.realty.commission.entity.Commission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 佣金展示 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionVO {

    private Long id;
    private Long dealId;
    private Long projectId;
    private Long allianceId;
    private BigDecimal receivableAmount;
    private BigDecimal payableAmount;
    private BigDecimal platformFee;

    /** 审核状态：1-待审核 2-审核通过 3-审核驳回 */
    private Integer auditStatus;
    private String auditRemark;
    private Long auditorId;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;

    /** 佣金明细 */
    private CommissionDetailDTO detail;

    public static CommissionVO fromEntity(Commission commission) {
        if (commission == null) {
            return null;
        }
        return CommissionVO.builder()
                .id(commission.getId())
                .dealId(commission.getDealId())
                .projectId(commission.getProjectId())
                .allianceId(commission.getAllianceId())
                .receivableAmount(commission.getReceivableAmount())
                .payableAmount(commission.getPayableAmount())
                .platformFee(commission.getPlatformFee())
                .auditStatus(commission.getAuditStatus())
                .auditRemark(commission.getAuditRemark())
                .auditorId(commission.getAuditorId())
                .auditTime(commission.getAuditTime())
                .createTime(commission.getCreateTime())
                .build();
    }
}
