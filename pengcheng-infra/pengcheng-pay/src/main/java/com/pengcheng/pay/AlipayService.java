package com.pengcheng.pay;

import cn.hutool.core.util.IdUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.pengcheng.common.feature.FeatureFlags;
import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = FeatureFlags.ALIPAY_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")
public class AlipayService extends AbstractPayService {

    private final SystemConfigHelper configHelper;

    @Override
    public Map<String, String> createTestOrder() {
        Map<String, String> result = new HashMap<>();
        String orderNo = "ALI" + IdUtil.getSnowflakeNextIdStr();
        result.put("orderNo", orderNo);

        try {
            JsonNode config = PaymentConfigSupport.getProviderConfig(configHelper, "alipay");
            if (config == null) {
                throw new RuntimeException("支付宝配置不存在");
            }

            boolean enabled = PaymentConfigSupport.getBoolean(config, "enabled", false);
            if (!enabled) {
                throw new RuntimeException("支付宝支付未启用");
            }

            String appId = PaymentConfigSupport.getString(config, "appId");
            String privateKey = PaymentConfigSupport.getString(config, "privateKey");
            String publicKey = PaymentConfigSupport.getString(config, "publicKey");
            String signType = config.has("signType") ? config.get("signType").asText() : "RSA2";
            String gatewayUrl = config.has("gatewayUrl") ? config.get("gatewayUrl").asText() : "https://openapi.alipay.com/gateway.do";
            String notifyUrl = PaymentConfigSupport.getString(config, "notifyUrl");

            if (appId.isEmpty() || privateKey.isEmpty() || publicKey.isEmpty()) {
                throw new RuntimeException("支付宝配置不完整，请检查AppID、应用私钥、支付宝公钥");
            }

            // 创建支付宝客户端
            AlipayClient alipayClient = new DefaultAlipayClient(
                    gatewayUrl,
                    appId,
                    privateKey,
                    "json",
                    "UTF-8",
                    publicKey,
                    signType
            );

            // 创建当面付预下单请求
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(notifyUrl);
            request.setBizContent("{" +
                    "\"out_trade_no\":\"" + orderNo + "\"," +
                    "\"total_amount\":\"0.01\"," +
                    "\"subject\":\"支付测试订单\"" +
                    "}");

            // 执行请求
            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                String qrCode = response.getQrCode();
                // 生成二维码图片
                String qrcodeBase64 = generateQRCode(qrCode);
                result.put("qrcode", qrcodeBase64);
                result.put("payUrl", qrCode);

                log.info("支付宝测试订单创建成功: orderNo={}, qrCode={}", orderNo, qrCode);
            } else {
                log.error("支付宝下单失败: code={}, msg={}, subCode={}, subMsg={}",
                    response.getCode(), response.getMsg(), response.getSubCode(), response.getSubMsg());
                throw new RuntimeException("支付宝下单失败: " + response.getSubMsg());
            }

        } catch (AlipayApiException e) {
            log.error("创建支付宝测试订单失败", e);
            throw new RuntimeException("创建支付宝测试订单失败: " + e.getErrMsg());
        } catch (Exception e) {
            log.error("创建支付宝测试订单失败", e);
            throw new RuntimeException("创建支付宝测试订单失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public String getPayType() {
        return "alipay";
    }

    @Override
    public String getPayTypeName() {
        return "支付宝";
    }

    /**
     * 查询订单状态
     */
    public Map<String, Object> queryOrder(String orderNo) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            JsonNode config = PaymentConfigSupport.getProviderConfig(configHelper, "alipay");
            if (config == null) {
                throw new RuntimeException("支付宝配置不存在");
            }
            
            String appId = PaymentConfigSupport.getString(config, "appId");
            String privateKey = PaymentConfigSupport.getString(config, "privateKey");
            String publicKey = PaymentConfigSupport.getString(config, "publicKey");
            String gatewayUrl = config.has("gatewayUrl") ? 
                config.get("gatewayUrl").asText() : "https://openapi.alipay.com/gateway.do";
            
            AlipayClient alipayClient = new DefaultAlipayClient(
                gatewayUrl, appId, privateKey, "json", "UTF-8", publicKey, "RSA2"
            );
            
            com.alipay.api.request.AlipayTradeQueryRequest request = 
                new com.alipay.api.request.AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
            
            com.alipay.api.response.AlipayTradeQueryResponse response = 
                alipayClient.execute(request);
            
            if (response.isSuccess()) {
                result.put("tradeStatus", response.getTradeStatus());
                result.put("tradeNo", response.getTradeNo());
                result.put("totalAmount", response.getTotalAmount());
                result.put("buyerId", response.getBuyerUserId());
            } else {
                log.error("支付宝查询失败：code={}, msg={}, subCode={}, subMsg={}",
                    response.getCode(), response.getMsg(), 
                    response.getSubCode(), response.getSubMsg());
                throw new RuntimeException("查询失败：" + response.getSubMsg());
            }
            
        } catch (Exception e) {
            log.error("查询订单失败：{}", orderNo, e);
            throw new RuntimeException("查询失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 申请退款
     */
    public Map<String, Object> refund(String outTradeNo, String outRefundNo, 
                                       Double amount, String reason) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            JsonNode config = PaymentConfigSupport.getProviderConfig(configHelper, "alipay");
            if (config == null) {
                throw new RuntimeException("支付宝配置不存在");
            }
            
            String appId = PaymentConfigSupport.getString(config, "appId");
            String privateKey = PaymentConfigSupport.getString(config, "privateKey");
            String publicKey = PaymentConfigSupport.getString(config, "publicKey");
            String gatewayUrl = config.has("gatewayUrl") ? 
                config.get("gatewayUrl").asText() : "https://openapi.alipay.com/gateway.do";
            
            AlipayClient alipayClient = new DefaultAlipayClient(
                gatewayUrl, appId, privateKey, "json", "UTF-8", publicKey, "RSA2"
            );
            
            com.alipay.api.request.AlipayTradeRefundRequest request = 
                new com.alipay.api.request.AlipayTradeRefundRequest();
            request.setBizContent("{" +
                "\"out_trade_no\":\"" + outTradeNo + "\"," +
                "\"refund_amount\":" + amount + "," +
                "\"out_request_no\":\"" + outRefundNo + "\"," +
                "\"refund_reason\":\"" + reason + "\"" +
                "}");
            
            com.alipay.api.response.AlipayTradeRefundResponse response = 
                alipayClient.execute(request);
            
            if (response.isSuccess()) {
                result.put("fundChange", response.getFundChange());
                result.put("refundAmount", response.getRefundFee());
            } else {
                log.error("支付宝退款失败：code={}, msg={}, subCode={}, subMsg={}",
                    response.getCode(), response.getMsg(), 
                    response.getSubCode(), response.getSubMsg());
                throw new RuntimeException("退款失败：" + response.getSubMsg());
            }
            
        } catch (Exception e) {
            log.error("申请退款失败", e);
            throw new RuntimeException("退款失败：" + e.getMessage());
        }
        
        return result;
    }

}
