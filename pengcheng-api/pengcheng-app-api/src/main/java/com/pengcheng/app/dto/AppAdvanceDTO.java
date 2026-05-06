package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 垫佣申请请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppAdvanceDTO {

    /** 关联成交记录ID */
    private Long dealId;

    /** 垫佣金额 */
    private BigDecimal amount;

    /** 垫佣原因 */
    private String reason;
}
