package com.pengcheng.hr.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.approval.constant.ApprovalConstants;
import com.pengcheng.hr.approval.dto.ApprovalFlowNodeVO;
import com.pengcheng.hr.approval.dto.ApprovalProgressVO;
import com.pengcheng.hr.approval.entity.ApprovalFlowNode;
import com.pengcheng.hr.approval.entity.ApprovalRecordNode;
import com.pengcheng.hr.approval.event.ApprovalFinalizedEvent;
import com.pengcheng.hr.approval.mapper.ApprovalFlowNodeMapper;
import com.pengcheng.hr.approval.mapper.ApprovalRecordNodeMapper;
import com.pengcheng.hr.approval.service.ApprovalFlowService;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.LeaveRequest;
import com.pengcheng.hr.attendance.mapper.CompensateRequestMapper;
import com.pengcheng.hr.attendance.mapper.LeaveRequestMapper;
import com.pengcheng.system.entity.SysDept;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.entity.SysUserRole;
import com.pengcheng.system.mapper.SysDeptMapper;
import com.pengcheng.system.mapper.SysUserRoleMapper;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalFlowServiceImpl implements ApprovalFlowService {

    private final ApprovalFlowNodeMapper flowNodeMapper;
    private final ApprovalRecordNodeMapper recordNodeMapper;
    private final SysUserService userService;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final CompensateRequestMapper compensateRequestMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ========== 流程启动 / 执行 ==========

    @Override
    @Transactional
    public void start(String businessType, Long businessId, Long applicantId) {
        List<ApprovalFlowNode> templates = listEnabledTemplate(businessType);
        if (templates.isEmpty()) {
            throw new IllegalStateException("未配置 " + businessType + " 的审批流，请联系管理员");
        }
        // 提交时一次性快照所有节点的候选审批人，避免后续人员变动影响已提交流程
        // 跳过节点的两种情况：
        //   ① 节点配置了 applies_to_role_ids 且申请人不持有任一指定角色 → 该节点对此申请人不生效
        //      场景：普通员工请假，"总经理"节点 applies_to=管理岗，员工不属管理岗 → 跳过
        //   ② 候选人列表（剔除申请人本人后）为空
        //      场景：部门负责人请假 → "部门负责人"节点候选只剩自己 → 跳过；
        //           老总请假 → "总经理"节点候选只剩自己 → 跳过；
        //           所有节点都跳完 → 整单自动通过
        Set<Long> applicantRoleIds = loadApplicantRoleIds(applicantId);
        int insertedCount = 0;
        for (ApprovalFlowNode tmpl : templates) {
            if (!matchesApplicantRole(tmpl.getAppliesToRoleIds(), applicantRoleIds)) {
                continue;
            }
            List<Long> raw = resolveApprovers(tmpl.getApproverType(), tmpl.getApproverValue(), applicantId);
            List<Long> approvers = raw.stream()
                    .filter(id -> id != null && !id.equals(applicantId))
                    .distinct()
                    .collect(Collectors.toList());
            if (approvers.isEmpty()) {
                continue;
            }
            ApprovalRecordNode node = new ApprovalRecordNode();
            node.setBusinessType(businessType);
            node.setBusinessId(businessId);
            node.setSeq(tmpl.getSeq());
            node.setNodeName(tmpl.getNodeName());
            node.setCandidateApproverIds(approvers.stream().map(String::valueOf).collect(Collectors.joining(",")));
            recordNodeMapper.insert(node);
            insertedCount++;
        }
        // 所有节点都被跳过（如老总请假，整条链候选都是自己）→ 自动通过
        if (insertedCount == 0) {
            finalizeBusiness(businessType, businessId, true);
        }
    }

    @Override
    @Transactional
    public void cancel(String businessType, Long businessId, Long applicantId) {
        if (businessType == null || businessId == null || applicantId == null) {
            throw new IllegalStateException("撤销参数不完整");
        }
        // 校验业务单存在 + 仍在审批中 + 操作人是原申请人
        Long ownerId;
        Integer currentStatus;
        if (ApprovalConstants.BUSINESS_TYPE_LEAVE.equals(businessType)) {
            LeaveRequest lr = leaveRequestMapper.selectById(businessId);
            if (lr == null) throw new IllegalStateException("请假记录不存在");
            ownerId = lr.getUserId();
            currentStatus = lr.getStatus();
        } else if (ApprovalConstants.BUSINESS_TYPE_COMPENSATE.equals(businessType)) {
            CompensateRequest cr = compensateRequestMapper.selectById(businessId);
            if (cr == null) throw new IllegalStateException("调休记录不存在");
            ownerId = cr.getUserId();
            currentStatus = cr.getStatus();
        } else {
            throw new IllegalStateException("不支持撤销的业务类型：" + businessType);
        }
        if (!Objects.equals(ownerId, applicantId)) {
            throw new IllegalStateException("仅申请人可撤销自己的申请");
        }
        if (currentStatus == null || currentStatus != ApprovalConstants.STATUS_PENDING) {
            throw new IllegalStateException("当前申请已审批完成或已撤销，不可撤销");
        }
        cancelPendingNodes(businessType, businessId, applicantId);
        // 业务单整体状态翻转为「已撤销」
        if (ApprovalConstants.BUSINESS_TYPE_LEAVE.equals(businessType)) {
            LeaveRequest lr = leaveRequestMapper.selectById(businessId);
            lr.setStatus(ApprovalConstants.STATUS_CANCELLED);
            leaveRequestMapper.updateById(lr);
        } else {
            CompensateRequest cr = compensateRequestMapper.selectById(businessId);
            cr.setStatus(ApprovalConstants.STATUS_CANCELLED);
            compensateRequestMapper.updateById(cr);
        }
        // 注意：不发布 ApprovalFinalizedEvent —— 撤销不应触发 AttendanceExemptListener 等"通过"后置动作
    }

    @Override
    @Transactional
    public void cancelPendingNodes(String businessType, Long businessId, Long applicantId) {
        if (businessType == null || businessId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<ApprovalRecordNode> pendingNodes = recordNodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalRecordNode>()
                        .eq(ApprovalRecordNode::getBusinessType, businessType)
                        .eq(ApprovalRecordNode::getBusinessId, businessId)
                        .isNull(ApprovalRecordNode::getResult));
        for (ApprovalRecordNode n : pendingNodes) {
            n.setApproverId(applicantId);
            n.setResult(ApprovalConstants.RESULT_CANCELLED);
            n.setRemark("申请人撤销");
            n.setApprovalTime(now);
            recordNodeMapper.updateById(n);
        }
    }

    @Override
    @Transactional
    public void approve(Long recordNodeId, Long approverId, boolean approved, String remark) {
        ApprovalRecordNode node = recordNodeMapper.selectById(recordNodeId);
        if (node == null) {
            throw new IllegalStateException("审批节点不存在");
        }
        if (node.getResult() != null) {
            throw new IllegalStateException("该节点已审批，不可重复操作");
        }
        if (!isCandidate(node, approverId)) {
            throw new IllegalStateException("无权审批此节点");
        }
        // 必须是当前最早未审批节点（防止越级审批）
        ApprovalRecordNode current = getCurrentNode(node.getBusinessType(), node.getBusinessId());
        if (current == null || !Objects.equals(current.getId(), node.getId())) {
            throw new IllegalStateException("非当前可处理节点");
        }

        node.setApproverId(approverId);
        node.setResult(approved ? ApprovalConstants.RESULT_APPROVED : ApprovalConstants.RESULT_REJECTED);
        node.setRemark(remark);
        node.setApprovalTime(LocalDateTime.now());
        recordNodeMapper.updateById(node);

        if (!approved) {
            finalizeBusiness(node.getBusinessType(), node.getBusinessId(), false);
            return;
        }
        // 通过：若已无后续节点，则整单通过
        ApprovalRecordNode next = getCurrentNode(node.getBusinessType(), node.getBusinessId());
        if (next == null) {
            finalizeBusiness(node.getBusinessType(), node.getBusinessId(), true);
        }
    }

    // ========== 查询 ==========

    @Override
    public ApprovalRecordNode getCurrentNode(String businessType, Long businessId) {
        return recordNodeMapper.selectOne(
                new LambdaQueryWrapper<ApprovalRecordNode>()
                        .eq(ApprovalRecordNode::getBusinessType, businessType)
                        .eq(ApprovalRecordNode::getBusinessId, businessId)
                        .isNull(ApprovalRecordNode::getResult)
                        .orderByAsc(ApprovalRecordNode::getSeq)
                        .last("LIMIT 1"));
    }

    @Override
    public ApprovalProgressVO getProgress(String businessType, Long businessId) {
        List<ApprovalRecordNode> nodes = recordNodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalRecordNode>()
                        .eq(ApprovalRecordNode::getBusinessType, businessType)
                        .eq(ApprovalRecordNode::getBusinessId, businessId)
                        .orderByAsc(ApprovalRecordNode::getSeq));

        int overall = ApprovalConstants.STATUS_PENDING;
        boolean anyRejected = nodes.stream()
                .anyMatch(n -> Objects.equals(n.getResult(), ApprovalConstants.RESULT_REJECTED));
        boolean allApproved = !nodes.isEmpty() && nodes.stream()
                .allMatch(n -> Objects.equals(n.getResult(), ApprovalConstants.RESULT_APPROVED));
        if (anyRejected) {
            overall = ApprovalConstants.STATUS_REJECTED;
        } else if (allApproved) {
            overall = ApprovalConstants.STATUS_APPROVED;
        }

        List<ApprovalProgressVO.NodeView> views = new ArrayList<>();
        for (ApprovalRecordNode n : nodes) {
            views.add(ApprovalProgressVO.NodeView.builder()
                    .seq(n.getSeq())
                    .nodeName(n.getNodeName())
                    .candidateApproverNames(resolveNames(parseIds(n.getCandidateApproverIds())))
                    .approverName(n.getApproverId() != null ? resolveName(n.getApproverId()) : null)
                    .result(n.getResult())
                    .remark(n.getRemark())
                    .approvalTime(n.getApprovalTime())
                    .build());
        }
        return ApprovalProgressVO.builder()
                .businessType(businessType)
                .businessId(businessId)
                .overallStatus(overall)
                .nodes(views)
                .build();
    }

    @Override
    public List<ApprovalRecordNode> findPending(Long approverId, String businessType) {
        return recordNodeMapper.findPendingByApprover(approverId, businessType);
    }

    @Override
    public List<ApprovalRecordNode> listRecordNodes(String businessType, Long businessId) {
        if (businessType == null || businessId == null) {
            return Collections.emptyList();
        }
        return recordNodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalRecordNode>()
                        .eq(ApprovalRecordNode::getBusinessType, businessType)
                        .eq(ApprovalRecordNode::getBusinessId, businessId)
                        .orderByAsc(ApprovalRecordNode::getSeq));
    }

    // ========== 审批人解析 ==========

    @Override
    public List<Long> resolveApprovers(String approverType, String approverValue, Long applicantId) {
        if (approverType == null) {
            return Collections.emptyList();
        }
        return switch (approverType) {
            case ApprovalConstants.APPROVER_TYPE_DIRECT_SUPERVISOR -> resolveDirectSupervisor(applicantId);
            case ApprovalConstants.APPROVER_TYPE_APPLICANT_DEPT_MANAGER -> resolveApplicantDeptManager(applicantId);
            case ApprovalConstants.APPROVER_TYPE_USER -> parseIds(approverValue);
            case ApprovalConstants.APPROVER_TYPE_ROLE -> resolveRoleApprovers(approverValue);
            default -> Collections.emptyList();
        };
    }

    /**
     * 直接上级解析顺序（与「单一数据源 + 向上兜底」设计一致）：
     *   1) 申请人所在部门 dept.leader_id 命中即返回
     *   2) 沿 dept.ancestors 自下而上找最近一个有 leader_id 的祖先部门
     *   3) 仍未命中 → 返回空，由 start() 抛"无可用审批人"
     *
     * 历史字段 user.leader_id 仍作为最高优先级兜底（极少数跨部门汇报的覆盖场景），
     * 避免因 ancestor 兜底丢掉显式指定的特例。
     */
    private List<Long> resolveDirectSupervisor(Long applicantId) {
        if (applicantId == null) {
            return Collections.emptyList();
        }
        SysUser user = userService.getById(applicantId);
        if (user == null) {
            return Collections.emptyList();
        }
        // 0) 显式指定的直接上级（极少数跨部门汇报的特例；同样排除自我闭环）
        if (user.getLeaderId() != null && !user.getLeaderId().equals(applicantId)) {
            return List.of(user.getLeaderId());
        }
        return resolveByDeptChain(user, applicantId);
    }

    /**
     * 申请人所在部门的负责人（自我排除 + 沿祖先回溯）。
     * 与 direct_supervisor 区别：忽略 user.leader_id 这一"私有上级"，始终走部门负责人线，
     * 适用于"必须由部门管理者审批"的节点配置。
     */
    private List<Long> resolveApplicantDeptManager(Long applicantId) {
        if (applicantId == null) {
            return Collections.emptyList();
        }
        SysUser user = userService.getById(applicantId);
        if (user == null) {
            return Collections.emptyList();
        }
        return resolveByDeptChain(user, applicantId);
    }

    /**
     * 沿"申请人所在部门 → 父部门 → 祖父部门 …"链向上找第一个
     * 「启用 + 有 leader_id 且 leader_id != 申请人本人」的部门，返回其 leader_id。
     * 申请人自己就是部门负责人时，自动跳到上级部门，避免"自己审自己"。
     */
    private List<Long> resolveByDeptChain(SysUser user, Long applicantId) {
        if (user.getDeptId() == null) {
            return Collections.emptyList();
        }
        SysDept dept = deptMapper.selectById(user.getDeptId());
        if (dept == null) {
            return Collections.emptyList();
        }
        if (isActive(dept) && isValidApprover(dept.getLeaderId(), applicantId)) {
            return List.of(dept.getLeaderId());
        }
        String ancestors = dept.getAncestors();
        if (ancestors == null || ancestors.isEmpty()) {
            return Collections.emptyList();
        }
        // ancestors 形如 "0,1,200"，从右往左是「最近的祖先 → 根」
        String[] parts = ancestors.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String token = parts[i].trim();
            if (token.isEmpty() || "0".equals(token)) continue;
            try {
                Long ancestorId = Long.parseLong(token);
                SysDept ancestor = deptMapper.selectById(ancestorId);
                if (ancestor != null && isActive(ancestor) && isValidApprover(ancestor.getLeaderId(), applicantId)) {
                    return List.of(ancestor.getLeaderId());
                }
            } catch (NumberFormatException ignored) {
                // 容错：ancestors 数据脏跳过即可
            }
        }
        return Collections.emptyList();
    }

    private boolean isActive(SysDept dept) {
        return dept.getStatus() != null && dept.getStatus() == 1;
    }

    private boolean isValidApprover(Long candidateId, Long applicantId) {
        return candidateId != null && !candidateId.equals(applicantId);
    }

    /** 节点 applies_to_role_ids 与申请人角色匹配判定：空 = 全员适用 */
    private boolean matchesApplicantRole(String appliesToRoleIds, Set<Long> applicantRoleIds) {
        if (appliesToRoleIds == null || appliesToRoleIds.isBlank()) {
            return true; // 未配置 = 全员适用
        }
        List<Long> targetRoleIds = parseIds(appliesToRoleIds);
        if (targetRoleIds.isEmpty()) {
            return true;
        }
        if (applicantRoleIds == null || applicantRoleIds.isEmpty()) {
            return false;
        }
        for (Long rid : targetRoleIds) {
            if (applicantRoleIds.contains(rid)) {
                return true;
            }
        }
        return false;
    }

    /** 加载申请人持有的所有角色 ID（用于节点 applies_to 匹配） */
    private Set<Long> loadApplicantRoleIds(Long applicantId) {
        if (applicantId == null) return Collections.emptySet();
        List<SysUserRole> rels = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, applicantId));
        return rels.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
    }

    private List<Long> resolveRoleApprovers(String approverValue) {
        List<Long> roleIds = parseIds(approverValue);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUserRole> rels = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, roleIds));
        return rels.stream().map(SysUserRole::getUserId).distinct().toList();
    }

    // ========== 模板配置 ==========

    @Override
    public List<ApprovalFlowNodeVO> listTemplate(String businessType) {
        return listEnabledTemplate(businessType).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public void replaceTemplate(String businessType, List<ApprovalFlowNodeVO> nodes) {
        flowNodeMapper.delete(new LambdaQueryWrapper<ApprovalFlowNode>()
                .eq(ApprovalFlowNode::getBusinessType, businessType));
        int seq = 1;
        for (ApprovalFlowNodeVO vo : nodes) {
            ApprovalFlowNode n = new ApprovalFlowNode();
            n.setBusinessType(businessType);
            n.setSeq(seq++);
            n.setNodeName(vo.getNodeName());
            n.setApproverType(vo.getApproverType());
            n.setApproverValue(vo.getApproverValue());
            n.setAppliesToRoleIds(vo.getAppliesToRoleIds());
            n.setEnabled(vo.getEnabled() == null ? 1 : vo.getEnabled());
            flowNodeMapper.insert(n);
        }
    }

    // ========== 内部辅助 ==========

    private List<ApprovalFlowNode> listEnabledTemplate(String businessType) {
        return flowNodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalFlowNode>()
                        .eq(ApprovalFlowNode::getBusinessType, businessType)
                        .eq(ApprovalFlowNode::getEnabled, 1)
                        .orderByAsc(ApprovalFlowNode::getSeq));
    }

    private boolean isCandidate(ApprovalRecordNode node, Long approverId) {
        return parseIds(node.getCandidateApproverIds()).contains(approverId);
    }

    private List<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong).toList();
    }

    private List<String> resolveNames(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUser> users = userService.listByIds(userIds);
        Map<Long, String> map = users.stream().collect(Collectors.toMap(SysUser::getId, this::nameOf));
        return userIds.stream().map(id -> map.getOrDefault(id, "未知")).toList();
    }

    private String resolveName(Long userId) {
        if (userId == null) {
            return "";
        }
        SysUser user = userService.getById(userId);
        return user == null ? "未知" : nameOf(user);
    }

    private String nameOf(SysUser u) {
        if (u == null) {
            return "未知";
        }
        return u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername();
    }

    private void finalizeBusiness(String businessType, Long businessId, boolean approved) {
        int status = approved ? ApprovalConstants.STATUS_APPROVED : ApprovalConstants.STATUS_REJECTED;
        if (ApprovalConstants.BUSINESS_TYPE_LEAVE.equals(businessType)) {
            LeaveRequest lr = leaveRequestMapper.selectById(businessId);
            if (lr != null) {
                lr.setStatus(status);
                leaveRequestMapper.updateById(lr);
            }
        } else if (ApprovalConstants.BUSINESS_TYPE_COMPENSATE.equals(businessType)) {
            CompensateRequest cr = compensateRequestMapper.selectById(businessId);
            if (cr != null) {
                cr.setStatus(status);
                compensateRequestMapper.updateById(cr);
            }
        }
        // 付款类（expense/advance/prepay）业务表在 pengcheng-realty 模块，此处不直接更新；
        // 由调用方 PaymentService.approvePaymentRequest 在调本引擎 approve 后自行回写 status。
        // 通知业务方（如考勤模块的豁免联动）。注意：监听器应在同一事务里执行（默认行为）以保证一致性
        eventPublisher.publishEvent(new ApprovalFinalizedEvent(this, businessType, businessId, approved));
    }

    private ApprovalFlowNodeVO toVO(ApprovalFlowNode n) {
        return ApprovalFlowNodeVO.builder()
                .id(n.getId())
                .businessType(n.getBusinessType())
                .seq(n.getSeq())
                .nodeName(n.getNodeName())
                .approverType(n.getApproverType())
                .approverValue(n.getApproverValue())
                .appliesToRoleIds(n.getAppliesToRoleIds())
                .enabled(n.getEnabled())
                .build();
    }
}
