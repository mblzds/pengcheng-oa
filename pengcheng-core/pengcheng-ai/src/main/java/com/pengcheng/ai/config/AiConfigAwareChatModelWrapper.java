package com.pengcheng.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 包装自动装配的 ChatModel，在每次调用前将「AI 模型配置」中的 model/temperature/maxTokens 注入到 Prompt 的 options 中，
 * 使管理后台的模型配置在对话时生效。
 */
@Slf4j
@Component
@Primary
public class AiConfigAwareChatModelWrapper implements ChatModel {

    private final ChatModel delegate;
    private final AiModelConfigService modelConfigService;

    public AiConfigAwareChatModelWrapper(ObjectProvider<ChatModel> chatModelProvider,
                                        AiModelConfigService modelConfigService) {
        this.delegate = chatModelProvider.stream()
                .filter(m -> !(m instanceof AiConfigAwareChatModelWrapper))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到可委托的 ChatModel Bean"));
        this.modelConfigService = modelConfigService;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        AiModelConfigService.EffectiveChatModelOptions opts = modelConfigService.getEffectiveChatOptions();
        if (opts == null) {
            return delegate.call(prompt);
        }
        ChatOptions chatOptions = buildDashScopeOptions(opts);
        if (chatOptions == null) {
            return delegate.call(prompt);
        }
        List<Message> messages = prompt.getInstructions();
        Prompt newPrompt = new Prompt(messages, chatOptions);
        return delegate.call(newPrompt);
    }

    /**
     * 通过反射构建 DashScope 的 ChatOptions，避免对 Alibaba 包名的编译期依赖
     */
    private ChatOptions buildDashScopeOptions(AiModelConfigService.EffectiveChatModelOptions opts) {
        try {
            Class<?> optionsClass = Class.forName("com.alibaba.cloud.ai.dashscope.api.DashScopeChatOptions");
            Object builder = optionsClass.getMethod("builder").invoke(null);
            builder.getClass().getMethod("withModel", String.class).invoke(builder, opts.modelId());
            builder.getClass().getMethod("withTemperature", double.class).invoke(builder, opts.temperature());
            builder.getClass().getMethod("withMaxTokens", Integer.class).invoke(builder, opts.maxTokens());
            return (ChatOptions) builder.getClass().getMethod("build").invoke(builder);
        } catch (ClassNotFoundException e) {
            try {
                Class<?> optionsClass = Class.forName("com.alibaba.cloud.ai.dashscope.DashScopeChatOptions");
                Object builder = optionsClass.getMethod("builder").invoke(null);
                builder.getClass().getMethod("withModel", String.class).invoke(builder, opts.modelId());
                builder.getClass().getMethod("withTemperature", double.class).invoke(builder, opts.temperature());
                builder.getClass().getMethod("withMaxTokens", Integer.class).invoke(builder, opts.maxTokens());
                return (ChatOptions) builder.getClass().getMethod("build").invoke(builder);
            } catch (Exception e2) {
                log.debug("DashScopeChatOptions 不可用，使用默认配置: {}", e2.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.debug("构建 DashScope options 失败: {}", e.getMessage());
            return null;
        }
    }
}
