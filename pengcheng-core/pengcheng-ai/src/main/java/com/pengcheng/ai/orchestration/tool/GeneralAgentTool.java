package com.pengcheng.ai.orchestration.tool;

import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AiToolContext;
import com.pengcheng.ai.orchestration.OrchestratedChatResult;
import com.pengcheng.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 通用智能体工具
 */
@Service
@RequiredArgsConstructor
public class GeneralAgentTool implements AiAgentTool {

    private final AiChatService aiChatService;

    @Override
    public AgentIntent supportedIntent() {
        return AgentIntent.GENERAL;
    }

    @Override
    public String toolName() {
        return "general-agent";
    }

    @Override
    public OrchestratedChatResult execute(AiToolContext context) {
        AiChatService.ChatResult result = aiChatService.chat(
                context.message(),
                context.conversationContext(),
                context.conversationId(),
                new AiChatService.PromptMeta(context.promptExperimentGroup(), context.promptVersion())
        );
        String displayType = StringUtils.hasText(result.displayType()) ? result.displayType() : "text";
        return new OrchestratedChatResult(
                result.content(),
                displayType,
                context.conversationId(),
                toolName()
        );
    }
}
