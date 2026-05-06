package com.pengcheng.realty.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户到访数据录入 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVisitDTO {

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 实际到访时间
     */
    private LocalDateTime actualVisitTime;

    /**
     * 实际到访人数
     */
    private Integer actualVisitCount;

    /**
     * 接待人员
     */
    private String receptionist;

    /**
     * 备注
     */
    private String remark;
}
