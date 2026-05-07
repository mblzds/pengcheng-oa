package com.pengcheng.hr.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批流节点模板
 * 按 business_type + seq 顺序组成完整流程
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_flow_node")
public class ApprovalFlowNode extends BaseEntity {

    /** 业务类型：leave 请假 / compensate 调休 */
    private String businessType;

    /** 节点顺序，从 1 开始 */
    private Integer seq;

    /** 节点显示名（直接上级 / HR 审批 ...） */
    private String nodeName;

    /** 审批人解析方式：direct_supervisor / role / user */
    private String approverType;

    /** role/user 时填 ID 列表（逗号分隔）；direct_supervisor 时为 NULL */
    private String approverValue;

    /** 0 禁用 1 启用 */
    private Integer enabled;
}
