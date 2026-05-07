package com.pengcheng.hr.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批流转实际记录
 * 每条业务申请按提交时模板快照实例化为 N 行节点
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_record_node")
public class ApprovalRecordNode extends BaseEntity {

    /** 业务类型 */
    private String businessType;

    /** 关联业务单 ID（leave_request.id 或 realty_compensate_request.id） */
    private Long businessId;

    /** 节点顺序 */
    private Integer seq;

    /** 节点名（快照） */
    private String nodeName;

    /** 候选审批人 ID 列表（逗号分隔，快照） */
    private String candidateApproverIds;

    /** 实际执行审批的用户 ID（NULL 表示尚未审批） */
    private Long approverId;

    /** 1 通过 / 2 驳回 / NULL 待审批 */
    private Integer result;

    /** 审批备注 */
    private String remark;

    /** 审批时间 */
    private LocalDateTime approvalTime;
}
