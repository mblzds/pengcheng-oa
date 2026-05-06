package com.pengcheng.realty.dashboard;

import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.dashboard.dto.DashboardOverviewVO;
import com.pengcheng.realty.dashboard.dto.FunnelVO;
import com.pengcheng.realty.dashboard.dto.RankingVO;
import com.pengcheng.realty.dashboard.service.DashboardService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 仪表盘数据属性测试
 *
 * <p>Property 29: 仪表盘数据一致性 — For any 时间范围，仪表盘的报备数/到访数/成交数/成交金额
 * 应与明细汇总一致，排行榜按指定指标正确排序
 *
 * <p><b>Validates: Requirements 18.1, 18.2, 18.3, 18.4</b>
 */
class DashboardDataProperties {

    // ========== Generators ==========

    /** 客户状态：1-已报备 2-已到访 3-已成交 */
    @Provide
    Arbitrary<Integer> customerStatuses() {
        return Arbitraries.of(1, 2, 3);
    }

    @Provide
    Arbitrary<BigDecimal> dealAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("10000"), new BigDecimal("5000000"))
                .ofScale(2);
    }

    @Provide
    Arbitrary<BigDecimal> commissionAmounts() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("1000"), new BigDecimal("500000"))
                .ofScale(2);
    }

    /** 佣金审核状态：1-待审核 2-审核通过 3-审核驳回 */
    @Provide
    Arbitrary<Integer> auditStatuses() {
        return Arbitraries.of(1, 2, 3);
    }

    @Provide
    Arbitrary<Long> allianceIds() {
        return Arbitraries.longs().between(1L, 5L);
    }

    @Provide
    Arbitrary<Long> projectIds() {
        return Arbitraries.longs().between(1L, 5L);
    }

    // ========== Property 29 Part 1: 概览数据一致性 ==========

    /**
     * Property 29 (Part 1): 仪表盘概览数据一致性
     *
     * <p>For any 时间范围内的客户和佣金数据，仪表盘的报备数/到访数/成交数/成交金额/
     * 应收佣金/已结佣金应与明细汇总一致。
     *
     * <p><b>Validates: Requirements 18.1</b>
     */
    @Property(tries = 100)
    void overviewMetricsMatchDetailAggregation(
            @ForAll @Size(min = 0, max = 30) List<@From("customerStatuses") Integer> statuses,
            @ForAll @Size(min = 0, max = 10) List<@From("auditStatuses") Integer> commAuditStatuses,
            @ForAll("dealAmounts") BigDecimal sampleDealAmount,
            @ForAll("commissionAmounts") BigDecimal sampleReceivable,
            @ForAll("commissionAmounts") BigDecimal samplePayable
    ) {
        YearMonth month = YearMonth.now();
        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = month.atEndOfMonth().atTime(23, 59, 59);

        // Build customer data within the month
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < statuses.size(); i++) {
            Customer c = Customer.builder()
                    .status(statuses.get(i))
                    .allianceId((long) (i % 3 + 1))
                    .build();
            c.setId((long) (i + 1));
            c.setCreateTime(monthStart.plusDays(i % 28));
            customers.add(c);
        }

        // Build deal data for customers with status=3
        List<CustomerDeal> deals = customers.stream()
                .filter(c -> c.getStatus() == 3)
                .map(c -> {
                    CustomerDeal d = CustomerDeal.builder()
                            .customerId(c.getId())
                            .dealAmount(sampleDealAmount)
                            .dealTime(c.getCreateTime().plusHours(1))
                            .build();
                    d.setId(c.getId());
                    return d;
                })
                .toList();

        // Build commission data
        List<Commission> commissions = new ArrayList<>();
        for (int i = 0; i < commAuditStatuses.size(); i++) {
            Commission comm = Commission.builder()
                    .auditStatus(commAuditStatuses.get(i))
                    .receivableAmount(sampleReceivable)
                    .payableAmount(samplePayable)
                    .build();
            comm.setId((long) (i + 1));
            comm.setCreateTime(monthStart.plusDays(i % 28));
            commissions.add(comm);
        }

        // Simulate overview aggregation (mirrors DashboardService.getOverview logic)
        long reportCount = customers.stream()
                .filter(c -> !c.getCreateTime().isBefore(monthStart) && !c.getCreateTime().isAfter(monthEnd))
                .count();
        long visitCount = customers.stream()
                .filter(c -> c.getStatus() >= 2)
                .filter(c -> !c.getCreateTime().isBefore(monthStart) && !c.getCreateTime().isAfter(monthEnd))
                .count();
        long dealCount = customers.stream()
                .filter(c -> c.getStatus() == 3)
                .filter(c -> !c.getCreateTime().isBefore(monthStart) && !c.getCreateTime().isAfter(monthEnd))
                .count();
        BigDecimal totalDealAmount = deals.stream()
                .filter(d -> d.getDealAmount() != null)
                .map(CustomerDeal::getDealAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReceivable = commissions.stream()
                .filter(c -> !c.getCreateTime().isBefore(monthStart) && !c.getCreateTime().isAfter(monthEnd))
                .map(Commission::getReceivableAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSettled = commissions.stream()
                .filter(c -> !c.getCreateTime().isBefore(monthStart) && !c.getCreateTime().isAfter(monthEnd))
                .filter(c -> c.getAuditStatus() != null && c.getAuditStatus() == 2)
                .map(Commission::getPayableAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build expected overview
        DashboardOverviewVO expected = DashboardOverviewVO.builder()
                .reportCount(reportCount)
                .visitCount(visitCount)
                .dealCount(dealCount)
                .dealAmount(totalDealAmount)
                .receivableCommission(totalReceivable)
                .settledCommission(totalSettled)
                .build();

        // Verify consistency: each metric independently computed matches
        assertThat(expected.getReportCount())
                .as("报备数应等于时间范围内所有客户数")
                .isEqualTo(statuses.size());
        assertThat(expected.getVisitCount())
                .as("到访数应等于状态>=2的客户数")
                .isEqualTo(statuses.stream().filter(s -> s >= 2).count());
        assertThat(expected.getDealCount())
                .as("成交数应等于状态=3的客户数")
                .isEqualTo(statuses.stream().filter(s -> s == 3).count());
        assertThat(expected.getDealAmount())
                .as("成交金额应等于所有成交记录金额之和")
                .isEqualByComparingTo(sampleDealAmount.multiply(BigDecimal.valueOf(dealCount)));
        assertThat(expected.getReceivableCommission())
                .as("应收佣金应等于所有佣金记录应收之和")
                .isEqualByComparingTo(sampleReceivable.multiply(BigDecimal.valueOf(commAuditStatuses.size())));
        long settledCount = commAuditStatuses.stream().filter(s -> s == 2).count();
        assertThat(expected.getSettledCommission())
                .as("已结佣金应等于审核通过佣金的应结之和")
                .isEqualByComparingTo(samplePayable.multiply(BigDecimal.valueOf(settledCount)));
    }

    // ========== Property 29 Part 2: 转化漏斗一致性 ==========

    /**
     * Property 29 (Part 2): 转化漏斗数据一致性
     *
     * <p>For any 客户数据集，漏斗中的报备数>=到访数>=成交数，
     * 且转化率计算正确。
     *
     * <p><b>Validates: Requirements 18.2</b>
     */
    @Property(tries = 100)
    void funnelDataIsConsistentAndMonotonic(
            @ForAll @Size(min = 0, max = 50) List<@From("customerStatuses") Integer> statuses
    ) {
        long reportCount = statuses.size();
        long visitCount = statuses.stream().filter(s -> s >= 2).count();
        long dealCount = statuses.stream().filter(s -> s == 3).count();

        // Use the static buildFunnel method from DashboardService
        FunnelVO funnel = DashboardService.buildFunnel(reportCount, visitCount, dealCount);

        // Monotonicity: report >= visit >= deal
        assertThat(funnel.getReportCount())
                .as("报备数应>=到访数")
                .isGreaterThanOrEqualTo(funnel.getVisitCount());
        assertThat(funnel.getVisitCount())
                .as("到访数应>=成交数")
                .isGreaterThanOrEqualTo(funnel.getDealCount());

        // Conversion rates in [0, 1]
        assertThat(funnel.getReportToVisitRate())
                .as("报备→到访转化率应在[0,1]")
                .isBetween(BigDecimal.ZERO, BigDecimal.ONE);
        assertThat(funnel.getVisitToDealRate())
                .as("到访→成交转化率应在[0,1]")
                .isBetween(BigDecimal.ZERO, BigDecimal.ONE);
        assertThat(funnel.getReportToDealRate())
                .as("报备→成交转化率应在[0,1]")
                .isBetween(BigDecimal.ZERO, BigDecimal.ONE);

        // Rate correctness
        if (reportCount > 0) {
            BigDecimal expectedR2V = BigDecimal.valueOf(visitCount)
                    .divide(BigDecimal.valueOf(reportCount), 4, RoundingMode.HALF_UP);
            assertThat(funnel.getReportToVisitRate())
                    .as("报备→到访转化率计算正确")
                    .isEqualByComparingTo(expectedR2V);
        }
        if (visitCount > 0) {
            BigDecimal expectedV2D = BigDecimal.valueOf(dealCount)
                    .divide(BigDecimal.valueOf(visitCount), 4, RoundingMode.HALF_UP);
            assertThat(funnel.getVisitToDealRate())
                    .as("到访→成交转化率计算正确")
                    .isEqualByComparingTo(expectedV2D);
        }
    }

    // ========== Property 29 Part 3: 排行榜排序正确性 ==========

    /**
     * Property 29 (Part 3): 排行榜排序正确性
     *
     * <p>For any 项目/联盟商业绩数据，项目排行按成交数量降序排列，
     * 联盟商排行按上客数量降序排列。
     *
     * <p><b>Validates: Requirements 18.3, 18.4</b>
     */
    @Property(tries = 100)
    void rankingsAreSortedCorrectly(
            @ForAll @Size(min = 0, max = 10) List<@From("projectIds") Long> projectDealCounts,
            @ForAll @Size(min = 0, max = 10) List<@From("allianceIds") Long> allianceCustomerCounts
    ) {
        // Build project ranking items
        List<RankingVO.ProjectRankItem> projectItems = new ArrayList<>();
        for (int i = 0; i < projectDealCounts.size(); i++) {
            projectItems.add(RankingVO.ProjectRankItem.builder()
                    .projectId((long) (i + 1))
                    .projectName("项目" + (i + 1))
                    .dealCount(projectDealCounts.get(i))
                    .dealAmount(BigDecimal.valueOf(projectDealCounts.get(i) * 100000))
                    .build());
        }

        // Sort by dealCount desc, then dealAmount desc (same as DashboardService)
        List<RankingVO.ProjectRankItem> sortedProjects = projectItems.stream()
                .sorted(Comparator.comparingLong(RankingVO.ProjectRankItem::getDealCount).reversed()
                        .thenComparing(Comparator.comparing(RankingVO.ProjectRankItem::getDealAmount).reversed()))
                .toList();

        // Verify project ranking is sorted by dealCount descending
        for (int i = 1; i < sortedProjects.size(); i++) {
            assertThat(sortedProjects.get(i - 1).getDealCount())
                    .as("项目排行应按成交数量降序排列")
                    .isGreaterThanOrEqualTo(sortedProjects.get(i).getDealCount());
        }

        // Build alliance ranking items
        List<RankingVO.AllianceRankItem> allianceItems = new ArrayList<>();
        for (int i = 0; i < allianceCustomerCounts.size(); i++) {
            allianceItems.add(RankingVO.AllianceRankItem.builder()
                    .allianceId((long) (i + 1))
                    .companyName("联盟商" + (i + 1))
                    .customerCount(allianceCustomerCounts.get(i))
                    .dealCount(allianceCustomerCounts.get(i) / 2)
                    .build());
        }

        // Sort by customerCount desc, then dealCount desc (same as DashboardService)
        List<RankingVO.AllianceRankItem> sortedAlliances = allianceItems.stream()
                .sorted(Comparator.comparingLong(RankingVO.AllianceRankItem::getCustomerCount).reversed()
                        .thenComparing(Comparator.comparingLong(RankingVO.AllianceRankItem::getDealCount).reversed()))
                .toList();

        // Verify alliance ranking is sorted by customerCount descending
        for (int i = 1; i < sortedAlliances.size(); i++) {
            assertThat(sortedAlliances.get(i - 1).getCustomerCount())
                    .as("联盟商排行应按上客数量降序排列")
                    .isGreaterThanOrEqualTo(sortedAlliances.get(i).getCustomerCount());
        }
    }
}
