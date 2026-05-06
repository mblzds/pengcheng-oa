package com.pengcheng.sms;

final class SmsLogSanitizer {

    private SmsLogSanitizer() {
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "<empty>";
        }
        String normalized = phone.replaceAll("\\s+", "");
        if (normalized.length() <= 7) {
            return "*".repeat(normalized.length());
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }

    static String maskCode(String code) {
        if (code == null || code.isBlank()) {
            return "<empty>";
        }
        return "*".repeat(Math.min(code.length(), 6));
    }
}
