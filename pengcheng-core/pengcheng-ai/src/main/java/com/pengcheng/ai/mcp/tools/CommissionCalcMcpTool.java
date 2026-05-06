package com.pengcheng.ai.mcp.tools;

import com.pengcheng.ai.function.CommissionCalcFunction;
import com.pengcheng.ai.mcp.McpTool;
import com.pengcheng.ai.mcp.McpToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 佣金计算 MCP Tool
 * 封装 CommissionCalcFunction 为 MCP 协议格式
 */
@Component
@RequiredArgsConstructor
public class CommissionCalcMcpTool implements McpTool {

    private final CommissionCalcFunction commissionCalcFunction;

    @Override
    public String name() {
        return "commission_calculate";
    }

    @Override
    public String description() {
        return "根据项目佣金规则和成交金额自动计算佣金明细，包含基础佣金、阶梯奖励、人工确认项";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "projectId", Map.of("type", "integer", "description", "项目 ID"),
                "dealAmount", Map.of("type", "string", "description", "成交金额（字符串避免精度丢失）"),
                "dealCount", Map.of("type", "integer", "description", "累计成交套数（含本次），默认 1")
        ));
        schema.put("required", new String[]{"projectId", "dealAmount"});
        return schema;
    }

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        Long projectId = ((Number) arguments.get("projectId")).longValue();
        String dealAmount = (String) arguments.get("dealAmount");
        Integer dealCount = arguments.containsKey("dealCount")
                ? ((Number) arguments.get("dealCount")).intValue() : null;

        try {
            CommissionCalcFunction.Response response = commissionCalcFunction.apply(
                    new CommissionCalcFunction.Request(projectId, dealAmount, dealCount));

            StringBuilder sb = new StringBuilder();
            if (response.success()) {
                sb.append("佣金计算成功\n");
                if (response.detail() != null) {
                    sb.append("佣金明细: ").append(response.detail()).append("\n");
                }
                if (!response.manualConfirmItems().isEmpty()) {
                    sb.append("需人工确认: ").append(String.join(", ", response.manualConfirmItems())).append("\n");
                }
            } else {
                sb.append("计算失败: ").append(response.message());
            }
            return McpToolResult.text(sb.toString());
        } catch (Exception e) {
            return McpToolResult.error("佣金计算失败: " + e.getMessage());
        }
    }
}
