package com.pengcheng.ai.mcp;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool 执行结果
 */
public record McpToolResult(
        List<Map<String, Object>> content,
        boolean isError
) {
    /** 成功返回文本 */
    public static McpToolResult text(String text) {
        return new McpToolResult(
                List.of(Map.of("type", "text", "text", text)),
                false
        );
    }

    /** 成功返回 JSON */
    public static McpToolResult json(Map<String, Object> data) {
        return new McpToolResult(
                List.of(Map.of("type", "text", "text", data.toString())),
                false
        );
    }

    /** 错误结果 */
    public static McpToolResult error(String message) {
        return new McpToolResult(
                List.of(Map.of("type", "text", "text", message)),
                true
        );
    }
}
