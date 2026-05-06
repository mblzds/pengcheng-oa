package com.pengcheng.ai.orchestration.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AiToolContext;
import com.pengcheng.ai.orchestration.OrchestratedChatResult;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 客户查询智能体工具
 */
@Service
@RequiredArgsConstructor
public class CustomerAgentTool implements AiAgentTool {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(1\\d{10})");

    private final RealtyCustomerMapper customerMapper;

    @Override
    public AgentIntent supportedIntent() {
        return AgentIntent.CUSTOMER;
    }

    @Override
    public String toolName() {
        return "customer-agent";
    }

    @Override
    public OrchestratedChatResult execute(AiToolContext context) {
        String phone = extractPhone(context.message());
        ToolResult result;
        if (StringUtils.hasText(phone)) {
            result = queryByPhone(context, phone);
        } else {
            result = querySummary(context);
        }
        return new OrchestratedChatResult(
                result.content(),
                "text",
                context.conversationId(),
                toolName(),
                result.payload()
        );
    }

    private ToolResult queryByPhone(AiToolContext context, String phone) {
        LambdaQueryWrapper<Customer> wrapper = scopedWrapper(context);
        wrapper.eq(Customer::getPhone, phone)
                .orderByDesc(Customer::getCreateTime)
                .last("LIMIT 10");

        List<Customer> customers = customerMapper.selectListWithScope(wrapper);
        if (customers.isEmpty()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "customer_phone");
            payload.put("agent", toolName());
            payload.put("phone", phone);
            payload.put("total", 0);
            payload.put("items", List.of());
            return new ToolResult("未查询到手机号 " + phone + " 对应的客户记录。", payload);
        }

        StringBuilder sb = new StringBuilder("手机号 ")
                .append(phone)
                .append(" 查询结果（")
                .append(customers.size())
                .append(" 条）：");
        for (Customer customer : customers) {
            sb.append("\n- #").append(customer.getId())
                    .append(" 姓名=").append(customer.getCustomerName())
                    .append(" 状态=").append(statusText(customer.getStatus()))
                    .append(" 池=").append(poolTypeText(customer.getPoolType()))
                    .append(" 报备编号=").append(customer.getReportNo());
        }

        List<Map<String, Object>> items = customers.stream()
                .map(customer -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", customer.getId());
                    row.put("customerName", customer.getCustomerName());
                    row.put("phoneMasked", customer.getPhoneMasked());
                    row.put("status", customer.getStatus());
                    row.put("statusText", statusText(customer.getStatus()));
                    row.put("poolType", customer.getPoolType());
                    row.put("poolTypeText", poolTypeText(customer.getPoolType()));
                    row.put("reportNo", customer.getReportNo());
                    row.put("createTime", customer.getCreateTime());
                    return row;
                })
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "customer_phone");
        payload.put("agent", toolName());
        payload.put("phone", phone);
        payload.put("total", customers.size());
        payload.put("items", items);
        return new ToolResult(sb.toString(), payload);
    }

    private ToolResult querySummary(AiToolContext context) {
        long reportCount = countByStatus(context, 1);
        long visitCount = countByStatus(context, 2);
        long dealCount = countByStatus(context, 3);
        long totalCount = reportCount + visitCount + dealCount;

        LambdaQueryWrapper<Customer> latestWrapper = scopedWrapper(context);
        latestWrapper.orderByDesc(Customer::getCreateTime).last("LIMIT 5");
        List<Customer> latest = customerMapper.selectListWithScope(latestWrapper);

        StringBuilder sb = new StringBuilder();
        sb.append("客户概览：")
                .append("\n- 客户总数：").append(totalCount)
                .append("\n- 已报备：").append(reportCount)
                .append("\n- 已到访：").append(visitCount)
                .append("\n- 已成交：").append(dealCount);

        if (context.projectIdHint() != null) {
            sb.append("\n- 项目范围：").append(context.projectIdHint());
        }

        if (!latest.isEmpty()) {
            sb.append("\n\n最近客户（最多5条）：");
            for (Customer customer : latest) {
                sb.append("\n- #").append(customer.getId())
                        .append(" ").append(customer.getCustomerName())
                        .append(" (").append(customer.getPhoneMasked()).append(")")
                        .append(" 状态=").append(statusText(customer.getStatus()));
                }
        }

        sb.append("\n\n如需精确查询，请在问题中带上11位手机号。");

        List<Map<String, Object>> latestItems = latest.stream()
                .map(customer -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", customer.getId());
                    row.put("customerName", customer.getCustomerName());
                    row.put("phoneMasked", customer.getPhoneMasked());
                    row.put("status", customer.getStatus());
                    row.put("statusText", statusText(customer.getStatus()));
                    row.put("createTime", customer.getCreateTime());
                    return row;
                })
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "customer_summary");
        payload.put("agent", toolName());
        payload.put("totalCount", totalCount);
        payload.put("reportCount", reportCount);
        payload.put("visitCount", visitCount);
        payload.put("dealCount", dealCount);
        payload.put("latest", latestItems);
        if (context.projectIdHint() != null) {
            payload.put("projectId", context.projectIdHint());
        }
        return new ToolResult(sb.toString(), payload);
    }

    private long countByStatus(AiToolContext context, int status) {
        LambdaQueryWrapper<Customer> wrapper = scopedWrapper(context);
        wrapper.eq(Customer::getStatus, status);
        return customerMapper.selectListWithScope(wrapper).size();
    }

    private LambdaQueryWrapper<Customer> scopedWrapper(AiToolContext context) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (context.projectIdHint() != null) {
            wrapper.inSql(Customer::getId,
                    "SELECT customer_id FROM customer_project WHERE project_id = " + context.projectIdHint());
        }
        return wrapper;
    }

    private String extractPhone(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher matcher = PHONE_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 1 -> "已报备";
            case 2 -> "已到访";
            case 3 -> "已成交";
            default -> "未知";
        };
    }

    private String poolTypeText(Integer poolType) {
        if (poolType == null) {
            return "未知";
        }
        return switch (poolType) {
            case 1 -> "公海池";
            case 2 -> "私海池";
            default -> "未知";
        };
    }

    private record ToolResult(String content, Map<String, Object> payload) {
    }
}
