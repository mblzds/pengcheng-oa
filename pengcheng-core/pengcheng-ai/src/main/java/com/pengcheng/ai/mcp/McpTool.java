package com.pengcheng.ai.mcp;

import java.util.Map;

/**
 * MCP Tool 统一接口
 * <p>
 * 遵循 Model Context Protocol 规范，每个 Tool 提供：
 * - 工具名称和描述
 * - 输入参数 JSON Schema
 * - 执行逻辑
 */
public interface McpTool {

    /** 工具名称（唯一标识） */
    String name();

    /** 工具描述 */
    String description();

    /** 输入参数 JSON Schema */
    Map<String, Object> inputSchema();

    /** 执行工具 */
    McpToolResult execute(Map<String, Object> arguments);
}
