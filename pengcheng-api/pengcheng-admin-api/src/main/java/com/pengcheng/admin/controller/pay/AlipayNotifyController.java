package com.pengcheng.admin.controller.pay;

import com.pengcheng.common.feature.FeatureFlags;
import com.pengcheng.common.result.Result;
import com.pengcheng.pay.AlipayService;
import com.pengcheng.pay.AlipayVerifyService;
import com.pengcheng.realty.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付宝回调通知控制器
 * <p>
 * 迁自 temp-disabled，搭配 {@link PaymentService#updatePayStatus(String, String, String, Double, String, String)}。
 * 幂等策略：
 * <ol>
 *   <li>{@link AlipayVerifyService#isProcessed(String)} 本地缓存/库去重</li>
 *   <li>PaymentService 侧 pay_notify_log 唯一索引兜底</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/pay/alipay")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = FeatureFlags.ALIPAY_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")
public class AlipayNotifyController {

    private final AlipayVerifyService verifyService;
    private final AlipayService alipayService;
    private final PaymentService paymentService;

    /** 支付宝异步回调，form-urlencoded */
    @PostMapping("/notify")
    public String handleNotify(@RequestParam Map<String, String> params) {
        log.info("[支付宝回调] 收到通知");

        if (!verifyService.verifyParams(params)) {
            log.error("[支付宝回调] 参数校验失败");
            return "failure";
        }
        if (!verifyService.verifySignature(params)) {
            log.error("[支付宝回调] 验签失败");
            return "failure";
        }

        AlipayVerifyService.AlipayNotifyResult result = verifyService.parseNotifyResult(params);
        log.info("[支付宝回调] 解析成功 orderNo={} tradeNo={} status={} amount={}",
                result.getOutTradeNo(), result.getTradeNo(),
                result.getTradeStatus(), result.getTotalAmount());

        if (verifyService.isProcessed(result.getNotifyId())) {
            log.info("[支付宝回调] 已处理，忽略：{}", result.getNotifyId());
            return "success";
        }

        if (!verifyService.isValidTradeStatus(result.getTradeStatus())) {
            log.warn("[支付宝回调] 非成功状态，跳过：{} - {}",
                    result.getOutTradeNo(), result.getTradeStatus());
            verifyService.markProcessed(result.getNotifyId());
            return "success";
        }

        boolean ok;
        try {
            ok = paymentService.updatePayStatus(
                    result.getOutTradeNo(),
                    result.getTradeNo(),
                    "alipay",
                    Double.parseDouble(result.getTotalAmount()),
                    result.getNotifyId(),
                    params.toString());
        } catch (Exception e) {
            log.error("[支付宝回调] 更新订单异常 orderNo={}", result.getOutTradeNo(), e);
            return "failure";
        }

        if (ok) {
            verifyService.markProcessed(result.getNotifyId());
            return "success";
        }
        return "failure";
    }

    @GetMapping("/order/{orderNo}")
    public Result<Map<String, Object>> queryOrder(@PathVariable String orderNo) {
        try {
            return Result.ok(alipayService.queryOrder(orderNo));
        } catch (Exception e) {
            log.error("查询支付宝订单失败：{}", orderNo, e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/refund")
    public Result<Map<String, Object>> refund(@RequestBody RefundRequest req) {
        try {
            return Result.ok(alipayService.refund(
                    req.getOutTradeNo(), req.getOutRefundNo(), req.getAmount(), req.getReason()));
        } catch (Exception e) {
            log.error("支付宝退款失败", e);
            return Result.fail("退款失败：" + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public Result<Boolean> verify(@RequestParam Map<String, String> params) {
        return Result.ok(verifyService.verifySignature(params));
    }

    @lombok.Data
    public static class RefundRequest {
        private String outTradeNo;
        private String outRefundNo;
        private Double amount;
        private String reason;
    }
}
