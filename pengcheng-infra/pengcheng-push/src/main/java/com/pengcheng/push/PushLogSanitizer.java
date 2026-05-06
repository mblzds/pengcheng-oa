package com.pengcheng.push;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class PushLogSanitizer {

    private static final String[] SENSITIVE_KEYS = {"token", "secret", "password", "code", "registration", "device"};

    private PushLogSanitizer() {
    }

    static String maskIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "<empty>";
        }
        if (value.length() <= 8) {
            return "*".repeat(value.length());
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    static Map<String, String> sanitizeExtras(Map<String, String> extras) {
        if (extras == null || extras.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : extras.entrySet()) {
            sanitized.put(entry.getKey(), isSensitiveKey(entry.getKey()) ? maskIdentifier(entry.getValue()) : entry.getValue());
        }
        return sanitized;
    }

    static boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (lower.contains(sensitiveKey)) {
                return true;
            }
        }
        return false;
    }
}
