package com.pengcheng.realty.customer;

import com.pengcheng.realty.customer.dto.CustomerDealDTO;
import com.pengcheng.realty.customer.dto.CustomerVisitDTO;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 客户状态流转与到访历史属性测试
 *
 * <p>Property 6: 客户状态机流转 — For any 客户，状态流转严格遵循已报备→已到访→已成交，违反前置状态的操作应被拒绝
 * <p>Property 7: 到访历史完整性 — For any 客户的多次到访，每次生成独立记录，总数等于操作次数，历史完整保留
 *
 * <p><b>Validates: Requirements 2.2, 2.3, 2.4, 3.2, 3.5</b>
 */
class CustomerStateTransitionProperties {

    // ========== Constants ==========
    private static final int STATUS_REPORTED = 1;
    private static final int STATUS_VISITED = 2;
    private static final int STATUS_DEAL = 3;

    // ========== Simulated data store ==========

    static class SimStore {
        private final AtomicLong idSeq = new AtomicLong(1);
        private final Map<Long, Customer> customers = new HashMap<>();
        private final List<CustomerVisit> visits = new ArrayList<>();
        private final List<CustomerDeal> deals = new ArrayList<>();

        Customer createReportedCustomer(String name, String phone, Long allianceId) {
            Customer c = Customer.builder()
                    .customerName(name)
                    .phone(phone)
                    .allianceId(allianceId)
                    .status(STATUS_REPORTED)
                    .poolType(2)
                    .protectionExpireTime(LocalDateTime.now().plusDays(3))
                    .lastFollowTime(LocalDateTime.now())
                    .build();
            c.setId(idSeq.getAndIncrement());
            c.setCreateTime(LocalDateTime.now());
            customers.put(c.getId(), c);
            return c;
        }

        Customer getCustomer(Long id) {
            return customers.get(id);
        }

        /**
         * Simulates CustomerVisitService.createVisit logic
         */
        Long createVisit(CustomerVisitDTO dto) {
            if (dto.getCustomerId() == null) {
                throw new IllegalArgumentException("客户ID不能为空");
            }
            if (dto.getActualVisitTime() == null) {
                throw new IllegalArgumentException("实际到访时间不能为空");
            }
            if (dto.getActualVisitCount() == null) {
                throw new IllegalArgumentException("实际到访人数不能为空");
            }
            if (dto.getReceptionist() == null || dto.getReceptionist().isBlank()) {
                throw new IllegalArgumentException("接待人员不能为空");
            }

            Customer customer = customers.get(dto.getCustomerId());
            if (customer == null) {
                throw new IllegalArgumentException("客户不存在");
            }

            if (customer.getStatus() != STATUS_REPORTED && customer.getStatus() != STATUS_VISITED) {
                throw new IllegalStateException("客户当前状态不允许录入到访数据，需要先完成客户报备");
            }

            CustomerVisit visit = CustomerVisit.builder()
                    .customerId(dto.getCustomerId())
                    .actualVisitTime(dto.getActualVisitTime())
                    .actualVisitCount(dto.getActualVisitCount())
                    .receptionist(dto.getReceptionist())
                    .remark(dto.getRemark())
                    .build();
            visit.setId(idSeq.getAndIncrement());
            visits.add(visit);

            if (customer.getStatus() == STATUS_REPORTED) {
                customer.setStatus(STATUS_VISITED);
            }

            return visit.getId();
        }

        /**
         * Simulates CustomerDealService.createDeal logic
         */
        Long createDeal(CustomerDealDTO dto) {
            if (dto.getCustomerId() == null) {
                throw new IllegalArgumentException("客户ID不能为空");
            }
            if (dto.getRoomNo() == null || dto.getRoomNo().isBlank()) {
                throw new IllegalArgumentException("成交房号不能为空");
            }
            if (dto.getDealAmount() == null) {
                throw new IllegalArgumentException("成交金额不能为空");
            }
            if (dto.getDealTime() == null) {
                throw new IllegalArgumentException("成交时间不能为空");
            }
            if (dto.getSubscribeType() == null) {
                throw new IllegalArgumentException("认购类型不能为空");
            }

            Customer customer = customers.get(dto.getCustomerId());
            if (customer == null) {
                throw new IllegalArgumentException("客户不存在");
            }

            if (customer.getStatus() != STATUS_VISITED) {
                throw new IllegalStateException("客户当前状态不允许录入成交数据，需要先录入到访数据");
            }

            CustomerDeal deal = CustomerDeal.builder()
                    .customerId(dto.getCustomerId())
                    .roomNo(dto.getRoomNo())
                    .dealAmount(dto.getDealAmount())
                    .dealTime(dto.getDealTime())
                    .signStatus(dto.getSignStatus())
                    .subscribeType(dto.getSubscribeType())
                    .onlineSignStatus(0)
                    .filingStatus(0)
                    .loanStatus(0)
                    .paymentStatus(0)
                    .build();
            deal.setId(idSeq.getAndIncrement());
            deals.add(deal);

            customer.setStatus(STATUS_DEAL);
            return deal.getId();
        }

        List<CustomerVisit> getVisitsByCustomerId(Long customerId) {
            return visits.stream()
                    .filter(v -> v.getCustomerId().equals(customerId))
                    .toList();
        }
    }

    // ========== Generators ==========

    @Provide
    Arbitrary<CustomerVisitDTO> validVisitDTOs() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 20),
                Arbitraries.of("接待员A", "接待员B", "接待员C", "接待员D")
        ).as((count, receptionist) ->
                CustomerVisitDTO.builder()
                        .actualVisitTime(LocalDateTime.now())
                        .actualVisitCount(count)
                        .receptionist(receptionist)
                        .build()
        );
    }

    @Provide
    Arbitrary<CustomerDealDTO> validDealDTOs() {
        return Combinators.combine(
                Arbitraries.of("A101", "B202", "C303", "D404", "E505"),
                Arbitraries.bigDecimals().between(
                        new BigDecimal("100000"),
                        new BigDecimal("10000000")
                ),
                Arbitraries.of(1, 2)
        ).as((roomNo, amount, subscribeType) ->
                CustomerDealDTO.builder()
                        .roomNo(roomNo)
                        .dealAmount(amount)
                        .dealTime(LocalDateTime.now())
                        .signStatus(1)
                        .subscribeType(subscribeType)
                        .build()
        );
    }

    // ========== Property 6: 客户状态机流转 ==========

    /**
     * Property 6a: 已报备客户可以录入到访，到访后状态变为已到访
     *
     * <p><b>Validates: Requirements 2.2</b>
     */
    @Property(tries = 100)
    void reportedCustomerCanVisitAndStatusBecomesVisited(
            @ForAll("validVisitDTOs") CustomerVisitDTO visitDto
    ) {
        SimStore store = new SimStore();
        Customer customer = store.createReportedCustomer("张", "13800001111", 1L);
        visitDto.setCustomerId(customer.getId());

        store.createVisit(visitDto);

        assertThat(store.getCustomer(customer.getId()).getStatus())
                .as("Customer status should be 已到访 (2) after visit")
                .isEqualTo(STATUS_VISITED);
    }

    /**
     * Property 6b: 已到访客户可以录入成交，成交后状态变为已成交
     *
     * <p><b>Validates: Requirements 3.2</b>
     */
    @Property(tries = 100)
    void visitedCustomerCanDealAndStatusBecomesDeal(
            @ForAll("validVisitDTOs") CustomerVisitDTO visitDto,
            @ForAll("validDealDTOs") CustomerDealDTO dealDto
    ) {
        SimStore store = new SimStore();
        Customer customer = store.createReportedCustomer("李", "13800002222", 1L);

        // First visit
        visitDto.setCustomerId(customer.getId());
        store.createVisit(visitDto);

        // Then deal
        dealDto.setCustomerId(customer.getId());
        store.createDeal(dealDto);

        assertThat(store.getCustomer(customer.getId()).getStatus())
                .as("Customer status should be 已成交 (3) after deal")
                .isEqualTo(STATUS_DEAL);
    }

    /**
     * Property 6c: 未报备客户（不存在）的到访录入应被拒绝
     *
     * <p><b>Validates: Requirements 2.4</b>
     */
    @Property(tries = 100)
    void visitForNonExistentCustomerShouldBeRejected(
            @ForAll("validVisitDTOs") CustomerVisitDTO visitDto,
            @ForAll @LongRange(min = 9000, max = 9999) long fakeCustomerId
    ) {
        SimStore store = new SimStore();
        visitDto.setCustomerId(fakeCustomerId);

        assertThatThrownBy(() -> store.createVisit(visitDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("客户不存在");
    }

    /**
     * Property 6d: 已成交客户不能再录入到访
     *
     * <p><b>Validates: Requirements 2.2</b>
     */
    @Property(tries = 100)
    void dealCustomerCannotVisitAgain(
            @ForAll("validVisitDTOs") CustomerVisitDTO visitDto,
            @ForAll("validDealDTOs") CustomerDealDTO dealDto
    ) {
        SimStore store = new SimStore();
        Customer customer = store.createReportedCustomer("王", "13800003333", 1L);

        // Visit then deal
        visitDto.setCustomerId(customer.getId());
        store.createVisit(visitDto);
        dealDto.setCustomerId(customer.getId());
        store.createDeal(dealDto);

        // Try to visit again after deal — should be rejected
        CustomerVisitDTO secondVisit = CustomerVisitDTO.builder()
                .customerId(customer.getId())
                .actualVisitTime(LocalDateTime.now())
                .actualVisitCount(1)
                .receptionist("接待员X")
                .build();

        assertThatThrownBy(() -> store.createVisit(secondVisit))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("客户当前状态不允许录入到访数据");
    }

    /**
     * Property 6e: 已报备客户不能直接录入成交（跳过到访）
     *
     * <p><b>Validates: Requirements 3.5</b>
     */
    @Property(tries = 100)
    void reportedCustomerCannotDealDirectly(
            @ForAll("validDealDTOs") CustomerDealDTO dealDto
    ) {
        SimStore store = new SimStore();
        Customer customer = store.createReportedCustomer("赵", "13800004444", 1L);
        dealDto.setCustomerId(customer.getId());

        assertThatThrownBy(() -> store.createDeal(dealDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("需要先录入到访数据");
    }

    /**
     * Property 6f: 未到访客户（不存在）的成交录入应被拒绝
     *
     * <p><b>Validates: Requirements 3.5</b>
     */
    @Property(tries = 100)
    void dealForNonExistentCustomerShouldBeRejected(
            @ForAll("validDealDTOs") CustomerDealDTO dealDto,
            @ForAll @LongRange(min = 9000, max = 9999) long fakeCustomerId
    ) {
        SimStore store = new SimStore();
        dealDto.setCustomerId(fakeCustomerId);

        assertThatThrownBy(() -> store.createDeal(dealDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("客户不存在");
    }

    // ========== Property 7: 到访历史完整性 ==========

    /**
     * Property 7: 到访历史完整性
     *
     * <p>For any 客户的多次到访，每次生成独立记录，总数等于操作次数，历史完整保留。
     *
     * <p><b>Validates: Requirements 2.3</b>
     */
    @Property(tries = 100)
    void multipleVisitsPreserveCompleteHistory(
            @ForAll @IntRange(min = 1, max = 10) int visitCount
    ) {
        SimStore store = new SimStore();
        Customer customer = store.createReportedCustomer("钱", "13800005555", 1L);

        Set<Long> visitIds = new HashSet<>();
        for (int i = 0; i < visitCount; i++) {
            CustomerVisitDTO dto = CustomerVisitDTO.builder()
                    .customerId(customer.getId())
                    .actualVisitTime(LocalDateTime.now().plusHours(i))
                    .actualVisitCount(i + 1)
                    .receptionist("接待员" + i)
                    .remark("第" + (i + 1) + "次到访")
                    .build();
            Long visitId = store.createVisit(dto);
            visitIds.add(visitId);
        }

        List<CustomerVisit> history = store.getVisitsByCustomerId(customer.getId());

        // Total visit records should equal the number of visit operations
        assertThat(history)
                .as("Visit record count should equal visit operation count")
                .hasSize(visitCount);

        // All visit IDs should be unique
        assertThat(visitIds)
                .as("All visit IDs should be unique")
                .hasSize(visitCount);

        // All records should belong to this customer
        assertThat(history)
                .as("All visit records should belong to the customer")
                .allMatch(v -> v.getCustomerId().equals(customer.getId()));

        // Customer status should be 已到访 after first visit
        assertThat(store.getCustomer(customer.getId()).getStatus())
                .as("Customer status should be 已到访 (2)")
                .isEqualTo(STATUS_VISITED);
    }
}
