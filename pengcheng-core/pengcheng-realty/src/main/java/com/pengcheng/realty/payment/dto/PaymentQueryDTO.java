package com.pengcheng.realty.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 付款申请查询 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQueryDTO {

    /** 当前页 */
    private Integer page;

    /** 每页条数 */
    private Integer pageSize;

    /** 申请类型：1-费用报销 2-垫佣 3-预付佣 */
    private Integer requestType;

    /** 审批状态：1-待审批 2-审批中 3-已通过 4-已驳回 */
    private Integer status;

    /** 申请人ID */
    private Long applicantId;

    public Integer getPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
