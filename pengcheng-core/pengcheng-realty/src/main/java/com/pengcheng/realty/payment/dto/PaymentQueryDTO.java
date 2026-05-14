package com.pengcheng.realty.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

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

    /**
     * 可见的申请人 userId 集合（由 controller 层根据当前登录用户的"基础职级 +
     * 付款模块加成"算出后透传给 service）。语义：
     *   null  = 不限（全员可见，admin/chairman/general_manager/finance 等）
     *   空集合 = 一个都看不到（兜底，service 直接返回空页）
     *   非空集合 = 限定为这些 userId 提交的申请
     */
    private Set<Long> allowedApplicantIds;

    public Integer getPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
