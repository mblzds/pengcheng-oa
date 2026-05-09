package com.pengcheng.hr.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批流节点配置 VO（用于后台配置 UI）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowNodeVO {

    private Long id;
    private String businessType;
    private Integer seq;
    private String nodeName;
    private String approverType;
    private String approverValue;
    /** 适用申请人角色 ID 列表（逗号分隔）；空 = 全员适用 */
    private String appliesToRoleIds;
    private Integer enabled;
}
