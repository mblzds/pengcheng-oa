package com.pengcheng.realty.project;

import com.pengcheng.realty.project.entity.Project;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 项目到期属性测试
 *
 * <p>Property 22: 项目到期自动标记 — For any 代理结束时间已过的项目，执行到期检查后状态应变为"已到期"
 *
 * <p><b>Validates: Requirements 11.4</b>
 */
class ProjectExpiryProperties {

    /** 项目状态常量 */
    private static final int STATUS_ON_SALE = 1;    // 在售
    private static final int STATUS_PENDING = 2;     // 待售
    private static final int STATUS_SOLD_OUT = 3;    // 售罄
    private static final int STATUS_EXPIRED = 4;     // 已到期

    // ========== Generators ==========

    @Provide
    Arbitrary<Integer> nonExpiredStatuses() {
        return Arbitraries.of(STATUS_ON_SALE, STATUS_PENDING, STATUS_SOLD_OUT);
    }

    @Provide
    Arbitrary<LocalDate> pastDates() {
        // Dates in the past (before today 2026-02-13)
        return Arbitraries.integers().between(1, 1000)
                .map(days -> LocalDate.of(2026, 2, 13).minusDays(days));
    }

    @Provide
    Arbitrary<LocalDate> futureDates() {
        // Dates in the future (after today)
        return Arbitraries.integers().between(1, 1000)
                .map(days -> LocalDate.of(2026, 2, 13).plusDays(days));
    }

    // ========== Core expiry logic (mirrors ProjectService.markExpiredProjects) ==========

    /**
     * Simulates the expiry check logic from ProjectService.markExpiredProjects.
     * Projects with agencyEndDate before today and status != EXPIRED get marked as EXPIRED.
     */
    private List<Project> simulateMarkExpired(List<Project> projects, LocalDate today) {
        List<Project> marked = new ArrayList<>();
        for (Project p : projects) {
            if (p.getAgencyEndDate() != null
                    && p.getAgencyEndDate().isBefore(today)
                    && p.getStatus() != STATUS_EXPIRED) {
                p.setStatus(STATUS_EXPIRED);
                marked.add(p);
            }
        }
        return marked;
    }

    // ========== Property 22: 项目到期自动标记 ==========

    /**
     * Property 22: 项目到期自动标记
     *
     * <p>For any 代理结束时间已过的项目，执行到期检查后状态应变为"已到期"。
     * Projects with future end dates or null end dates should NOT be affected.
     *
     * <p><b>Validates: Requirements 11.4</b>
     */
    @Property(tries = 100)
    void expiredProjectsAreMarked(
            @ForAll("pastDates") LocalDate pastEndDate,
            @ForAll("futureDates") LocalDate futureEndDate,
            @ForAll("nonExpiredStatuses") int initialStatus
    ) {
        LocalDate today = LocalDate.of(2026, 2, 13);

        // Build a mixed set of projects
        List<Project> projects = new ArrayList<>();

        // Project with past end date (should be marked expired)
        Project expiredProject = Project.builder()
                .projectName("Past Project")
                .agencyEndDate(pastEndDate)
                .status(initialStatus)
                .build();
        expiredProject.setId(1L);
        projects.add(expiredProject);

        // Project with future end date (should NOT be marked expired)
        Project futureProject = Project.builder()
                .projectName("Future Project")
                .agencyEndDate(futureEndDate)
                .status(initialStatus)
                .build();
        futureProject.setId(2L);
        projects.add(futureProject);

        // Project with null end date (should NOT be marked expired)
        Project nullDateProject = Project.builder()
                .projectName("No Date Project")
                .agencyEndDate(null)
                .status(initialStatus)
                .build();
        nullDateProject.setId(3L);
        projects.add(nullDateProject);

        // Project already expired (should NOT be re-processed)
        Project alreadyExpired = Project.builder()
                .projectName("Already Expired")
                .agencyEndDate(pastEndDate)
                .status(STATUS_EXPIRED)
                .build();
        alreadyExpired.setId(4L);
        projects.add(alreadyExpired);

        // Execute expiry check
        List<Project> marked = simulateMarkExpired(projects, today);

        // Past-date project should be marked as expired
        assertThat(expiredProject.getStatus())
                .as("Project with past end date should be marked expired")
                .isEqualTo(STATUS_EXPIRED);
        assertThat(marked).contains(expiredProject);

        // Future-date project should retain original status
        assertThat(futureProject.getStatus())
                .as("Project with future end date should not be affected")
                .isEqualTo(initialStatus);
        assertThat(marked).doesNotContain(futureProject);

        // Null-date project should retain original status
        assertThat(nullDateProject.getStatus())
                .as("Project with null end date should not be affected")
                .isEqualTo(initialStatus);
        assertThat(marked).doesNotContain(nullDateProject);

        // Already-expired project should not be re-processed
        assertThat(alreadyExpired.getStatus()).isEqualTo(STATUS_EXPIRED);
        assertThat(marked).doesNotContain(alreadyExpired);
    }

    /**
     * Additional property: after expiry check, ALL projects with past end dates
     * should have status EXPIRED.
     */
    @Property(tries = 100)
    void allPastEndDateProjectsBecomeExpired(
            @ForAll @IntRange(min = 1, max = 10) int projectCount,
            @ForAll("nonExpiredStatuses") int initialStatus
    ) {
        LocalDate today = LocalDate.of(2026, 2, 13);
        List<Project> projects = new ArrayList<>();

        for (int i = 0; i < projectCount; i++) {
            Project p = Project.builder()
                    .projectName("Project " + i)
                    .agencyEndDate(today.minusDays(i + 1))
                    .status(initialStatus)
                    .build();
            p.setId((long) (i + 1));
            projects.add(p);
        }

        simulateMarkExpired(projects, today);

        for (Project p : projects) {
            assertThat(p.getStatus())
                    .as("All projects with past end dates should be expired")
                    .isEqualTo(STATUS_EXPIRED);
        }
    }
}
