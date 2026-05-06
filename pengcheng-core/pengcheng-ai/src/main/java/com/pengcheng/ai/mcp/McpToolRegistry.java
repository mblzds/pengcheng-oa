package com.pengcheng.ai.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MCP Tool 注册表
 * 管理所有已注册的 MCP 工具，支持查询、启禁用
 */
@Slf4j
@Component
public class McpToolRegistry {

    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
    private final Set<String> disabledTools = ConcurrentHashMap.newKeySet();

    public McpToolRegistry(List<McpTool> mcpTools) {
        for (McpTool tool : mcpTools) {
            tools.put(tool.name(), tool);
            log.info("[MCP] 注册工具: {} - {}", tool.name(), tool.description());
        }
        log.info("[MCP] 共注册 {} 个工具", tools.size());
    }

    /** 获取所有已注册工具 */
    public Collection<McpTool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /** 获取已启用的工具 */
    public List<McpTool> getEnabledTools() {
        return tools.values().stream()
                .filter(t -> !disabledTools.contains(t.name()))
                .collect(Collectors.toList());
    }

    /** 按名称查找工具 */
    public Optional<McpTool> findTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /** 执行工具 */
    public McpToolResult executeTool(String name, Map<String, Object> arguments) {
        McpTool tool = tools.get(name);
        if (tool == null) {
            return McpToolResult.error("未找到工具: " + name);
        }
        if (disabledTools.contains(name)) {
            return McpToolResult.error("工具已禁用: " + name);
        }
        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            log.error("[MCP] 工具执行异常: {}", name, e);
            return McpToolResult.error("工具执行异常: " + e.getMessage());
        }
    }

    /** 启用工具 */
    public void enable(String name) {
        disabledTools.remove(name);
    }

    /** 禁用工具 */
    public void disable(String name) {
        disabledTools.add(name);
    }

    /** 工具是否已启用 */
    public boolean isEnabled(String name) {
        return tools.containsKey(name) && !disabledTools.contains(name);
    }

    /** 获取工具描述列表（供前端展示） */
    public List<Map<String, Object>> getToolDescriptors() {
        return tools.values().stream().map(t -> {
            Map<String, Object> desc = new LinkedHashMap<>();
            desc.put("name", t.name());
            desc.put("description", t.description());
            desc.put("inputSchema", t.inputSchema());
            desc.put("enabled", !disabledTools.contains(t.name()));
            return desc;
        }).collect(Collectors.toList());
    }
}
