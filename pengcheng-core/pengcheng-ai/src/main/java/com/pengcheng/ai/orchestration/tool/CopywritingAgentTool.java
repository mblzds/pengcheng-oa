package com.pengcheng.ai.orchestration.tool;

import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AgentScene;
import com.pengcheng.ai.orchestration.AiToolContext;
import com.pengcheng.ai.orchestration.OrchestratedChatResult;
import com.pengcheng.ai.service.AiContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 文案智能体工具
 */
@Service
@RequiredArgsConstructor
public class CopywritingAgentTool implements AiAgentTool {

    private final AiContentService aiContentService;

    @Override
    public AgentIntent supportedIntent() {
        return AgentIntent.COPYWRITING;
    }

    @Override
    public String toolName() {
        return "copywriting-agent";
    }

    @Override
    public OrchestratedChatResult execute(AiToolContext context) {
        String channel = context.scene() == AgentScene.APP ? "wechat_moments" : "general";
        String content = aiContentService.generateMarketingContent(context.message(), channel);
        return new OrchestratedChatResult(
                content,
                "text",
                context.conversationId(),
                toolName()
        );
    }
}

