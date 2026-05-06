package com.pengcheng.ai.agent;

import com.pengcheng.ai.function.CustomerQueryFunction;
import com.pengcheng.ai.service.AiFallbackHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 智能判客智能体
 * <p>
 * 新客户报备时自动比对公海池和私海池中已有客户，判定是否存在重复报备。
 * <p>
 * 工作流程：
 * <ol>
 *   <li>接收新报备的客户手机号</li>
 *   <li>通过 CustomerQueryFunction 查询系统中已有的同手机号客户</li>
 *   <li>AI 分析判定是否存在重复</li>
 *   <li>返回判客结果</li>
 * </ol>
 * <p>
 * 降级策略：AI 服务不可用时，回退到规则引擎（手机号精确匹配）判客。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerAnalysisAgent {

    private final CustomerQueryFunction customerQueryFunction;
    private final AiFallbackHandler fallbackHandler;
    private final ChatClient chatClient;

    /**
     * 分析新报备客户是否与已有客户重复
     *
     * @param phone 新报备客户手机号
     * @return 判客结果
     */
    public DuplicateCheckResult checkDuplicate(String phone) {
        return fallbackHandler.executeWithFallback(
                () -> aiCheckDuplicate(phone),
                () -> ruleBasedCheckDuplicate(phone),
                "智能判客"
        );
    }

    /**
     * AI 智能判客：通过 ChatClient + Function Calling 分析客户重复情况
     */
    private DuplicateCheckResult aiCheckDuplicate(String phone) {
        // 先通过 Function 查询已有客户数据
        CustomerQueryFunction.Response queryResult = customerQueryFunction.apply(
                new CustomerQueryFunction.Request(phone));

        if (queryResult.totalCount() == 0) {
            return new DuplicateCheckResult(false, List.of(), "未发现重复客户，可以报备。");
        }

        // 使用 AI 分析重复情况并生成建议
        String analysisPrompt = buildAnalysisPrompt(phone, queryResult);
        try {
            String aiAnalysis = chatClient.prompt()
                    .user(analysisPrompt)
                    .call()
                    .content();

            List<ExistingCustomerInfo> existingCustomers = queryResult.customers().stream()
                    .map(c -> new ExistingCustomerInfo(
                            c.id(), c.customerName(), c.phoneMasked(),
                            c.statusText(), c.poolTypeText(), c.reportNo()))
                    .toList();

            return new DuplicateCheckResult(true, existingCustomers, aiAnalysis);
        } catch (Exception e) {
            log.warn("AI 分析失败，使用规则引擎结果: {}", e.getMessage());
            return buildRuleBasedResult(queryResult);
        }
    }

    /**
     * 规则引擎判客（降级方案）：基于手机号精确匹配
     */
    private DuplicateCheckResult ruleBasedCheckDuplicate(String phone) {
        CustomerQueryFunction.Response queryResult = customerQueryFunction.apply(
                new CustomerQueryFunction.Request(phone));
        return buildRuleBasedResult(queryResult);
    }

    private DuplicateCheckResult buildRuleBasedResult(CustomerQueryFunction.Response queryResult) {
        if (queryResult.totalCount() == 0) {
            return new DuplicateCheckResult(false, List.of(), "未发现重复客户，可以报备。");
        }

        List<ExistingCustomerInfo> existingCustomers = queryResult.customers().stream()
                .map(c -> new ExistingCustomerInfo(
                        c.id(), c.customerName(), c.phoneMasked(),
                        c.statusText(), c.poolTypeText(), c.reportNo()))
                .toList();

        String message = String.format("发现 %d 条已有客户记录（手机号匹配），请确认是否继续报备。",
                queryResult.totalCount());
        return new DuplicateCheckResult(true, existingCustomers, message);
    }

    private String buildAnalysisPrompt(String phone, CustomerQueryFunction.Response queryResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("系统中发现以下与新报备手机号相同的已有客户记录，请分析是否存在重复报备风险：\n\n");
        for (var c : queryResult.customers()) {
            sb.append(String.format("- 客户姓氏: %s, 手机号: %s, 状态: %s, 池类型: %s, 报备编号: %s\n",
                    c.customerName(), c.phoneMasked(), c.statusText(), c.poolTypeText(), c.reportNo()));
        }
        sb.append("\n请简要分析重复风险并给出建议（50字以内）。");
        return sb.toString();
    }

    /**
     * 判客结果
     */
    public record DuplicateCheckResult(
            boolean hasDuplicate,
            List<ExistingCustomerInfo> existingCustomers,
            String analysisMessage
    ) {}

    /**
     * 已有客户信息摘要
     */
    public record ExistingCustomerInfo(
            Long id,
            String customerName,
            String phoneMasked,
            String statusText,
            String poolTypeText,
            String reportNo
    ) {}
}
