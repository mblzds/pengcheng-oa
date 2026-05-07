package com.pengcheng.hr.approval.service;

import com.pengcheng.hr.approval.dto.ApprovalFlowNodeVO;
import com.pengcheng.hr.approval.dto.ApprovalProgressVO;
import com.pengcheng.hr.approval.entity.ApprovalRecordNode;

import java.util.List;

/**
 * 审批流引擎服务
 */
public interface ApprovalFlowService {

    /**
     * 业务申请提交后实例化审批流：按当前 business_type 的模板节点链生成 record_node 列表，
     * 写入第一个节点的候选人快照供首位审批人处理。
     *
     * @param businessType {@link com.pengcheng.hr.approval.constant.ApprovalConstants#BUSINESS_TYPE_LEAVE} 或 COMPENSATE
     * @param businessId   业务单 ID
     * @param applicantId  申请人用户 ID（用于解析 direct_supervisor）
     * @throws IllegalStateException 当对应 business_type 没有任何 enabled 节点时
     */
    void start(String businessType, Long businessId, Long applicantId);

    /**
     * 执行某节点的审批操作。
     * - 通过：若是最后节点 → 整单通过（回写业务表 + 触发后置动作）；否则推进到下一节点。
     * - 驳回：直接终态 → 整单驳回。
     *
     * @param recordNodeId 节点行 ID
     * @param approverId   操作人 ID（必须出现在 candidate_approver_ids 中）
     * @param approved     true=通过 / false=驳回
     * @param remark       审批意见
     * @throws IllegalStateException 当节点已被审批 / 操作人无权限 / 整单已终态时
     */
    void approve(Long recordNodeId, Long approverId, boolean approved, String remark);

    /**
     * 查询某业务申请当前可处理的节点（result IS NULL 中 seq 最小者）。
     * 已无待处理节点时返回 null。
     */
    ApprovalRecordNode getCurrentNode(String businessType, Long businessId);

    /**
     * 查询整条审批流转链（含已审批节点、当前节点、未来节点占位）。
     */
    ApprovalProgressVO getProgress(String businessType, Long businessId);

    /**
     * 查询当前用户作为候选人的待审批节点列表。
     *
     * @param businessType 可空，为空则不过滤业务类型
     */
    List<ApprovalRecordNode> findPending(Long approverId, String businessType);

    /**
     * 解析某节点模板的候选审批人 ID 列表（按 approver_type 分发）。
     */
    List<Long> resolveApprovers(String approverType, String approverValue, Long applicantId);

    /**
     * 拉取某 business_type 的模板节点配置（按 seq 升序）。
     */
    List<ApprovalFlowNodeVO> listTemplate(String businessType);

    /**
     * 全量替换某 business_type 的节点配置（用于后台一次性保存）。
     * 实现应在事务内：软删旧节点 → 插入新节点。
     */
    void replaceTemplate(String businessType, List<ApprovalFlowNodeVO> nodes);
}
