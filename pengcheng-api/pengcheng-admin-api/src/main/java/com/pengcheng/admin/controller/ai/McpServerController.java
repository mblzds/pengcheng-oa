package com.pengcheng.admin.controller.ai;

import com.pengcheng.ai.mcp.McpToolRegistry;
import com.pengcheng.ai.mcp.McpToolResult;
import com.pengcheng.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP Server 端点
 * 提供 MCP 协议兼容的 Tool 查询和执行接口
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpServerController {

    private final McpToolRegistry toolRegistry;

    /** 列出所有已注册的 MCP Tool（含 Schema） */
    @GetMapping("/tools")
    public Result<List<Map<String, Object>>> listTools() {
        return Result.ok(toolRegistry.getToolDescriptors());
    }

    /** 执行 MCP Tool */
    @PostMapping("/tools/{name}/execute")
    public Result<McpToolResult> executeTool(@PathVariable String name,
                                              @RequestBody Map<String, Object> arguments) {
        McpToolResult result = toolRegistry.executeTool(name, arguments);
        if (result.isError()) {
            return Result.fail(result.content().get(0).get("text").toString());
        }
        return Result.ok(result);
    }

    /** 启用 MCP Tool */
    @PostMapping("/tools/{name}/enable")
    public Result<Void> enableTool(@PathVariable String name) {
        toolRegistry.enable(name);
        return Result.ok();
    }

    /** 禁用 MCP Tool */
    @PostMapping("/tools/{name}/disable")
    public Result<Void> disableTool(@PathVariable String name) {
        toolRegistry.disable(name);
        return Result.ok();
    }
}
