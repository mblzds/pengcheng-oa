package com.pengcheng.ai.orchestration;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 编排状态对象，贯穿 StateGraph 各节点
 */
@Data
@Builder
public class OrchestrationState {
    private String conversationId;
    private String message;
    private String conversationContext;
    private AgentScene scene;
    private AgentIntent intent;
    private Long projectIdHint;
    private Long userId;
    private List<String> roleCodes;
    private String mode;

    private boolean permissionAllowed;
    private String permissionDenyReason;
    private String dataScope;
    private String projectScope;

    private String promptVersion;
    private String experimentGroup;

    private String responseContent;
    private String displayType;
    private String routedAgent;
    private Map<String, Object> structuredData;
    private String callChain;

    private boolean completed;
    private Throwable error;
}
