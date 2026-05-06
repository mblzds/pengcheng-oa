package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行审批请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppApproveDTO {

    /** 是否通过 */
    private Boolean approved;

    /** 审批意见/驳回原因 */
    private String reason;

    /** 审批类型：leave/payment/commission */
    private String type;
}
