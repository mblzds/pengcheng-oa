package com.pengcheng.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * AI 内容生成服务
 * <p>
 * 根据输入关键词生成适合朋友圈、短信等渠道的营销文案。
 * <p>
 * 降级策略：AI 服务不可用时返回提示信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiContentService {

    private final ChatClient chatClient;
    private final AiFallbackHandler fallbackHandler;

    private static final String MARKETING_SYSTEM_PROMPT =
            "你是一个房地产营销文案专家。请根据用户提供的关键词，生成适合指定渠道的营销文案。\n"
            + "文案要求：\n"
            + "1. 语言生动、有吸引力\n"
            + "2. 突出项目卖点和优惠信息\n"
            + "3. 适合目标渠道的风格和字数限制\n"
            + "4. 包含行动号召（CTA）\n"
            + "请用中文回答。";

    /**
     * 生成营销文案
     *
     * @param keywords 营销关键词（如项目名称、卖点、优惠等）
     * @param channel  目标渠道：wechat_moments(朋友圈), sms(短信), general(通用)
     * @return 生成的营销文案
     */
    public String generateMarketingContent(String keywords, String channel) {
        return fallbackHandler.executeWithFallback(
                () -> doGenerate(keywords, channel),
                () -> "AI服务暂时不可用，请稍后再试。",
                "营销文案生成"
        );
    }

    private String doGenerate(String keywords, String channel) {
        String channelHint = switch (channel != null ? channel : "general") {
            case "wechat_moments" -> "朋友圈文案，100-200字，配合emoji，适合社交分享";
            case "sms" -> "短信文案，70字以内，简洁有力，包含联系方式占位符";
            default -> "通用营销文案，200-300字，适合多渠道使用";
        };

        String userPrompt = String.format("关键词：%s\n目标渠道：%s\n请生成一段营销文案。", keywords, channelHint);

        return chatClient.prompt()
                .system(MARKETING_SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();
    }
}
