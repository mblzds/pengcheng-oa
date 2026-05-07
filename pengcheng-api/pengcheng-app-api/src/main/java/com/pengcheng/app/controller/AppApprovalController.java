package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.app.dto.AppApproveDTO;
import com.pengcheng.app.dto.ApprovalDetailVO;
import com.pengcheng.app.dto.ApprovalPendingVO;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.approval.constant.ApprovalConstants;
import com.pengcheng.hr.approval.dto.ApprovalProgressVO;
import com.pengcheng.hr.approval.entity.ApprovalRecordNode;
import com.pengcheng.hr.approval.service.ApprovalFlowService;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.LeaveRequest;
import com.pengcheng.hr.attendance.mapper.CompensateRequestMapper;
import com.pengcheng.hr.attendance.mapper.LeaveRequestMapper;
import com.pengcheng.realty.commission.dto.CommissionAuditDTO;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.commission.service.CommissionService;
import com.pengcheng.realty.common.exception.ApprovalFlowException;
import com.pengcheng.realty.payment.dto.PaymentApprovalDTO;
import com.pengcheng.realty.payment.entity.PaymentApproval;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import com.pengcheng.realty.payment.mapper.PaymentRequestMapper;
import com.pengcheng.realty.payment.service.PaymentService;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * App端审批控制器
 * 提供待审批列表聚合、审批详情、执行审批接口
 *
 * 请假/调休：通过 {@link ApprovalFlowService} 多节点流转引擎驱动；
 * 付款/佣金：复用 PaymentService / CommissionService 的现有审批逻辑。
 */
@RestController
@RequestMapping("/app/approval")
@RequiredArgsConstructor
@SaCheckLogin
public class AppApprovalController {

    private final LeaveRequestMapper leaveRequestMapper;
    private final CompensateRequestMapper compensateRequestMapper;
    private final PaymentRequestMapper paymentRequestMapper;
    private final PaymentService paymentService;
    private final CommissionMapper commissionMapper;
    private final CommissionService commissionService;
    private final SysUserService userService;
    private final ApprovalFlowService approvalFlowService;

    /**
     * 待审批列表（聚合查询）
     * 请假/调休：当前用户作为候选审批人的 record_node（最早未审批节点）
     * 付款/佣金：维持原有 status 过滤逻辑
     */
    @GetMapping("/pending")
    public Result<ApprovalPendingVO> pending() {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 请假/调休：从审批流引擎拿当前用户的待办节点
        List<ApprovalRecordNode> pendingNodes = approvalFlowService.findPending(currentUserId, null);
        List<ApprovalPendingVO.ApprovalItem> leaveItems = new ArrayList<>();
        for (ApprovalRecordNode node : pendingNodes) {
            // 仅对最早未审批节点开放（防止越级看到后续节点）
            ApprovalRecordNode current = approvalFlowService.getCurrentNode(node.getBusinessType(), node.getBusinessId());
            if (current == null || !current.getId().equals(node.getId())) {
                continue;
            }
            ApprovalPendingVO.ApprovalItem item = buildLeaveOrCompensateItem(node);
            if (item != null) {
                leaveItems.add(item);
            }
        }

        // 待审批付款（status=1 待审批 或 status=2 审批中）
        List<PaymentRequest> pendingPayments = paymentRequestMapper.selectList(
                new LambdaQueryWrapper<PaymentRequest>()
                        .in(PaymentRequest::getStatus, PaymentService.STATUS_PENDING, PaymentService.STATUS_IN_PROGRESS)
                        .orderByDesc(PaymentRequest::getCreateTime));

        // 待审核佣金
        List<Commission> pendingCommissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getAuditStatus, CommissionService.AUDIT_STATUS_PENDING)
                        .orderByDesc(Commission::getCreateTime));

        List<ApprovalPendingVO.ApprovalItem> paymentItems = new ArrayList<>();
        for (PaymentRequest pr : pendingPayments) {
            paymentItems.add(ApprovalPendingVO.ApprovalItem.builder()
                    .id(pr.getId())
                    .type(resolvePaymentType(pr.getRequestType()))
                    .applicantName(resolveUserName(pr.getApplicantId()))
                    .summary(buildPaymentSummary(pr))
                    .amount(pr.getAmount())
                    .applyTime(pr.getCreateTime())
                    .build());
        }

        List<ApprovalPendingVO.ApprovalItem> commissionItems = new ArrayList<>();
        for (Commission c : pendingCommissions) {
            commissionItems.add(ApprovalPendingVO.ApprovalItem.builder()
                    .id(c.getId())
                    .type("commission")
                    .applicantName(resolveUserName(c.getCreateBy()))
                    .summary("佣金审核 - 应收: " + c.getReceivableAmount())
                    .amount(c.getReceivableAmount())
                    .applyTime(c.getCreateTime())
                    .build());
        }

        int totalCount = leaveItems.size() + paymentItems.size() + commissionItems.size();

        return Result.ok(ApprovalPendingVO.builder()
                .leaveItems(leaveItems)
                .paymentItems(paymentItems)
                .commissionItems(commissionItems)
                .totalCount(totalCount)
                .build());
    }

    /**
     * 审批详情
     */
    @GetMapping("/{id}")
    public Result<ApprovalDetailVO> detail(@PathVariable Long id, @RequestParam String type) {
        return switch (type) {
            case "leave" -> Result.ok(buildLeaveDetail(id));
            case "compensate" -> Result.ok(buildCompensateDetail(id));
            case "expense", "advance", "prepay" -> Result.ok(buildPaymentDetail(id));
            case "commission" -> Result.ok(buildCommissionDetail(id));
            default -> Result.fail(400, "无效的审批类型: " + type);
        };
    }

    /**
     * 执行审批（通过/驳回）
     * 请假/调休的 path id 是业务单 ID（leave_request.id / realty_compensate_request.id），
     * 由引擎解析当前节点后执行；付款/佣金沿用各自的 service 接口。
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody AppApproveDTO dto) {
        Long approverId = StpUtil.getLoginIdAsLong();
        String type = dto.getType();

        if (type == null || type.isBlank()) {
            return Result.fail(400, "审批类型不能为空");
        }

        switch (type) {
            case "leave" -> approveFlow(ApprovalConstants.BUSINESS_TYPE_LEAVE, id, approverId, dto);
            case "compensate" -> approveFlow(ApprovalConstants.BUSINESS_TYPE_COMPENSATE, id, approverId, dto);
            case "expense", "advance", "prepay" -> approvePayment(id, approverId, dto.getApproved(), dto.getReason());
            case "commission" -> approveCommission(id, approverId, dto.getApproved(), dto.getReason());
            default -> {
                return Result.fail(400, "无效的审批类型: " + type);
            }
        }
        return Result.ok();
    }

    // ========== 详情构建：请假/调休（含完整流转链） ==========

    private ApprovalDetailVO buildLeaveDetail(Long id) {
        LeaveRequest lr = leaveRequestMapper.selectById(id);
        if (lr == null) {
            throw new IllegalArgumentException("请假记录不存在");
        }
        return ApprovalDetailVO.builder()
                .id(lr.getId())
                .type("leave")
                .applicantName(resolveUserName(lr.getUserId()))
                .summary(buildLeaveSummary(lr))
                .status(lr.getStatus())
                .applyTime(lr.getCreateTime())
                .histories(buildFlowHistories(ApprovalConstants.BUSINESS_TYPE_LEAVE, id))
                .build();
    }

    private ApprovalDetailVO buildCompensateDetail(Long id) {
        CompensateRequest cr = compensateRequestMapper.selectById(id);
        if (cr == null) {
            throw new IllegalArgumentException("调休记录不存在");
        }
        return ApprovalDetailVO.builder()
                .id(cr.getId())
                .type("compensate")
                .applicantName(resolveUserName(cr.getUserId()))
                .summary("调休申请 - " + cr.getCompensateDate())
                .status(cr.getStatus())
                .applyTime(cr.getCreateTime())
                .histories(buildFlowHistories(ApprovalConstants.BUSINESS_TYPE_COMPENSATE, id))
                .build();
    }

    /**
     * 把 ApprovalProgressVO.NodeView 列表映射为前端兼容的 ApprovalHistory 列表
     * 待审批节点：approverName 显示候选人，result 为 NULL
     */
    private List<ApprovalDetailVO.ApprovalHistory> buildFlowHistories(String businessType, Long businessId) {
        ApprovalProgressVO progress = approvalFlowService.getProgress(businessType, businessId);
        List<ApprovalDetailVO.ApprovalHistory> list = new ArrayList<>();
        for (ApprovalProgressVO.NodeView v : progress.getNodes()) {
            String approverDisplay = v.getApproverName();
            if (approverDisplay == null || approverDisplay.isBlank()) {
                approverDisplay = v.getCandidateApproverNames() == null || v.getCandidateApproverNames().isEmpty()
                        ? "无候选人"
                        : "候选: " + String.join(" / ", v.getCandidateApproverNames());
            }
            list.add(ApprovalDetailVO.ApprovalHistory.builder()
                    .nodeName(v.getNodeName())
                    .approverName(approverDisplay)
                    .result(v.getResult())
                    .remark(v.getRemark())
                    .approvalTime(v.getApprovalTime())
                    .build());
        }
        return list;
    }

    // ========== 详情构建：付款/佣金（保持原有逻辑） ==========

    private ApprovalDetailVO buildPaymentDetail(Long id) {
        PaymentRequest pr = paymentRequestMapper.selectById(id);
        if (pr == null) {
            throw new IllegalArgumentException("付款申请不存在");
        }
        List<PaymentApproval> approvals = paymentService.getApprovalHistory(id);
        List<ApprovalDetailVO.ApprovalHistory> histories = approvals.stream()
                .map(a -> ApprovalDetailVO.ApprovalHistory.builder()
                        .approverName(resolveUserName(a.getApproverId()))
                        .result(a.getResult())
                        .remark(a.getRemark())
                        .approvalTime(a.getApprovalTime())
                        .build())
                .toList();

        return ApprovalDetailVO.builder()
                .id(pr.getId())
                .type(resolvePaymentType(pr.getRequestType()))
                .applicantName(resolveUserName(pr.getApplicantId()))
                .summary(buildPaymentSummary(pr))
                .amount(pr.getAmount())
                .status(pr.getStatus())
                .applyTime(pr.getCreateTime())
                .histories(histories)
                .build();
    }

    private ApprovalDetailVO buildCommissionDetail(Long id) {
        Commission c = commissionMapper.selectById(id);
        if (c == null) {
            throw new IllegalArgumentException("佣金记录不存在");
        }
        List<ApprovalDetailVO.ApprovalHistory> histories = new ArrayList<>();
        if (c.getAuditStatus() != CommissionService.AUDIT_STATUS_PENDING && c.getAuditorId() != null) {
            histories.add(ApprovalDetailVO.ApprovalHistory.builder()
                    .approverName(resolveUserName(c.getAuditorId()))
                    .result(c.getAuditStatus() == CommissionService.AUDIT_STATUS_APPROVED ? 1 : 2)
                    .remark(c.getAuditRemark())
                    .approvalTime(c.getAuditTime())
                    .build());
        }

        return ApprovalDetailVO.builder()
                .id(c.getId())
                .type("commission")
                .applicantName(resolveUserName(c.getCreateBy()))
                .summary("佣金审核 - 应收: " + c.getReceivableAmount())
                .amount(c.getReceivableAmount())
                .status(c.getAuditStatus())
                .applyTime(c.getCreateTime())
                .histories(histories)
                .build();
    }

    // ========== 审批执行 ==========

    private void approveFlow(String businessType, Long businessId, Long approverId, AppApproveDTO dto) {
        ApprovalRecordNode current = approvalFlowService.getCurrentNode(businessType, businessId);
        if (current == null) {
            throw new ApprovalFlowException("该申请已完成审批，不可重复操作");
        }
        approvalFlowService.approve(current.getId(), approverId, Boolean.TRUE.equals(dto.getApproved()), dto.getReason());
    }

    private void approvePayment(Long id, Long approverId, Boolean approved, String reason) {
        PaymentApprovalDTO dto = PaymentApprovalDTO.builder()
                .requestId(id)
                .approverId(approverId)
                .approved(approved)
                .remark(reason)
                .build();
        paymentService.approvePaymentRequest(dto);
    }

    private void approveCommission(Long id, Long auditorId, Boolean approved, String reason) {
        CommissionAuditDTO dto = CommissionAuditDTO.builder()
                .commissionId(id)
                .auditorId(auditorId)
                .approved(approved)
                .remark(reason)
                .build();
        commissionService.auditCommission(dto);
    }

    // ========== 辅助方法 ==========

    private ApprovalPendingVO.ApprovalItem buildLeaveOrCompensateItem(ApprovalRecordNode node) {
        if (ApprovalConstants.BUSINESS_TYPE_LEAVE.equals(node.getBusinessType())) {
            LeaveRequest lr = leaveRequestMapper.selectById(node.getBusinessId());
            if (lr == null) return null;
            return ApprovalPendingVO.ApprovalItem.builder()
                    .id(lr.getId())
                    .type("leave")
                    .applicantName(resolveUserName(lr.getUserId()))
                    .summary(buildLeaveSummary(lr))
                    .applyTime(lr.getCreateTime())
                    .build();
        }
        if (ApprovalConstants.BUSINESS_TYPE_COMPENSATE.equals(node.getBusinessType())) {
            CompensateRequest cr = compensateRequestMapper.selectById(node.getBusinessId());
            if (cr == null) return null;
            return ApprovalPendingVO.ApprovalItem.builder()
                    .id(cr.getId())
                    .type("compensate")
                    .applicantName(resolveUserName(cr.getUserId()))
                    .summary("调休申请 - " + cr.getCompensateDate())
                    .applyTime(cr.getCreateTime())
                    .build();
        }
        return null;
    }

    String resolveUserName(Long userId) {
        if (userId == null) return "未知";
        SysUser user = userService.getById(userId);
        return user != null ? user.getNickname() : "未知";
    }

    private String buildLeaveSummary(LeaveRequest lr) {
        String typeLabel = switch (lr.getLeaveType()) {
            case 1 -> "事假";
            case 2 -> "病假";
            case 3 -> "年假";
            case 4 -> "婚假";
            case 5 -> "产假";
            case 6 -> "调休";
            default -> "其他";
        };
        return typeLabel + " " + lr.getStartTime().toLocalDate() + " ~ " + lr.getEndTime().toLocalDate();
    }

    private String buildPaymentSummary(PaymentRequest pr) {
        String typeLabel = switch (pr.getRequestType()) {
            case PaymentService.TYPE_EXPENSE -> "费用报销";
            case PaymentService.TYPE_ADVANCE_COMMISSION -> "垫佣申请";
            case PaymentService.TYPE_PREPAY_COMMISSION -> "预付佣申请";
            default -> "付款申请";
        };
        return typeLabel + " - ¥" + pr.getAmount();
    }

    static String resolvePaymentType(Integer requestType) {
        if (requestType == null) return "expense";
        return switch (requestType) {
            case PaymentService.TYPE_EXPENSE -> "expense";
            case PaymentService.TYPE_ADVANCE_COMMISSION -> "advance";
            case PaymentService.TYPE_PREPAY_COMMISSION -> "prepay";
            default -> "expense";
        };
    }
}
