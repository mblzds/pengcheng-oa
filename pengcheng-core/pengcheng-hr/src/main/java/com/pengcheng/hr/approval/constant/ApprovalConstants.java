package com.pengcheng.hr.approval.constant;

/**
 * 审批流相关常量
 */
public final class ApprovalConstants {

    private ApprovalConstants() {}

    // 业务类型
    public static final String BUSINESS_TYPE_LEAVE = "leave";
    public static final String BUSINESS_TYPE_COMPENSATE = "compensate";
    public static final String BUSINESS_TYPE_EXPENSE = "expense";
    public static final String BUSINESS_TYPE_ADVANCE = "advance";
    public static final String BUSINESS_TYPE_PREPAY = "prepay";

    // 审批人解析方式
    public static final String APPROVER_TYPE_DIRECT_SUPERVISOR = "direct_supervisor";
    /** 申请人所在部门的负责人（自我排除 + 沿祖先回溯）；和 direct_supervisor 区别在于
     *  忽略 user.leader_id（避免被跨部门汇报的"私有上级"覆盖），始终走部门负责人线 */
    public static final String APPROVER_TYPE_APPLICANT_DEPT_MANAGER = "applicant_dept_manager";
    public static final String APPROVER_TYPE_ROLE = "role";
    public static final String APPROVER_TYPE_USER = "user";

    // 节点审批结果
    public static final int RESULT_APPROVED = 1;
    public static final int RESULT_REJECTED = 2;
    /** 申请人主动撤销时，把所有 result IS NULL 的节点终态化为此值；
     *  审批人待办列表过滤 result IS NULL，因此被撤销的节点不会再出现在任何人的待审批入口 */
    public static final int RESULT_CANCELLED = 3;

    // 业务单整体状态（leave_request.status / realty_compensate_request.status）
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_APPROVED = 2;
    public static final int STATUS_REJECTED = 3;
    /** 申请人在审批完成前主动撤销 */
    public static final int STATUS_CANCELLED = 4;
}
