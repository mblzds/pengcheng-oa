package com.pengcheng.hr.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.hr.approval.entity.GeneralApprovalRequest;

/**
 * 通用审批申请服务（Phase 2 of 通用流水线）。
 * 负责非内置 business_type 的提交、撤销、审批后置回写。
 * 节点流转仍由 ApprovalFlowService 引擎驱动。
 */
public interface GeneralApprovalService {

    /**
     * 提交一条通用审批申请并启动审批流。
     *
     * @param businessType 必须是 approval_business_type 表里 builtin=0 的有效 key
     * @return 新建申请 ID
     * @throws IllegalArgumentException 业务类型无效 / 是内置类型 / 标题缺失
     * @throws IllegalStateException    该业务类型未配置审批流模板
     */
    Long submit(String businessType, Long applicantId, String title, String description);

    /**
     * 申请人主动撤销。仅 status=审批中 时允许；引擎里未审批节点同步终态化。
     */
    void cancel(Long requestId, Long applicantId);

    /**
     * 审批：把当前节点 approve/reject 后由引擎剩余节点状态推导整单 status，回写。
     */
    void approve(Long requestId, Long approverId, boolean approved, String remark);

    GeneralApprovalRequest getById(Long id);

    /**
     * 申请人侧分页：按 status / businessType 过滤；按 createTime DESC。
     */
    IPage<GeneralApprovalRequest> pageMine(Long applicantId, String businessType, Integer status,
                                           long page, long pageSize);
}
