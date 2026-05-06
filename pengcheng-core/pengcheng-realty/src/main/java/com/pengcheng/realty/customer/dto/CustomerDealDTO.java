package com.pengcheng.realty.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户成交数据录入 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDealDTO {

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 成交房号
     */
    private String roomNo;

    /**
     * 成交金额
     */
    private BigDecimal dealAmount;

    /**
     * 成交时间
     */
    private LocalDateTime dealTime;

    /**
     * 签约状态：1-已签约 2-未签约
     */
    private Integer signStatus;

    /**
     * 认购类型：1-小订 2-大定
     */
    private Integer subscribeType;
}
