package com.pengcheng.admin.controller.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.employee.entity.EmployeeChange;
import com.pengcheng.hr.employee.entity.EmployeeProfile;
import com.pengcheng.hr.employee.service.EmployeeChangeService;
import com.pengcheng.hr.employee.service.EmployeeProfileService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 人事管理-员工档案与异动（公司级公共服务，不限于房产业务）
 */
@RestController
@RequestMapping("/admin/hr/employee")
@RequiredArgsConstructor
public class HrEmployeeController {

    private final EmployeeProfileService employeeProfileService;
    private final EmployeeChangeService employeeChangeService;

    @GetMapping("/profile/{userId}")
    public Result<EmployeeProfile> getProfile(@PathVariable Long userId) {
        return Result.ok(employeeProfileService.getByUserId(userId));
    }

    @PutMapping("/profile")
    @SaCheckPermission("hr:employee:edit")
    @Log(title = "员工档案", businessType = BusinessType.UPDATE)
    public Result<Void> saveProfile(@RequestBody EmployeeProfile profile) {
        employeeProfileService.saveOrUpdate(profile);
        return Result.ok();
    }

    @GetMapping("/changes")
    public Result<IPage<EmployeeChange>> changePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer changeType,
            @RequestParam(required = false) Integer status) {
        Page<EmployeeChange> page = new Page<>(pageNum, pageSize);
        return Result.ok(employeeChangeService.page(page, userId, changeType, status));
    }

    @GetMapping("/changes/{id}")
    public Result<EmployeeChange> getChange(@PathVariable Long id) {
        return Result.ok(employeeChangeService.getById(id));
    }

    @PostMapping("/changes")
    @SaCheckPermission("hr:employee:change")
    @Log(title = "人事异动", businessType = BusinessType.INSERT)
    public Result<Long> createChange(@RequestBody EmployeeChange change) {
        return Result.ok(employeeChangeService.create(change));
    }

    @PostMapping("/changes/{id}/effective")
    @SaCheckPermission("hr:employee:change")
    @Log(title = "人事异动生效", businessType = BusinessType.UPDATE)
    public Result<Void> setChangeEffective(@PathVariable Long id) {
        employeeChangeService.setEffective(id);
        return Result.ok();
    }
}
