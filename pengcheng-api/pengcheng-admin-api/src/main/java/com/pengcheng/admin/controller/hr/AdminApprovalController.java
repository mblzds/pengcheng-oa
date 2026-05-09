package com.pengcheng.admin.controller.hr;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.admin.dto.approval.AdminApprovalDetailVO;
import com.pengcheng.admin.dto.approval.AdminApprovalItemVO;
import com.pengcheng.admin.dto.approval.AdminApproveDTO;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.approval.constant.ApprovalConstants;
import com.pengcheng.hr.approval.dto.ApprovalProgressVO;
import com.pengcheng.hr.approval.entity.ApprovalRecordNode;
import com.pengcheng.hr.approval.service.ApprovalFlowService;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.LeaveRequest;
import com.pengcheng.hr.attendance.mapper.CompensateRequestMapper;
import com.pengcheng.hr.attendance.mapper.LeaveRequestMapper;
import com.pengcheng.hr.attendance.util.LeaveDaysUtil;
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
import com.pengcheng.system.helper.SystemConfigHelper;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 后台「我的待审批」聚合控制器
 *
 * 设计要点：
 * - 列表只返回流转到 当前登录用户 的待办（请假/调休按引擎当前节点候选人过滤；付款/佣金按全集返回，
 *   因这两类目前无多节点流转，权限通过菜单授予）
 * - 与小程序 {@code AppApprovalController} 共用底层引擎，避免重复实现，但前后端分别维护各自 DTO
 */
@RestController
@RequestMapping("/admin/approval")
@RequiredArgsConstructor
@SaCheckLogin
public class AdminApprovalController {

    private final LeaveRequestMapper leaveRequestMapper;
    private final CompensateRequestMapper compensateRequestMapper;
    private final PaymentRequestMapper paymentRequestMapper;
    private final PaymentService paymentService;
    private final CommissionMapper commissionMapper;
    private final CommissionService commissionService;
    private final SysUserService userService;
    private final ApprovalFlowService approvalFlowService;
    private final SystemConfigHelper systemConfigHelper;

    /** 待审批列表（已铺平为表格行；按申请时间倒序） */
    @GetMapping("/pending")
    public Result<List<AdminApprovalItemVO>> pending() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        List<AdminApprovalItemVO> rows = new ArrayList<>();

        // 请假 / 调休：当前用户作为候选审批人 且 是引擎当前节点
        List<ApprovalRecordNode> pendingNodes = approvalFlowService.findPending(currentUserId, null);
        for (ApprovalRecordNode node : pendingNodes) {
            ApprovalRecordNode current = approvalFlowService.getCurrentNode(node.getBusinessType(), node.getBusinessId());
            if (current == null || !current.getId().equals(node.getId())) {
                continue;
            }
            AdminApprovalItemVO row = buildLeaveOrCompensateRow(node);
            if (row != null) rows.add(row);
        }

        // 付款申请（待审批 + 审批中）
        List<PaymentRequest> pendingPayments = paymentRequestMapper.selectList(
                new LambdaQueryWrapper<PaymentRequest>()
                        .in(PaymentRequest::getStatus, PaymentService.STATUS_PENDING, PaymentService.STATUS_IN_PROGRESS)
                        .orderByDesc(PaymentRequest::getCreateTime));
        for (PaymentRequest pr : pendingPayments) {
            rows.add(AdminApprovalItemVO.builder()
                    .id(pr.getId())
                    .type(resolvePaymentType(pr.getRequestType()))
                    .applicantName(resolveUserName(pr.getApplicantId()))
                    .summary(buildPaymentSummary(pr))
                    .amount(pr.getAmount())
                    .applyTime(pr.getCreateTime())
                    .currentNodeName(pr.getStatus() == PaymentService.STATUS_PENDING ? "财务审批" : "审批中")
                    .build());
        }

        // 佣金审核
        List<Commission> pendingCommissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getAuditStatus, CommissionService.AUDIT_STATUS_PENDING)
                        .orderByDesc(Commission::getCreateTime));
        for (Commission c : pendingCommissions) {
            rows.add(AdminApprovalItemVO.builder()
                    .id(c.getId())
                    .type("commission")
                    .applicantName(resolveUserName(c.getCreateBy()))
                    .summary("佣金审核 - 应收: " + c.getReceivableAmount())
                    .amount(c.getReceivableAmount())
                    .applyTime(c.getCreateTime())
                    .currentNodeName("佣金审核")
                    .build());
        }

        rows.sort((a, b) -> {
            if (a.getApplyTime() == null) return 1;
            if (b.getApplyTime() == null) return -1;
            return b.getApplyTime().compareTo(a.getApplyTime());
        });
        return Result.ok(rows);
    }

    /** 审批详情 */
    @GetMapping("/{id}")
    public Result<AdminApprovalDetailVO> detail(@PathVariable Long id, @RequestParam String type) {
        return switch (type) {
            case "leave" -> Result.ok(buildLeaveDetail(id));
            case "compensate" -> Result.ok(buildCompensateDetail(id));
            case "expense", "advance", "prepay" -> Result.ok(buildPaymentDetail(id));
            case "commission" -> Result.ok(buildCommissionDetail(id));
            default -> Result.fail(400, "无效的审批类型: " + type);
        };
    }

    /** 执行审批（通过 / 驳回） */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody AdminApproveDTO dto) {
        Long approverId = StpUtil.getLoginIdAsLong();
        String type = dto.getType();
        if (type == null || type.isBlank()) {
            return Result.fail(400, "审批类型不能为空");
        }
        if (Boolean.FALSE.equals(dto.getApproved())
                && (dto.getReason() == null || dto.getReason().isBlank())) {
            return Result.fail(400, "驳回时必须填写驳回原因");
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

    // ========== 详情构建 ==========

    private AdminApprovalDetailVO buildLeaveDetail(Long id) {
        LeaveRequest lr = leaveRequestMapper.selectById(id);
        if (lr == null) throw new IllegalArgumentException("请假记录不存在");
        return AdminApprovalDetailVO.builder()
                .id(lr.getId())
                .type("leave")
                .applicantName(resolveUserName(lr.getUserId()))
                .summary(buildLeaveSummary(lr))
                .status(lr.getStatus())
                .applyTime(lr.getCreateTime())
                .leaveTypeLabel(resolveLeaveTypeLabel(lr.getLeaveType()))
                .startTime(lr.getStartTime())
                .endTime(lr.getEndTime())
                .days(calcLeaveDays(lr))
                .reason(lr.getReason())
                .histories(buildFlowHistories(ApprovalConstants.BUSINESS_TYPE_LEAVE, id))
                .build();
    }

    private AdminApprovalDetailVO buildCompensateDetail(Long id) {
        CompensateRequest cr = compensateRequestMapper.selectById(id);
        if (cr == null) throw new IllegalArgumentException("调休记录不存在");
        return AdminApprovalDetailVO.builder()
                .id(cr.getId())
                .type("compensate")
                .applicantName(resolveUserName(cr.getUserId()))
                .summary("调休申请 - " + cr.getCompensateDate())
                .status(cr.getStatus())
                .applyTime(cr.getCreateTime())
                .leaveTypeLabel("调休")
                .days(1.0)
                .reason(cr.getReason())
                .histories(buildFlowHistories(ApprovalConstants.BUSINESS_TYPE_COMPENSATE, id))
                .build();
    }

    private AdminApprovalDetailVO buildPaymentDetail(Long id) {
        PaymentRequest pr = paymentRequestMapper.selectById(id);
        if (pr == null) throw new IllegalArgumentException("付款申请不存在");
        List<PaymentApproval> approvals = paymentService.getApprovalHistory(id);
        List<AdminApprovalDetailVO.ApprovalHistory> histories = approvals.stream()
                .map(a -> AdminApprovalDetailVO.ApprovalHistory.builder()
                        .approverName(resolveUserName(a.getApproverId()))
                        .result(a.getResult())
                        .remark(a.getRemark())
                        .approvalTime(a.getApprovalTime())
                        .build())
                .toList();
        return AdminApprovalDetailVO.builder()
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

    private AdminApprovalDetailVO buildCommissionDetail(Long id) {
        Commission c = commissionMapper.selectById(id);
        if (c == null) throw new IllegalArgumentException("佣金记录不存在");
        List<AdminApprovalDetailVO.ApprovalHistory> histories = new ArrayList<>();
        if (c.getAuditStatus() != CommissionService.AUDIT_STATUS_PENDING && c.getAuditorId() != null) {
            histories.add(AdminApprovalDetailVO.ApprovalHistory.builder()
                    .approverName(resolveUserName(c.getAuditorId()))
                    .result(c.getAuditStatus() == CommissionService.AUDIT_STATUS_APPROVED ? 1 : 2)
                    .remark(c.getAuditRemark())
                    .approvalTime(c.getAuditTime())
                    .build());
        }
        return AdminApprovalDetailVO.builder()
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

    private List<AdminApprovalDetailVO.ApprovalHistory> buildFlowHistories(String businessType, Long businessId) {
        ApprovalProgressVO progress = approvalFlowService.getProgress(businessType, businessId);
        List<AdminApprovalDetailVO.ApprovalHistory> list = new ArrayList<>();
        for (ApprovalProgressVO.NodeView v : progress.getNodes()) {
            String approverDisplay = v.getApproverName();
            if (approverDisplay == null || approverDisplay.isBlank()) {
                approverDisplay = v.getCandidateApproverNames() == null || v.getCandidateApproverNames().isEmpty()
                        ? "无候选人"
                        : "候选: " + String.join(" / ", v.getCandidateApproverNames());
            }
            list.add(AdminApprovalDetailVO.ApprovalHistory.builder()
                    .nodeName(v.getNodeName())
                    .approverName(approverDisplay)
                    .result(v.getResult())
                    .remark(v.getRemark())
                    .approvalTime(v.getApprovalTime())
                    .build());
        }
        return list;
    }

    // ========== 审批执行 ==========

    private void approveFlow(String businessType, Long businessId, Long approverId, AdminApproveDTO dto) {
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

    // ========== 辅助 ==========

    private AdminApprovalItemVO buildLeaveOrCompensateRow(ApprovalRecordNode node) {
        if (ApprovalConstants.BUSINESS_TYPE_LEAVE.equals(node.getBusinessType())) {
            LeaveRequest lr = leaveRequestMapper.selectById(node.getBusinessId());
            if (lr == null) return null;
            return AdminApprovalItemVO.builder()
                    .id(lr.getId())
                    .type("leave")
                    .applicantName(resolveUserName(lr.getUserId()))
                    .summary(buildLeaveSummary(lr))
                    .applyTime(lr.getCreateTime())
                    .currentNodeName(node.getNodeName())
                    .leaveTypeLabel(resolveLeaveTypeLabel(lr.getLeaveType()))
                    .dateRange(buildLeaveDateRange(lr))
                    .days(calcLeaveDays(lr))
                    .reason(lr.getReason())
                    .build();
        }
        if (ApprovalConstants.BUSINESS_TYPE_COMPENSATE.equals(node.getBusinessType())) {
            CompensateRequest cr = compensateRequestMapper.selectById(node.getBusinessId());
            if (cr == null) return null;
            return AdminApprovalItemVO.builder()
                    .id(cr.getId())
                    .type("compensate")
                    .applicantName(resolveUserName(cr.getUserId()))
                    .summary("调休申请 - " + cr.getCompensateDate())
                    .applyTime(cr.getCreateTime())
                    .currentNodeName(node.getNodeName())
                    .leaveTypeLabel("调休")
                    .dateRange(cr.getCompensateDate() != null ? cr.getCompensateDate().toString() : null)
                    .days(1.0)
                    .reason(cr.getReason())
                    .build();
        }
        return null;
    }

    private String resolveUserName(Long userId) {
        if (userId == null) return "未知";
        SysUser u = userService.getById(userId);
        return u != null ? u.getNickname() : "未知";
    }

    private String buildLeaveSummary(LeaveRequest lr) {
        return resolveLeaveTypeLabel(lr.getLeaveType())
                + " " + lr.getStartTime().toLocalDate()
                + " ~ " + lr.getEndTime().toLocalDate();
    }

    private static final DateTimeFormatter MD_HM = DateTimeFormatter.ofPattern("M/d HH:mm");

    private String resolveLeaveTypeLabel(Integer leaveType) {
        if (leaveType == null) return "其他";
        return switch (leaveType) {
            case 1 -> "事假";
            case 2 -> "病假";
            case 3 -> "年假";
            case 4 -> "婚假";
            case 5 -> "产假";
            case 6 -> "调休";
            default -> "其他";
        };
    }

    /** 起止格式："5/9 09:00 ~ 5/11 18:00"；同一天则简化为 "5/9 09:00~12:00" */
    private String buildLeaveDateRange(LeaveRequest lr) {
        if (lr.getStartTime() == null || lr.getEndTime() == null) return null;
        String start = lr.getStartTime().format(MD_HM);
        if (lr.getStartTime().toLocalDate().equals(lr.getEndTime().toLocalDate())) {
            return start + "~" + lr.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        return start + " ~ " + lr.getEndTime().format(MD_HM);
    }

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    /** 请假天数计算（按考勤工时）。详见 {@link LeaveDaysUtil}。 */
    private Double calcLeaveDays(LeaveRequest lr) {
        if (lr == null) return null;
        LocalTime workStart = parseHHmm(systemConfigHelper.getAttendanceWorkStartTime(), LocalTime.of(9, 0));
        LocalTime workEnd = parseHHmm(systemConfigHelper.getAttendanceWorkEndTime(), LocalTime.of(18, 0));
        return LeaveDaysUtil.calcDays(lr.getStartTime(), lr.getEndTime(), workStart, workEnd);
    }

    private LocalTime parseHHmm(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return LocalTime.parse(value, HHMM);
        } catch (Exception e) {
            return fallback;
        }
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

    private static String resolvePaymentType(Integer requestType) {
        if (requestType == null) return "expense";
        return switch (requestType) {
            case PaymentService.TYPE_EXPENSE -> "expense";
            case PaymentService.TYPE_ADVANCE_COMMISSION -> "advance";
            case PaymentService.TYPE_PREPAY_COMMISSION -> "prepay";
            default -> "expense";
        };
    }
}
