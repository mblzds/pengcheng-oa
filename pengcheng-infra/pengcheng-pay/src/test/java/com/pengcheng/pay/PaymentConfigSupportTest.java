package com.pengcheng.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.helper.SystemConfigHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentConfigSupport")
class PaymentConfigSupportTest {

    @Test
    @DisplayName("优先读取 payment 分组下的支付配置")
    void prefersNestedPaymentGroup() {
        SystemConfigHelper configHelper = new StubSystemConfigHelper(Map.of(
                "payment", "{\"wechatPay\":{\"apiV3Key\":\"nested-key\"}}",
                "wechatPay", "{\"apiV3Key\":\"legacy-key\"}"
        ));

        JsonNode config = PaymentConfigSupport.getProviderConfig(configHelper, "wechatPay");

        assertThat(PaymentConfigSupport.getString(config, "apiV3Key")).isEqualTo("nested-key");
    }

    @Test
    @DisplayName("新分组缺字段时使用旧分组补缺")
    void mergesLegacyValuesWhenNestedConfigIsPartial() {
        SystemConfigHelper configHelper = new StubSystemConfigHelper(Map.of(
                "payment", "{\"wechatPay\":{\"apiV3Key\":\"nested-key\"}}",
                "wechatPay", "{\"platformCert\":\"legacy-cert\"}"
        ));

        JsonNode config = PaymentConfigSupport.getProviderConfig(configHelper, "wechatPay");

        assertThat(PaymentConfigSupport.getString(config, "apiV3Key")).isEqualTo("nested-key");
        assertThat(PaymentConfigSupport.getString(config, "platformCert")).isEqualTo("legacy-cert");
    }

    @Test
    @DisplayName("payment 分组缺失时回退到旧支付分组")
    void fallsBackToLegacyGroup() {
        SystemConfigHelper configHelper = new StubSystemConfigHelper(Map.of(
                "alipay", "{\"publicKey\":\"legacy-public-key\"}"
        ));

        assertThat(PaymentConfigSupport.getProviderString(configHelper, "alipay", "publicKey", null))
                .isEqualTo("legacy-public-key");
    }

    private static final class StubSystemConfigHelper extends SystemConfigHelper {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private final Map<String, JsonNode> configs = new HashMap<>();

        private StubSystemConfigHelper(Map<String, String> rawConfigs) {
            super(null, OBJECT_MAPPER);
            rawConfigs.forEach((key, value) -> {
                try {
                    configs.put(key, OBJECT_MAPPER.readTree(value));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        }

        @Override
        public JsonNode getConfig(String groupCode) {
            return configs.get(groupCode);
        }
    }
}
