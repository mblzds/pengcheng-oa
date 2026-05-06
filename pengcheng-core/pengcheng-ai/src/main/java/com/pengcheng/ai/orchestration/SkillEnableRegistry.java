package com.pengcheng.ai.orchestration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 启用/禁用注册表
 * <p>
 * 管理端在「Skill 管理」页启用/禁用后，编排层据此过滤，被禁用的 Skill 不会被调用。
 * 有 Redis 时禁用状态持久化到 Redis（key: pengcheng:ai:skill:disabled），重启后保留；无 Redis 时仅内存。
 */
@Slf4j
@Component
public class SkillEnableRegistry {

    private static final String REDIS_KEY = "pengcheng:ai:skill:disabled";

    private final Set<String> disabledToolNames = ConcurrentHashMap.newKeySet();
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public SkillEnableRegistry(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @PostConstruct
    public void loadFromRedis() {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            try {
                Set<String> members = redis.opsForSet().members(REDIS_KEY);
                if (members != null && !members.isEmpty()) {
                    disabledToolNames.addAll(members);
                    log.info("Skill 禁用状态已从 Redis 加载: {} 项", members.size());
                }
            } catch (Exception e) {
                log.warn("从 Redis 加载 Skill 禁用列表失败，使用内存状态: {}", e.getMessage());
            }
        }
    }

    /** 是否已禁用 */
    public boolean isDisabled(String toolName) {
        return toolName != null && disabledToolNames.contains(toolName);
    }

    /** 禁用指定 Skill */
    public void disable(String toolName) {
        if (toolName == null) return;
        disabledToolNames.add(toolName);
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            try {
                redis.opsForSet().add(REDIS_KEY, toolName);
            } catch (Exception e) {
                log.warn("写入 Redis 禁用列表失败: {}", e.getMessage());
            }
        }
    }

    /** 启用指定 Skill */
    public void enable(String toolName) {
        if (toolName == null) return;
        disabledToolNames.remove(toolName);
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            try {
                redis.opsForSet().remove(REDIS_KEY, toolName);
            } catch (Exception e) {
                log.warn("从 Redis 移除禁用项失败: {}", e.getMessage());
            }
        }
    }

    /** 返回当前已禁用的工具名称集合（只读视图） */
    public Set<String> getDisabledToolNames() {
        return Set.copyOf(disabledToolNames);
    }
}
