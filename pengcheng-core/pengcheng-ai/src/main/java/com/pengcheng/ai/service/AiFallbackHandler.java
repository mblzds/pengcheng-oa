package com.pengcheng.ai.service;

import com.pengcheng.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * AI 服务降级处理器
 * <p>
 * 降级策略：
 * <ul>
 *   <li>智能佣金计算失败 → 回退手动录入模式（返回 null，由前端提示手动填写）</li>
 *   <li>智能判客失败 → 回退规则引擎判客（使用手机号精确匹配）</li>
 *   <li>RAG 问答失败 → 返回"AI服务暂时不可用"提示</li>
 *   <li>成交概率计算失败 → 保留上次评分，标记为"待更新"</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiFallbackHandler {

    private final AiProperties aiProperties;

    /**
     * 执行 AI 操作，失败时使用降级逻辑
     *
     * @param aiOperation AI 操作
     * @param fallback    降级操作
     * @param operationName 操作名称（用于日志）
     * @return AI 操作结果或降级结果
     */
    public <T> T executeWithFallback(Supplier<T> aiOperation, Supplier<T> fallback, String operationName) {
        if (!aiProperties.isEnabled()) {
            log.info("AI 服务已禁用，使用降级策略: {}", operationName);
            return fallback.get();
        }
        try {
            return aiOperation.get();
        } catch (Exception e) {
            log.warn("AI 服务调用失败 [{}]，启用降级策略: {}", operationName, e.getMessage());
            return fallback.get();
        }
    }

    /**
     * RAG 问答降级：返回不可用提示
     */
    public String ragFallbackMessage() {
        return "AI服务暂时不可用，请稍后再试或联系相关人员获取帮助。";
    }
}
