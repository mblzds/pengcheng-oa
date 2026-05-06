package com.pengcheng.push;

import java.util.Map;

public final class PushServiceHarness {

    private PushServiceHarness() {
    }

    public static void main(String[] args) {
        Map<String, String> extras = Map.of(
                "token", "token-value",
                "deviceId", "device-001",
                "traceId", "trace-1"
        );

        assertFalse(new JpushPushService("", "").pushToUser("u1", "title", "content", extras),
                "Jpush should fail closed when config is missing");
        assertFalse(new JpushPushService("app-key", "master-secret").pushToDevice("registration-001", "title", "content", extras),
                "Jpush should fail closed when integration is not implemented");

        assertFalse(new UmengPushService("", "").pushToAll("title", "content", extras),
                "Umeng should fail closed when config is missing");
        assertFalse(new UmengPushService("app-key", "master-secret").pushToTags(java.util.List.of("vip"), "title", "content", extras),
                "Umeng should fail closed when integration is not implemented");

        assertFalse(new GetuiPushService("", "").pushToUsers(java.util.List.of("u1", "u2"), "title", "content", extras),
                "Getui should fail closed when config is missing");
        assertFalse(new GetuiPushService("app-key", "master-secret").pushToDevice("registration-002", "title", "content", extras),
                "Getui should fail closed when integration is not implemented");

        assertTrue(new ConsolePushService().pushToDevice("registration-003", "title", "content", extras),
                "Console push should still succeed");

        System.out.println("PushServiceHarness OK");
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
}
