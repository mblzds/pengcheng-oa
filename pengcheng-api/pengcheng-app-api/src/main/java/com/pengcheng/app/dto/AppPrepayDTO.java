package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 预付佣申请请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPrepayDTO {

    /** 联盟商ID */
    private Long allianceId;

    /** 关联成交记录ID */
    private Long dealId;

    /** 预付金额 */
    private BigDecimal amount;

    /** 预付原因 */
    private String reason;
}
