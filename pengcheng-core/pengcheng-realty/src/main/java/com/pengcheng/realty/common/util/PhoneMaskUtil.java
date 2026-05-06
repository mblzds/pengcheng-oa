package com.pengcheng.realty.common.util;

/**
 * 手机号脱敏工具类
 * <p>
 * 对11位手机号进行脱敏处理：前3位 + 4个星号 + 后4位
 * 例如：13812345678 → 138****5678
 */
public final class PhoneMaskUtil {

    private static final String MASK = "****";
    private static final int MIN_MASK_LENGTH = 7;

    private PhoneMaskUtil() {
        // utility class
    }

    /**
     * 对手机号进行脱敏处理
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号，null输入返回null，长度不足7位原样返回
     */
    public static String mask(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.length() < MIN_MASK_LENGTH) {
            return phone;
        }
        return phone.substring(0, 3) + MASK + phone.substring(phone.length() - 4);
    }
}
