package com.pengcheng.realty.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成交后续手续更新 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDealUpdateDTO {

    /**
     * 成交记录ID
     */
    private Long dealId;

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
