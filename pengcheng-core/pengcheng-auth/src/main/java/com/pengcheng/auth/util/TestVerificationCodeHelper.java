package com.pengcheng.auth.util;

/**
 * 测试环境验证码辅助类。
 * 当进程环境变量 TEST=1 或 TEST=true 时，验证码统一走固定值，便于联调和自动化测试。
 */
public final class TestVerificationCodeHelper {

    public static final String FIXED_IMAGE_CAPTCHA = "1234";
    public static final String FIXED_SMS_CODE = "123456";

    private TestVerificationCodeHelper() {
    }

    public static boolean isTestMode() {
        return isTruthy(readConfig("TEST", "test"))
                || isTruthy(readConfig("TEST_MODE", "test.mode"));
    }

    private static String readConfig(String envKey, String propertyKey) {
        String value = System.getenv(envKey);
        if (value == null || value.isBlank()) {
            value = System.getProperty(envKey);
        }
        if (value == null || value.isBlank()) {
            value = System.getProperty(propertyKey);
        }
        return value;
    }

    private static boolean isTruthy(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    public static String getImageCaptcha() {
        return FIXED_IMAGE_CAPTCHA;
    }

    public static String getSmsCode() {
        return FIXED_SMS_CODE;
    }
}
