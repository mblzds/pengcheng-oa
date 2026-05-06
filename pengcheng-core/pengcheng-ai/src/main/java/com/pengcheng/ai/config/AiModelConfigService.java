package com.pengcheng.ai.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 从「AI 模型配置」读取当前生效的对话模型与参数
 * 供 ChatModel 包装器在每次调用时注入 model / temperature / maxTokens
 */
@Service
@RequiredArgsConstructor
public class AiModelConfigService {

    private static final String GROUP_AI_CONFIG = "aiConfig";

    /** 视为对话模型的 modelId（按优先级），仅对 DashScope 生效 */
    private static final String[] CHAT_MODEL_IDS = { "qwen-max", "qwen-turbo" };

    private final SystemConfigHelper configHelper;

    /**
     * 返回当前生效的对话模型配置；若未配置或全禁用则返回 null（使用 yml 默认）
     */
    public EffectiveChatModelOptions getEffectiveChatOptions() {
        JsonNode config = configHelper.getConfig(GROUP_AI_CONFIG);
        JsonNode modelsNode = config != null ? config.get("models") : null;
        if (modelsNode == null) {
            return null;
        }
        for (String modelId : CHAT_MODEL_IDS) {
            if (!modelsNode.has(modelId)) continue;
            JsonNode m = modelsNode.get(modelId);
            if (m.has("enabled") && !m.get("enabled").asBoolean()) continue;
            String id = modelId;
            double temperature = m.has("temperature") ? m.get("temperature").asDouble() : 0.7;
            int maxTokens = m.has("maxTokens") ? m.get("maxTokens").asInt() : 8000;
            if (maxTokens <= 0) maxTokens = 8000;
            return new EffectiveChatModelOptions(id, temperature, maxTokens);
        }
        return null;
    }

    /** 当前生效的对话模型选项（仅对 DashScope 生效） */
    public record EffectiveChatModelOptions(String modelId, double temperature, int maxTokens) {}
}
