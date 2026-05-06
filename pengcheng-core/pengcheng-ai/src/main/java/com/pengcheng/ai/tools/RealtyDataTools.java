package com.pengcheng.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.receivable.service.ReceivableService;
import com.pengcheng.realty.receivable.vo.ReceivableStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 房产业务只读数据工具（Spring AI 1.1 Function Calling 样例）。
 * <p>
 * 通过 {@code @Tool} 暴露给 LLM，由 ChatClient 在对话中按需调用 — LLM 解析用户意图后
 * 自动决定是否触发，结果返回给模型组织最终回答。
 * <p>
 * 注意事项：
 * <ul>
 *   <li>仅包含 <b>只读聚合</b>，不接受写参数，防止 prompt injection 引发数据变更</li>
 *   <li>方法返回 {@code Map<String,Object>} 或 POJO，Jackson 自动转 JSON 注入到 prompt</li>
 *   <li>工具粒度：按"业务问题"建模，而非直接暴露 CRUD 接口</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtyDataTools {

    private final CustomerDealMapper customerDealMapper;
    private final ReceivableService receivableService;

    /**
     * 查询今日签约/成交数与金额。用户提问示例："今天签了几单？" / "今日成交金额"
     */
    @Tool(description = """
            查询指定日期范围内的房产成交单数与成交总金额。
            适用场景：用户询问签约数、成交数、业绩等经营指标。
            若用户未指定日期，默认查询"今日"。
            """)
    public Map<String, Object> getDealSummary(
            @ToolParam(description = "开始日期，ISO 格式 yyyy-MM-dd；null 表示今日") String fromDate,
            @ToolParam(description = "结束日期，ISO 格式 yyyy-MM-dd；null 表示今日") String toDate) {

        LocalDate from = parseOrToday(fromDate);
        LocalDate to = parseOrToday(toDate);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<CustomerDeal> deals = customerDealMapper.selectList(
                new LambdaQueryWrapper<CustomerDeal>()
                        .ge(CustomerDeal::getDealTime, start)
                        .lt(CustomerDeal::getDealTime, end));

        BigDecimal totalAmount = deals.stream()
                .map(d -> d.getDealAmount() == null ? BigDecimal.ZERO : d.getDealAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fromDate", from.toString());
        result.put("toDate", to.toString());
        result.put("dealCount", deals.size());
        result.put("totalAmount", totalAmount);
        result.put("averageAmount", deals.isEmpty()
                ? BigDecimal.ZERO
                : totalAmount.divide(BigDecimal.valueOf(deals.size()), 2, java.math.RoundingMode.HALF_UP));

        log.info("[Tool/getDealSummary] {} ~ {}: {} 单 / {}", from, to, deals.size(), totalAmount);
        return result;
    }

    /**
     * 查询回款总览：应收/已收/未收/逾期。用户提问示例："现在回款进度怎么样" / "有多少钱没回来"
     */
    @Tool(description = """
            查询整个系统的回款总览，包括应收总额、已收总额、未收总额、逾期金额与逾期期数。
            适用场景：用户询问现金流、回款进度、资金紧张度等。
            """)
    public ReceivableStatsVO getReceivableOverview() {
        ReceivableStatsVO stats = receivableService.stats();
        log.info("[Tool/getReceivableOverview] due={} paid={} unpaid={} overdue={}",
                stats.getTotalDue(), stats.getTotalPaid(),
                stats.getTotalUnpaid(), stats.getTotalOverdue());
        return stats;
    }

    private static LocalDate parseOrToday(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
