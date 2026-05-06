package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申请页成交记录选项 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealOptionVO {

    private Long id;
    private Long customerId;
    private String customerName;
    private String roomNo;
    private BigDecimal dealAmount;
    private LocalDateTime dealTime;
}
