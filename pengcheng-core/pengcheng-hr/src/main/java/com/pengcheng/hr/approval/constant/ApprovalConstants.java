package com.pengcheng.hr.approval.constant;

/**
 * 审批流相关常量
 */
public final class ApprovalConstants {

    private ApprovalConstants() {}

    // 业务类型
    public static final String BUSINESS_TYPE_LEAVE = "leave";
    public static final String BUSINESS_TYPE_COMPENSATE = "compensate";

    // 审批人解析方式
    public static final String APPROVER_TYPE_DIRECT_SUPERVISOR = "direct_supervisor";
    public static final String APPROVER_TYPE_ROLE = "role";
    public static final String APPROVER_TYPE_USER = "user";

    // 节点审批结果
    public static final int RESULT_APPROVED = 1;
    public static final int RESULT_REJECTED = 2;

    // 业务单整体状态（leave_request.status / realty_compensate_request.status）
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_APPROVED = 2;
    public static final int STATUS_REJECTED = 3;
}
