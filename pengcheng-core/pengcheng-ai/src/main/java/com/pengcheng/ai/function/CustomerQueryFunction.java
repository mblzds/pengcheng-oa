package com.pengcheng.ai.function;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 客户数据查询 Function（供 AI Function Calling 使用）
 * <p>
 * AI 智能判客时，通过此 Function 查询公海池和私海池中的客户数据，
 * 判定新报备客户是否与已有客户重复。
 */
@Slf4j
@Component("customerQueryFunction")
@RequiredArgsConstructor
public class CustomerQueryFunction implements Function<CustomerQueryFunction.Request, CustomerQueryFunction.Response> {

    private final RealtyCustomerMapper customerMapper;

    @Override
    public Response apply(Request request) {
        log.debug("AI 客户查询 Function 调用: phone={}", request.phone());
        try {
            LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Customer::getPhone, request.phone());
            List<Customer> customers = customerMapper.selectList(wrapper);

            List<CustomerInfo> results = customers.stream()
                    .map(c -> new CustomerInfo(
                            c.getId(),
                            c.getCustomerName(),
                            c.getPhoneMasked(),
                            c.getStatus(),
                            statusText(c.getStatus()),
                            c.getPoolType(),
                            poolTypeText(c.getPoolType()),
                            c.getReportNo()
                    ))
                    .toList();

            return new Response(results, results.size());
        } catch (Exception e) {
            log.error("AI 客户查询失败: {}", e.getMessage());
            return new Response(Collections.emptyList(), 0);
        }
    }

    private String statusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "已报备";
            case 2 -> "已到访";
            case 3 -> "已成交";
            default -> "未知";
        };
    }

    private String poolTypeText(Integer poolType) {
        if (poolType == null) return "未知";
        return switch (poolType) {
            case 1 -> "公海池";
            case 2 -> "私海池";
            default -> "未知";
        };
    }

    /**
     * Function Calling 请求参数
     */
    public record Request(String phone) {}

    /**
     * Function Calling 响应
     */
    public record Response(List<CustomerInfo> customers, int totalCount) {}

    /**
     * 客户信息摘要（返回给 AI）
     */
    public record CustomerInfo(
            Long id,
            String customerName,
            String phoneMasked,
            Integer status,
            String statusText,
            Integer poolType,
            String poolTypeText,
            String reportNo
    ) {}
}
