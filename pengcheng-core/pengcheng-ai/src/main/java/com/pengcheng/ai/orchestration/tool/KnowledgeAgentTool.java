package com.pengcheng.ai.orchestration.tool;

import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AiToolContext;
import com.pengcheng.ai.orchestration.OrchestratedChatResult;
import com.pengcheng.ai.rag.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 知识库智能体工具
 */
@Service
@RequiredArgsConstructor
public class KnowledgeAgentTool implements AiAgentTool {

    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    public AgentIntent supportedIntent() {
        return AgentIntent.KNOWLEDGE;
    }

    @Override
    public String toolName() {
        return "knowledge-agent";
    }

    @Override
    public OrchestratedChatResult execute(AiToolContext context) {
        String answer = knowledgeBaseService.queryKnowledge(context.message(), context.projectIdHint());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "knowledge");
        payload.put("agent", toolName());
        payload.put("question", context.message());
        payload.put("answer", answer);
        if (context.projectIdHint() != null) {
            payload.put("projectId", context.projectIdHint());
        }

        return new OrchestratedChatResult(
                answer,
                "text",
                context.conversationId(),
                toolName(),
                payload
        );
    }
}
