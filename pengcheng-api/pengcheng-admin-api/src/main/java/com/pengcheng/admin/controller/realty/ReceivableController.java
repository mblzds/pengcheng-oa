package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.receivable.dto.ReceivablePlanCreateDTO;
import com.pengcheng.realty.receivable.dto.ReceivablePlanQueryDTO;
import com.pengcheng.realty.receivable.dto.ReceivableRecordCreateDTO;
import com.pengcheng.realty.receivable.entity.ReceivableAlert;
import com.pengcheng.realty.receivable.entity.ReceivablePlan;
import com.pengcheng.realty.receivable.entity.ReceivableRecord;
import com.pengcheng.realty.receivable.service.ReceivableService;
import com.pengcheng.realty.receivable.vo.ReceivableStatsVO;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 回款管理控制器
 */
@RestController
@RequestMapping("/admin/receivable")
@RequiredArgsConstructor
public class ReceivableController {

    private final ReceivableService receivableService;

    /** 为一条成交生成 N 期回款计划 */
    @SaCheckPermission("realty:receivable:add")
    @Log(title = "创建回款计划", businessType = BusinessType.INSERT)
    @PostMapping("/plan")
    public Result<List<Long>> createPlan(@RequestBody ReceivablePlanCreateDTO dto) {
        return Result.ok(receivableService.createPlan(dto));
    }

    /** 分页查询回款计划 */
    @SaCheckPermission("realty:receivable:list")
    @PostMapping("/plan/page")
    public Result<PageResult<ReceivablePlan>> pagePlans(@RequestBody ReceivablePlanQueryDTO query) {
        return Result.ok(receivableService.pagePlans(query));
    }

    /** 登记一笔实际到账流水 */
    @SaCheckPermission("realty:receivable:record")
    @Log(title = "登记到账", businessType = BusinessType.INSERT)
    @PostMapping("/record")
    public Result<Long> registerRecord(@RequestBody ReceivableRecordCreateDTO dto) {
        return Result.ok(receivableService.registerRecord(dto));
    }

    /** 查某分期的所有到账流水 */
    @SaCheckPermission("realty:receivable:list")
    @GetMapping("/record/list/{planId}")
    public Result<List<ReceivableRecord>> listRecords(@PathVariable Long planId) {
        return Result.ok(receivableService.listRecordsOfPlan(planId));
    }

    /** 未处理的告警列表 */
    @SaCheckPermission("realty:receivable:list")
    @GetMapping("/alert/open")
    public Result<List<ReceivableAlert>> openAlerts() {
        return Result.ok(receivableService.listOpenAlerts());
    }

    /** 回款总览统计 */
    @SaCheckPermission("realty:receivable:list")
    @GetMapping("/stats")
    public Result<ReceivableStatsVO> stats() {
        return Result.ok(receivableService.stats());
    }

    /** 手动触发逾期扫描（运维/调试用） */
    @SaCheckPermission("realty:receivable:check")
    @Log(title = "手动触发回款逾期扫描", businessType = BusinessType.OTHER)
    @PostMapping("/check/overdue")
    public Result<int[]> runOverdueCheck() {
        return Result.ok(receivableService.runOverdueCheck());
    }
}
