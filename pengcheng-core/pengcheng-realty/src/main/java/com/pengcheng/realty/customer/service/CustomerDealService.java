package com.pengcheng.realty.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.common.exception.InvalidStateTransitionException;
import com.pengcheng.realty.customer.dto.CustomerDealDTO;
import com.pengcheng.realty.customer.dto.CustomerDealUpdateDTO;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.entity.CustomerProject;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.CustomerProjectMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.project.entity.Project;
import com.pengcheng.realty.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 客户成交管理服务
 */
@Service
@RequiredArgsConstructor
public class CustomerDealService {

    private final CustomerDealMapper customerDealMapper;
    private final RealtyCustomerMapper customerMapper;
    private final AllianceMapper allianceMapper;
    private final ProjectMapper projectMapper;
    private final CustomerProjectMapper customerProjectMapper;

    /** 客户状态：已到访 */
    private static final int STATUS_VISITED = 2;
    /** 客户状态：已成交 */
    private static final int STATUS_DEAL = 3;

    /**
     * 录入成交数据
     * <p>
     * 校验客户状态为"已到访"，录入成交信息后更新客户状态为"已成交"。
     */
    @Transactional
    public Long createDeal(CustomerDealDTO dto) {
        validateDealDTO(dto);

        // 查询客户
        Customer customer = customerMapper.selectById(dto.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }

        // 校验客户状态：必须为已到访
        if (customer.getStatus() != STATUS_VISITED) {
            throw new InvalidStateTransitionException("客户当前状态不允许录入成交数据，需要先录入到访数据");
        }

        // 创建成交记录
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
        customerDealMapper.insert(deal);

        // 更新客户状态为"已成交"
        customer.setStatus(STATUS_DEAL);
        customerMapper.updateById(customer);

        return deal.getId();
    }

    /**
     * 更新成交后续手续状态（网签、备案、贷款、回款）
     */
    @Transactional
    public void updateDeal(CustomerDealUpdateDTO dto) {
        if (dto.getDealId() == null) {
            throw new IllegalArgumentException("成交记录ID不能为空");
        }

        CustomerDeal deal = customerDealMapper.selectById(dto.getDealId());
        if (deal == null) {
            throw new IllegalArgumentException("成交记录不存在");
        }

        if (dto.getOnlineSignStatus() != null) {
            deal.setOnlineSignStatus(dto.getOnlineSignStatus());
        }
        if (dto.getFilingStatus() != null) {
            deal.setFilingStatus(dto.getFilingStatus());
        }
        if (dto.getLoanStatus() != null) {
            deal.setLoanStatus(dto.getLoanStatus());
        }
        if (dto.getPaymentStatus() != null) {
            deal.setPaymentStatus(dto.getPaymentStatus());
        }

        customerDealMapper.updateById(deal);
    }

    /**
     * 查询客户的成交记录（按成交时间倒序）。
     */
    public List<CustomerDeal> listDealsByCustomerId(Long customerId) {
        return customerDealMapper.selectList(
                new LambdaQueryWrapper<CustomerDeal>()
                        .eq(CustomerDeal::getCustomerId, customerId)
                        .orderByDesc(CustomerDeal::getDealTime)
        );
    }

    /**
     * 分页查询成交明细，附带项目 / 中介 / 客户信息。
     * <p>
     * 数据权限继承客户列表：先通过 customer 表的 @DataScope 取出可见客户集合，
     * 再按 customer_id IN (...) 过滤 customer_deal，等价于"能看到客户 → 能看到该客户的成交"。
     * 项目按客户在 customer_project 中关联的第一条取（一对多取主项目）。
     *
     * @return IPage 项目结构：records / total / current / size，前端按 IPage 消费
     */
    public IPage<DealListItem> pageDeals(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        // 1. 拉当前用户可见客户（拦截器自动注入数据权限 WHERE）。
        //    全量字段拉一遍，避免下游回填客户名/电话/中介信息时再查一次 customer。
        List<Customer> visibleCustomers = customerMapper.selectListWithScope(new LambdaQueryWrapper<>());
        if (visibleCustomers.isEmpty()) {
            return new Page<>(safePage, safeSize, 0);
        }
        Map<Long, Customer> customerMap = visibleCustomers.stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Customer::getId, Function.identity(), (a, b) -> a));
        Set<Long> visibleIds = customerMap.keySet();

        // 2. 分页查 customer_deal（仅限可见客户）
        IPage<CustomerDeal> dealPage = customerDealMapper.selectPage(
                new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<CustomerDeal>()
                        .in(CustomerDeal::getCustomerId, visibleIds)
                        .orderByDesc(CustomerDeal::getDealTime));

        List<CustomerDeal> records = dealPage.getRecords();
        if (records.isEmpty()) {
            IPage<DealListItem> empty = new Page<>(safePage, safeSize, dealPage.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        // 3. 批量拉关联：alliance 公司名、项目名（一次查询，避免 N+1）
        Set<Long> dealCustomerIds = records.stream()
                .map(CustomerDeal::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> allianceIds = dealCustomerIds.stream()
                .map(id -> customerMap.get(id))
                .filter(Objects::nonNull)
                .map(Customer::getAllianceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> allianceNameMap = allianceIds.isEmpty()
                ? Collections.emptyMap()
                : allianceMapper.selectBatchIds(allianceIds).stream()
                        .filter(a -> a.getId() != null && a.getCompanyName() != null)
                        .collect(Collectors.toMap(Alliance::getId, Alliance::getCompanyName, (a, b) -> a));

        // 客户 → 项目 ID（按 customer_project.id 升序，每个客户取第一条）
        Map<Long, Long> customerToProjectId = new HashMap<>();
        List<CustomerProject> cps = customerProjectMapper.selectList(
                new LambdaQueryWrapper<CustomerProject>()
                        .in(CustomerProject::getCustomerId, dealCustomerIds)
                        .orderByAsc(CustomerProject::getId));
        for (CustomerProject cp : cps) {
            if (cp.getCustomerId() == null) continue;
            customerToProjectId.putIfAbsent(cp.getCustomerId(), cp.getProjectId());
        }
        Set<Long> projectIds = customerToProjectId.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> projectNameMap = projectIds.isEmpty()
                ? Collections.emptyMap()
                : projectMapper.selectBatchIds(projectIds).stream()
                        .filter(p -> p.getId() != null && p.getProjectName() != null)
                        .collect(Collectors.toMap(Project::getId, Project::getProjectName, (a, b) -> a));

        // 4. 组装结果
        IPage<DealListItem> resultPage = new Page<>(safePage, safeSize, dealPage.getTotal());
        List<DealListItem> items = records.stream().map(deal -> {
            Customer c = customerMap.get(deal.getCustomerId());
            Long projectId = customerToProjectId.get(deal.getCustomerId());
            return DealListItem.builder()
                    .dealId(deal.getId())
                    .customerId(deal.getCustomerId())
                    .customerName(c != null ? c.getCustomerName() : null)
                    .phoneMasked(c != null ? c.getPhoneMasked() : null)
                    .projectId(projectId)
                    .projectName(projectId != null ? projectNameMap.get(projectId) : null)
                    .allianceId(c != null ? c.getAllianceId() : null)
                    .allianceName(c != null && c.getAllianceId() != null ? allianceNameMap.get(c.getAllianceId()) : null)
                    .agentName(c != null ? c.getAgentName() : null)
                    .agentPhone(c != null ? c.getAgentPhone() : null)
                    .roomNo(deal.getRoomNo())
                    .dealAmount(deal.getDealAmount())
                    .dealTime(deal.getDealTime())
                    .subscribeType(deal.getSubscribeType())
                    .signStatus(deal.getSignStatus())
                    .paymentStatus(deal.getPaymentStatus())
                    .build();
        }).collect(Collectors.toList());
        resultPage.setRecords(items);
        return resultPage;
    }

    /**
     * 成交列表项（service 层中转 DTO，避免 service 模块直接依赖 app-api 的 VO）。
     * Controller 层会按需映射到对外 VO。
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DealListItem {
        private Long dealId;
        private Long customerId;
        private String customerName;
        private String phoneMasked;
        private Long projectId;
        private String projectName;
        private Long allianceId;
        private String allianceName;
        private String agentName;
        private String agentPhone;
        private String roomNo;
        private java.math.BigDecimal dealAmount;
        private java.time.LocalDateTime dealTime;
        private Integer subscribeType;
        private Integer signStatus;
        private Integer paymentStatus;
    }

    /**
     * 校验成交 DTO 必填字段
     */
    private void validateDealDTO(CustomerDealDTO dto) {
        if (dto.getCustomerId() == null) {
            throw new IllegalArgumentException("客户ID不能为空");
        }
        if (!StringUtils.hasText(dto.getRoomNo())) {
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
    }
}
