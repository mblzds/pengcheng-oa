package com.pengcheng.pay;

import com.pengcheng.common.feature.FeatureFlags;
import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * 微信支付回调验签服务
 * 基于微信支付 API v3 官方验签逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = FeatureFlags.WECHAT_PAY_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")
public class WechatPayVerifyService {

    private final SystemConfigHelper configHelper;

    /**
     * 验证微信支付回调签名
     *
     * @param body      回调请求体
     * @param signature 签名（Wechatpay-Signature 头）
     * @param nonce     随机串（Wechatpay-Nonce 头）
     * @param timestamp 时间戳（Wechatpay-Timestamp 头）
     * @param serialNo  证书序列号（Wechatpay-Serial 头）
     * @return 验签是否通过
     */
    public boolean verifySignature(String body, String signature, String nonce, 
                                   String timestamp, String serialNo) {
        try {
            if (isBlank(body) || isBlank(signature) || isBlank(nonce) || isBlank(timestamp) || isBlank(serialNo)) {
                log.error("微信支付回调请求头或请求体缺失");
                return false;
            }

            X509Certificate certificate = getPlatformCertificate(serialNo);
            if (certificate == null) {
                log.error("未找到微信支付平台证书，serialNo={}", serialNo);
                return false;
            }

            String signStr = buildVerifyString(body, nonce, timestamp);
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(certificate.getPublicKey());
            sign.update(signStr.getBytes(StandardCharsets.UTF_8));

            boolean verified = sign.verify(Base64.getDecoder().decode(signature));
            
            if (!verified) {
                log.error("微信支付回调验签失败");
            } else {
                log.info("微信支付回调验签成功");
            }

            return verified;

        } catch (Exception e) {
            log.error("微信支付回调验签异常", e);
            return false;
        }
    }

    /**
     * 构建验签字符串
     * 格式：timestamp\nnonce\nbody\n
     */
    private String buildVerifyString(String body, String nonce, String timestamp) {
        return timestamp + "\n" + nonce + "\n" + body + "\n";
    }

    /**
     * 获取微信支付平台证书
     */
    private X509Certificate getPlatformCertificate(String serialNo) {
        try {
            String certContent = PaymentConfigSupport.getProviderString(configHelper, "wechatPay", "platformCert", null);
            if (isBlank(certContent)) {
                log.error("未配置微信支付平台证书，serialNo={}", serialNo);
                return null;
            }

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            String certStr = certContent
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
            
            return (X509Certificate) cf.generateCertificate(
                new java.io.ByteArrayInputStream(Base64.getDecoder().decode(certStr))
            );
        } catch (Exception e) {
            log.error("加载证书失败", e);
            return null;
        }
    }

    /**
     * 验证回调时间戳（防止重放攻击）
     * 时间超过 5 分钟的请求视为无效
     */
    public boolean verifyTimestamp(String timestamp) {
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            long diff = Math.abs(currentTime - requestTime);
            
            // 允许 5 分钟的时间差
            if (diff > 300) {
                log.warn("回调时间戳过期，diff={}s", diff);
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            log.error("时间戳格式错误", e);
            return false;
        }
    }

    /**
     * 解析回调请求体
     */
    public WechatPayNotifyResource parseNotifyBody(String body) {
        try {
            if (isBlank(body)) {
                log.error("微信支付回调 body 为空");
                return null;
            }
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            WechatPayNotifyRequest request = mapper.readValue(body, WechatPayNotifyRequest.class);
            if (request.getResource() == null) {
                log.error("微信支付回调 resource 为空");
                return null;
            }
            
            String decryptedContent = decryptResource(
                    request.getResource().getAssociatedData(),
                    request.getResource().getNonce(),
                    request.getResource().getCiphertext()
            );
            
            WechatPayNotifyResource resource = new WechatPayNotifyResource();
            resource.setOriginalBody(decryptedContent);
            
            // 解析解密后的内容
            com.fasterxml.jackson.databind.JsonNode contentNode = mapper.readTree(decryptedContent);
            resource.setOutTradeNo(contentNode.get("out_trade_no").asText());
            resource.setTransactionId(contentNode.get("transaction_id").asText());
            resource.setTradeState(contentNode.get("trade_state").asText());
            resource.setAmount(contentNode.get("amount").get("total").asInt());
            
            return resource;
        } catch (Exception e) {
            log.error("解析回调请求体失败", e);
            return null;
        }
    }

    /**
     * 解密回调数据（AES-256-GCM）
     */
    private String decryptResource(String associatedData, String nonce, String ciphertext) throws Exception {
        if (isBlank(nonce) || isBlank(ciphertext)) {
            throw new IllegalArgumentException("微信支付回调解密参数缺失");
        }
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        String apiKey = PaymentConfigSupport.getProviderString(configHelper, "wechatPay", "apiV3Key", null);
        if (isBlank(apiKey)) {
            throw new IllegalStateException("未配置微信支付 APIv3 密钥");
        }
        if (apiKey.length() != 32) {
            throw new IllegalStateException("微信支付 APIv3 密钥长度非法");
        }
        byte[] keyBytes = apiKey.getBytes(StandardCharsets.UTF_8);
        
        javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(
            128, nonce.getBytes(StandardCharsets.UTF_8)
        );
        
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, spec);
        if (associatedData != null && !associatedData.isEmpty()) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        byte[] result = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(result, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 回调请求体结构
     */
    @lombok.Data
    public static class WechatPayNotifyRequest {
        private String id;
        private String create_time;
        private String resource_type;
        private String event_type;
        private String summary;
        private WechatPayResource resource;
    }

    /**
     * 回调资源结构
     */
    @lombok.Data
    public static class WechatPayResource {
        private String original_type;
        private String algorithm;
        private String ciphertext;
        @com.fasterxml.jackson.annotation.JsonProperty("associated_data")
        private String associatedData;
        private String nonce;
    }

    /**
     * 解析后的回调资源
     */
    @lombok.Data
    public static class WechatPayNotifyResource {
        private String originalBody;
        private String outTradeNo;      // 商户订单号
        private String transactionId;   // 微信支付订单号
        private String tradeState;      // 交易状态
        private int amount;             // 订单金额（分）
    }
}
