package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.commission.dto.CommissionAuditDTO;
import com.pengcheng.realty.commission.dto.CommissionCreateDTO;
import com.pengcheng.realty.commission.dto.CommissionQueryDTO;
import com.pengcheng.realty.commission.dto.CommissionVO;
import com.pengcheng.realty.commission.entity.CommissionChangeLog;
import com.pengcheng.admin.controller.hr.AttendanceScopeHelper;
import com.pengcheng.realty.commission.service.CommissionService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成交佣金管理控制器
 */
@RestController
@RequestMapping("/admin/commission")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;
    private final AttendanceScopeHelper scopeHelper;

    /**
     * 佣金分页查询
     */
    @GetMapping("/page")
    @SaCheckPermission("realty:commission:list")
    public Result<PageResult<CommissionVO>> page(CommissionQueryDTO query) {
        // 双层可见性收口：基础职级 + 佣金加成（默认 finance）
        query.setAllowedCreatorIds(scopeHelper.visibleUserIdsForCommission());
        return Result.ok(commissionService.pageCommissions(query));
    }

    /**
     * 录入佣金
     */
    @PostMapping("/create")
    @SaCheckPermission("realty:commission:add")
    @Log(title = "佣金管理", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody CommissionCreateDTO dto) {
        return Result.ok(commissionService.createCommission(dto));
    }

    /**
     * 财务审核佣金
     */
    @PostMapping("/audit")
    @SaCheckPermission("realty:commission:audit")
    @Log(title = "佣金审核", businessType = BusinessType.UPDATE)
    public Result<Void> audit(@RequestBody CommissionAuditDTO dto) {
        commissionService.auditCommission(dto);
        return Result.ok();
    }

    /**
     * 查询佣金变更日志
     */
    @GetMapping("/changelog")
    public Result<List<CommissionChangeLog>> changeLogs(@RequestParam Long commissionId) {
        return Result.ok(commissionService.getChangeLogs(commissionId));
    }

    /**
     * 检查结佣触发条件
     */
    @GetMapping("/settlement/check")
    public Result<Boolean> checkSettlement(@RequestParam Long dealId) {
        return Result.ok(commissionService.checkSettlementTrigger(dealId));
    }
}
