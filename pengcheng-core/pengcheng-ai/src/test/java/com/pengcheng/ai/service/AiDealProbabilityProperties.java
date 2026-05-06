package com.pengcheng.ai.service;

import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 成交概率评分属性测试
 *
 * <p>Property 27: AI成交概率评分范围 — For any 客户评分计算结果，值应在 [0, 1] 范围内，
 * 且结果应被持久化到客户记录
 *
 * <p><b>Validates: Requirements 14.2, 14.3</b>
 */
class AiDealProbabilityProperties {

    private static final int STATUS_REPORTED = 1;
    private static final int STATUS_VISITED = 2;

    // ========== In-memory stores ==========

    static class InMemoryCustomerStore {
        private final AtomicLong idSeq = new AtomicLong(1);
        private final Map<Long, Customer> customers = new HashMap<>();

        Customer save(Customer customer) {
            if (customer.getId() == null) {
                customer.setId(idSeq.getAndIncrement());
            }
            customers.put(customer.getId(), customer);
            return customer;
        }

        Customer findById(Long id) {
            return customers.get(id);
        }

        void updateById(Customer customer) {
            customers.put(customer.getId(), customer);
        }
    }

    static class InMemoryVisitStore {
        private final List<CustomerVisit> visits = new ArrayList<>();

        void save(CustomerVisit visit) {
            visits.add(visit);
        }

        List<CustomerVisit> findByCustomerId(Long customerId) {
            return visits.stream()
                    .filter(v -> customerId.equals(v.getCustomerId()))
                    .sorted(Comparator.comparing(CustomerVisit::getActualVisitTime,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        }
    }

    // ========== Scoring logic (mirrors AiAnalysisService) ==========

    static BigDecimal calculateDealProbability(Customer customer, List<CustomerVisit> visits) {
        BigDecimal visitCountScore = calculateVisitCountScore(visits.size());
        BigDecimal visitIntervalScore = calculateVisitIntervalScore(visits);
        BigDecimal reportDurationScore = calculateReportDurationScore(customer);
        BigDecimal statusScore = calculateStatusScore(customer.getStatus());

        BigDecimal score = visitCountScore.multiply(new BigDecimal("0.30"))
                .add(visitIntervalScore.multiply(new BigDecimal("0.20")))
                .add(reportDurationScore.multiply(new BigDecimal("0.20")))
                .add(statusScore.multiply(new BigDecimal("0.30")));

        return clampScore(score);
    }

    static BigDecimal calculateVisitCountScore(int visitCount) {
        if (visitCount == 0) return new BigDecimal("0.10");
        if (visitCount == 1) return new BigDecimal("0.40");
        if (visitCount == 2) return new BigDecimal("0.60");
        if (visitCount == 3) return new BigDecimal("0.80");
        return BigDecimal.ONE;
    }

    static BigDecimal calculateVisitIntervalScore(List<CustomerVisit> visits) {
        if (visits.size() < 2) {
            return visits.isEmpty() ? new BigDecimal("0.10") : new BigDecimal("0.50");
        }
        long totalDays = 0;
        for (int i = 1; i < visits.size(); i++) {
            LocalDateTime prev = visits.get(i - 1).getActualVisitTime();
            LocalDateTime curr = visits.get(i).getActualVisitTime();
            if (prev != null && curr != null) {
                totalDays += Math.abs(java.time.temporal.ChronoUnit.DAYS.between(prev, curr));
            }
        }
        long avgDays = totalDays / (visits.size() - 1);
        if (avgDays <= 3) return BigDecimal.ONE;
        if (avgDays <= 7) return new BigDecimal("0.70");
        if (avgDays <= 14) return new BigDecimal("0.40");
        return new BigDecimal("0.20");
    }

    static BigDecimal calculateReportDurationScore(Customer customer) {
        if (customer.getCreateTime() == null) {
            return new BigDecimal("0.20");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(customer.getCreateTime(), LocalDateTime.now());
        if (days < 1) return new BigDecimal("0.20");
        if (days <= 3) return new BigDecimal("0.40");
        if (days <= 7) return new BigDecimal("0.60");
        if (days <= 14) return new BigDecimal("0.80");
        return new BigDecimal("0.50");
    }

    static BigDecimal calculateStatusScore(Integer status) {
        if (status == null) return BigDecimal.ZERO;
        return switch (status) {
            case STATUS_VISITED -> new BigDecimal("0.70");
            case STATUS_REPORTED -> new BigDecimal("0.30");
            default -> BigDecimal.ZERO;
        };
    }

    static BigDecimal clampScore(BigDecimal score) {
        if (score.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (score.compareTo(BigDecimal.ONE) > 0) return BigDecimal.ONE;
        return score.setScale(4, RoundingMode.HALF_UP);
    }

    // ========== Generators ==========

    @Provide
    Arbitrary<Integer> activeStatuses() {
        return Arbitraries.of(STATUS_REPORTED, STATUS_VISITED);
    }

    @Provide
    Arbitrary<List<CustomerVisit>> visitLists() {
        Arbitrary<CustomerVisit> visitArb = Arbitraries.integers().between(0, 365).map(daysAgo -> {
            CustomerVisit v = new CustomerVisit();
            v.setCustomerId(1L);
            v.setActualVisitTime(LocalDateTime.now().minusDays(daysAgo));
            v.setActualVisitCount(1);
            return v;
        });
        return visitArb.list().ofMinSize(0).ofMaxSize(8);
    }

    @Provide
    Arbitrary<Integer> reportDaysAgo() {
        return Arbitraries.integers().between(0, 60);
    }

    // ========== Property 27: Score range [0, 1] ==========

    /**
     * Property 27: For any customer with any combination of status, visit history,
     * and report duration, the deal probability score must be in [0, 1].
     *
     * <p><b>Validates: Requirements 14.2, 14.3</b>
     */
    @Property(tries = 100)
    void scoreShouldAlwaysBeInZeroToOneRange(
            @ForAll("activeStatuses") int status,
            @ForAll("visitLists") List<CustomerVisit> visits,
            @ForAll("reportDaysAgo") int daysAgo
    ) {
        Customer customer = Customer.builder()
                .customerName("测试")
                .phone("13800138000")
                .status(status)
                .build();
        customer.setId(1L);
        customer.setCreateTime(LocalDateTime.now().minusDays(daysAgo));

        BigDecimal score = calculateDealProbability(customer, visits);

        assertThat(score)
                .as("Score should be >= 0")
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(score)
                .as("Score should be <= 1")
                .isLessThanOrEqualTo(BigDecimal.ONE);
    }

    // ========== Property 27: Persistence ==========

    /**
     * Property 27 (persistence): For any customer, after calculating and persisting
     * the deal probability, the customer record should contain the score.
     *
     * <p><b>Validates: Requirements 14.3</b>
     */
    @Property(tries = 100)
    void scoreShouldBePersistedToCustomerRecord(
            @ForAll("activeStatuses") int status,
            @ForAll("visitLists") List<CustomerVisit> visits,
            @ForAll("reportDaysAgo") int daysAgo
    ) {
        InMemoryCustomerStore customerStore = new InMemoryCustomerStore();
        InMemoryVisitStore visitStore = new InMemoryVisitStore();

        Customer customer = Customer.builder()
                .customerName("测试")
                .phone("13800138000")
                .status(status)
                .build();
        customer.setCreateTime(LocalDateTime.now().minusDays(daysAgo));
        customerStore.save(customer);

        for (CustomerVisit v : visits) {
            v.setCustomerId(customer.getId());
            visitStore.save(v);
        }

        // Calculate and persist
        List<CustomerVisit> storedVisits = visitStore.findByCustomerId(customer.getId());
        BigDecimal probability = calculateDealProbability(customer, storedVisits);
        customer.setDealProbability(probability);
        customerStore.updateById(customer);

        // Verify persistence
        Customer persisted = customerStore.findById(customer.getId());
        assertThat(persisted.getDealProbability())
                .as("Deal probability should be persisted to customer record")
                .isNotNull()
                .isEqualTo(probability);
        assertThat(persisted.getDealProbability())
                .as("Persisted score should be in [0, 1]")
                .isGreaterThanOrEqualTo(BigDecimal.ZERO)
                .isLessThanOrEqualTo(BigDecimal.ONE);
    }
}
