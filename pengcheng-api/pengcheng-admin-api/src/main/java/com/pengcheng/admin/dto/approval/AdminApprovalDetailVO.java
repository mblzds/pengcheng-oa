package com.pengcheng.admin.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台审批详情 VO（详情抽屉/弹窗使用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApprovalDetailVO {

    private Long id;
    private String type;
    private String applicantName;
    private String summary;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime applyTime;
    private List<ApprovalHistory> histories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalHistory {
        private String nodeName;
        private String approverName;
        /** 1=通过 2=驳回 NULL=待审批 */
        private Integer result;
        private String remark;
        private LocalDateTime approvalTime;
    }
}
