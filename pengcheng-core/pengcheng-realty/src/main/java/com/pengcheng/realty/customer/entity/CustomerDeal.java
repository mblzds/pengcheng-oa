package com.pengcheng.realty.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户成交记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("customer_deal")
public class CustomerDeal extends BaseEntity {

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

    /**
     * 网签状态：0-未网签 1-已网签
     */
    private Integer onlineSignStatus;

    /**
     * 备案状态：0-未备案 1-已备案
     */
    private Integer filingStatus;

    /**
     * 贷款状态：0-未申请 1-审批中 2-已放款 3-已拒绝
     */
    private Integer loanStatus;

    /**
     * 回款状态：0-未回款 1-部分回款 2-全部回款
     */
    private Integer paymentStatus;
}
