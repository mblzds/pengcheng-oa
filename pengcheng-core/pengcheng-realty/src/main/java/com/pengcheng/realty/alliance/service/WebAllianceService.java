package com.pengcheng.realty.alliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.realty.alliance.dto.AllianceDashboardVO;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.customer.dto.CustomerVO;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.commission.dto.CommissionVO;
import com.pengcheng.realty.project.dto.ProjectVO;
import com.pengcheng.realty.project.entity.Project;
import com.pengcheng.realty.project.entity.ProjectCommissionRule;
import com.pengcheng.realty.project.mapper.ProjectCommissionRuleMapper;
import com.pengcheng.realty.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 联盟商系统业务服务
 * <p>
 * 为联盟商外部系统提供项目查看、客户跟踪、佣金查询、业务概览等能力。
 */
@Service
@RequiredArgsConstructor
public class WebAllianceService {

    private final ProjectMapper projectMapper;
    private final ProjectCommissionRuleMapper commissionRuleMapper;
    private final RealtyCustomerMapper customerMapper;
    private final CommissionMapper commissionMapper;

    /**
     * 查询在售项目列表，支持按片区筛选
     */
    public List<ProjectVO> listOnSaleProjects(String district) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getStatus, 1); // 1-在售
        if (StringUtils.hasText(district)) {
            wrapper.eq(Project::getDistrict, district);
        }
        wrapper.orderByDesc(Project::getCreateTime);
        return projectMapper.selectList(wrapper).stream()
                .map(ProjectVO::fromEntity)
                .toList();
    }

    /**
     * 获取项目详情（含佣金规则）
     */
    public ProjectVO getProjectDetail(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        ProjectVO vo = ProjectVO.fromEntity(project);

        // 获取当前生效的佣金规则
        LambdaQueryWrapper<ProjectCommissionRule> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.eq(ProjectCommissionRule::getProjectId, projectId)
                .eq(ProjectCommissionRule::getStatus, 1) // 1-生效
                .orderByDesc(ProjectCommissionRule::getVersion)
                .last("LIMIT 1");
        ProjectCommissionRule rule = commissionRuleMapper.selectOne(ruleWrapper);
        vo.setCommissionRule(rule);

        return vo;
    }

    /**
     * 分页查询联盟商的客户列表
     */
    public PageResult<CustomerVO> pageCustomersByAlliance(Long allianceId, Integer page, Integer pageSize) {
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getAllianceId, allianceId)
                .orderByDesc(Customer::getCreateTime);

        IPage<Customer> result = customerMapper.selectPage(new Page<>(p, ps), wrapper);
        List<CustomerVO> voList = result.getRecords().stream()
                .map(CustomerVO::fromEntity)
                .toList();
        return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 分页查询联盟商的佣金列表
     */
    public PageResult<CommissionVO> pageCommissionsByAlliance(Long allianceId, Integer page, Integer pageSize) {
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        LambdaQueryWrapper<Commission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Commission::getAllianceId, allianceId)
                .orderByDesc(Commission::getCreateTime);

        IPage<Commission> result = commissionMapper.selectPage(new Page<>(p, ps), wrapper);
        List<CommissionVO> voList = result.getRecords().stream()
                .map(CommissionVO::fromEntity)
                .toList();
        return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 获取联盟商业务概览数据
     */
    public AllianceDashboardVO getDashboardOverview(Long allianceId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // 本月报备数
        long reportCount = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getAllianceId, allianceId)
                        .ge(Customer::getCreateTime, monthStart)
                        .le(Customer::getCreateTime, monthEnd));

        // 本月到访数（状态>=2 表示已到访或已成交）
        long visitCount = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getAllianceId, allianceId)
                        .ge(Customer::getStatus, 2)
                        .ge(Customer::getCreateTime, monthStart)
                        .le(Customer::getCreateTime, monthEnd));

        // 本月成交数
        long dealCount = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getAllianceId, allianceId)
                        .eq(Customer::getStatus, 3)
                        .ge(Customer::getCreateTime, monthStart)
                        .le(Customer::getCreateTime, monthEnd));

        // 待结佣金额（审核通过但未结算的佣金）
        LambdaQueryWrapper<Commission> commWrapper = new LambdaQueryWrapper<>();
        commWrapper.eq(Commission::getAllianceId, allianceId)
                .eq(Commission::getAuditStatus, 2); // 2-审核通过（待结算）
        List<Commission> pendingCommissions = commissionMapper.selectList(commWrapper);
        BigDecimal pendingAmount = pendingCommissions.stream()
                .map(Commission::getPayableAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AllianceDashboardVO.builder()
                .monthlyReportCount(reportCount)
                .monthlyVisitCount(visitCount)
                .monthlyDealCount(dealCount)
                .pendingCommissionAmount(pendingAmount)
                .build();
    }
}
