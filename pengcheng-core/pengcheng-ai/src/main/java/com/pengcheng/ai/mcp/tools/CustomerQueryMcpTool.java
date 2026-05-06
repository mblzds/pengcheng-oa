package com.pengcheng.ai.mcp.tools;

import com.pengcheng.ai.function.CustomerQueryFunction;
import com.pengcheng.ai.mcp.McpTool;
import com.pengcheng.ai.mcp.McpToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户查询 MCP Tool
 * 封装 CustomerQueryFunction 为 MCP 协议格式
 */
@Component
@RequiredArgsConstructor
public class CustomerQueryMcpTool implements McpTool {

    private final CustomerQueryFunction customerQueryFunction;

    @Override
    public String name() {
        return "customer_query";
    }

    @Override
    public String description() {
        return "根据手机号查询客户信息，判定客户是否已存在于系统中";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "phone", Map.of("type", "string", "description", "客户手机号（11位）")
        ));
        schema.put("required", new String[]{"phone"});
        return schema;
    }

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        String phone = (String) arguments.get("phone");
        if (phone == null || phone.length() != 11) {
            return McpToolResult.error("请提供有效的 11 位手机号");
        }
        try {
            CustomerQueryFunction.Response response = customerQueryFunction.apply(
                    new CustomerQueryFunction.Request(phone));
            StringBuilder sb = new StringBuilder();
            sb.append("查询结果：共 ").append(response.totalCount()).append(" 条记录\n");
            for (CustomerQueryFunction.CustomerInfo info : response.customers()) {
                sb.append("- #").append(info.id())
                        .append(" ").append(info.customerName())
                        .append(" (").append(info.phoneMasked()).append(")")
                        .append(" 状态=").append(info.statusText())
                        .append(" 池=").append(info.poolTypeText())
                        .append("\n");
            }
            return McpToolResult.text(sb.toString());
        } catch (Exception e) {
            return McpToolResult.error("客户查询失败: " + e.getMessage());
        }
    }
}
