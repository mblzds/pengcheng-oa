package com.pengcheng.realty.customer;

import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.service.CustomerPoolService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 客户公海/私海池属性测试
 *
 * <p>Property 8: 公海池自动回收 — For any 私海池客户，超过无跟进天数或未到访天数后，回收任务执行后池类型应变为公海
 * <p>Property 9: 公海池领取重置保护期 — For any 公海池客户，被领取后池类型变为私海，保护期重置
 *
 * <p><b>Validates: Requirements 4.2, 4.3, 4.4</b>
 */
class CustomerPoolProperties {

    private static final int POOL_PUBLIC = 1;
    private static final int POOL_PRIVATE = 2;
    private static final int STATUS_REPORTED = 1;
    private static final int STATUS_VISITED = 2;
    private static final int STATUS_DEAL = 3;
    private static final int DEFAULT_PROTECTION_DAYS = 3;

    // ========== Simulated pool store ==========

    static class SimPoolStore {
        private final AtomicLong idSeq = new AtomicLong(1);
        private final Map<Long, Customer> customers = new HashMap<>();

        Customer createPrivateCustomer(int status, LocalDateTime lastFollowTime, LocalDateTime createTime) {
            Customer c = Customer.builder()
                    .customerName("客户")
                    .phone("13800001111")
                    .status(status)
                    .poolType(POOL_PRIVATE)
                    .lastFollowTime(lastFollowTime)
                    .protectionExpireTime(createTime.plusDays(DEFAULT_PROTECTION_DAYS))
                    .build();
            c.setId(idSeq.getAndIncrement());
            c.setCreateTime(createTime);
            customers.put(c.getId(), c);
            return c;
        }

        Customer createPublicCustomer(int status) {
            Customer c = Customer.builder()
                    .customerName("公海客户")
                    .phone("13900002222")
                    .status(status)
                    .poolType(POOL_PUBLIC)
                    .lastFollowTime(LocalDateTime.now().minusDays(30))
                    .build();
            c.setId(idSeq.getAndIncrement());
            c.setCreateTime(LocalDateTime.now().minusDays(60));
            customers.put(c.getId(), c);
            return c;
        }

        /**
         * Simulates recycleToPublicPool logic using CustomerPoolService.shouldRecycle
         */
        int recycleToPublicPool(LocalDateTime now, int noFollowDays, int noVisitDays) {
            int recycled = 0;
            for (Customer customer : customers.values()) {
                if (CustomerPoolService.shouldRecycle(customer, now, noFollowDays, noVisitDays)) {
                    customer.setPoolType(POOL_PUBLIC);
                    recycled++;
                }
            }
            return recycled;
        }

        /**
         * Simulates claimFromPublicPool logic
         */
        void claimFromPublicPool(Long customerId, Long userId, LocalDateTime now) {
            Customer customer = customers.get(customerId);
            if (customer == null) {
                throw new IllegalArgumentException("客户不存在");
            }
            if (customer.getPoolType() != POOL_PUBLIC) {
                throw new IllegalStateException("该客户不在公海池中，无法领取");
            }
            customer.setPoolType(POOL_PRIVATE);
            customer.setProtectionExpireTime(now.plusDays(DEFAULT_PROTECTION_DAYS));
            customer.setLastFollowTime(now);
            customer.setCreatorId(userId);
        }

        Customer getCustomer(Long id) {
            return customers.get(id);
        }
    }

    // ========== Generators ==========

    @Provide
    Arbitrary<Integer> recyclableStatuses() {
        return Arbitraries.of(STATUS_REPORTED, STATUS_VISITED);
    }

    @Provide
    Arbitrary<Integer> noFollowDaysConfig() {
        return Arbitraries.integers().between(1, 30);
    }

    @Provide
    Arbitrary<Integer> noVisitDaysConfig() {
        return Arbitraries.integers().between(10, 90);
    }

    // ========== Property 8: 公海池自动回收 ==========

    /**
     * Property 8a: 超过无跟进天数的私海池客户应被回收至公海池
     *
     * <p><b>Validates: Requirements 4.2</b>
     */
    @Property(tries = 100)
    void noFollowCustomerRecycledToPublicPool(
            @ForAll("recyclableStatuses") int status,
            @ForAll("noFollowDaysConfig") int noFollowDays,
            @ForAll @IntRange(min = 1, max = 100) int extraDays
    ) {
        SimPoolStore store = new SimPoolStore();
        LocalDateTime now = LocalDateTime.of(2026, 2, 13, 10, 0);
        // Last follow time is noFollowDays + extraDays ago (definitely exceeds threshold)
        LocalDateTime lastFollow = now.minusDays(noFollowDays + extraDays);
        LocalDateTime createTime = lastFollow.minusDays(1);

        Customer customer = store.createPrivateCustomer(status, lastFollow, createTime);

        store.recycleToPublicPool(now, noFollowDays, 90);

        assertThat(store.getCustomer(customer.getId()).getPoolType())
                .as("Customer with no follow for %d days (threshold %d) should be in public pool",
                        noFollowDays + extraDays, noFollowDays)
                .isEqualTo(POOL_PUBLIC);
    }

    /**
     * Property 8b: 超过未到访天数的已报备客户应被回收至公海池
     *
     * <p><b>Validates: Requirements 4.3</b>
     */
    @Property(tries = 100)
    void noVisitReportedCustomerRecycledToPublicPool(
            @ForAll("noVisitDaysConfig") int noVisitDays,
            @ForAll @IntRange(min = 1, max = 100) int extraDays
    ) {
        SimPoolStore store = new SimPoolStore();
        LocalDateTime now = LocalDateTime.of(2026, 2, 13, 10, 0);
        // Create time is noVisitDays + extraDays ago
        LocalDateTime createTime = now.minusDays(noVisitDays + extraDays);
        // Last follow is recent (so only the no-visit rule triggers)
        LocalDateTime lastFollow = now.minusDays(1);

        Customer customer = store.createPrivateCustomer(STATUS_REPORTED, lastFollow, createTime);

        store.recycleToPublicPool(now, 999, noVisitDays); // large noFollowDays so only noVisit triggers

        assertThat(store.getCustomer(customer.getId()).getPoolType())
                .as("Reported customer with no visit for %d days (threshold %d) should be in public pool",
                        noVisitDays + extraDays, noVisitDays)
                .isEqualTo(POOL_PUBLIC);
    }

    /**
     * Property 8c: 已成交客户不应被回收
     *
     * <p><b>Validates: Requirements 4.2, 4.3</b>
     */
    @Property(tries = 100)
    void dealCustomerNeverRecycled(
            @ForAll("noFollowDaysConfig") int noFollowDays,
            @ForAll("noVisitDaysConfig") int noVisitDays
    ) {
        SimPoolStore store = new SimPoolStore();
        LocalDateTime now = LocalDateTime.of(2026, 2, 13, 10, 0);
        // Very old follow and create time — would trigger both rules if not for deal status
        LocalDateTime oldTime = now.minusDays(365);

        Customer customer = store.createPrivateCustomer(STATUS_DEAL, oldTime, oldTime);

        store.recycleToPublicPool(now, noFollowDays, noVisitDays);

        assertThat(store.getCustomer(customer.getId()).getPoolType())
                .as("Deal customer should never be recycled")
                .isEqualTo(POOL_PRIVATE);
    }

    /**
     * Property 8d: 最近有跟进且未超过未到访天数的客户不应被回收
     *
     * <p><b>Validates: Requirements 4.2, 4.3</b>
     */
    @Property(tries = 100)
    void recentFollowCustomerNotRecycled(
            @ForAll("recyclableStatuses") int status,
            @ForAll("noFollowDaysConfig") int noFollowDays
    ) {
        SimPoolStore store = new SimPoolStore();
        LocalDateTime now = LocalDateTime.of(2026, 2, 13, 10, 0);
        // Last follow is within threshold
        LocalDateTime lastFollow = now.minusDays(noFollowDays).plusHours(1);
        // Create time is recent too (within noVisitDays)
        LocalDateTime createTime = now.minusDays(5);

        Customer customer = store.createPrivateCustomer(status, lastFollow, createTime);

        store.recycleToPublicPool(now, noFollowDays, 90);

        assertThat(store.getCustomer(customer.getId()).getPoolType())
                .as("Customer with recent follow should stay in private pool")
                .isEqualTo(POOL_PRIVATE);
    }

    // ========== Property 9: 公海池领取重置保护期 ==========

    /**
     * Property 9: 公海池领取重置保护期
     *
     * <p>For any 公海池客户，被领取后池类型变为私海，保护期重置。
     *
     * <p><b>Validates: Requirements 4.4</b>
     */
    @Property(tries = 100)
    void claimFromPublicPoolResetsProtection(
            @ForAll("recyclableStatuses") int status,
            @ForAll @LongRange(min = 1, max = 1000) long userId
    ) {
        SimPoolStore store = new SimPoolStore();
        LocalDateTime now = LocalDateTime.of(2026, 2, 13, 10, 0);

        Customer customer = store.createPublicCustomer(status);

        store.claimFromPublicPool(customer.getId(), userId, now);

        Customer claimed = store.getCustomer(customer.getId());

        assertThat(claimed.getPoolType())
                .as("Claimed customer should be in private pool")
                .isEqualTo(POOL_PRIVATE);

        assertThat(claimed.getProtectionExpireTime())
                .as("Protection expire time should be reset to now + 3 days")
                .isEqualTo(now.plusDays(DEFAULT_PROTECTION_DAYS));

        assertThat(claimed.getLastFollowTime())
                .as("Last follow time should be reset to claim time")
                .isEqualTo(now);

        assertThat(claimed.getCreatorId())
                .as("Creator should be set to the claiming user")
                .isEqualTo(userId);
    }

    /**
     * Property 9b: 领取非公海池客户应被拒绝
     *
     * <p><b>Validates: Requirements 4.4</b>
     */
    @Property(tries = 100)
    void claimPrivateCustomerShouldBeRejected(
            @ForAll("recyclableStatuses") int status,
            @ForAll @LongRange(min = 1, max = 1000) long userId
    ) {
        SimPoolStore store = new SimPoolStore();
        LocalDateTime now = LocalDateTime.of(2026, 2, 13, 10, 0);
        LocalDateTime createTime = now.minusDays(1);

        Customer customer = store.createPrivateCustomer(status, now, createTime);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> store.claimFromPublicPool(customer.getId(), userId, now)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不在公海池中");
    }
}
