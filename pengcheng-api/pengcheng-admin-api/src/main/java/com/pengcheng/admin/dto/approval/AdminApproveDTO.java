package com.pengcheng.admin.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行审批请求 DTO（后台）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApproveDTO {

    /** 审批类型：leave / compensate / expense / advance / prepay / commission */
    private String type;

    /** true=通过 false=驳回 */
    private Boolean approved;

    /** 审批意见 / 驳回原因（驳回必填） */
    private String reason;
}
