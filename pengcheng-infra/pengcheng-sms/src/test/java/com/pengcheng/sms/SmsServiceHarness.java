package com.pengcheng.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.helper.SystemConfigHelper;

public final class SmsServiceHarness {

    private SmsServiceHarness() {
    }

    public static void main(String[] args) {
        SystemConfigHelper missingConfig = new StubSystemConfigHelper("", "", "", "", "", "");
        SystemConfigHelper configured = new StubSystemConfigHelper(
                "aliyun-id", "aliyun-secret", "aliyun-sign", "aliyun-template", "tencent-id", "tencent-secret"
        );

        assertFalse(new AliyunSmsService(missingConfig).sendCode("13800138000", "123456"),
                "Aliyun should fail closed when config is missing");
        assertFalse(new AliyunSmsService(configured).sendCode("13800138000", "123456"),
                "Aliyun should fail closed when integration is not implemented");
        assertFalse(new TencentSmsService(missingConfig).sendCode("13800138000", "123456"),
                "Tencent should fail closed when config is missing");
        assertFalse(new TencentSmsService(configured).sendCode("13800138000", "123456"),
                "Tencent should fail closed when integration is not implemented");
        assertTrue(new ConsoleSmsService().sendCode("13800138000", "123456"),
                "Console SMS should still succeed");

        System.out.println("SmsServiceHarness OK");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertFalse(boolean value, String message) {
        if (value) {
            throw new IllegalStateException(message);
        }
    }

    private static final class StubSystemConfigHelper extends SystemConfigHelper {

        private final String aliyunAccessKeyId;
        private final String aliyunAccessKeySecret;
        private final String aliyunSignName;
        private final String aliyunTemplateCode;
        private final String tencentSecretId;
        private final String tencentSecretKey;

        private StubSystemConfigHelper(
                String aliyunAccessKeyId,
                String aliyunAccessKeySecret,
                String aliyunSignName,
                String aliyunTemplateCode,
                String tencentSecretId,
                String tencentSecretKey
        ) {
            super(null, new ObjectMapper());
            this.aliyunAccessKeyId = aliyunAccessKeyId;
            this.aliyunAccessKeySecret = aliyunAccessKeySecret;
            this.aliyunSignName = aliyunSignName;
            this.aliyunTemplateCode = aliyunTemplateCode;
            this.tencentSecretId = tencentSecretId;
            this.tencentSecretKey = tencentSecretKey;
        }

        @Override
        public String getSmsAliyunAccessKeyId() {
            return aliyunAccessKeyId;
        }

        @Override
        public String getSmsAliyunAccessKeySecret() {
            return aliyunAccessKeySecret;
        }

        @Override
        public String getSmsAliyunSignName() {
            return aliyunSignName;
        }

        @Override
        public String getSmsAliyunTemplateCode() {
            return aliyunTemplateCode;
        }

        @Override
        public String getSmsTencentSecretId() {
            return tencentSecretId;
        }

        @Override
        public String getSmsTencentSecretKey() {
            return tencentSecretKey;
        }

        @Override
        public String getSmsTencentAppId() {
            return "sms-app-id";
        }

        @Override
        public String getSmsTencentSignName() {
            return "sms-sign";
        }

        @Override
        public String getSmsTencentTemplateId() {
            return "sms-template";
        }
    }
}
