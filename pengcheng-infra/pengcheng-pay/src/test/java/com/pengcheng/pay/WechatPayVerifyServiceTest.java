package com.pengcheng.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.helper.SystemConfigHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WechatPayVerifyService")
class WechatPayVerifyServiceTest {

    @Test
    @DisplayName("缺少请求头或 body 时验签失败")
    void verifySignatureFailsWhenHeadersMissing() {
        WechatPayVerifyService service = new WechatPayVerifyService(new StubSystemConfigHelper(Map.of()));

        assertThat(service.verifySignature("", "signature", "nonce", "1711111111", "serial")).isFalse();
        assertThat(service.verifySignature("{}", "", "nonce", "1711111111", "serial")).isFalse();
        assertThat(service.verifySignature("{}", "signature", "nonce", "1711111111", "")).isFalse();
    }

    @Test
    @DisplayName("缺少平台证书时验签失败")
    void verifySignatureFailsWhenPlatformCertMissing() {
        WechatPayVerifyService service = new WechatPayVerifyService(new StubSystemConfigHelper(Map.of(
                "payment", "{\"wechatPay\":{\"apiV3Key\":\"12345678901234567890123456789012\"}}"
        )));

        assertThat(service.verifySignature("{}", "c2lnbmF0dXJl", "nonce", "1711111111", "serial")).isFalse();
    }

    @Test
    @DisplayName("缺少 apiV3Key 时解析回调失败")
    void parseNotifyBodyFailsWhenApiV3KeyMissing() {
        WechatPayVerifyService service = new WechatPayVerifyService(new StubSystemConfigHelper(Map.of(
                "payment", "{\"wechatPay\":{\"platformCert\":\"CERT\"}}"
        )));

        String body = """
                {
                  "resource": {
                    "associated_data": "transaction",
                    "nonce": "0123456789ab",
                    "ciphertext": "Y2lwaGVydGV4dA=="
                  }
                }
                """;

        assertThat(service.parseNotifyBody(body)).isNull();
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
