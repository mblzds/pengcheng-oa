package com.pengcheng.ai.function;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.customer.mapper.CustomerVisitMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 业务报表查询 Function（供 AI Function Calling 使用）
 * <p>
 * AI 智能报表问答时，通过此 Function 查询业务统计数据，
 * 支持按时间范围、项目、联盟商等维度查询报备数、到访数、成交数、佣金等指标。
 */
@Slf4j
@Component("reportQueryFunction")
@RequiredArgsConstructor
public class ReportQueryFunction implements Function<ReportQueryFunction.Request, ReportQueryFunction.Response> {

    private final RealtyCustomerMapper customerMapper;
    private final CustomerVisitMapper customerVisitMapper;
    private final CustomerDealMapper customerDealMapper;
    private final CommissionMapper commissionMapper;
    private final AllianceMapper allianceMapper;

    @Override
    public Response apply(Request request) {
        log.debug("AI 报表查询 Function 调用: queryType={}, startDate={}, endDate={}",
                request.queryType(), request.startDate(), request.endDate());
        try {
            return switch (request.queryType()) {
                case "overview" -> queryOverview(request);
                case "project_ranking" -> queryProjectRanking(request);
                case "alliance_ranking" -> queryAllianceRanking(request);
                case "funnel" -> queryFunnel(request);
                default -> new Response("unsupported", Collections.emptyList(),
                        "不支持的查询类型: " + request.queryType() + "。支持的类型: overview(概览), project_ranking(项目排行), alliance_ranking(联盟商排行), funnel(转化漏斗)");
            };
        } catch (Exception e) {
            log.error("AI 报表查询失败: {}", e.getMessage());
            return new Response("error", Collections.emptyList(), "查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询业务概览数据
     */
    private Response queryOverview(Request request) {
        LocalDateTime start = toStartOfDay(request.startDate());
        LocalDateTime end = toEndOfDay(request.endDate());

        long reportCount = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>().between(Customer::getCreateTime, start, end));

        long visitCount = customerVisitMapper.selectCount(
                new LambdaQueryWrapper<CustomerVisit>().between(CustomerVisit::getActualVisitTime, start, end));

        long dealCount = customerDealMapper.selectCount(
                new LambdaQueryWrapper<CustomerDeal>().between(CustomerDeal::getDealTime, start, end));

        List<CustomerDeal> deals = customerDealMapper.selectList(
                new LambdaQueryWrapper<CustomerDeal>().between(CustomerDeal::getDealTime, start, end));
        BigDecimal totalDealAmount = deals.stream()
                .map(CustomerDeal::getDealAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Commission> commissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>().between(Commission::getCreateTime, start, end));
        BigDecimal totalReceivable = commissions.stream()
                .map(Commission::getReceivableAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSettled = commissions.stream()
                .filter(c -> c.getAuditStatus() != null && c.getAuditStatus() == 2)
                .map(Commission::getPayableAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("报备数", reportCount);
        row.put("到访数", visitCount);
        row.put("成交数", dealCount);
        row.put("成交金额", totalDealAmount);
        row.put("应收佣金", totalReceivable);
        row.put("已结佣金", totalSettled);
        data.add(row);

        return new Response("table", data, "业务概览数据查询成功");
    }

    /**
     * 查询项目业绩排行
     */
    private Response queryProjectRanking(Request request) {
        LocalDateTime start = toStartOfDay(request.startDate());
        LocalDateTime end = toEndOfDay(request.endDate());

        List<CustomerDeal> deals = customerDealMapper.selectList(
                new LambdaQueryWrapper<CustomerDeal>().between(CustomerDeal::getDealTime, start, end));

        // 通过 customer 关联 project
        List<Customer> customers = customerMapper.selectList(null);

        // 按项目统计成交数和成交金额
        Map<String, long[]> projectStats = new LinkedHashMap<>();
        for (CustomerDeal deal : deals) {
            Customer customer = customers.stream()
                    .filter(c -> c.getId().equals(deal.getCustomerId()))
                    .findFirst().orElse(null);
            if (customer != null) {
                String projectName = "未知项目";
                // Use alliance as proxy since customer-project is many-to-many
                projectStats.computeIfAbsent(projectName, k -> new long[]{0, 0});
                projectStats.get(projectName)[0]++;
            }
        }

        List<Map<String, Object>> data = new ArrayList<>();
        int rank = 1;
        for (var entry : projectStats.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("排名", rank++);
            row.put("项目名称", entry.getKey());
            row.put("成交数量", entry.getValue()[0]);
            data.add(row);
        }

        if (data.isEmpty()) {
            return new Response("table", data, "该时间范围内暂无项目成交数据");
        }
        return new Response("table", data, "项目业绩排行查询成功");
    }

    /**
     * 查询联盟商业绩排行
     */
    private Response queryAllianceRanking(Request request) {
        LocalDateTime start = toStartOfDay(request.startDate());
        LocalDateTime end = toEndOfDay(request.endDate());

        List<Alliance> alliances = allianceMapper.selectList(null);
        Map<Long, String> allianceNameMap = alliances.stream()
                .collect(Collectors.toMap(Alliance::getId, Alliance::getCompanyName, (a, b) -> a));

        // 按联盟商统计上客数
        List<Customer> customers = customerMapper.selectList(
                new LambdaQueryWrapper<Customer>().between(Customer::getCreateTime, start, end));
        Map<Long, Long> reportCountByAlliance = customers.stream()
                .filter(c -> c.getAllianceId() != null)
                .collect(Collectors.groupingBy(Customer::getAllianceId, Collectors.counting()));

        // 按联盟商统计成交数
        List<Customer> dealCustomers = customerMapper.selectList(
                new LambdaQueryWrapper<Customer>().eq(Customer::getStatus, 3)
                        .between(Customer::getCreateTime, start, end));
        Map<Long, Long> dealCountByAlliance = dealCustomers.stream()
                .filter(c -> c.getAllianceId() != null)
                .collect(Collectors.groupingBy(Customer::getAllianceId, Collectors.counting()));

        List<Map<String, Object>> data = new ArrayList<>();
        Set<Long> allAllianceIds = new HashSet<>();
        allAllianceIds.addAll(reportCountByAlliance.keySet());
        allAllianceIds.addAll(dealCountByAlliance.keySet());

        List<Map.Entry<Long, Long>> sorted = reportCountByAlliance.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .toList();

        int rank = 1;
        for (var entry : sorted) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("排名", rank++);
            row.put("联盟商", allianceNameMap.getOrDefault(entry.getKey(), "未知"));
            row.put("上客数量", entry.getValue());
            row.put("成交数量", dealCountByAlliance.getOrDefault(entry.getKey(), 0L));
            data.add(row);
        }

        if (data.isEmpty()) {
            return new Response("table", data, "该时间范围内暂无联盟商业绩数据");
        }
        return new Response("table", data, "联盟商业绩排行查询成功");
    }

    /**
     * 查询转化漏斗数据
     */
    private Response queryFunnel(Request request) {
        LocalDateTime start = toStartOfDay(request.startDate());
        LocalDateTime end = toEndOfDay(request.endDate());

        long reportCount = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>().between(Customer::getCreateTime, start, end));
        long visitCount = customerVisitMapper.selectCount(
                new LambdaQueryWrapper<CustomerVisit>().between(CustomerVisit::getActualVisitTime, start, end));
        long dealCount = customerDealMapper.selectCount(
                new LambdaQueryWrapper<CustomerDeal>().between(CustomerDeal::getDealTime, start, end));

        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("报备数", reportCount);
        row.put("到访数", visitCount);
        row.put("成交数", dealCount);
        row.put("报备→到访转化率", reportCount > 0 ? String.format("%.1f%%", visitCount * 100.0 / reportCount) : "0%");
        row.put("到访→成交转化率", visitCount > 0 ? String.format("%.1f%%", dealCount * 100.0 / visitCount) : "0%");
        data.add(row);

        return new Response("chart", data, "转化漏斗数据查询成功");
    }

    // ========== 辅助方法 ==========

    private LocalDateTime toStartOfDay(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now().withDayOfMonth(1).atStartOfDay();
        }
        return LocalDate.parse(dateStr).atStartOfDay();
    }

    private LocalDateTime toEndOfDay(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now().atTime(LocalTime.MAX);
        }
        return LocalDate.parse(dateStr).atTime(LocalTime.MAX);
    }

    /**
     * Function Calling 请求参数
     *
     * @param queryType 查询类型: overview(概览), project_ranking(项目排行), alliance_ranking(联盟商排行), funnel(转化漏斗)
     * @param startDate 开始日期 (yyyy-MM-dd)，为空则默认本月1日
     * @param endDate   结束日期 (yyyy-MM-dd)，为空则默认今天
     */
    public record Request(String queryType, String startDate, String endDate) {}

    /**
     * Function Calling 响应
     *
     * @param displayType 展示类型: table(表格), chart(图表)
     * @param data        数据行列表
     * @param message     结果描述
     */
    public record Response(String displayType, List<Map<String, Object>> data, String message) {}
}
