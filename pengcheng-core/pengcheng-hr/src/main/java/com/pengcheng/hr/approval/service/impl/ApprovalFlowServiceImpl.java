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
        for (ApprovalFlowNode tmpl : templates) {
            List<Long> approvers = resolveApprovers(tmpl.getApproverType(), tmpl.getApproverValue(), applicantId);
            if (approvers.isEmpty()) {
                throw new IllegalStateException(
                        "节点【" + tmpl.getNodeName() + "】无可用审批人，请联系管理员检查审批流配置");
            }
            ApprovalRecordNode node = new ApprovalRecordNode();
            node.setBusinessType(businessType);
            node.setBusinessId(businessId);
            node.setSeq(tmpl.getSeq());
            node.setNodeName(tmpl.getNodeName());
            node.setCandidateApproverIds(approvers.stream().map(String::valueOf).collect(Collectors.joining(",")));
            recordNodeMapper.insert(node);
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

    // ========== 审批人解析 ==========

    @Override
    public List<Long> resolveApprovers(String approverType, String approverValue, Long applicantId) {
        if (approverType == null) {
            return Collections.emptyList();
        }
        return switch (approverType) {
            case ApprovalConstants.APPROVER_TYPE_DIRECT_SUPERVISOR -> resolveDirectSupervisor(applicantId);
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
        // 0) 显式指定的直接上级（一般为空，仅特例）
        if (user.getLeaderId() != null) {
            return List.of(user.getLeaderId());
        }
        if (user.getDeptId() == null) {
            return Collections.emptyList();
        }
        // 1) 本部门负责人（部门必须启用）
        SysDept dept = deptMapper.selectById(user.getDeptId());
        if (dept == null) {
            return Collections.emptyList();
        }
        if (isActive(dept) && dept.getLeaderId() != null) {
            return List.of(dept.getLeaderId());
        }
        // 2) 沿 ancestors 向上回溯，找到最近一个「启用 + 有负责人」的祖先部门
        //    停用部门跳过，避免审批流转到不再生效的部门负责人
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
                if (ancestor != null && isActive(ancestor) && ancestor.getLeaderId() != null) {
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
                .enabled(n.getEnabled())
                .build();
    }
}
