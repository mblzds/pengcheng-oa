package com.pengcheng.hr.approval.service;

import com.pengcheng.hr.approval.dto.ApprovalFlowNodeVO;
import com.pengcheng.hr.approval.dto.ApprovalProgressVO;
import com.pengcheng.hr.approval.entity.ApprovalRecordNode;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
     * 申请人主动撤销审批流（仅适用于由本引擎管理且业务表在 pengcheng-hr 模块中的类型，
     * 即 {@link ApprovalConstants#BUSINESS_TYPE_LEAVE} 与 COMPENSATE）。
     * - 仅业务单仍处于 STATUS_PENDING 时允许（已通过/驳回/已撤销不可重复操作）。
     * - 校验 applicantId 必须是业务单原申请人。
     * - 内部调用 {@link #cancelPendingNodes} 终态化未审批节点；并把业务表 status 翻转为 STATUS_CANCELLED。
     * - 不发布 ApprovalFinalizedEvent，因此不会触发"通过侧"后置动作。
     *
     * 跨模块业务（付款类）应当：调用方先在自己模块完成业务表校验/更新，再调
     * {@link #cancelPendingNodes} 把节点收尾，无需走此入口。
     */
    void cancel(String businessType, Long businessId, Long applicantId);

    /**
     * 仅终态化未审批节点（不验证业务单状态、不更新业务表），供跨模块（付款类）的撤销逻辑使用。
     * 把所有 result IS NULL 的节点设为 RESULT_CANCELLED，并把 approver_id 写为申请人，使其
     * 从所有审批人的待办列表中消失。多次调用幂等。
     */
    void cancelPendingNodes(String businessType, Long businessId, Long applicantId);

    /**
     * 查询某业务申请当前可处理的节点（result IS NULL 中 seq 最小者）。
     * 已无待处理节点时返回 null。
     */
    ApprovalRecordNode getCurrentNode(String businessType, Long businessId);

    /**
     * 批量获取多个业务单的当前节点名（避免 N+1）。语义同 {@link #getCurrentNode}：
     * 每个 businessId 取 result IS NULL 中 seq 最小者的 nodeName；终态业务单（已通过/驳回/撤销）
     * 不会出现在返回 Map 中。
     */
    Map<Long, String> getCurrentNodeNames(String businessType, Collection<Long> businessIds);

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
     * 查询某人作为审批人在 since 之后已完成审批（result IS NOT NULL）的节点，按 approvalTime 倒序。
     * 用于"我审过的"列表回看。佣金审核不走 approval_record_node 表，此处不会返回。
     *
     * @param since 可空，为空则不过滤时间下限
     */
    List<ApprovalRecordNode> findApprovedBy(Long approverId, LocalDateTime since);

    /**
     * 查询某业务申请的全部审批节点（按 seq 升序），含已审批 + 待审批节点。
     * 跨模块（如 PaymentService）拉审批历史用，避免直接依赖 ApprovalRecordNodeMapper。
     */
    List<ApprovalRecordNode> listRecordNodes(String businessType, Long businessId);

    /**
     * 解析某节点模板的候选审批人 ID 列表（按 approver_type 分发）。
     */
    List<Long> resolveApprovers(String approverType, String approverValue, Long applicantId);

    /**
     * 拉取某 business_type 的模板节点配置（按 seq 升序）。
     */
    List<ApprovalFlowNodeVO> listTemplate(String businessType);

    /**
     * 列举管理后台审批流配置页应展示的业务类型 tab 列表 = 后端注册的内置类型（leave / compensate /
     * expense / advance / prepay）∪ 当前 approval_flow_node 表里已经出现过的自定义类型。
     * 自定义类型 label 回显原 key，nodeCount = 各类型当前 enabled+未删除节点数。
     */
    List<com.pengcheng.hr.approval.dto.BusinessTypeOption> listBusinessTypes();

    /**
     * 全量替换某 business_type 的节点配置（用于后台一次性保存）。
     * 实现应在事务内：软删旧节点 → 插入新节点。
     */
    void replaceTemplate(String businessType, List<ApprovalFlowNodeVO> nodes);
}
