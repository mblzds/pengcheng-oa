package com.pengcheng.push;

import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 推送服务工厂
 * 使用工厂模式 + 策略模式管理不同的推送服务实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushServiceFactory {

    private final SystemConfigHelper configHelper;

    /**
     * 缓存的推送服务实例
     */
    private volatile PushService cachedService;
    private volatile String cachedProvider;

    /**
     * 获取推送服务实例
     */
    public PushService getPushService() {
        String provider = configHelper.getPushProvider();

        // 如果服务商未变化，使用缓存的实例
        if (cachedService != null && provider.equals(cachedProvider)) {
            return cachedService;
        }

        synchronized (this) {
            // 双重检查
            if (cachedService != null && provider.equals(cachedProvider)) {
                return cachedService;
            }

            cachedService = createPushService(provider);
            cachedProvider = provider;
            return cachedService;
        }
    }

    /**
     * 创建推送服务实例
     */
    private PushService createPushService(String provider) {
        String appKey = configHelper.getPushAppKey();
        String masterSecret = configHelper.getPushMasterSecret();

        PushService service = switch (provider) {
            case JpushPushService.PROVIDER_TYPE -> new JpushPushService(appKey, masterSecret);
            case UmengPushService.PROVIDER_TYPE -> new UmengPushService(appKey, masterSecret);
            case GetuiPushService.PROVIDER_TYPE -> new GetuiPushService(appKey, masterSecret);
            default -> {
                log.warn("未知的推送服务商: {}，使用控制台输出", provider);
                yield new ConsolePushService();
            }
        };

        log.info("创建推送服务实例: {} - {}", provider, service.getProviderName());
        return service;
    }

    /**
     * 刷新推送服务
     */
    public void refresh() {
        synchronized (this) {
            cachedService = null;
            cachedProvider = null;
        }
        log.info("推送服务缓存已清空");
    }

    /**
     * 推送给单个用户的便捷方法
     */
    public boolean pushToUser(String userId, String title, String content) {
        return getPushService().pushToUser(userId, title, content, null);
    }

    /**
     * 推送给所有用户的便捷方法
     */
    public boolean pushToAll(String title, String content) {
        return getPushService().pushToAll(title, content, null);
    }
}
