package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.app.dto.AppAdvanceDTO;
import com.pengcheng.app.dto.AppExpenseDTO;
import com.pengcheng.app.dto.AppPrepayDTO;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.payment.dto.PaymentQueryDTO;
import com.pengcheng.realty.payment.dto.PaymentRequestDTO;
import com.pengcheng.realty.payment.dto.PaymentVO;
import com.pengcheng.realty.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * App端付款申请控制器
 * 提供费用报销、垫佣申请、预付佣申请、申请记录查询接口
 */
@RestController
@RequestMapping("/app/payment")
@RequiredArgsConstructor
@SaCheckLogin
public class AppPaymentController {

    private final PaymentService paymentService;

    /**
     * 提交费用报销申请
     * 请求体含 expenseType/amount/occurTime/description/attachments
     */
    @PostMapping("/expense")
    public Result<Long> expense(@RequestBody AppExpenseDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        String attachmentsJson = null;
        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            attachmentsJson = String.join(",", dto.getAttachments());
        }

        PaymentRequestDTO requestDTO = PaymentRequestDTO.builder()
                .applicantId(userId)
                .requestType(PaymentService.TYPE_EXPENSE)
                .expenseType(dto.getExpenseType())
                .amount(dto.getAmount())
                .description(dto.getDescription())
                .occurTime(dto.getOccurTime())
                .attachments(attachmentsJson)
                .build();

        Long requestId = paymentService.createPaymentRequest(requestDTO);
        return Result.ok(requestId);
    }

    /**
     * 提交垫佣申请
     * 请求体含 dealId/amount/reason
     */
    @PostMapping("/advance")
    public Result<Long> advance(@RequestBody AppAdvanceDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        PaymentRequestDTO requestDTO = PaymentRequestDTO.builder()
                .applicantId(userId)
                .requestType(PaymentService.TYPE_ADVANCE_COMMISSION)
                .amount(dto.getAmount())
                .relatedDealId(dto.getDealId())
                .description(dto.getReason())
                .build();

        Long requestId = paymentService.createPaymentRequest(requestDTO);
        return Result.ok(requestId);
    }

    /**
     * 提交预付佣申请
     * 请求体含 allianceId/dealId/amount/reason
     */
    @PostMapping("/prepay")
    public Result<Long> prepay(@RequestBody AppPrepayDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        PaymentRequestDTO requestDTO = PaymentRequestDTO.builder()
                .applicantId(userId)
                .requestType(PaymentService.TYPE_PREPAY_COMMISSION)
                .amount(dto.getAmount())
                .relatedDealId(dto.getDealId())
                .relatedAllianceId(dto.getAllianceId())
                .description(dto.getReason())
                .build();

        Long requestId = paymentService.createPaymentRequest(requestDTO);
        return Result.ok(requestId);
    }

    /**
     * 查询付款申请记录
     * 支持 type/status/page/pageSize 参数
     */
    @GetMapping("/list")
    public Result<PageResult<PaymentVO>> list(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long userId = StpUtil.getLoginIdAsLong();

        PaymentQueryDTO query = PaymentQueryDTO.builder()
                .applicantId(userId)
                .requestType(type)
                .status(status)
                .page(page)
                .pageSize(pageSize)
                .build();

        PageResult<PaymentVO> result = paymentService.pagePaymentRequests(query);
        return Result.ok(result);
    }
}
