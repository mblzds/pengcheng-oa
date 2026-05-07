package com.pengcheng.hr.approval.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 审批流终态事件
 * 整单状态从「审批中」翻转为「通过」或「驳回」时由引擎发布，
 * 业务方按需订阅（如考勤豁免联动）。
 */
@Getter
public class ApprovalFinalizedEvent extends ApplicationEvent {

    private final String businessType;
    private final Long businessId;
    /** true=整单通过 / false=驳回 */
    private final boolean approved;

    public ApprovalFinalizedEvent(Object source, String businessType, Long businessId, boolean approved) {
        super(source);
        this.businessType = businessType;
        this.businessId = businessId;
        this.approved = approved;
    }
}
