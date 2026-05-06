package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.customer.dto.CustomerCreateDTO;
import com.pengcheng.realty.customer.dto.CustomerCreateResultVO;
import com.pengcheng.realty.customer.dto.CustomerDealDTO;
import com.pengcheng.realty.customer.dto.CustomerDealUpdateDTO;
import com.pengcheng.realty.customer.dto.CustomerQueryDTO;
import com.pengcheng.realty.customer.dto.CustomerVisitDTO;
import com.pengcheng.realty.customer.dto.CustomerVO;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import com.pengcheng.realty.customer.dto.PoolClaimDTO;
import com.pengcheng.realty.customer.dto.PoolRecycleConfigDTO;
import com.pengcheng.realty.customer.service.CustomerDealService;
import com.pengcheng.realty.customer.service.CustomerPoolService;
import com.pengcheng.realty.customer.service.CustomerService;
import com.pengcheng.realty.customer.service.CustomerVisitService;
import com.pengcheng.realty.project.entity.Project;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户管理控制器（报备、到访、成交）
 */
@RestController
@RequestMapping("/admin/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerVisitService customerVisitService;
    private final CustomerDealService customerDealService;
    private final CustomerPoolService customerPoolService;

    /**
     * 客户分页查询
     */
    @GetMapping("/page")
    @SaCheckPermission("realty:customer:list")
    @Log(title = "客户管理", businessType = BusinessType.QUERY)
    public Result<PageResult<CustomerVO>> page(CustomerQueryDTO query) {
        return Result.ok(customerService.pageCustomers(query));
    }

    /**
     * 创建客户报备（集成 AI 智能判客）
     */
    @PostMapping("/create")
    @SaCheckPermission("realty:customer:add")
    @Log(title = "客户报备", businessType = BusinessType.INSERT)
    public Result<CustomerCreateResultVO> create(@RequestBody CustomerCreateDTO dto) {
        return Result.ok(customerService.createCustomer(dto));
    }

    /**
     * 按项目名称关键字搜索项目（报备时选择）
     */
    @GetMapping("/project/search")
    public Result<List<Project>> searchProjects(@RequestParam(required = false) String keyword) {
        return Result.ok(customerService.searchProjects(keyword));
    }

    /**
     * 按公司名称关键字搜索联盟商（报备时选择，排除已停用）
     */
    @GetMapping("/alliance/search")
    public Result<List<Alliance>> searchAlliances(@RequestParam(required = false) String keyword) {
        return Result.ok(customerService.searchAlliances(keyword));
    }

    // ========== 到访管理 ==========

    /**
     * 录入到访数据
     */
    @PostMapping("/visit")
    @SaCheckPermission("realty:customer:visit")
    @Log(title = "客户到访", businessType = BusinessType.INSERT)
    public Result<Long> createVisit(@RequestBody CustomerVisitDTO dto) {
        return Result.ok(customerVisitService.createVisit(dto));
    }

    /**
     * 查询客户到访记录
     */
    @GetMapping("/visit/list")
    public Result<List<CustomerVisit>> listVisits(@RequestParam Long customerId) {
        return Result.ok(customerVisitService.listVisitsByCustomerId(customerId));
    }

    // ========== 成交管理 ==========

    /**
     * 录入成交数据
     */
    @PostMapping("/deal")
    @SaCheckPermission("realty:customer:deal")
    @Log(title = "客户成交", businessType = BusinessType.INSERT)
    public Result<Long> createDeal(@RequestBody CustomerDealDTO dto) {
        return Result.ok(customerDealService.createDeal(dto));
    }

    /**
     * 更新成交后续手续状态
     */
    @PostMapping("/deal/update")
    @SaCheckPermission("realty:customer:deal")
    @Log(title = "客户成交", businessType = BusinessType.UPDATE)
    public Result<Void> updateDeal(@RequestBody CustomerDealUpdateDTO dto) {
        customerDealService.updateDeal(dto);
        return Result.ok();
    }

    /**
     * 查询客户成交记录
     */
    @GetMapping("/deal/list")
    public Result<List<CustomerDeal>> listDeals(@RequestParam Long customerId) {
        return Result.ok(customerDealService.listDealsByCustomerId(customerId));
    }

    // ========== 公海/私海池管理 ==========

    /**
     * 公海池客户分页查询
     */
    @GetMapping("/pool/public")
    @Log(title = "公海池客户", businessType = BusinessType.QUERY)
    public Result<PageResult<CustomerVO>> publicPoolPage(CustomerQueryDTO query) {
        return Result.ok(customerService.publicPoolPage(query));
    }

    /**
     * 公海池统计数据
     */
    @GetMapping("/pool/stats")
    public Result<com.pengcheng.realty.customer.dto.PoolStatsVO> poolStats() {
        return Result.ok(customerService.getPoolStats());
    }

    /**
     * 批量从公海池领取客户
     */
    @PostMapping("/pool/claim")
    @SaCheckPermission("realty:customer:pool")
    @Log(title = "公海池领取", businessType = BusinessType.UPDATE)
    public Result<Void> claimFromPublicPool(@RequestBody PoolClaimDTO dto) {
        for (Long customerId : dto.getCustomerIds()) {
            customerPoolService.claimFromPublicPool(customerId, dto.getUserId());
        }
        return Result.ok();
    }

    /**
     * 手动触发公海池回收
     */
    @PostMapping("/pool/recycle")
    @SaCheckPermission("realty:customer:pool")
    @Log(title = "公海池回收", businessType = BusinessType.UPDATE)
    public Result<Integer> recycleToPublicPool() {
        return Result.ok(customerPoolService.recycleToPublicPool());
    }

    /**
     * 更新公海池回收规则配置
     */
    @PostMapping("/pool/config")
    @SaCheckPermission("realty:customer:poolConfig")
    @Log(title = "公海池配置", businessType = BusinessType.UPDATE)
    public Result<Void> updateRecycleConfig(@RequestBody PoolRecycleConfigDTO dto) {
        customerPoolService.updateRecycleConfig(dto.getNoFollowDays(), dto.getNoVisitDays());
        return Result.ok();
    }

    /**
     * 获取当前回收规则配置
     */
    @GetMapping("/pool/config")
    public Result<PoolRecycleConfigDTO> getRecycleConfig() {
        PoolRecycleConfigDTO config = PoolRecycleConfigDTO.builder()
                .noFollowDays(customerPoolService.getNoFollowDays())
                .noVisitDays(customerPoolService.getNoVisitDays())
                .protectionDays(3)
                .autoRecycleEnabled(true)
                .build();
        return Result.ok(config);
    }
}
