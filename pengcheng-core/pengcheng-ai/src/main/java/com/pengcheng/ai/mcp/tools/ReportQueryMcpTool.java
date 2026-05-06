package com.pengcheng.ai.mcp.tools;

import com.pengcheng.ai.function.ReportQueryFunction;
import com.pengcheng.ai.mcp.McpTool;
import com.pengcheng.ai.mcp.McpToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 报表查询 MCP Tool
 * 封装 ReportQueryFunction 为 MCP 协议格式
 */
@Component
@RequiredArgsConstructor
public class ReportQueryMcpTool implements McpTool {

    private final ReportQueryFunction reportQueryFunction;

    @Override
    public String name() {
        return "report_query";
    }

    @Override
    public String description() {
        return "查询业务报表数据，支持按维度和时间范围查询销售、客户、佣金等统计数据";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "queryType", Map.of("type", "string", "description",
                        "查询类型: overview=概览, project_ranking=项目排行, alliance_ranking=联盟商排行, funnel=转化漏斗"),
                "startDate", Map.of("type", "string", "description",
                        "开始日期 (yyyy-MM-dd)，为空默认本月1日"),
                "endDate", Map.of("type", "string", "description",
                        "结束日期 (yyyy-MM-dd)，为空默认今天")
        ));
        schema.put("required", new String[]{"queryType"});
        return schema;
    }

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        String queryType = (String) arguments.get("queryType");
        String startDate = arguments.containsKey("startDate") ? (String) arguments.get("startDate") : null;
        String endDate = arguments.containsKey("endDate") ? (String) arguments.get("endDate") : null;

        try {
            ReportQueryFunction.Response response = reportQueryFunction.apply(
                    new ReportQueryFunction.Request(queryType, startDate, endDate));
            StringBuilder sb = new StringBuilder();
            sb.append(response.message()).append("\n");
            sb.append("展示类型: ").append(response.displayType()).append("\n");
            if (response.data() != null && !response.data().isEmpty()) {
                for (Map<String, Object> row : response.data()) {
                    for (var entry : row.entrySet()) {
                        sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("  ");
                    }
                    sb.append("\n");
                }
            }
            return McpToolResult.text(sb.toString());
        } catch (Exception e) {
            return McpToolResult.error("报表查询失败: " + e.getMessage());
        }
    }
}
