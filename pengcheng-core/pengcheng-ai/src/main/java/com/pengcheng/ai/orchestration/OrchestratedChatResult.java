package com.pengcheng.ai.orchestration;

import java.util.Map;

/**
 * 编排后的统一对话响应
 *
 * @param content        响应内容
 * @param displayType    展示类型
 * @param conversationId 会话ID
 * @param routedAgent    命中的智能体
 * @param structuredData 结构化数据
 */
public record OrchestratedChatResult(
        String content,
        String displayType,
        String conversationId,
        String routedAgent,
        Map<String, Object> structuredData
) {
    public OrchestratedChatResult(String content,
                                  String displayType,
                                  String conversationId,
                                  String routedAgent) {
        this(content, displayType, conversationId, routedAgent, null);
    }
}
