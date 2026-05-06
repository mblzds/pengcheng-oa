package com.pengcheng.realty.alliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.realty.alliance.dto.AllianceCreateDTO;
import com.pengcheng.realty.alliance.dto.AllianceQueryDTO;
import com.pengcheng.realty.alliance.dto.AllianceStatsVO;
import com.pengcheng.realty.alliance.dto.AllianceVO;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.hr.attendance.entity.SignInRecord;
import com.pengcheng.hr.attendance.mapper.SignInRecordMapper;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.common.constants.RealtyRoleConstants;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import com.pengcheng.realty.payment.mapper.PaymentRequestMapper;
import com.pengcheng.system.entity.SysRole;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.service.SysRoleService;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 联盟商管理服务
 */
@Service
@RequiredArgsConstructor
public class AllianceService {

    private final AllianceMapper allianceMapper;
    private final RealtyCustomerMapper customerMapper;
    private final CustomerDealMapper customerDealMapper;
    private final CommissionMapper commissionMapper;
    private final PaymentRequestMapper paymentRequestMapper;
    private final SignInRecordMapper signInRecordMapper;
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;

    /**
     * 创建联盟商并自动创建系统登录账号
     */
    @Transactional
    public Long createAlliance(AllianceCreateDTO dto) {
        validateRequired(dto);

        // 创建系统登录账号
        SysUser user = new SysUser();
        user.setUsername("alliance_" + System.currentTimeMillis());
        user.setNickname(dto.getContactName());
        user.setPhone(dto.getContactPhone());
        user.setStatus(1);
        user.setUserType("admin");

        // 查找联盟商负责人角色
        SysRole allianceRole = sysRoleService.getByCode(RealtyRoleConstants.ALLIANCE_MANAGER);
        List<Long> roleIds = allianceRole != null
                ? Collections.singletonList(allianceRole.getId())
                : Collections.emptyList();

        sysUserService.create(user, roleIds, Collections.emptyList());

        // 创建联盟商
        Alliance alliance = Alliance.builder()
                .companyName(dto.getCompanyName())
                .officeAddress(dto.getOfficeAddress())
                .contactName(dto.getContactName())
                .contactPhone(dto.getContactPhone())
                .staffSize(dto.getStaffSize())
                .level(dto.getLevel())
                .status(1) // 默认启用
                .userId(user.getId())
                .channelUserId(dto.getChannelUserId())
                .build();
        allianceMapper.insert(alliance);
        return alliance.getId();
    }

    /**
     * 编辑联盟商
     */
    @Transactional
    public void updateAlliance(AllianceCreateDTO dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("联盟商ID不能为空");
        }
        Alliance alliance = allianceMapper.selectById(dto.getId());
        if (alliance == null) {
            throw new IllegalArgumentException("联盟商不存在");
        }

        if (StringUtils.hasText(dto.getCompanyName())) {
            alliance.setCompanyName(dto.getCompanyName());
        }
        if (StringUtils.hasText(dto.getOfficeAddress())) {
            alliance.setOfficeAddress(dto.getOfficeAddress());
        }
        if (StringUtils.hasText(dto.getContactName())) {
            alliance.setContactName(dto.getContactName());
        }
        if (StringUtils.hasText(dto.getContactPhone())) {
            alliance.setContactPhone(dto.getContactPhone());
        }
        if (dto.getStaffSize() != null) {
            alliance.setStaffSize(dto.getStaffSize());
        }
        if (dto.getLevel() != null) {
            alliance.setLevel(dto.getLevel());
        }
        if (dto.getChannelUserId() != null) {
            alliance.setChannelUserId(dto.getChannelUserId());
        }
        allianceMapper.updateById(alliance);
    }

    /**
     * 启用联盟商
     */
    @Transactional
    public void enableAlliance(Long allianceId) {
        Alliance alliance = getAllianceOrThrow(allianceId);
        alliance.setStatus(1);
        allianceMapper.updateById(alliance);

        // 启用关联账号
        if (alliance.getUserId() != null) {
            SysUser user = sysUserService.getById(alliance.getUserId());
            if (user != null) {
                user.setStatus(1);
                sysUserService.updateById(user);
            }
        }
    }

    /**
     * 停用联盟商（禁止登录+报备不可选）
     */
    @Transactional
    public void disableAlliance(Long allianceId) {
        Alliance alliance = getAllianceOrThrow(allianceId);
        alliance.setStatus(0);
        allianceMapper.updateById(alliance);

        // 禁用关联账号
        if (alliance.getUserId() != null) {
            SysUser user = sysUserService.getById(alliance.getUserId());
            if (user != null) {
                user.setStatus(0);
                sysUserService.updateById(user);
            }
        }
    }

    /**
     * 分页查询联盟商（带数据权限）
     */
    public PageResult<AllianceVO> pageAlliances(AllianceQueryDTO query) {
        LambdaQueryWrapper<Alliance> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getCompanyName())) {
            wrapper.like(Alliance::getCompanyName, query.getCompanyName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Alliance::getStatus, query.getStatus());
        }
        if (query.getLevel() != null) {
            wrapper.eq(Alliance::getLevel, query.getLevel());
        }
        wrapper.orderByDesc(Alliance::getCreateTime);

        IPage<Alliance> page = allianceMapper.selectPageWithScope(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<AllianceVO> voList = page.getRecords().stream()
                .map(AllianceVO::fromEntity)
                .toList();
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 查询启用状态的联盟商列表（用于报备时选择，排除已停用）
     */
    public List<AllianceVO> listEnabled() {
        LambdaQueryWrapper<Alliance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Alliance::getStatus, 1);
        wrapper.orderByDesc(Alliance::getCreateTime);
        List<Alliance> list = allianceMapper.selectListWithScope(wrapper);
        return list.stream().map(AllianceVO::fromEntity).toList();
    }

    /**
     * 查询联盟商运营数据统计
     */
    public AllianceStatsVO getAllianceStats(Long allianceId) {
        Alliance alliance = getAllianceOrThrow(allianceId);

        // 上客数量：该联盟商的报备客户总数
        Long customerCount = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>().eq(Customer::getAllianceId, allianceId));

        // 成交数据：通过客户关联查询成交记录
        List<Customer> customers = customerMapper.selectList(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getAllianceId, allianceId)
                        .eq(Customer::getStatus, 3)); // 已成交
        Long dealCount = (long) customers.size();

        // 成交业绩：汇总成交金额
        BigDecimal dealAmount = BigDecimal.ZERO;
        if (!customers.isEmpty()) {
            List<Long> customerIds = customers.stream().map(Customer::getId).toList();
            List<CustomerDeal> deals = customerDealMapper.selectList(
                    new LambdaQueryWrapper<CustomerDeal>().in(CustomerDeal::getCustomerId, customerIds));
            dealAmount = deals.stream()
                    .map(d -> d.getDealAmount() != null ? d.getDealAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 结佣情况：查询该联盟商的佣金记录
        List<Commission> commissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>().eq(Commission::getAllianceId, allianceId));
        BigDecimal settledCommission = commissions.stream()
                .filter(c -> c.getAuditStatus() != null && c.getAuditStatus() == 2) // 审核通过
                .map(c -> c.getPayableAmount() != null ? c.getPayableAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingCommission = commissions.stream()
                .filter(c -> c.getAuditStatus() != null && c.getAuditStatus() == 1) // 待审核
                .map(c -> c.getPayableAmount() != null ? c.getPayableAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 费用统计：查询关联该联盟商的付款申请（已通过的）
        LambdaQueryWrapper<PaymentRequest> expenseWrapper = new LambdaQueryWrapper<PaymentRequest>()
                .eq(PaymentRequest::getRelatedAllianceId, allianceId)
                .eq(PaymentRequest::getRequestType, 1) // 费用报销
                .eq(PaymentRequest::getStatus, 3); // 已通过
        List<PaymentRequest> expenses = paymentRequestMapper.selectList(expenseWrapper);

        // 派车费用（交通费 expenseType=1）
        BigDecimal transportExpense = expenses.stream()
                .filter(e -> e.getExpenseType() != null && e.getExpenseType() == 1)
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 推广费用（其他 expenseType=5，按业务约定推广费归类为"其他"）
        BigDecimal promotionExpense = expenses.stream()
                .filter(e -> e.getExpenseType() != null && e.getExpenseType() == 5)
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 宴请费用（餐饮费 expenseType=2）
        BigDecimal entertainmentExpense = expenses.stream()
                .filter(e -> e.getExpenseType() != null && e.getExpenseType() == 2)
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 渠道走访次数：对接渠道人员的签到记录数
        Long channelVisitCount = 0L;
        if (alliance.getChannelUserId() != null) {
            channelVisitCount = signInRecordMapper.selectCount(
                    new LambdaQueryWrapper<SignInRecord>()
                            .eq(SignInRecord::getUserId, alliance.getChannelUserId()));
        }

        return AllianceStatsVO.builder()
                .allianceId(allianceId)
                .companyName(alliance.getCompanyName())
                .customerCount(customerCount)
                .dealCount(dealCount)
                .dealAmount(dealAmount)
                .settledCommission(settledCommission)
                .pendingCommission(pendingCommission)
                .transportExpense(transportExpense)
                .promotionExpense(promotionExpense)
                .entertainmentExpense(entertainmentExpense)
                .channelVisitCount(channelVisitCount)
                .build();
    }

    /**
     * 根据ID获取联盟商详情
     */
    public AllianceVO getAlliance(Long id) {
        return AllianceVO.fromEntity(allianceMapper.selectById(id));
    }

    /**
     * 校验必填字段
     */
    public void validateRequired(AllianceCreateDTO dto) {
        if (!StringUtils.hasText(dto.getCompanyName())) {
            throw new IllegalArgumentException("联盟公司名称不能为空");
        }
        if (!StringUtils.hasText(dto.getOfficeAddress())) {
            throw new IllegalArgumentException("办公地址不能为空");
        }
        if (!StringUtils.hasText(dto.getContactName())) {
            throw new IllegalArgumentException("负责人姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getContactPhone())) {
            throw new IllegalArgumentException("联系方式不能为空");
        }
        if (dto.getStaffSize() == null) {
            throw new IllegalArgumentException("人员规模不能为空");
        }
        if (dto.getLevel() == null) {
            throw new IllegalArgumentException("联盟商等级不能为空");
        }
    }

    private Alliance getAllianceOrThrow(Long allianceId) {
        Alliance alliance = allianceMapper.selectById(allianceId);
        if (alliance == null) {
            throw new IllegalArgumentException("联盟商不存在");
        }
        return alliance;
    }
}
