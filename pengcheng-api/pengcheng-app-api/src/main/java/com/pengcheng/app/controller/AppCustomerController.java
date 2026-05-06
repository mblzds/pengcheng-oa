package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.app.dto.CustomerDetailVO;
import com.pengcheng.app.dto.DealOptionVO;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.customer.dto.*;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.customer.service.CustomerDealService;
import com.pengcheng.realty.customer.service.CustomerService;
import com.pengcheng.realty.customer.service.CustomerVisitService;
import com.pengcheng.realty.project.entity.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * App端客户业务控制器
 * 提供客户报备、列表查询、详情查看、到访录入、成交录入接口
 */
@RestController
@RequestMapping("/app/customer")
@RequiredArgsConstructor
@SaCheckLogin
public class AppCustomerController {

    private final CustomerService customerService;
    private final CustomerVisitService customerVisitService;
    private final CustomerDealService customerDealService;
    private final RealtyCustomerMapper customerMapper;
    private final CustomerDealMapper customerDealMapper;
    private final AllianceMapper allianceMapper;

    /**
     * 创建客户报备
     * 成功返回报备编号，重复报备返回 HTTP 409
     */
    @PostMapping("/report")
    public Result<CustomerCreateResultVO> report(@RequestBody CustomerCreateDTO dto) {
        CustomerCreateResultVO result = customerService.createCustomer(dto);
        return Result.ok(result);
    }

    /**
     * 搜索项目（用于报备页项目选择）
     */
    @GetMapping("/projects")
    public Result<List<Project>> projects(@RequestParam(required = false) String keyword) {
        return Result.ok(customerService.searchProjects(keyword));
    }

    /**
     * 搜索联盟商（用于报备页公司选择）
     */
    @GetMapping("/alliances")
    public Result<List<Alliance>> alliances(@RequestParam(required = false) String keyword) {
        return Result.ok(customerService.searchAlliances(keyword));
    }

    /**
     * 成交记录选项（用于垫佣/预付佣申请）
     */
    @GetMapping("/deals")
    public Result<List<DealOptionVO>> deals() {
        LambdaQueryWrapper<CustomerDeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CustomerDeal::getDealTime).last("LIMIT 200");
        List<CustomerDeal> deals = customerDealMapper.selectList(wrapper);
        List<DealOptionVO> result = deals.stream().map(deal -> {
            Customer customer = customerMapper.selectById(deal.getCustomerId());
            return DealOptionVO.builder()
                    .id(deal.getId())
                    .customerId(deal.getCustomerId())
                    .customerName(customer != null ? customer.getCustomerName() : null)
                    .roomNo(deal.getRoomNo())
                    .dealAmount(deal.getDealAmount())
                    .dealTime(deal.getDealTime())
                    .build();
        }).toList();
        return Result.ok(result);
    }

    /**
     * 客户列表（支持按姓氏、项目、状态筛选，分页）
     */
    @GetMapping("/list")
    public Result<PageResult<CustomerVO>> list(
            @RequestParam(required = false) String surname,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        CustomerQueryDTO query = CustomerQueryDTO.builder()
                .customerName(surname)
                .projectId(projectId)
                .status(status)
                .page(page)
                .pageSize(pageSize)
                .build();
        PageResult<CustomerVO> pageResult = customerService.pageCustomers(query);
        return Result.ok(pageResult);
    }

    /**
     * 客户详情（聚合报备信息、到访记录、成交信息、跟进历史）
     */
    @GetMapping("/{id}")
    public Result<CustomerDetailVO> detail(@PathVariable Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            return Result.fail(400, "客户不存在");
        }

        // 报备信息
        String allianceName = null;
        if (customer.getAllianceId() != null) {
            Alliance alliance = allianceMapper.selectById(customer.getAllianceId());
            if (alliance != null) {
                allianceName = alliance.getCompanyName();
            }
        }
        CustomerDetailVO.ReportInfo reportInfo = CustomerDetailVO.ReportInfo.builder()
                .id(customer.getId())
                .reportNo(customer.getReportNo())
                .customerName(customer.getCustomerName())
                .phoneMasked(customer.getPhoneMasked())
                .visitCount(customer.getVisitCount())
                .visitTime(customer.getVisitTime())
                .allianceName(allianceName)
                .agentName(customer.getAgentName())
                .agentPhone(customer.getAgentPhone())
                .status(customer.getStatus())
                .createTime(customer.getCreateTime())
                .build();

        // 到访记录
        List<CustomerVisit> visits = customerVisitService.listVisitsByCustomerId(id);
        List<CustomerDetailVO.VisitRecord> visitRecords = visits.stream()
                .map(v -> CustomerDetailVO.VisitRecord.builder()
                        .id(v.getId())
                        .actualVisitTime(v.getActualVisitTime())
                        .actualVisitCount(v.getActualVisitCount())
                        .receptionist(v.getReceptionist())
                        .remark(v.getRemark())
                        .build())
                .toList();

        // 成交信息
        CustomerDetailVO.DealInfo dealInfo = null;
        LambdaQueryWrapper<CustomerDeal> dealWrapper = new LambdaQueryWrapper<>();
        dealWrapper.eq(CustomerDeal::getCustomerId, id).last("LIMIT 1");
        CustomerDeal deal = customerDealMapper.selectOne(dealWrapper);
        if (deal != null) {
            dealInfo = CustomerDetailVO.DealInfo.builder()
                    .id(deal.getId())
                    .roomNo(deal.getRoomNo())
                    .dealAmount(deal.getDealAmount())
                    .dealTime(deal.getDealTime())
                    .signStatus(deal.getSignStatus())
                    .subscribeType(deal.getSubscribeType())
                    .build();
        }

        // 跟进历史时间线
        List<CustomerDetailVO.TimelineItem> timeline = buildTimeline(customer, visits, deal);

        CustomerDetailVO detailVO = CustomerDetailVO.builder()
                .reportInfo(reportInfo)
                .visits(visitRecords)
                .deal(dealInfo)
                .timeline(timeline)
                .build();

        return Result.ok(detailVO);
    }

    /**
     * 录入到访
     */
    @PostMapping("/visit")
    public Result<Void> visit(@RequestBody CustomerVisitDTO dto) {
        customerVisitService.createVisit(dto);
        return Result.ok();
    }

    /**
     * 录入成交
     * 状态流转错误返回 HTTP 400
     */
    @PostMapping("/deal")
    public Result<Void> deal(@RequestBody CustomerDealDTO dto) {
        customerDealService.createDeal(dto);
        return Result.ok();
    }

    /**
     * 构建跟进历史时间线（报备→到访→成交）
     */
    private List<CustomerDetailVO.TimelineItem> buildTimeline(
            Customer customer, List<CustomerVisit> visits, CustomerDeal deal) {
        List<CustomerDetailVO.TimelineItem> timeline = new ArrayList<>();

        // 报备事件
        timeline.add(CustomerDetailVO.TimelineItem.builder()
                .eventType("report")
                .description("客户报备 - " + customer.getCustomerName())
                .eventTime(customer.getCreateTime())
                .build());

        // 到访事件
        for (CustomerVisit v : visits) {
            timeline.add(CustomerDetailVO.TimelineItem.builder()
                    .eventType("visit")
                    .description("客户到访 - 到访人数: " + v.getActualVisitCount())
                    .eventTime(v.getActualVisitTime())
                    .build());
        }

        // 成交事件
        if (deal != null) {
            timeline.add(CustomerDetailVO.TimelineItem.builder()
                    .eventType("deal")
                    .description("客户成交 - 房号: " + deal.getRoomNo())
                    .eventTime(deal.getDealTime())
                    .build());
        }

        // 按时间降序排列
        timeline.sort((a, b) -> b.getEventTime().compareTo(a.getEventTime()));
        return timeline;
    }
}
