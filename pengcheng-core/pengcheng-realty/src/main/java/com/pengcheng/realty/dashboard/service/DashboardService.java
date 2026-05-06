package com.pengcheng.realty.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.entity.CustomerProject;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.customer.mapper.CustomerProjectMapper;
import com.pengcheng.realty.dashboard.dto.*;
import com.pengcheng.realty.project.entity.Project;
import com.pengcheng.realty.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计仪表盘服务
 * <p>
 * 提供核心指标概览、转化漏斗、项目/联盟商业绩排行榜等统计能力。
 * 支持按时间范围切换实时更新所有统计数据。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RealtyCustomerMapper customerMapper;
    private final CustomerDealMapper customerDealMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final CommissionMapper commissionMapper;
    private final ProjectMapper projectMapper;
    private final AllianceMapper allianceMapper;

    /**
     * 获取核心指标概览
     * <p>
     * 默认查询当月数据，支持自定义时间范围。
     */
    public DashboardOverviewVO getOverview(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = resolveStart(startDate);
        LocalDateTime end = resolveEnd(endDate);

        // 报备数：时间范围内创建的客户
        long reportCount = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>()
                        .ge(Customer::getCreateTime, start)
                        .le(Customer::getCreateTime, end));

        // 到访数：状态>=2（已到访或已成交）且在时间范围内创建
        long visitCount = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>()
                        .ge(Customer::getStatus, 2)
                        .ge(Customer::getCreateTime, start)
                        .le(Customer::getCreateTime, end));

        // 成交数：状态=3（已成交）且在时间范围内创建
        long dealCount = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getStatus, 3)
                        .ge(Customer::getCreateTime, start)
                        .le(Customer::getCreateTime, end));

        // 成交金额：时间范围内的成交记录金额汇总
        List<CustomerDeal> deals = customerDealMapper.selectList(
                new LambdaQueryWrapper<CustomerDeal>()
                        .ge(CustomerDeal::getDealTime, start)
                        .le(CustomerDeal::getDealTime, end));
        BigDecimal dealAmount = deals.stream()
                .map(CustomerDeal::getDealAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 应收佣金：时间范围内创建的佣金记录
        List<Commission> commissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .ge(Commission::getCreateTime, start)
                        .le(Commission::getCreateTime, end));
        BigDecimal receivable = commissions.stream()
                .map(Commission::getReceivableAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 已结佣金：审核通过的佣金
        BigDecimal settled = commissions.stream()
                .filter(c -> c.getAuditStatus() != null && c.getAuditStatus() == 2)
                .map(Commission::getPayableAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardOverviewVO.builder()
                .reportCount(reportCount)
                .visitCount(visitCount)
                .dealCount(dealCount)
                .dealAmount(dealAmount)
                .receivableCommission(receivable)
                .settledCommission(settled)
                .build();
    }

    /**
     * 获取报备-到访-成交转化漏斗数据
     * <p>
     * 支持按项目、联盟商、时间维度筛选。
     */
    public FunnelVO getFunnel(FunnelQueryDTO query) {
        LocalDateTime start = resolveStart(query.getStartDate());
        LocalDateTime end = resolveEnd(query.getEndDate());

        // 如果按项目筛选，需要通过 customer_project 中间表找到关联客户
        Set<Long> projectCustomerIds = null;
        if (query.getProjectId() != null) {
            List<CustomerProject> cps = customerProjectMapper.selectList(
                    new LambdaQueryWrapper<CustomerProject>()
                            .eq(CustomerProject::getProjectId, query.getProjectId()));
            projectCustomerIds = cps.stream()
                    .map(CustomerProject::getCustomerId)
                    .collect(Collectors.toSet());
            if (projectCustomerIds.isEmpty()) {
                return buildEmptyFunnel();
            }
        }

        // 报备数
        long reportCount = customerMapper.selectCount(
                buildFunnelWrapper(start, end, query.getAllianceId(), projectCustomerIds, null));

        // 到访数（状态>=2）
        long visitCount = customerMapper.selectCount(
                buildFunnelWrapper(start, end, query.getAllianceId(), projectCustomerIds, 2));

        // 成交数（状态=3）
        long dealCount = customerMapper.selectCount(
                buildFunnelWrapper(start, end, query.getAllianceId(), projectCustomerIds, 3));

        return buildFunnel(reportCount, visitCount, dealCount);
    }

    /**
     * 获取业绩排行榜
     * <p>
     * 项目排行按成交数量和成交金额排序，联盟商排行按上客数量和成交数量排序。
     */
    public RankingVO getRanking(RankingQueryDTO query) {
        LocalDateTime start = resolveStart(query.getStartDate());
        LocalDateTime end = resolveEnd(query.getEndDate());

        // ===== 项目业绩排行 =====
        // 获取时间范围内已成交客户
        List<Customer> dealCustomers = customerMapper.selectList(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getStatus, 3)
                        .ge(Customer::getCreateTime, start)
                        .le(Customer::getCreateTime, end));

        Set<Long> dealCustomerIds = dealCustomers.stream()
                .map(Customer::getId)
                .collect(Collectors.toSet());

        // 通过 customer_project 关联获取项目维度的成交数
        final Map<Long, Long> projectDealCountMap;
        if (!dealCustomerIds.isEmpty()) {
            List<CustomerProject> cps = customerProjectMapper.selectList(
                    new LambdaQueryWrapper<CustomerProject>()
                            .in(CustomerProject::getCustomerId, dealCustomerIds));
            projectDealCountMap = cps.stream()
                    .collect(Collectors.groupingBy(CustomerProject::getProjectId, Collectors.counting()));
        } else {
            projectDealCountMap = new HashMap<>();
        }

        // 获取成交金额（通过 CustomerDeal）
        final Map<Long, BigDecimal> customerDealAmountMap;
        if (!dealCustomerIds.isEmpty()) {
            List<CustomerDeal> deals = customerDealMapper.selectList(
                    new LambdaQueryWrapper<CustomerDeal>()
                            .in(CustomerDeal::getCustomerId, dealCustomerIds));
            customerDealAmountMap = deals.stream()
                    .filter(d -> d.getDealAmount() != null)
                    .collect(Collectors.toMap(CustomerDeal::getCustomerId, CustomerDeal::getDealAmount,
                            BigDecimal::add));
        } else {
            customerDealAmountMap = new HashMap<>();
        }

        // 计算每个项目的成交金额
        Map<Long, BigDecimal> projectDealAmountMap = new HashMap<>();
        if (!dealCustomerIds.isEmpty()) {
            List<CustomerProject> allCps = customerProjectMapper.selectList(
                    new LambdaQueryWrapper<CustomerProject>()
                            .in(CustomerProject::getCustomerId, dealCustomerIds));
            for (CustomerProject cp : allCps) {
                BigDecimal amount = customerDealAmountMap.getOrDefault(cp.getCustomerId(), BigDecimal.ZERO);
                projectDealAmountMap.merge(cp.getProjectId(), amount, BigDecimal::add);
            }
        }

        // 获取项目名称
        Set<Long> allProjectIds = new HashSet<>();
        allProjectIds.addAll(projectDealCountMap.keySet());
        allProjectIds.addAll(projectDealAmountMap.keySet());

        Map<Long, String> projectNameMap = new HashMap<>();
        if (!allProjectIds.isEmpty()) {
            List<Project> projects = projectMapper.selectBatchIds(allProjectIds);
            projects.forEach(p -> projectNameMap.put(p.getId(), p.getProjectName()));
        }

        List<RankingVO.ProjectRankItem> projectRanking = allProjectIds.stream()
                .map(pid -> RankingVO.ProjectRankItem.builder()
                        .projectId(pid)
                        .projectName(projectNameMap.getOrDefault(pid, ""))
                        .dealCount(projectDealCountMap.getOrDefault(pid, 0L))
                        .dealAmount(projectDealAmountMap.getOrDefault(pid, BigDecimal.ZERO))
                        .build())
                .sorted(Comparator.comparingLong(RankingVO.ProjectRankItem::getDealCount).reversed()
                        .thenComparing(Comparator.comparing(RankingVO.ProjectRankItem::getDealAmount).reversed()))
                .toList();

        // ===== 联盟商业绩排行 =====
        // 获取时间范围内所有客户（上客数量=报备数）
        List<Customer> allCustomers = customerMapper.selectList(
                new LambdaQueryWrapper<Customer>()
                        .ge(Customer::getCreateTime, start)
                        .le(Customer::getCreateTime, end));

        Map<Long, Long> allianceCustomerCountMap = allCustomers.stream()
                .filter(c -> c.getAllianceId() != null)
                .collect(Collectors.groupingBy(Customer::getAllianceId, Collectors.counting()));

        Map<Long, Long> allianceDealCountMap = dealCustomers.stream()
                .filter(c -> c.getAllianceId() != null)
                .collect(Collectors.groupingBy(Customer::getAllianceId, Collectors.counting()));

        Set<Long> allAllianceIds = new HashSet<>();
        allAllianceIds.addAll(allianceCustomerCountMap.keySet());
        allAllianceIds.addAll(allianceDealCountMap.keySet());

        Map<Long, String> allianceNameMap = new HashMap<>();
        if (!allAllianceIds.isEmpty()) {
            List<Alliance> alliances = allianceMapper.selectBatchIds(allAllianceIds);
            alliances.forEach(a -> allianceNameMap.put(a.getId(), a.getCompanyName()));
        }

        List<RankingVO.AllianceRankItem> allianceRanking = allAllianceIds.stream()
                .map(aid -> RankingVO.AllianceRankItem.builder()
                        .allianceId(aid)
                        .companyName(allianceNameMap.getOrDefault(aid, ""))
                        .customerCount(allianceCustomerCountMap.getOrDefault(aid, 0L))
                        .dealCount(allianceDealCountMap.getOrDefault(aid, 0L))
                        .build())
                .sorted(Comparator.comparingLong(RankingVO.AllianceRankItem::getCustomerCount).reversed()
                        .thenComparing(Comparator.comparingLong(RankingVO.AllianceRankItem::getDealCount).reversed()))
                .toList();

        return RankingVO.builder()
                .projectRanking(projectRanking)
                .allianceRanking(allianceRanking)
                .build();
    }

    // ========== Helper methods ==========

    private LocalDateTime resolveStart(LocalDate startDate) {
        if (startDate != null) {
            return startDate.atStartOfDay();
        }
        return YearMonth.now().atDay(1).atStartOfDay();
    }

    private LocalDateTime resolveEnd(LocalDate endDate) {
        if (endDate != null) {
            return endDate.atTime(23, 59, 59);
        }
        return YearMonth.now().atEndOfMonth().atTime(23, 59, 59);
    }

    private FunnelVO buildEmptyFunnel() {
        return FunnelVO.builder()
                .reportCount(0L)
                .visitCount(0L)
                .dealCount(0L)
                .reportToVisitRate(BigDecimal.ZERO)
                .visitToDealRate(BigDecimal.ZERO)
                .reportToDealRate(BigDecimal.ZERO)
                .build();
    }

    public static FunnelVO buildFunnel(long reportCount, long visitCount, long dealCount) {
        BigDecimal r2v = reportCount > 0
                ? BigDecimal.valueOf(visitCount).divide(BigDecimal.valueOf(reportCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal v2d = visitCount > 0
                ? BigDecimal.valueOf(dealCount).divide(BigDecimal.valueOf(visitCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal r2d = reportCount > 0
                ? BigDecimal.valueOf(dealCount).divide(BigDecimal.valueOf(reportCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return FunnelVO.builder()
                .reportCount(reportCount)
                .visitCount(visitCount)
                .dealCount(dealCount)
                .reportToVisitRate(r2v)
                .visitToDealRate(v2d)
                .reportToDealRate(r2d)
                .build();
    }

    /**
     * Build a funnel query wrapper with optional status filter.
     * @param minStatus null=all, 2=visited+, 3=deal only
     */
    private LambdaQueryWrapper<Customer> buildFunnelWrapper(
            LocalDateTime start, LocalDateTime end,
            Long allianceId, Set<Long> projectCustomerIds, Integer minStatus) {
        LambdaQueryWrapper<Customer> w = new LambdaQueryWrapper<Customer>()
                .ge(Customer::getCreateTime, start)
                .le(Customer::getCreateTime, end);
        if (allianceId != null) {
            w.eq(Customer::getAllianceId, allianceId);
        }
        if (projectCustomerIds != null) {
            w.in(Customer::getId, projectCustomerIds);
        }
        if (minStatus != null) {
            if (minStatus == 3) {
                w.eq(Customer::getStatus, 3);
            } else {
                w.ge(Customer::getStatus, minStatus);
            }
        }
        return w;
    }
}
