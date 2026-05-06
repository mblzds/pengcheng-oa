package com.pengcheng.ai.orchestration;

import java.util.List;

/**
 * 工具执行上下文
 */
public record AiToolContext(
        String conversationId,
        String message,
        String conversationContext,
        AgentScene scene,
        AgentIntent intent,
        Long projectIdHint,
        Long userId,
        List<String> roleCodes,
        String dataScope,
        String projectScope,
        String promptExperimentGroup,
        String promptVersion
) {
}
