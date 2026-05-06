package com.pengcheng.ai.orchestration.tool;

import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AiToolContext;
import com.pengcheng.ai.orchestration.OrchestratedChatResult;
import com.pengcheng.ai.service.AiChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 报表智能体工具
 */
@Service
@RequiredArgsConstructor
public class ReportAgentTool implements AiAgentTool {

    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;

    @Override
    public AgentIntent supportedIntent() {
        return AgentIntent.REPORT;
    }

    @Override
    public String toolName() {
        return "report-agent";
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
        Map<String, Object> structuredData = buildStructuredData(result.content(), displayType);
        return new OrchestratedChatResult(
                result.content(),
                displayType,
                context.conversationId(),
                toolName(),
                structuredData
        );
    }

    private Map<String, Object> buildStructuredData(String content, String displayType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "report");
        payload.put("displayType", displayType);
        payload.put("agent", toolName());
        Object parsed = tryParseJson(content);
        if (parsed != null) {
            payload.put("data", parsed);
        } else {
            payload.put("summary", content);
        }
        return payload;
    }

    private Object tryParseJson(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String trimmed = content.trim();
        if ((!trimmed.startsWith("{") || !trimmed.endsWith("}"))
                && (!trimmed.startsWith("[") || !trimmed.endsWith("]"))) {
            return null;
        }
        try {
            return objectMapper.readValue(trimmed, Object.class);
        } catch (Exception ignore) {
            return null;
        }
    }
}
