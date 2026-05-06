package com.pengcheng.realty.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 公海池领取客户 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolClaimDTO {

    /**
     * 客户 ID（单个领取时使用）
     */
    private Long customerId;

    /**
     * 客户 ID 列表（批量领取时使用）
     */
    private List<Long> customerIds;

    /**
     * 领取人用户 ID
     */
    private Long userId;
}
