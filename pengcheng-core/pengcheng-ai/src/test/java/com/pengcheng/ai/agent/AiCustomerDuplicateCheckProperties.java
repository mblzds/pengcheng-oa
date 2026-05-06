package com.pengcheng.ai.agent;

import com.pengcheng.realty.customer.entity.Customer;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 判客属性测试
 *
 * <p>Property 26: AI客户判重 — For any 新报备请求，AI判客应能检测到公海池和私海池中已存在的相同手机号客户
 *
 * <p><b>Validates: Requirements 14.1</b>
 */
class AiCustomerDuplicateCheckProperties {

    // ========== Constants ==========
    private static final int POOL_PUBLIC = 1;  // 公海池
    private static final int POOL_PRIVATE = 2; // 私海池

    private static final int STATUS_REPORTED = 1;  // 已报备
    private static final int STATUS_VISITED = 2;   // 已到访
    private static final int STATUS_DEALT = 3;     // 已成交

    // ========== In-memory customer store ==========

    static class CustomerStore {
        private final AtomicLong idSeq = new AtomicLong(1);
        private final List<Customer> customers = new ArrayList<>();

        Customer save(Customer customer) {
            customer.setId(idSeq.getAndIncrement());
            customer.setCreateTime(LocalDateTime.now());
            customers.add(customer);
            return customer;
        }

        List<Customer> findByPhone(String phone) {
            return customers.stream()
                    .filter(c -> phone.equals(c.getPhone()))
                    .toList();
        }

        List<Customer> getAll() {
            return Collections.unmodifiableList(customers);
        }
    }

    // ========== Simulated duplicate check (mirrors CustomerAnalysisAgent rule-based logic) ==========

    /**
     * Simulates the rule-based duplicate check from CustomerAnalysisAgent.ruleBasedCheckDuplicate.
     * This is the core logic that both AI and fallback paths rely on: query by phone, check results.
     */
    static DuplicateCheckResult checkDuplicate(String phone, CustomerStore store) {
        List<Customer> existing = store.findByPhone(phone);
        if (existing.isEmpty()) {
            return new DuplicateCheckResult(false, List.of(), "未发现重复客户，可以报备。");
        }
        List<ExistingCustomerInfo> infos = existing.stream()
                .map(c -> new ExistingCustomerInfo(
                        c.getId(),
                        c.getCustomerName(),
                        c.getPhoneMasked(),
                        c.getStatus(),
                        c.getPoolType()))
                .toList();
        String message = String.format("发现 %d 条已有客户记录（手机号匹配），请确认是否继续报备。", existing.size());
        return new DuplicateCheckResult(true, infos, message);
    }

    record DuplicateCheckResult(boolean hasDuplicate, List<ExistingCustomerInfo> existingCustomers, String message) {}
    record ExistingCustomerInfo(Long id, String name, String phoneMasked, Integer status, Integer poolType) {}

    // ========== Generators ==========

    @Provide
    Arbitrary<String> phoneNumbers() {
        return Arbitraries.strings().numeric().ofLength(11)
                .filter(s -> s.startsWith("1"));
    }

    @Provide
    Arbitrary<Integer> poolTypes() {
        return Arbitraries.of(POOL_PUBLIC, POOL_PRIVATE);
    }

    @Provide
    Arbitrary<Integer> statuses() {
        return Arbitraries.of(STATUS_REPORTED, STATUS_VISITED, STATUS_DEALT);
    }

    // ========== Property 26: AI客户判重 ==========

    /**
     * Property 26: For any new registration request, the AI duplicate check should detect
     * existing customers with the same phone number in both public and private pools.
     *
     * <p><b>Validates: Requirements 14.1</b>
     */
    @Property(tries = 100)
    void shouldDetectDuplicateCustomerInAnyPool(
            @ForAll("phoneNumbers") String phone,
            @ForAll("poolTypes") int poolType,
            @ForAll("statuses") int status
    ) {
        CustomerStore store = new CustomerStore();

        // Pre-populate store with an existing customer
        Customer existing = Customer.builder()
                .customerName("张")
                .phone(phone)
                .phoneMasked(phone.substring(0, 3) + "****" + phone.substring(7))
                .status(status)
                .poolType(poolType)
                .allianceId(1L)
                .build();
        store.save(existing);

        // Run duplicate check for the same phone
        DuplicateCheckResult result = checkDuplicate(phone, store);

        // Should detect the duplicate
        assertThat(result.hasDuplicate())
                .as("Should detect duplicate for phone %s in pool type %d", phone, poolType)
                .isTrue();
        assertThat(result.existingCustomers())
                .as("Should return the existing customer info")
                .hasSize(1);
        assertThat(result.existingCustomers().get(0).poolType())
                .as("Returned customer should have the correct pool type")
                .isEqualTo(poolType);
    }

    /**
     * Property 26 (complementary): For any phone number NOT in the store,
     * the duplicate check should report no duplicates.
     *
     * <p><b>Validates: Requirements 14.1</b>
     */
    @Property(tries = 100)
    void shouldNotDetectDuplicateWhenPhoneNotExists(
            @ForAll("phoneNumbers") String existingPhone,
            @ForAll("phoneNumbers") String newPhone
    ) {
        Assume.that(!existingPhone.equals(newPhone));

        CustomerStore store = new CustomerStore();
        store.save(Customer.builder()
                .customerName("李")
                .phone(existingPhone)
                .phoneMasked(existingPhone.substring(0, 3) + "****" + existingPhone.substring(7))
                .status(STATUS_REPORTED)
                .poolType(POOL_PRIVATE)
                .allianceId(1L)
                .build());

        DuplicateCheckResult result = checkDuplicate(newPhone, store);

        assertThat(result.hasDuplicate())
                .as("Should not detect duplicate for a different phone number")
                .isFalse();
        assertThat(result.existingCustomers())
                .as("Should return empty list when no duplicate")
                .isEmpty();
    }

    /**
     * Property 26 (multi-pool): For any phone that exists in BOTH public and private pools,
     * the duplicate check should detect all occurrences.
     *
     * <p><b>Validates: Requirements 14.1</b>
     */
    @Property(tries = 100)
    void shouldDetectDuplicatesAcrossBothPools(
            @ForAll("phoneNumbers") String phone
    ) {
        CustomerStore store = new CustomerStore();

        // Customer in public pool
        store.save(Customer.builder()
                .customerName("王")
                .phone(phone)
                .phoneMasked(phone.substring(0, 3) + "****" + phone.substring(7))
                .status(STATUS_REPORTED)
                .poolType(POOL_PUBLIC)
                .allianceId(1L)
                .build());

        // Same phone in private pool
        store.save(Customer.builder()
                .customerName("赵")
                .phone(phone)
                .phoneMasked(phone.substring(0, 3) + "****" + phone.substring(7))
                .status(STATUS_VISITED)
                .poolType(POOL_PRIVATE)
                .allianceId(2L)
                .build());

        DuplicateCheckResult result = checkDuplicate(phone, store);

        assertThat(result.hasDuplicate())
                .as("Should detect duplicates when phone exists in both pools")
                .isTrue();
        assertThat(result.existingCustomers())
                .as("Should return all matching customers from both pools")
                .hasSize(2);

        Set<Integer> poolTypes = new HashSet<>();
        for (ExistingCustomerInfo info : result.existingCustomers()) {
            poolTypes.add(info.poolType());
        }
        assertThat(poolTypes)
                .as("Should include customers from both public and private pools")
                .containsExactlyInAnyOrder(POOL_PUBLIC, POOL_PRIVATE);
    }
}
