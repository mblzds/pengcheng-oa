package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.app.dto.AppApproveDTO;
import com.pengcheng.app.dto.ApprovalDetailVO;
import com.pengcheng.app.dto.ApprovalPendingVO;
import com.pengcheng.common.result.Result;
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

    /**
     * 待审批列表（聚合查询）
     * 聚合 LeaveRequest(status=1) + CompensateRequest(status=1) + PaymentRequest(status=1/2) + Commission(auditStatus=1)
     * 按类型分组返回
     */
    @GetMapping("/pending")
    public Result<ApprovalPendingVO> pending() {
        // 待审批请假
        List<LeaveRequest> pendingLeaves = leaveRequestMapper.selectList(
                new LambdaQueryWrapper<LeaveRequest>().eq(LeaveRequest::getStatus, 1)
                        .orderByDesc(LeaveRequest::getCreateTime));

        // 待审批调休
        List<CompensateRequest> pendingCompensates = compensateRequestMapper.selectList(
                new LambdaQueryWrapper<CompensateRequest>().eq(CompensateRequest::getStatus, 1)
                        .orderByDesc(CompensateRequest::getCreateTime));

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

        // 构建请假/调休审批项
        List<ApprovalPendingVO.ApprovalItem> leaveItems = new ArrayList<>();
        for (LeaveRequest lr : pendingLeaves) {
            leaveItems.add(ApprovalPendingVO.ApprovalItem.builder()
                    .id(lr.getId())
                    .type("leave")
                    .applicantName(resolveUserName(lr.getUserId()))
                    .summary(buildLeaveSummary(lr))
                    .applyTime(lr.getCreateTime())
                    .build());
        }
        for (CompensateRequest cr : pendingCompensates) {
            leaveItems.add(ApprovalPendingVO.ApprovalItem.builder()
                    .id(cr.getId())
                    .type("compensate")
                    .applicantName(resolveUserName(cr.getUserId()))
                    .summary("调休申请 - " + cr.getCompensateDate())
                    .applyTime(cr.getCreateTime())
                    .build());
        }

        // 构建付款审批项
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

        // 构建佣金审核项
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

        ApprovalPendingVO vo = ApprovalPendingVO.builder()
                .leaveItems(leaveItems)
                .paymentItems(paymentItems)
                .commissionItems(commissionItems)
                .totalCount(totalCount)
                .build();

        return Result.ok(vo);
    }

    /**
     * 审批详情
     * 根据 type 参数查询对应业务实体，返回审批详情（含审批流转历史时间线）
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
     * 根据 type 分发到对应 Service 的审批方法
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody AppApproveDTO dto) {
        Long approverId = StpUtil.getLoginIdAsLong();
        String type = dto.getType();

        if (type == null || type.isBlank()) {
            return Result.fail(400, "审批类型不能为空");
        }

        switch (type) {
            case "leave" -> approveLeave(id, dto.getApproved());
            case "compensate" -> approveCompensate(id, dto.getApproved());
            case "expense", "advance", "prepay" -> approvePayment(id, approverId, dto.getApproved(), dto.getReason());
            case "commission" -> approveCommission(id, approverId, dto.getApproved(), dto.getReason());
            default -> {
                return Result.fail(400, "无效的审批类型: " + type);
            }
        }
        return Result.ok();
    }

    // ========== 审批详情构建 ==========

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
                .histories(List.of()) // 请假为单级审批，无流转历史
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
                .histories(List.of())
                .build();
    }

    private ApprovalDetailVO buildPaymentDetail(Long id) {
        PaymentRequest pr = paymentRequestMapper.selectById(id);
        if (pr == null) {
            throw new IllegalArgumentException("付款申请不存在");
        }
        // 查询审批流转历史
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

    private void approveLeave(Long id, Boolean approved) {
        LeaveRequest lr = leaveRequestMapper.selectById(id);
        if (lr == null) {
            throw new IllegalArgumentException("请假记录不存在");
        }
        if (lr.getStatus() != 1) {
            throw new ApprovalFlowException("该申请已完成审批，不可重复操作");
        }
        lr.setStatus(approved ? 2 : 3);
        leaveRequestMapper.updateById(lr);
    }

    private void approveCompensate(Long id, Boolean approved) {
        CompensateRequest cr = compensateRequestMapper.selectById(id);
        if (cr == null) {
            throw new IllegalArgumentException("调休记录不存在");
        }
        if (cr.getStatus() != 1) {
            throw new ApprovalFlowException("该申请已完成审批，不可重复操作");
        }
        cr.setStatus(approved ? 2 : 3);
        compensateRequestMapper.updateById(cr);
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
