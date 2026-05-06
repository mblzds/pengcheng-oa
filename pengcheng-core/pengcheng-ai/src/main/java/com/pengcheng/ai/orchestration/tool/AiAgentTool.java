package com.pengcheng.ai.orchestration.tool;

import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AiToolContext;
import com.pengcheng.ai.orchestration.OrchestratedChatResult;

/**
 * AI 工具统一契约
 */
public interface AiAgentTool {

    AgentIntent supportedIntent();

    String toolName();

    OrchestratedChatResult execute(AiToolContext context);
}

