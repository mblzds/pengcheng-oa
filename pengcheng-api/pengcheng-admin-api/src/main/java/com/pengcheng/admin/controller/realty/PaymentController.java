package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.payment.dto.PaymentApprovalDTO;
import com.pengcheng.realty.payment.dto.PaymentQueryDTO;
import com.pengcheng.realty.payment.dto.PaymentRequestDTO;
import com.pengcheng.realty.payment.dto.PaymentVO;
import com.pengcheng.realty.payment.entity.PaymentApproval;
import com.pengcheng.admin.controller.hr.AttendanceScopeHelper;
import com.pengcheng.realty.payment.service.PaymentService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 付款申请管理控制器
 */
@RestController
@RequestMapping("/admin/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AttendanceScopeHelper scopeHelper;

    /**
     * 创建付款申请
     */
    @PostMapping("/create")
    @SaCheckPermission("realty:payment:add")
    @Log(title = "付款申请", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody PaymentRequestDTO dto) {
        return Result.ok(paymentService.createPaymentRequest(dto));
    }

    /**
     * 审批付款申请
     */
    @PostMapping("/approve")
    @SaCheckPermission("realty:payment:approve")
    @Log(title = "付款审批", businessType = BusinessType.UPDATE)
    public Result<Void> approve(@RequestBody PaymentApprovalDTO dto) {
        paymentService.approvePaymentRequest(dto);
        return Result.ok();
    }

    /**
     * 分页查询付款申请
     */
    @GetMapping("/page")
    @SaCheckPermission("realty:payment:list")
    public Result<PageResult<PaymentVO>> page(PaymentQueryDTO query) {
        // 双层可见性收口：基础职级 data_scope + 付款模块加成（默认 finance）
        // null = 全员可见，service 不加 WHERE；非空集合 = 限定为这些 applicantId
        query.setAllowedApplicantIds(scopeHelper.visibleUserIdsForPayment());
        return Result.ok(paymentService.pagePaymentRequests(query));
    }

    /**
     * 查询审批流转历史
     */
    @GetMapping("/approvals")
    public Result<List<PaymentApproval>> approvals(@RequestParam Long requestId) {
        return Result.ok(paymentService.getApprovalHistory(requestId));
    }
}
