package com.pengcheng.ai.memory;

import com.pengcheng.ai.memory.service.MemoryService;
import com.pengcheng.ai.orchestration.ConversationMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 记忆注入 Advisor
 * <p>
 * 在 AI 对话前，将相关记忆上下文和会话历史注入到系统提示词中。
 * 设计为可独立使用的组件，后续可直接集成为 Spring AI 的 Advisor 接口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryAdvisor {

    private final MemoryService memoryService;
    private final ConversationMemoryService conversationMemoryService;

    /**
     * 构建完整的增强系统提示词
     *
     * @param userId         当前用户 ID
     * @param customerId     当前关联客户 ID（可为 null）
     * @param conversationId 当前会话 ID
     * @param userQuery      用户当前提问
     * @param baseSystemPrompt 基础系统提示词
     * @return 增强后的系统提示词
     */
    public String buildEnhancedPrompt(Long userId, Long customerId, String conversationId, String userQuery, String baseSystemPrompt) {
        StringBuilder enhanced = new StringBuilder();

        if (StringUtils.hasText(baseSystemPrompt)) {
            enhanced.append(baseSystemPrompt);
        }

        String memoryContext = memoryService.buildMemoryContext(userId, customerId, userQuery);
        if (StringUtils.hasText(memoryContext)) {
            enhanced.append("\n\n--- 记忆上下文 ---\n").append(memoryContext);
        }

        String convContext = conversationMemoryService.buildConversationContext(conversationId);
        if (StringUtils.hasText(convContext)) {
            enhanced.append("\n\n--- 会话历史 ---\n").append(convContext);
        }

        String result = enhanced.toString().trim();
        if (result.isEmpty()) {
            return baseSystemPrompt;
        }

        log.debug("增强提示词长度: base={}, enhanced={}", 
                baseSystemPrompt != null ? baseSystemPrompt.length() : 0, 
                result.length());
        return result;
    }

    /**
     * 对话完成后的后处理：保存会话记忆
     */
    public void afterCompletion(Long userId, String conversationId, String userQuery, String assistantResponse) {
        try {
            conversationMemoryService.appendUserMessage(conversationId, userQuery);
            conversationMemoryService.appendAssistantMessage(conversationId, assistantResponse);
        } catch (Exception e) {
            log.warn("保存会话记忆失败: conversationId={}", conversationId, e);
        }
    }
}
