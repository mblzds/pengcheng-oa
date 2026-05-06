package com.pengcheng.realty.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.event.DataChangeEvent;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.common.exception.AllianceDisabledException;
import com.pengcheng.realty.common.exception.CustomerDuplicateException;
import com.pengcheng.realty.common.util.PhoneMaskUtil;
import com.pengcheng.realty.customer.dto.CustomerCreateDTO;
import com.pengcheng.realty.customer.dto.CustomerCreateResultVO;
import com.pengcheng.realty.customer.dto.CustomerQueryDTO;
import com.pengcheng.realty.customer.dto.CustomerVO;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerPoolEventLog;
import com.pengcheng.realty.customer.entity.CustomerProject;
import com.pengcheng.realty.customer.mapper.CustomerPoolEventLogMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.customer.mapper.CustomerProjectMapper;
import com.pengcheng.realty.project.entity.Project;
import com.pengcheng.realty.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户报备管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final RealtyCustomerMapper customerMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final CustomerPoolEventLogMapper customerPoolEventLogMapper;
    private final ProjectMapper projectMapper;
    private final AllianceMapper allianceMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * AI 智能判客（可选注入，AI 模块未加载时为 null）
     */
    @Autowired(required = false)
    private CustomerDuplicateChecker duplicateChecker;

    /** 默认保护期天数 */
    private static final int DEFAULT_PROTECTION_DAYS = 3;
    private static final DateTimeFormatter REPORT_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final AtomicLong REPORT_NO_SEQUENCE = new AtomicLong();

    /**
     * 创建客户报备（集成 AI 智能判客）
     * <p>
     * 报备流程：
     * 1. 校验必填字段
     * 2. 校验联盟商状态
     * 3. 保护期内去重检查
     * 4. AI 智能判客（比对公海池和私海池已有客户）
     * 5. 创建报备记录
     *
     * @return 报备结果，包含 AI 判客分析信息
     */
    @Transactional
    public CustomerCreateResultVO createCustomer(CustomerCreateDTO dto) {
        validateRequired(dto);

        // 校验联盟商存在且启用
        Alliance alliance = allianceMapper.selectById(dto.getAllianceId());
        if (alliance == null) {
            throw new IllegalArgumentException("联盟商不存在");
        }
        if (alliance.getStatus() != null && alliance.getStatus() == 0) {
            throw new AllianceDisabledException("该联盟商已停用，无法选择");
        }

        // 保护期内去重检查（同一手机号+同一项目）
        for (Long projectId : dto.getProjectIds()) {
            if (isInProtectionPeriod(dto.getPhone(), projectId)) {
                throw new CustomerDuplicateException("该客户在项目中已存在有效保护期内的报备记录");
            }
        }

        // AI 智能判客：比对公海池和私海池中已有客户
        CustomerDuplicateChecker.DuplicateCheckResult aiResult = performDuplicateCheck(dto.getPhone());

        LocalDateTime now = LocalDateTime.now();
        String reportNo = generateReportNo();

        // 构建客户实体
        Customer customer = Customer.builder()
                .reportNo(reportNo)
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
        customerMapper.insert(customer);

        // 插入客户-项目关联
        for (Long projectId : dto.getProjectIds()) {
            CustomerProject cp = CustomerProject.builder()
                    .customerId(customer.getId())
                    .projectId(projectId)
                    .build();
            customerProjectMapper.insert(cp);
        }

        // 广播数据变更事件
        eventPublisher.publishEvent(new DataChangeEvent(this, "create", "customer", customer.getId()));

        return CustomerCreateResultVO.builder()
                .customerId(customer.getId())
                .reportNo(reportNo)
                .hasDuplicate(aiResult != null && aiResult.hasDuplicate())
                .analysisMessage(aiResult != null ? aiResult.analysisMessage() : null)
                .existingCustomers(aiResult != null ? aiResult.existingCustomers() : Collections.emptyList())
                .build();
    }

    /**
     * 执行 AI 智能判客检查
     * <p>
     * 如果 AI 模块未加载（duplicateChecker 为 null），返回 null 表示跳过判客。
     */
    private CustomerDuplicateChecker.DuplicateCheckResult performDuplicateCheck(String phone) {
        if (duplicateChecker == null) {
            log.debug("AI 判客模块未加载，跳过智能判客");
            return null;
        }
        try {
            return duplicateChecker.checkDuplicate(phone);
        } catch (Exception e) {
            log.warn("AI 智能判客异常，跳过: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 分页查询客户列表（带数据权限）
     */
    public PageResult<CustomerVO> pageCustomers(CustomerQueryDTO query) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getCustomerName())) {
            wrapper.like(Customer::getCustomerName, query.getCustomerName());
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.like(Customer::getPhoneMasked, query.getPhone());
        }
        if (query.getAllianceId() != null) {
            wrapper.eq(Customer::getAllianceId, query.getAllianceId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Customer::getStatus, query.getStatus());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(Customer::getCreateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(Customer::getCreateTime, query.getEndTime());
        }
        // 项目筛选：通过子查询 customer_project 中间表
        if (query.getProjectId() != null) {
            wrapper.inSql(Customer::getId,
                    "SELECT customer_id FROM customer_project WHERE project_id = " + query.getProjectId());
        }
        wrapper.orderByDesc(Customer::getCreateTime);

        IPage<Customer> page = customerMapper.selectPageWithScope(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<CustomerVO> voList = page.getRecords().stream()
                .map(CustomerVO::fromEntity)
                .toList();
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 检查客户是否在保护期内（同项目+同手机号）
     */
    public boolean isInProtectionPeriod(String phone, Long projectId) {
        // 查找该手机号的所有客户
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getPhone, phone)
                .gt(Customer::getProtectionExpireTime, LocalDateTime.now());
        List<Customer> customers = customerMapper.selectList(wrapper);

        if (customers.isEmpty()) {
            return false;
        }

        // 检查是否有关联到同一项目的
        for (Customer c : customers) {
            LambdaQueryWrapper<CustomerProject> cpWrapper = new LambdaQueryWrapper<>();
            cpWrapper.eq(CustomerProject::getCustomerId, c.getId())
                    .eq(CustomerProject::getProjectId, projectId);
            if (customerProjectMapper.selectCount(cpWrapper) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按项目名称关键字搜索项目列表（用于报备时选择）
     */
    public List<Project> searchProjects(String keyword) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Project::getProjectName, keyword);
        }
        wrapper.eq(Project::getStatus, 1); // 仅在售项目
        wrapper.orderByDesc(Project::getCreateTime);
        return projectMapper.selectList(wrapper);
    }

    /**
     * 按公司名称关键字搜索联盟商列表（排除已停用，用于报备时选择）
     */
    public List<Alliance> searchAlliances(String keyword) {
        LambdaQueryWrapper<Alliance> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Alliance::getCompanyName, keyword);
        }
        wrapper.eq(Alliance::getStatus, 1); // 排除已停用
        wrapper.orderByDesc(Alliance::getCreateTime);
        return allianceMapper.selectList(wrapper);
    }

    /**
     * 校验必填字段
     */
    public void validateRequired(CustomerCreateDTO dto) {
        if (dto.getProjectIds() == null || dto.getProjectIds().isEmpty()) {
            throw new IllegalArgumentException("带看项目不能为空");
        }
        if (!StringUtils.hasText(dto.getCustomerName())) {
            throw new IllegalArgumentException("客户姓氏不能为空");
        }
        if (!StringUtils.hasText(dto.getPhone())) {
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
        if (!StringUtils.hasText(dto.getAgentName())) {
            throw new IllegalArgumentException("经纪人姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getAgentPhone())) {
            throw new IllegalArgumentException("经纪人联系方式不能为空");
        }
    }

    /**
     * 生成唯一报备编号：BP + 年月日时分秒毫秒 + 单调序列
     */
    public String generateReportNo() {
        String timestamp = LocalDateTime.now().format(REPORT_NO_FORMATTER);
        long sequence = REPORT_NO_SEQUENCE.incrementAndGet();
        return "BP" + timestamp + String.format("%06d", sequence);
    }

    /**
     * 公海池客户分页查询
     */
    public PageResult<CustomerVO> publicPoolPage(CustomerQueryDTO query) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();

        // 只查询公海池客户
        wrapper.eq(Customer::getPoolType, 1);

        if (StringUtils.hasText(query.getCustomerName())) {
            wrapper.like(Customer::getCustomerName, query.getCustomerName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Customer::getStatus, query.getStatus());
        }
        if (query.getAllianceId() != null) {
            wrapper.eq(Customer::getAllianceId, query.getAllianceId());
        }
        if (query.getProjectId() != null) {
            wrapper.inSql(Customer::getId,
                    "SELECT customer_id FROM customer_project WHERE project_id = " + query.getProjectId());
        }
        wrapper.orderByDesc(Customer::getCreateTime);

        IPage<Customer> page = customerMapper.selectPageWithScope(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<CustomerVO> voList = page.getRecords().stream()
                .map(CustomerVO::fromEntity)
                .toList();
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 获取公海池统计数据
     */
    public com.pengcheng.realty.customer.dto.PoolStatsVO getPoolStats() {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();

        // 公海池总数
        long total = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>().eq(Customer::getPoolType, 1));

        // 今日新增（创建时间为今天且初始为公海池）
        long todayNew = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getPoolType, 1)
                        .ge(Customer::getCreateTime, todayStart));

        // 今日领取和回收需要查询操作日志，这里简化处理
        long todayClaimed = customerPoolEventLogMapper.selectCount(
                new LambdaQueryWrapper<CustomerPoolEventLog>()
                        .eq(CustomerPoolEventLog::getEventType, CustomerPoolEventLog.EVENT_TYPE_CLAIM)
                        .ge(CustomerPoolEventLog::getEventTime, todayStart));
        long todayRecycled = customerPoolEventLogMapper.selectCount(
                new LambdaQueryWrapper<CustomerPoolEventLog>()
                        .eq(CustomerPoolEventLog::getEventType, CustomerPoolEventLog.EVENT_TYPE_RECYCLE)
                        .ge(CustomerPoolEventLog::getEventTime, todayStart));

        return com.pengcheng.realty.customer.dto.PoolStatsVO.builder()
                .total((int) total)
                .todayNew((int) todayNew)
                .todayClaimed((int) todayClaimed)
                .todayRecycled((int) todayRecycled)
                .build();
    }
}
