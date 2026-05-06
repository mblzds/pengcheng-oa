package com.pengcheng.realty.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公海池回收规则配置 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolRecycleConfigDTO {

    /**
     * 无跟进天数（超过此天数回收至公海池，默认 7 天）
     */
    private Integer noFollowDays;

    /**
     * 未到访天数（超过此天数回收至公海池，默认 30 天）
     */
    private Integer noVisitDays;

    /**
     * 保护期天数（领取后保护期，默认 3 天）
     */
    private Integer protectionDays;

    /**
     * 是否启用自动回收（默认 true）
     */
    private Boolean autoRecycleEnabled;
}
