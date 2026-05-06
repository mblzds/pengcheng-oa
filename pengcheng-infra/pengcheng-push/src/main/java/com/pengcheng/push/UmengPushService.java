package com.pengcheng.push;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 友盟推送服务
 */
@Slf4j
public class UmengPushService implements PushService {

    public static final String PROVIDER_TYPE = "umeng";
    private final String appKey;
    private final String masterSecret;

    public UmengPushService(String appKey, String masterSecret) {
        this.appKey = appKey;
        this.masterSecret = masterSecret;
        log.info("初始化友盟推送服务, appKey={}", PushLogSanitizer.maskIdentifier(appKey));
    }

    @Override
    public String getProviderType() {
        return PROVIDER_TYPE;
    }

    @Override
    public String getProviderName() {
        return "友盟推送";
    }

    @Override
    public boolean pushToUser(String userId, String title, String content, Map<String, String> extras) {
        return failClosed("用户", userId, title, content, extras);
    }

    @Override
    public boolean pushToUsers(List<String> userIds, String title, String content, Map<String, String> extras) {
        return failClosed("多用户", String.valueOf(userIds), title, content, extras);
    }

    @Override
    public boolean pushToAll(String title, String content, Map<String, String> extras) {
        return failClosed("全量", "ALL", title, content, extras);
    }

    @Override
    public boolean pushToTags(List<String> tags, String title, String content, Map<String, String> extras) {
        return failClosed("标签", String.valueOf(tags), title, content, extras);
    }

    @Override
    public boolean pushToDevice(String registrationId, String title, String content, Map<String, String> extras) {
        return failClosed("设备", PushLogSanitizer.maskIdentifier(registrationId), title, content, extras);
    }

    private boolean failClosed(String targetType, String target, String title, String content, Map<String, String> extras) {
        if (appKey == null || appKey.isBlank() || masterSecret == null || masterSecret.isBlank()) {
            log.error("【友盟推送】配置不完整，拒绝发送: targetType={}, target={}", targetType, target);
            return false;
        }
        log.error("【友盟推送】未实现实际供应商调用，拒绝假成功: targetType={}, target={}, title={}, content={}, extras={}",
                targetType, target, title, content, PushLogSanitizer.sanitizeExtras(extras));
        return false;
    }
}
