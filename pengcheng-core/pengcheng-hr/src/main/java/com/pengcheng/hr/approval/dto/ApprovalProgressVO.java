package com.pengcheng.hr.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批流转进度 VO（用于申请详情/审批详情页面展示完整时间线）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalProgressVO {

    private String businessType;
    private Long businessId;
    /** 1 审批中 / 2 通过 / 3 驳回 */
    private Integer overallStatus;
    /** 节点链（已审批 + 当前节点 + 未来节点） */
    private List<NodeView> nodes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeView {
        private Integer seq;
        private String nodeName;
        /** 候选审批人姓名列表（用于 UI 展示） */
        private List<String> candidateApproverNames;
        /** 实际审批人姓名（已审批节点） */
        private String approverName;
        /** 1 通过 / 2 驳回 / NULL 待审批 */
        private Integer result;
        private String remark;
        private LocalDateTime approvalTime;
    }
}
