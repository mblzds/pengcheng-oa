package com.pengcheng.admin.controller.pay;

import com.pengcheng.common.feature.FeatureFlags;
import com.pengcheng.common.result.Result;
import com.pengcheng.pay.WechatPayService;
import com.pengcheng.pay.WechatPayVerifyService;
import com.pengcheng.realty.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 微信支付回调通知控制器（迁自 temp-disabled）。
 */
@Slf4j
@RestController
@RequestMapping("/api/pay/wechat")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = FeatureFlags.WECHAT_PAY_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")
public class WechatPayNotifyController {

    private final WechatPayVerifyService verifyService;
    private final WechatPayService wechatPayService;
    private final PaymentService paymentService;

    @PostMapping("/notify")
    public String handleNotify(
            @RequestBody String body,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Serial") String serialNo,
            @RequestHeader(value = "Wechatpay-Signature-Type", required = false) String signatureType) {

        log.info("[微信支付回调] 收到通知 serial={}", serialNo);

        if (!verifyService.verifyTimestamp(timestamp)) {
            log.error("[微信支付回调] 时间戳校验失败");
            return buildResponse("FAIL", "时间戳校验失败");
        }
        if (!verifyService.verifySignature(body, signature, nonce, timestamp, serialNo)) {
            log.error("[微信支付回调] 验签失败");
            return buildResponse("FAIL", "验签失败");
        }

        WechatPayVerifyService.WechatPayNotifyResource resource = verifyService.parseNotifyBody(body);
        if (resource == null) {
            log.error("[微信支付回调] 解析失败");
            return buildResponse("FAIL", "解析失败");
        }

        log.info("[微信支付回调] 解析成功 orderNo={} txId={} state={} amount={}",
                resource.getOutTradeNo(), resource.getTransactionId(),
                resource.getTradeState(), resource.getAmount());

        if (!"SUCCESS".equals(resource.getTradeState())) {
            log.warn("[微信支付回调] 非成功状态：{} - {}",
                    resource.getOutTradeNo(), resource.getTradeState());
            return buildResponse("SUCCESS", "状态已忽略");
        }

        boolean ok;
        try {
            ok = paymentService.updatePayStatus(
                    resource.getOutTradeNo(),
                    resource.getTransactionId(),
                    "wechat",
                    resource.getAmount() / 100.0,
                    // 微信无独立 notifyId，用交易号+时间戳组合保证幂等
                    resource.getTransactionId() + ":" + timestamp,
                    body);
        } catch (Exception e) {
            log.error("[微信支付回调] 更新异常 orderNo={}", resource.getOutTradeNo(), e);
            return buildResponse("FAIL", "更新异常");
        }

        return ok ? buildResponse("SUCCESS", "成功")
                  : buildResponse("FAIL", "订单状态更新失败");
    }

    private String buildResponse(String code, String message) {
        return String.format("{\"code\":\"%s\",\"message\":\"%s\"}", code, message);
    }

    @GetMapping("/order/{orderNo}")
    public Result<Map<String, Object>> queryOrder(@PathVariable String orderNo) {
        try {
            return Result.ok(wechatPayService.queryOrder(orderNo));
        } catch (Exception e) {
            log.error("查询微信订单失败：{}", orderNo, e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/refund")
    public Result<Map<String, Object>> refund(@RequestBody RefundRequest req) {
        try {
            return Result.ok(wechatPayService.refund(
                    req.getOutTradeNo(), req.getOutRefundNo(), req.getAmount(), req.getReason()));
        } catch (Exception e) {
            log.error("微信退款失败", e);
            return Result.fail("退款失败：" + e.getMessage());
        }
    }

    @lombok.Data
    public static class RefundRequest {
        private String outTradeNo;
        private String outRefundNo;
        private Integer amount;
        private String reason;
    }
}
