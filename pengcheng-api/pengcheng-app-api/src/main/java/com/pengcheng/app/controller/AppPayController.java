package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.app.dto.AppPayCreateDTO;
import com.pengcheng.common.feature.FeatureFlags;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.common.result.Result;
import com.pengcheng.pay.PayServiceFactory;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import com.pengcheng.realty.payment.service.PaymentService;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * App 端真实支付下单控制器
 */
@RestController
@RequestMapping("/app/pay")
@RequiredArgsConstructor
@SaCheckLogin
@ConditionalOnProperty(prefix = FeatureFlags.WECHAT_PAY_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")
public class AppPayController {

    private final PaymentService paymentService;
    private final PayServiceFactory payServiceFactory;
    private final SysUserService userService;

    /**
     * 基于已审批的付款申请创建小程序支付参数。
     * 复用 payment_request.order_no 作为第三方业务单号，回调后直接落回同一条申请。
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody AppPayCreateDTO dto) {
        if (dto == null || dto.getRequestId() == null) {
            throw new BusinessException("付款申请ID不能为空");
        }

        String payType = dto.getPayType() == null || dto.getPayType().isBlank()
                ? "wechat"
                : dto.getPayType().trim().toLowerCase();
        if (!"wechat".equals(payType)) {
            throw new BusinessException("当前仅支持微信小程序支付");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        PaymentRequest request = paymentService.getPaymentRequestById(dto.getRequestId());
        if (request == null) {
            throw new BusinessException("付款申请不存在");
        }
        if (!userId.equals(request.getApplicantId())) {
            throw new BusinessException("仅可为自己的付款申请发起支付");
        }
        if (request.getStatus() == null || request.getStatus() != PaymentService.STATUS_APPROVED) {
            throw new BusinessException("付款申请尚未审批通过，无法发起支付");
        }
        if (request.getPayStatus() != null && request.getPayStatus() == PaymentService.PAY_STATUS_PAID) {
            throw new BusinessException("该付款申请已完成支付");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("付款申请金额无效");
        }

        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("当前登录用户不存在");
        }
        if (user.getOpenId() == null || user.getOpenId().isBlank()) {
            throw new BusinessException("当前账号未绑定微信 openId，请重新登录小程序");
        }

        Map<String, String> paymentParams = payServiceFactory.createMiniProgramPayOrder(
                request.getOrderNo(),
                amount,
                paymentService.buildPayDescription(request),
                user.getOpenId());

        paymentService.markPaying(request.getId(), payType);

        Map<String, Object> result = new HashMap<>(paymentParams);
        result.put("requestId", request.getId());
        result.put("orderNo", request.getOrderNo());
        result.put("payType", payType);
        result.put("amount", amount);
        result.put("relatedDealId", request.getRelatedDealId());
        return Result.ok(result);
    }
}
