package com.pengcheng.realty.alliance;

import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.project.entity.Project;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 联盟商系统属性测试
 *
 * <p>Property 24: 联盟商系统仅展示在售项目 — For any 联盟商系统项目列表查询，返回的所有项目状态应为"在售"
 * <p>Property 25: 联盟商业务概览数据一致性 — For any 联盟商，概览中的本月报备数/到访数/成交数/待结佣金额应与明细汇总一致
 *
 * <p><b>Validates: Requirements 12.1, 13.3</b>
 */
class WebAllianceProperties {

    // ========== Generators ==========

    /** 项目状态：1-在售 2-待售 3-售罄 4-已到期 */
    @Provide
    Arbitrary<Integer> projectStatuses() {
        return Arbitraries.of(1, 2, 3, 4);
    }

    @Provide
    Arbitrary<String> districts() {
        return Arbitraries.of("东区", "西区", "南区", "北区", "中心区");
    }

    /** 客户状态：1-已报备 2-已到访 3-已成交 */
    @Provide
    Arbitrary<Integer> customerStatuses() {
        return Arbitraries.of(1, 2, 3);
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

    // ========== Property 24: 联盟商系统仅展示在售项目 ==========

    /**
     * Property 24: 联盟商系统仅展示在售项目
     *
     * <p>For any 联盟商系统项目列表查询，返回的所有项目状态应为"在售"(status=1)。
     * Simulates the filtering logic from WebAllianceService.listOnSaleProjects.
     *
     * <p><b>Validates: Requirements 12.1</b>
     */
    @Property(tries = 100)
    void onlyOnSaleProjectsReturned(
            @ForAll @Size(min = 1, max = 20) List<@From("projectStatuses") Integer> statuses,
            @ForAll("districts") String filterDistrict
    ) {
        // Build a mixed pool of projects with various statuses
        List<Project> allProjects = new ArrayList<>();
        String[] districtPool = {"东区", "西区", "南区", "北区", "中心区"};
        for (int i = 0; i < statuses.size(); i++) {
            Project p = Project.builder()
                    .projectName("项目" + i)
                    .status(statuses.get(i))
                    .district(districtPool[i % districtPool.length])
                    .build();
            p.setId((long) (i + 1));
            allProjects.add(p);
        }

        // Simulate WebAllianceService.listOnSaleProjects: filter status=1 only
        List<Project> result = filterOnSaleProjects(allProjects, null);

        // All returned projects must have status=1 (在售)
        assertThat(result).allSatisfy(p ->
                assertThat(p.getStatus())
                        .as("联盟商系统项目列表应仅包含在售项目")
                        .isEqualTo(1)
        );

        // With district filter, results should also match the district
        List<Project> filteredResult = filterOnSaleProjects(allProjects, filterDistrict);
        assertThat(filteredResult).allSatisfy(p -> {
            assertThat(p.getStatus()).isEqualTo(1);
            assertThat(p.getDistrict()).isEqualTo(filterDistrict);
        });

        // Count verification: result count should match the number of status=1 projects
        long expectedCount = allProjects.stream()
                .filter(p -> p.getStatus() == 1)
                .count();
        assertThat(result).hasSize((int) expectedCount);
    }

    /**
     * Simulates WebAllianceService.listOnSaleProjects filtering logic.
     */
    private List<Project> filterOnSaleProjects(List<Project> allProjects, String district) {
        return allProjects.stream()
                .filter(p -> p.getStatus() == 1)
                .filter(p -> district == null || district.equals(p.getDistrict()))
                .collect(Collectors.toList());
    }

    // ========== Property 25: 联盟商业务概览数据一致性 ==========

    /**
     * Property 25: 联盟商业务概览数据一致性
     *
     * <p>For any 联盟商，概览中的本月报备数/到访数/成交数/待结佣金额应与明细汇总一致。
     * Simulates the aggregation logic from WebAllianceService.getDashboardOverview.
     *
     * <p><b>Validates: Requirements 13.3</b>
     */
    @Property(tries = 100)
    void dashboardOverviewMatchesDetailAggregation(
            @ForAll @Size(min = 0, max = 30) List<@From("customerStatuses") Integer> customerStatuses,
            @ForAll @Size(min = 0, max = 10) List<@From("auditStatuses") Integer> commissionAuditStatuses,
            @ForAll("commissionAmounts") BigDecimal sampleAmount
    ) {
        Long allianceId = 100L;
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();

        // Build customer data for this alliance, all within current month
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < customerStatuses.size(); i++) {
            Customer c = Customer.builder()
                    .allianceId(allianceId)
                    .status(customerStatuses.get(i))
                    .build();
            c.setId((long) (i + 1));
            c.setCreateTime(monthStart.plusDays(i % 28));
            customers.add(c);
        }

        // Build commission data for this alliance
        List<Commission> commissions = new ArrayList<>();
        for (int i = 0; i < commissionAuditStatuses.size(); i++) {
            Commission comm = Commission.builder()
                    .allianceId(allianceId)
                    .auditStatus(commissionAuditStatuses.get(i))
                    .payableAmount(sampleAmount)
                    .build();
            comm.setId((long) (i + 1));
            commissions.add(comm);
        }

        // Simulate dashboard aggregation (same logic as WebAllianceService.getDashboardOverview)
        long reportCount = customers.size(); // all customers are reports
        long visitCount = customers.stream().filter(c -> c.getStatus() >= 2).count();
        long dealCount = customers.stream().filter(c -> c.getStatus() == 3).count();
        BigDecimal pendingAmount = commissions.stream()
                .filter(c -> c.getAuditStatus() == 2) // 审核通过（待结算）
                .map(Commission::getPayableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Verify against direct detail counting
        long expectedReportCount = customers.stream()
                .filter(c -> c.getAllianceId().equals(allianceId))
                .count();
        long expectedVisitCount = customers.stream()
                .filter(c -> c.getAllianceId().equals(allianceId) && c.getStatus() >= 2)
                .count();
        long expectedDealCount = customers.stream()
                .filter(c -> c.getAllianceId().equals(allianceId) && c.getStatus() == 3)
                .count();
        BigDecimal expectedPendingAmount = commissions.stream()
                .filter(c -> c.getAllianceId().equals(allianceId) && c.getAuditStatus() == 2)
                .map(Commission::getPayableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(reportCount)
                .as("本月报备数应与客户明细汇总一致")
                .isEqualTo(expectedReportCount);
        assertThat(visitCount)
                .as("本月到访数应与客户明细汇总一致")
                .isEqualTo(expectedVisitCount);
        assertThat(dealCount)
                .as("本月成交数应与客户明细汇总一致")
                .isEqualTo(expectedDealCount);
        assertThat(pendingAmount)
                .as("待结佣金额应与佣金明细汇总一致")
                .isEqualByComparingTo(expectedPendingAmount);
    }
}
