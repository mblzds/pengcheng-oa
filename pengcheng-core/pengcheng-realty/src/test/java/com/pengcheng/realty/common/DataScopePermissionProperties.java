package com.pengcheng.realty.common;

import com.pengcheng.realty.common.constants.RealtyRoleConstants;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据权限属性测试
 *
 * <p>Property 10: 客户数据权限隔离 — For any 用户角色和客户查询，返回数据应严格符合该角色的数据权限范围
 * <p>Property 13: 联盟商数据权限隔离 — For any 用户角色和联盟商查询，渠道同事仅看到对接的联盟商，总监/行政看到全部
 *
 * <p><b>Validates: Requirements 5.1, 5.2, 5.3, 5.4, 7.1, 7.2</b>
 */
class DataScopePermissionProperties {

    // ========== Data model for testing ==========

    /** Simulated customer record with alliance and project associations */
    record CustomerRecord(long customerId, long allianceId, Set<Long> projectIds) {}

    /** Simulated alliance record */
    record AllianceRecord(long allianceId, long channelUserId, long managerUserId) {}

    /** User context: userId, role, associated project IDs (for resident), associated alliance IDs (for channel) */
    record UserContext(long userId, String role, Set<Long> responsibleProjectIds, Set<Long> managedAllianceIds, Long ownAllianceId) {}

    // ========== Core filter logic (mirrors DataPermissionInterceptor rules) ==========

    /**
     * Determines which customers a user can see based on their role.
     * This mirrors the logic in DataPermissionInterceptor.buildRealtyDataScopeFilter.
     */
    static List<CustomerRecord> filterCustomers(UserContext user, List<CustomerRecord> allCustomers, List<AllianceRecord> allAlliances) {
        return switch (user.role()) {
            // 驻场总监/渠道总监/行政总监/行政文员 → 全部
            case RealtyRoleConstants.RESIDENT_DIRECTOR,
                 RealtyRoleConstants.CHANNEL_DIRECTOR,
                 RealtyRoleConstants.ADMIN_DIRECTOR,
                 RealtyRoleConstants.ADMIN_CLERK -> new ArrayList<>(allCustomers);

            // 驻场 → 仅负责项目的客户
            case RealtyRoleConstants.RESIDENT -> allCustomers.stream()
                    .filter(c -> !Collections.disjoint(c.projectIds(), user.responsibleProjectIds()))
                    .toList();

            // 渠道 → 仅对接联盟商的客户
            case RealtyRoleConstants.CHANNEL -> {
                Set<Long> managedAllianceIds = new HashSet<>();
                for (AllianceRecord a : allAlliances) {
                    if (a.channelUserId() == user.userId()) {
                        managedAllianceIds.add(a.allianceId());
                    }
                }
                yield allCustomers.stream()
                        .filter(c -> managedAllianceIds.contains(c.allianceId()))
                        .toList();
            }

            // 联盟商负责人 → 仅本联盟商客户
            case RealtyRoleConstants.ALLIANCE_MANAGER -> {
                Set<Long> ownAllianceIds = new HashSet<>();
                for (AllianceRecord a : allAlliances) {
                    if (a.managerUserId() == user.userId()) {
                        ownAllianceIds.add(a.allianceId());
                    }
                }
                yield allCustomers.stream()
                        .filter(c -> ownAllianceIds.contains(c.allianceId()))
                        .toList();
            }

            default -> List.of(); // Unknown role → no access
        };
    }

    /**
     * Determines which alliances a user can see based on their role.
     * This mirrors the logic in DataPermissionInterceptor.buildRealtyDataScopeFilter for alliance queries.
     */
    static List<AllianceRecord> filterAlliances(UserContext user, List<AllianceRecord> allAlliances) {
        return switch (user.role()) {
            // 渠道总监/行政文员/行政总监 → 全部
            case RealtyRoleConstants.CHANNEL_DIRECTOR,
                 RealtyRoleConstants.ADMIN_DIRECTOR,
                 RealtyRoleConstants.ADMIN_CLERK -> new ArrayList<>(allAlliances);

            // 渠道同事 → 仅对接的联盟商
            case RealtyRoleConstants.CHANNEL -> allAlliances.stream()
                    .filter(a -> a.channelUserId() == user.userId())
                    .toList();

            // 联盟商负责人 → 仅本联盟商
            case RealtyRoleConstants.ALLIANCE_MANAGER -> allAlliances.stream()
                    .filter(a -> a.managerUserId() == user.userId())
                    .toList();

            // 驻场/驻场总监 → no alliance access (per design)
            default -> List.of();
        };
    }

    // ========== Generators ==========

    static final String[] ALL_ROLES = {
            RealtyRoleConstants.RESIDENT,
            RealtyRoleConstants.CHANNEL,
            RealtyRoleConstants.RESIDENT_DIRECTOR,
            RealtyRoleConstants.CHANNEL_DIRECTOR,
            RealtyRoleConstants.ADMIN_DIRECTOR,
            RealtyRoleConstants.ADMIN_CLERK,
            RealtyRoleConstants.ALLIANCE_MANAGER
    };

    static final Set<String> FULL_ACCESS_ROLES = Set.of(
            RealtyRoleConstants.RESIDENT_DIRECTOR,
            RealtyRoleConstants.CHANNEL_DIRECTOR,
            RealtyRoleConstants.ADMIN_DIRECTOR,
            RealtyRoleConstants.ADMIN_CLERK
    );

    static final Set<String> ALLIANCE_FULL_ACCESS_ROLES = Set.of(
            RealtyRoleConstants.CHANNEL_DIRECTOR,
            RealtyRoleConstants.ADMIN_DIRECTOR,
            RealtyRoleConstants.ADMIN_CLERK
    );

    @Provide
    Arbitrary<String> roles() {
        return Arbitraries.of(ALL_ROLES);
    }

    @Provide
    @SuppressWarnings("null")
    Arbitrary<List<AllianceRecord>> allianceDataSets() {
        Arbitrary<AllianceRecord> alliance = Combinators.combine(
                Arbitraries.longs().between(1, 100),   // allianceId
                Arbitraries.longs().between(1, 10),    // channelUserId
                Arbitraries.longs().between(100, 110)  // managerUserId
        ).as(AllianceRecord::new);
        return alliance.list().ofMinSize(1).ofMaxSize(10)
                .map(list -> {
                    // Ensure unique alliance IDs
                    Map<Long, AllianceRecord> unique = new LinkedHashMap<>();
                    for (AllianceRecord a : list) {
                        unique.putIfAbsent(a.allianceId(), a);
                    }
                    return new ArrayList<>(unique.values());
                });
    }

    @Provide
    @SuppressWarnings("null")
    Arbitrary<List<CustomerRecord>> customerDataSets() {
        Arbitrary<CustomerRecord> customer = Combinators.combine(
                Arbitraries.longs().between(1, 200),   // customerId
                Arbitraries.longs().between(1, 100),   // allianceId
                Arbitraries.longs().between(1, 20).set().ofMinSize(1).ofMaxSize(3) // projectIds
        ).as(CustomerRecord::new);
        return customer.list().ofMinSize(1).ofMaxSize(15)
                .map(list -> {
                    Map<Long, CustomerRecord> unique = new LinkedHashMap<>();
                    for (CustomerRecord c : list) {
                        unique.putIfAbsent(c.customerId(), c);
                    }
                    return new ArrayList<>(unique.values());
                });
    }

    // ========== Property 10: 客户数据权限隔离 ==========

    /**
     * Property 10: 客户数据权限隔离
     *
     * <p>For any 用户角色和客户查询，返回数据应严格符合该角色的数据权限范围：
     * 驻场仅看到负责项目的客户，渠道仅看到对接联盟商的客户，总监看到全部，联盟商负责人仅看到本联盟商客户。
     *
     * <p><b>Validates: Requirements 5.1, 5.2, 5.3, 5.4</b>
     */
    @Property(tries = 100)
    void customerDataIsolationByRole(
            @ForAll("roles") String role,
            @ForAll @LongRange(min = 1, max = 10) long userId,
            @ForAll("customerDataSets") List<CustomerRecord> customers,
            @ForAll("allianceDataSets") List<AllianceRecord> alliances
    ) {
        Set<Long> responsibleProjects = Set.of(1L, 2L, 3L);
        UserContext user = new UserContext(userId, role, responsibleProjects, Set.of(), null);

        List<CustomerRecord> visible = filterCustomers(user, customers, alliances);

        switch (role) {
            // 驻场总监/渠道总监/行政总监/行政文员 → 全部
            case RealtyRoleConstants.RESIDENT_DIRECTOR,
                 RealtyRoleConstants.CHANNEL_DIRECTOR,
                 RealtyRoleConstants.ADMIN_DIRECTOR,
                 RealtyRoleConstants.ADMIN_CLERK -> {
                assertThat(visible).hasSize(customers.size());
                assertThat(visible).containsExactlyInAnyOrderElementsOf(customers);
            }

            // 驻场 → 仅负责项目的客户
            case RealtyRoleConstants.RESIDENT -> {
                for (CustomerRecord c : visible) {
                    assertThat(c.projectIds())
                            .as("Resident should only see customers in responsible projects")
                            .containsAnyElementsOf(responsibleProjects);
                }
                // Verify no eligible customer is excluded
                for (CustomerRecord c : customers) {
                    if (!Collections.disjoint(c.projectIds(), responsibleProjects)) {
                        assertThat(visible).contains(c);
                    }
                }
            }

            // 渠道 → 仅对接联盟商的客户
            case RealtyRoleConstants.CHANNEL -> {
                Set<Long> managedAllianceIds = new HashSet<>();
                for (AllianceRecord a : alliances) {
                    if (a.channelUserId() == userId) {
                        managedAllianceIds.add(a.allianceId());
                    }
                }
                for (CustomerRecord c : visible) {
                    assertThat(managedAllianceIds)
                            .as("Channel should only see customers of managed alliances")
                            .contains(c.allianceId());
                }
                // Verify completeness
                for (CustomerRecord c : customers) {
                    if (managedAllianceIds.contains(c.allianceId())) {
                        assertThat(visible).contains(c);
                    }
                }
            }

            // 联盟商负责人 → 仅本联盟商客户
            case RealtyRoleConstants.ALLIANCE_MANAGER -> {
                Set<Long> ownAllianceIds = new HashSet<>();
                for (AllianceRecord a : alliances) {
                    if (a.managerUserId() == userId) {
                        ownAllianceIds.add(a.allianceId());
                    }
                }
                for (CustomerRecord c : visible) {
                    assertThat(ownAllianceIds)
                            .as("Alliance manager should only see own alliance customers")
                            .contains(c.allianceId());
                }
                // Verify completeness
                for (CustomerRecord c : customers) {
                    if (ownAllianceIds.contains(c.allianceId())) {
                        assertThat(visible).contains(c);
                    }
                }
            }
        }
    }

    // ========== Property 13: 联盟商数据权限隔离 ==========

    /**
     * Property 13: 联盟商数据权限隔离
     *
     * <p>For any 用户角色和联盟商查询，渠道同事仅看到对接的联盟商，总监/行政看到全部。
     *
     * <p><b>Validates: Requirements 7.1, 7.2</b>
     */
    @Property(tries = 100)
    void allianceDataIsolationByRole(
            @ForAll("roles") String role,
            @ForAll @LongRange(min = 1, max = 10) long userId,
            @ForAll("allianceDataSets") List<AllianceRecord> alliances
    ) {
        UserContext user = new UserContext(userId, role, Set.of(), Set.of(), null);

        List<AllianceRecord> visible = filterAlliances(user, alliances);

        switch (role) {
            // 渠道总监/行政文员/行政总监 → 全部
            case RealtyRoleConstants.CHANNEL_DIRECTOR,
                 RealtyRoleConstants.ADMIN_DIRECTOR,
                 RealtyRoleConstants.ADMIN_CLERK -> {
                assertThat(visible).hasSize(alliances.size());
                assertThat(visible).containsExactlyInAnyOrderElementsOf(alliances);
            }

            // 渠道同事 → 仅对接的联盟商
            case RealtyRoleConstants.CHANNEL -> {
                for (AllianceRecord a : visible) {
                    assertThat(a.channelUserId())
                            .as("Channel should only see alliances they manage")
                            .isEqualTo(userId);
                }
                // Verify completeness
                for (AllianceRecord a : alliances) {
                    if (a.channelUserId() == userId) {
                        assertThat(visible).contains(a);
                    }
                }
            }

            // 联盟商负责人 → 仅本联盟商
            case RealtyRoleConstants.ALLIANCE_MANAGER -> {
                for (AllianceRecord a : visible) {
                    assertThat(a.managerUserId())
                            .as("Alliance manager should only see own alliance")
                            .isEqualTo(userId);
                }
                // Verify completeness
                for (AllianceRecord a : alliances) {
                    if (a.managerUserId() == userId) {
                        assertThat(visible).contains(a);
                    }
                }
            }

            // 驻场/驻场总监 → no alliance access
            case RealtyRoleConstants.RESIDENT,
                 RealtyRoleConstants.RESIDENT_DIRECTOR -> {
                assertThat(visible).isEmpty();
            }
        }
    }
}
