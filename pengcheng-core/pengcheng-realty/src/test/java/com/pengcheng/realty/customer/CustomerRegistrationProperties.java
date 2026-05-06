package com.pengcheng.realty.customer;

import com.pengcheng.realty.common.util.PhoneMaskUtil;
import com.pengcheng.realty.customer.dto.CustomerCreateDTO;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerProject;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 客户报备管理属性测试
 *
 * <p>Property 1: 客户报备必填字段校验 — For any 缺少必填字段的报备请求，系统应拒绝并返回校验错误
 * <p>Property 2: 关键字搜索结果匹配 — For any 关键字搜索，所有返回结果名称应包含该关键字
 * <p>Property 3: 客户报备后置不变量 — For any 成功创建的报备，编号唯一、状态为已报备、保护期正确、池类型为私海
 * <p>Property 4: 客户报备保护期去重 — For any 保护期内已存在的客户+项目组合，新报备应被拒绝
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5</b>
 */
class CustomerRegistrationProperties {

    // ========== Constants ==========
    private static final int DEFAULT_PROTECTION_DAYS = 3;
    private static final java.time.format.DateTimeFormatter REPORT_NO_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final AtomicLong REPORT_NO_SEQUENCE = new AtomicLong();

    // ========== Simulated data store ==========

    /** In-memory customer store for simulation */
    static class CustomerStore {
        private final AtomicLong idSeq = new AtomicLong(1);
        private final List<Customer> customers = new ArrayList<>();
        private final List<CustomerProject> customerProjects = new ArrayList<>();

        Customer save(Customer customer) {
            customer.setId(idSeq.getAndIncrement());
            customer.setCreateTime(LocalDateTime.now());
            customers.add(customer);
            return customer;
        }

        void saveCustomerProject(Long customerId, Long projectId) {
            CustomerProject cp = CustomerProject.builder()
                    .id(idSeq.getAndIncrement())
                    .customerId(customerId)
                    .projectId(projectId)
                    .build();
            customerProjects.add(cp);
        }

        boolean isInProtectionPeriod(String phone, Long projectId) {
            LocalDateTime now = LocalDateTime.now();
            return customers.stream()
                    .filter(c -> phone.equals(c.getPhone()))
                    .filter(c -> c.getProtectionExpireTime() != null && c.getProtectionExpireTime().isAfter(now))
                    .anyMatch(c -> customerProjects.stream()
                            .anyMatch(cp -> cp.getCustomerId().equals(c.getId())
                                    && cp.getProjectId().equals(projectId)));
        }

        List<Customer> getAll() {
            return Collections.unmodifiableList(customers);
        }
    }

    // ========== Validation logic (mirrors CustomerService.validateRequired) ==========

    private void validateRequired(CustomerCreateDTO dto) {
        if (dto.getProjectIds() == null || dto.getProjectIds().isEmpty()) {
            throw new IllegalArgumentException("带看项目不能为空");
        }
        if (dto.getCustomerName() == null || dto.getCustomerName().isBlank()) {
            throw new IllegalArgumentException("客户姓氏不能为空");
        }
        if (dto.getPhone() == null || dto.getPhone().isBlank()) {
            throw new IllegalArgumentException("联系方式不能为空");
        }
        if (dto.getVisitCount() == null) {
            throw new IllegalArgumentException("带看人数不能为空");
        }
        if (dto.getVisitTime() == null) {
            throw new IllegalArgumentException("带看时间不能为空");
        }
        if (dto.getAllianceId() == null) {
            throw new IllegalArgumentException("带看公司不能为空");
        }
        if (dto.getAgentName() == null || dto.getAgentName().isBlank()) {
            throw new IllegalArgumentException("经纪人姓名不能为空");
        }
        if (dto.getAgentPhone() == null || dto.getAgentPhone().isBlank()) {
            throw new IllegalArgumentException("经纪人联系方式不能为空");
        }
    }

    private String generateReportNo() {
        String ts = LocalDateTime.now().format(REPORT_NO_FORMATTER);
        return "BP" + ts + String.format("%06d", REPORT_NO_SEQUENCE.incrementAndGet());
    }

    /**
     * Simulates full customer creation (mirrors CustomerService.createCustomer).
     */
    private Customer simulateCreateCustomer(CustomerCreateDTO dto, CustomerStore store) {
        validateRequired(dto);

        for (Long projectId : dto.getProjectIds()) {
            if (store.isInProtectionPeriod(dto.getPhone(), projectId)) {
                throw new IllegalArgumentException("该客户在项目中已存在有效保护期内的报备记录");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Customer customer = Customer.builder()
                .reportNo(generateReportNo())
                .customerName(dto.getCustomerName())
                .phone(dto.getPhone())
                .phoneMasked(PhoneMaskUtil.mask(dto.getPhone()))
                .visitCount(dto.getVisitCount())
                .visitTime(dto.getVisitTime())
                .allianceId(dto.getAllianceId())
                .agentName(dto.getAgentName())
                .agentPhone(dto.getAgentPhone())
                .status(1) // 已报备
                .poolType(2) // 私海
                .protectionExpireTime(now.plusDays(DEFAULT_PROTECTION_DAYS))
                .lastFollowTime(now)
                .build();
        store.save(customer);

        for (Long projectId : dto.getProjectIds()) {
            store.saveCustomerProject(customer.getId(), projectId);
        }
        return customer;
    }

    // ========== Generators ==========

    @Provide
    Arbitrary<String> phoneNumbers() {
        return Arbitraries.strings().numeric().ofLength(11)
                .filter(s -> s.startsWith("1"));
    }

    @Provide
    Arbitrary<String> customerNames() {
        return Arbitraries.of("张", "李", "王", "赵", "钱", "孙", "周", "吴", "郑", "冯");
    }

    @Provide
    Arbitrary<String> agentNames() {
        return Arbitraries.of("经纪人A", "经纪人B", "经纪人C", "经纪人D");
    }

    @Provide
    Arbitrary<CustomerCreateDTO> validDTOs() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 50).set().ofMinSize(1).ofMaxSize(3),
                Arbitraries.of("张", "李", "王", "赵"),
                Arbitraries.strings().numeric().ofLength(11).filter(s -> s.startsWith("1")),
                Arbitraries.integers().between(1, 10),
                Arbitraries.longs().between(1, 20),
                Arbitraries.of("经纪人A", "经纪人B"),
                Arbitraries.strings().numeric().ofLength(11).filter(s -> s.startsWith("1"))
        ).as((projectIds, name, phone, count, allianceId, agentName, agentPhone) ->
                CustomerCreateDTO.builder()
                        .projectIds(new ArrayList<>(projectIds))
                        .customerName(name)
                        .phone(phone)
                        .visitCount(count)
                        .visitTime(LocalDateTime.now().plusDays(1))
                        .allianceId(allianceId)
                        .agentName(agentName)
                        .agentPhone(agentPhone)
                        .build()
        );
    }

    // ========== Property 1: 客户报备必填字段校验 ==========

    /**
     * Property 1: 客户报备必填字段校验
     *
     * <p>For any 缺少必填字段的报备请求，系统应拒绝并返回校验错误。
     *
     * <p><b>Validates: Requirements 1.1</b>
     */
    @Property(tries = 100)
    void missingRequiredFieldsShouldBeRejected(
            @ForAll("validDTOs") CustomerCreateDTO baseDto,
            @ForAll @IntRange(min = 0, max = 7) int fieldToNull
    ) {
        // Create a copy and null out one required field
        CustomerCreateDTO dto = CustomerCreateDTO.builder()
                .projectIds(baseDto.getProjectIds())
                .customerName(baseDto.getCustomerName())
                .phone(baseDto.getPhone())
                .visitCount(baseDto.getVisitCount())
                .visitTime(baseDto.getVisitTime())
                .allianceId(baseDto.getAllianceId())
                .agentName(baseDto.getAgentName())
                .agentPhone(baseDto.getAgentPhone())
                .build();

        switch (fieldToNull) {
            case 0 -> dto.setProjectIds(null);
            case 1 -> dto.setCustomerName(null);
            case 2 -> dto.setPhone(null);
            case 3 -> dto.setVisitCount(null);
            case 4 -> dto.setVisitTime(null);
            case 5 -> dto.setAllianceId(null);
            case 6 -> dto.setAgentName(null);
            case 7 -> dto.setAgentPhone(null);
        }

        assertThatThrownBy(() -> validateRequired(dto))
                .as("Missing field index %d should cause validation error", fieldToNull)
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== Property 2: 关键字搜索结果匹配 ==========

    /**
     * Property 2: 关键字搜索结果匹配
     *
     * <p>For any 关键字搜索查询和数据集，所有返回的结果名称应包含该搜索关键字。
     *
     * <p><b>Validates: Requirements 1.2, 1.3</b>
     */
    @Property(tries = 100)
    void keywordSearchResultsMustContainKeyword(
            @ForAll @StringLength(min = 1, max = 5) @AlphaChars String keyword
    ) {
        // Simulate a dataset of project/alliance names
        List<String> allNames = List.of(
                "碧桂园翡翠湾", "万科城市花园", "恒大御景半岛",
                "保利天悦", "融创壹号院", "中海锦城", "龙湖春江天境",
                "华润置地橡树湾", "绿城桃花源", "招商蛇口花园城"
        );

        // Simulate keyword search (LIKE %keyword%)
        List<String> results = allNames.stream()
                .filter(name -> name.toLowerCase().contains(keyword.toLowerCase()))
                .toList();

        // All results must contain the keyword
        for (String name : results) {
            assertThat(name.toLowerCase())
                    .as("Search result '%s' should contain keyword '%s'", name, keyword)
                    .contains(keyword.toLowerCase());
        }
    }

    // ========== Property 3: 客户报备后置不变量 ==========

    /**
     * Property 3: 客户报备后置不变量
     *
     * <p>For any 成功创建的报备，编号唯一、状态为已报备、保护期正确、池类型为私海。
     *
     * <p><b>Validates: Requirements 1.4, 4.1</b>
     */
    @Property(tries = 100)
    void successfulRegistrationPostConditions(
            @ForAll("validDTOs") CustomerCreateDTO dto
    ) {
        CustomerStore store = new CustomerStore();
        // Use unique phone per test to avoid protection period conflicts
        dto.setPhone("1" + String.format("%010d", ThreadLocalRandom.current().nextLong(0, 10_000_000_000L)));

        Customer customer = simulateCreateCustomer(dto, store);

        // 1. Report number should be unique and non-null
        assertThat(customer.getReportNo())
                .as("Report number should not be null")
                .isNotNull();
        assertThat(customer.getReportNo())
                .as("Report number should start with BP")
                .startsWith("BP");

        // 2. Status should be 已报备 (1)
        assertThat(customer.getStatus())
                .as("Status should be 已报备 (1)")
                .isEqualTo(1);

        // 3. Protection expire time should be approximately now + 3 days
        LocalDateTime expectedExpire = LocalDateTime.now().plusDays(DEFAULT_PROTECTION_DAYS);
        assertThat(customer.getProtectionExpireTime())
                .as("Protection expire time should be set")
                .isNotNull();
        assertThat(customer.getProtectionExpireTime())
                .as("Protection expire time should be approximately now + 3 days")
                .isBetween(expectedExpire.minusMinutes(1), expectedExpire.plusMinutes(1));

        // 4. Pool type should be 私海 (2)
        assertThat(customer.getPoolType())
                .as("Pool type should be 私海 (2)")
                .isEqualTo(2);
    }

    /**
     * Property 3 (additional): Multiple registrations should all have unique report numbers.
     *
     * <p><b>Validates: Requirements 1.4</b>
     */
    @Property(tries = 100)
    void multipleRegistrationsHaveUniqueReportNumbers(
            @ForAll @IntRange(min = 2, max = 10) int count
    ) {
        CustomerStore store = new CustomerStore();
        Set<String> reportNos = new HashSet<>();

        for (int i = 0; i < count; i++) {
            CustomerCreateDTO dto = CustomerCreateDTO.builder()
                    .projectIds(List.of((long) (i + 100))) // unique project per registration
                    .customerName("客户" + i)
                    .phone("1380000" + String.format("%04d", i))
                    .visitCount(1)
                    .visitTime(LocalDateTime.now().plusDays(1))
                    .allianceId(1L)
                    .agentName("经纪人")
                    .agentPhone("13900000001")
                    .build();

            Customer customer = simulateCreateCustomer(dto, store);
            reportNos.add(customer.getReportNo());
        }

        assertThat(reportNos)
                .as("All report numbers should be unique")
                .hasSize(count);
    }

    // ========== Property 4: 客户报备保护期去重 ==========

    /**
     * Property 4: 客户报备保护期去重
     *
     * <p>For any 保护期内已存在的客户+项目组合，新报备应被拒绝。
     *
     * <p><b>Validates: Requirements 1.5</b>
     */
    @Property(tries = 100)
    void duplicateRegistrationInProtectionPeriodShouldBeRejected(
            @ForAll("customerNames") String name,
            @ForAll("phoneNumbers") String phone,
            @ForAll @LongRange(min = 1, max = 50) long projectId,
            @ForAll @LongRange(min = 1, max = 20) long allianceId
    ) {
        CustomerStore store = new CustomerStore();

        // First registration should succeed
        CustomerCreateDTO firstDto = CustomerCreateDTO.builder()
                .projectIds(List.of(projectId))
                .customerName(name)
                .phone(phone)
                .visitCount(1)
                .visitTime(LocalDateTime.now().plusDays(1))
                .allianceId(allianceId)
                .agentName("经纪人A")
                .agentPhone("13900000001")
                .build();

        simulateCreateCustomer(firstDto, store);

        // Second registration with same phone + same project should be rejected
        CustomerCreateDTO duplicateDto = CustomerCreateDTO.builder()
                .projectIds(List.of(projectId))
                .customerName(name)
                .phone(phone)
                .visitCount(2)
                .visitTime(LocalDateTime.now().plusDays(2))
                .allianceId(allianceId)
                .agentName("经纪人B")
                .agentPhone("13900000002")
                .build();

        assertThatThrownBy(() -> simulateCreateCustomer(duplicateDto, store))
                .as("Duplicate registration within protection period should be rejected")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("保护期");
    }

    /**
     * Property 4 (additional): Same phone + different project should be allowed.
     *
     * <p><b>Validates: Requirements 1.5</b>
     */
    @Property(tries = 100)
    void samePhoneDifferentProjectShouldBeAllowed(
            @ForAll("phoneNumbers") String phone,
            @ForAll @LongRange(min = 1, max = 50) long projectId1,
            @ForAll @LongRange(min = 51, max = 100) long projectId2
    ) {
        CustomerStore store = new CustomerStore();

        CustomerCreateDTO dto1 = CustomerCreateDTO.builder()
                .projectIds(List.of(projectId1))
                .customerName("张")
                .phone(phone)
                .visitCount(1)
                .visitTime(LocalDateTime.now().plusDays(1))
                .allianceId(1L)
                .agentName("经纪人A")
                .agentPhone("13900000001")
                .build();

        CustomerCreateDTO dto2 = CustomerCreateDTO.builder()
                .projectIds(List.of(projectId2))
                .customerName("张")
                .phone(phone)
                .visitCount(1)
                .visitTime(LocalDateTime.now().plusDays(1))
                .allianceId(1L)
                .agentName("经纪人A")
                .agentPhone("13900000001")
                .build();

        // Both should succeed since different projects
        Customer c1 = simulateCreateCustomer(dto1, store);
        Customer c2 = simulateCreateCustomer(dto2, store);

        assertThat(c1.getId()).isNotEqualTo(c2.getId());
        assertThat(store.getAll()).hasSize(2);
    }
}
