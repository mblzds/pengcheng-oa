package com.pengcheng.realty.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

/**
 * 手机号 AES 加密/解密 TypeHandler
 * <p>
 * 用于 MyBatis-Plus 字段级透明加解密。写入数据库时自动加密，读取时自动解密。
 * 密钥通过系统属性 {@code realty.phone.encrypt.key} 配置，默认使用内置密钥。
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class PhoneEncryptTypeHandler extends BaseTypeHandler<String> {

    private static final String ALGORITHM = "AES";
    private static final String DEFAULT_KEY = "MarsRealty@2024!!";

    private final SecretKeySpec secretKey;

    public PhoneEncryptTypeHandler() {
        String key = System.getProperty("realty.phone.encrypt.key", DEFAULT_KEY);
        // AES requires 16/24/32 byte key; pad or truncate to 16 bytes
        byte[] keyBytes = new byte[16];
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, 16));
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Visible-for-testing constructor that accepts a custom key.
     */
    public PhoneEncryptTypeHandler(String encryptKey) {
        byte[] keyBytes = new byte[16];
        byte[] raw = encryptKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, 16));
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return decrypt(value);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return decrypt(value);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return decrypt(value);
    }

    /**
     * AES 加密
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt phone number", e);
        }
    }

    /**
     * AES 解密
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt phone number", e);
        }
    }
}
