package com.pengcheng.admin.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台「我的待审批」表格行 VO
 *
 * 列规划：申请人 / 类型 / 摘要 / 金额 / 提交时间 / 当前节点 / 操作
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApprovalItemVO {

    /** 业务单 ID（leave_request.id / compensate_request.id / payment_request.id / commission.id） */
    private Long id;

    /** 审批类型：leave / compensate / expense / advance / prepay / commission */
    private String type;

    /** 申请人姓名 */
    private String applicantName;

    /** 摘要描述 */
    private String summary;

    /** 金额（付款/佣金类有值，请假/调休为 null） */
    private BigDecimal amount;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 当前节点名（请假/调休 流程节点名；其他类型为业务节点描述如「财务审批」） */
    private String currentNodeName;
}
