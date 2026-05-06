package com.pengcheng.pay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.pengcheng.common.feature.FeatureFlags;
import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * 支付宝支付回调验签服务
 * 基于支付宝官方 SDK 验签逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = FeatureFlags.ALIPAY_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")
public class AlipayVerifyService {

    private final SystemConfigHelper configHelper;
    private final StringRedisTemplate redisTemplate;

    private static final String PROCESSED_NOTIFY_KEY_PREFIX = "pay:alipay:notify:";
    private static final long PROCESSED_NOTIFY_TTL_HOURS = 24;

    /**
     * 验证支付宝回调签名
     *
     * @param params 回调参数（去除 sign 和 sign_type）
     * @return 验签是否通过
     */
    public boolean verifySignature(Map<String, String> params) {
        try {
            String alipayPublicKey = PaymentConfigSupport.getProviderString(configHelper, "alipay", "publicKey", null);
            if (alipayPublicKey == null || alipayPublicKey.isEmpty()) {
                log.error("支付宝公钥未配置");
                return false;
            }

            // 获取签名类型，默认 RSA2
            String signType = params.getOrDefault("sign_type", "RSA2");
            
            // 获取字符集，默认 UTF-8
            String charset = params.getOrDefault("charset", "UTF-8");

            // 移除 sign 和 sign_type 参数
            Map<String, String> signParams = new HashMap<>(params);
            signParams.remove("sign");
            signParams.remove("sign_type");

            // 调用支付宝 SDK 验签
            boolean verified = AlipaySignature.rsaCheckV1(
                signParams,
                alipayPublicKey,
                charset,
                signType
            );

            if (!verified) {
                log.error("支付宝回调验签失败");
            } else {
                log.info("支付宝回调验签成功");
            }

            return verified;

        } catch (AlipayApiException e) {
            log.error("支付宝回调验签异常", e);
            return false;
        }
    }

    /**
     * 验证回调参数完整性
     */
    public boolean verifyParams(Map<String, String> params) {
        // 必选参数检查
        String[] requiredFields = {"notify_id", "out_trade_no", "trade_no", "trade_status", "total_amount"};
        
        for (String field : requiredFields) {
            if (!params.containsKey(field) || params.get(field) == null || params.get(field).isEmpty()) {
                log.error("支付宝回调缺少必选参数：{}", field);
                return false;
            }
        }

        return true;
    }

    /**
     * 解析回调参数
     */
    public AlipayNotifyResult parseNotifyResult(Map<String, String> params) {
        AlipayNotifyResult result = new AlipayNotifyResult();
        
        result.setOutTradeNo(params.get("out_trade_no"));      // 商户订单号
        result.setTradeNo(params.get("trade_no"));              // 支付宝交易号
        result.setTradeStatus(params.get("trade_status"));      // 交易状态
        result.setTotalAmount(params.get("total_amount"));      // 订单金额
        result.setBuyerId(params.get("buyer_id"));              // 买家支付宝用户 ID
        result.setBuyerLogonId(params.get("buyer_logon_id"));   // 买家支付宝账号
        result.setSellerId(params.get("seller_id"));            // 卖家支付宝用户 ID
        result.setGmtPayment(params.get("gmt_payment"));        // 付款时间
        result.setAppId(params.get("app_id"));                  // AppID
        result.setNotifyType(params.get("notify_type"));        // 通知类型
        result.setNotifyId(params.get("notify_id"));            // 通知 ID
        result.setNotifyTime(params.get("notify_time"));        // 通知时间
        result.setSubject(params.get("subject"));               // 订单标题
        result.setBody(params.get("body"));                     // 订单描述
        
        return result;
    }

    /**
     * 验证交易状态
     */
    public boolean isValidTradeStatus(String tradeStatus) {
        // 只有 TRADE_SUCCESS 和 TRADE_FINISHED 才是有效支付状态
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    /**
     * 是否已处理过该通知（幂等性检查）
     */
    public boolean isProcessed(String notifyId) {
        if (notifyId == null || notifyId.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PROCESSED_NOTIFY_KEY_PREFIX + notifyId));
        } catch (Exception e) {
            log.warn("支付宝回调幂等检查失败 notifyId={}", notifyId, e);
            return false;
        }
    }

    /**
     * 标记通知已处理
     */
    public void markProcessed(String notifyId) {
        if (notifyId == null || notifyId.isBlank()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    PROCESSED_NOTIFY_KEY_PREFIX + notifyId,
                    "1",
                    PROCESSED_NOTIFY_TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("支付宝回调幂等标记失败 notifyId={}", notifyId, e);
        }
    }

    /**
     * 支付宝回调结果
     */
    @lombok.Data
    public static class AlipayNotifyResult {
        private String outTradeNo;        // 商户订单号
        private String tradeNo;           // 支付宝交易号
        private String tradeStatus;       // 交易状态
        private String totalAmount;       // 订单金额
        private String buyerId;           // 买家支付宝用户 ID
        private String buyerLogonId;      // 买家支付宝账号
        private String sellerId;          // 卖家支付宝用户 ID
        private String gmtPayment;        // 付款时间
        private String appId;             // AppID
        private String notifyType;        // 通知类型
        private String notifyId;          // 通知 ID
        private String notifyTime;        // 通知时间
        private String subject;           // 订单标题
        private String body;              // 订单描述
    }
}
