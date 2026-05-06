package com.pengcheng.realty.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户到访记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("customer_visit")
public class CustomerVisit extends BaseEntity {

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
