package com.pengcheng.realty.common;

import com.pengcheng.realty.common.handler.PhoneEncryptTypeHandler;
import com.pengcheng.realty.common.util.PhoneMaskUtil;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 手机号脱敏和加密属性测试
 *
 * <p>Property 5: 手机号脱敏格式 — For any 11位手机号，脱敏后格式为前3位+4星号+后4位，长度为11
 * <p>Property 30: 敏感数据加密存储往返一致性 — For any 手机号，加密后解密应得到原始值
 *
 * <p><b>Validates: Requirements 1.7, 19.3</b>
 */
class PhoneEncryptAndMaskProperties {

    /**
     * Generates valid 11-digit Chinese mobile phone numbers.
     * First digit is always 1, second digit is 3-9.
     */
    @Provide
    Arbitrary<String> chinesePhoneNumbers() {
        return Arbitraries.strings()
                .withCharRange('0', '9')
                .ofLength(9)
                .map(suffix -> "1" + Arbitraries.of("3", "4", "5", "6", "7", "8", "9").sample() + suffix);
    }

    /**
     * Property 5: 手机号脱敏格式
     *
     * <p>For any 11位手机号，脱敏后格式为前3位+4星号+后4位，长度为11。
     *
     * <p><b>Validates: Requirements 1.7</b>
     */
    @Property(tries = 100)
    void maskedPhoneHasCorrectFormat(@ForAll("chinesePhoneNumbers") String phone) {
        String masked = PhoneMaskUtil.mask(phone);

        // Length must remain 11
        assertThat(masked).hasSize(11);

        // First 3 characters match original
        assertThat(masked.substring(0, 3)).isEqualTo(phone.substring(0, 3));

        // Middle 4 characters are asterisks
        assertThat(masked.substring(3, 7)).isEqualTo("****");

        // Last 4 characters match original
        assertThat(masked.substring(7, 11)).isEqualTo(phone.substring(7, 11));
    }

    /**
     * Property 30: 敏感数据加密存储往返一致性
     *
     * <p>For any 手机号，加密后解密应得到原始值。
     *
     * <p><b>Validates: Requirements 19.3</b>
     */
    @Property(tries = 100)
    void encryptThenDecryptReturnsOriginal(@ForAll("chinesePhoneNumbers") String phone) {
        PhoneEncryptTypeHandler handler = new PhoneEncryptTypeHandler("TestKey@12345678");

        String encrypted = handler.encrypt(phone);
        String decrypted = handler.decrypt(encrypted);

        // Round-trip must produce the original value
        assertThat(decrypted).isEqualTo(phone);

        // Encrypted form must differ from plaintext
        assertThat(encrypted).isNotEqualTo(phone);
    }
}
