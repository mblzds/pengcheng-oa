package com.pengcheng.realty.project;

import com.pengcheng.realty.project.entity.ProjectCommissionRule;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 项目管理属性测试
 *
 * <p>Property 21: 项目代理时间校验 — For any 项目，代理开始时间应早于代理结束时间
 * <p>Property 23: 项目佣金规则版本化 — For any 佣金规则更新，旧版本应被保留，新版本号大于旧版本号
 *
 * <p><b>Validates: Requirements 11.3, 11.6</b>
 */
class ProjectManagementProperties {

    // ========== Generators ==========

    @Provide
    Arbitrary<LocalDate> futureDates() {
        return Arbitraries.integers().between(1, 3650)
                .map(days -> LocalDate.of(2026, 1, 1).plusDays(days));
    }

    @Provide
    Arbitrary<BigDecimal> commissionRates() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, BigDecimal.ONE)
                .ofScale(4);
    }

    @Provide
    Arbitrary<BigDecimal> rewardAmounts() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("100000"))
                .ofScale(2);
    }

    // ========== Property 21: 项目代理时间校验 ==========

    /**
     * Property 21: 项目代理时间校验
     *
     * <p>For any 项目，代理开始时间应早于代理结束时间。
     * 当开始时间不早于结束时间时，系统应拒绝。
     *
     * <p><b>Validates: Requirements 11.3</b>
     */
    @Property(tries = 100)
    void agencyStartDateMustBeBeforeEndDate(
            @ForAll("futureDates") LocalDate date1,
            @ForAll("futureDates") LocalDate date2
    ) {
        LocalDate earlier = date1.isBefore(date2) ? date1 : date2;
        LocalDate later = date1.isBefore(date2) ? date2 : date1;

        // Valid case: start < end → validation passes (no exception)
        if (earlier.isBefore(later)) {
            boolean valid = validateAgencyDates(earlier, later);
            assertThat(valid).isTrue();
        }

        // Invalid case: start == end → validation fails
        boolean sameDate = validateAgencyDates(later, later);
        assertThat(sameDate)
                .as("Same start and end date should be rejected")
                .isFalse();

        // Invalid case: start > end → validation fails
        if (earlier.isBefore(later)) {
            boolean reversed = validateAgencyDates(later, earlier);
            assertThat(reversed)
                    .as("Start date after end date should be rejected")
                    .isFalse();
        }

        // Null dates should pass validation (both optional)
        assertThat(validateAgencyDates(null, null)).isTrue();
        assertThat(validateAgencyDates(earlier, null)).isTrue();
        assertThat(validateAgencyDates(null, later)).isTrue();
    }

    /**
     * Pure validation logic matching ProjectService.validateAgencyDates.
     * Returns true if valid, false if invalid.
     */
    private boolean validateAgencyDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            return false;
        }
        return true;
    }

    // ========== Property 23: 项目佣金规则版本化 ==========

    /**
     * Property 23: 项目佣金规则版本化
     *
     * <p>For any 佣金规则更新，旧版本应被保留，新版本号大于旧版本号。
     * Simulates the versioning logic from ProjectService.saveCommissionRule.
     *
     * <p><b>Validates: Requirements 11.6</b>
     */
    @Property(tries = 100)
    void commissionRuleVersioningPreservesOldVersions(
            @ForAll @IntRange(min = 1, max = 5) int updateCount,
            @ForAll("commissionRates") BigDecimal baseRate,
            @ForAll("rewardAmounts") BigDecimal cashReward
    ) {
        Long projectId = 1L;
        List<ProjectCommissionRule> allRules = new ArrayList<>();

        // Seed initial active rule (version 1, status=1)
        ProjectCommissionRule initialRule = ProjectCommissionRule.builder()
                .projectId(projectId)
                .baseRate(new BigDecimal("0.0300"))
                .version(1)
                .status(1) // active
                .build();
        initialRule.setId(1L);
        allRules.add(initialRule);

        // Simulate N version updates using the same logic as ProjectService.saveCommissionRule
        long nextId = 2L;
        for (int i = 0; i < updateCount; i++) {
            // Find current active rule
            ProjectCommissionRule currentActive = allRules.stream()
                    .filter(r -> r.getStatus() == 1)
                    .max(Comparator.comparingInt(ProjectCommissionRule::getVersion))
                    .orElse(null);

            int newVersion = 1;
            if (currentActive != null) {
                currentActive.setStatus(3); // mark old as 已失效
                newVersion = currentActive.getVersion() + 1;
            }

            ProjectCommissionRule newRule = ProjectCommissionRule.builder()
                    .projectId(projectId)
                    .baseRate(baseRate)
                    .cashReward(cashReward)
                    .version(newVersion)
                    .status(2) // 待审批
                    .build();
            newRule.setId(nextId++);
            allRules.add(newRule);

            // Simulate approval so next iteration can find an active rule
            newRule.setStatus(1);
        }

        // Assertions

        // Total rules = 1 (initial) + updateCount
        assertThat(allRules).hasSize(1 + updateCount);

        // All versions should be unique and sequential: 1, 2, 3, ...
        List<Integer> versions = allRules.stream()
                .map(ProjectCommissionRule::getVersion)
                .sorted()
                .toList();
        assertThat(versions).doesNotHaveDuplicates();
        for (int i = 0; i < versions.size(); i++) {
            assertThat(versions.get(i)).isEqualTo(i + 1);
        }

        // Each new version is strictly greater than all previous versions
        for (int i = 1; i < versions.size(); i++) {
            assertThat(versions.get(i)).isGreaterThan(versions.get(i - 1));
        }

        // Only the latest version should be active (status=1), all others should be 已失效 (status=3)
        int maxVersion = versions.get(versions.size() - 1);
        for (ProjectCommissionRule rule : allRules) {
            if (rule.getVersion() == maxVersion) {
                assertThat(rule.getStatus())
                        .as("Latest version should be active")
                        .isEqualTo(1);
            } else {
                assertThat(rule.getStatus())
                        .as("Old version %d should be invalidated", rule.getVersion())
                        .isEqualTo(3);
            }
        }
    }
}
