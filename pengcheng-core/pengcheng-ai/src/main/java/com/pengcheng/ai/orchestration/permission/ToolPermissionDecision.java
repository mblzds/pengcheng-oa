package com.pengcheng.ai.orchestration.permission;

/**
 * 工具权限决策
 */
public record ToolPermissionDecision(
        boolean allowed,
        String reason,
        String dataScope,
        String projectScope
) {
}

