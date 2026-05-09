package com.pengcheng.admin.controller.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.attendance.dto.*;
import com.pengcheng.hr.attendance.entity.AttendanceRecord;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.LeaveRequest;
import com.pengcheng.hr.attendance.service.AttendanceService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 考勤/请假/调休管理（公司级假勤，归属人事模块）
 * 路径保持 /admin/attendance 以兼容前端与菜单
 */
@RestController
@RequestMapping("/admin/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceScopeHelper scopeHelper;

    @PostMapping("/clock-in")
    @Log(title = "考勤打卡", businessType = BusinessType.INSERT)
    public Result<Long> clockIn(@RequestBody ClockInDTO dto) {
        return Result.ok(attendanceService.clockIn(dto));
    }

    @PostMapping("/clock-out")
    @Log(title = "考勤打卡", businessType = BusinessType.INSERT)
    public Result<Long> clockOut(@RequestBody ClockInDTO dto) {
        return Result.ok(attendanceService.clockOut(dto));
    }

    @PostMapping("/leave")
    @Log(title = "请假申请", businessType = BusinessType.INSERT)
    public Result<Long> submitLeave(@RequestBody LeaveRequestDTO dto) {
        return Result.ok(attendanceService.submitLeaveRequest(dto));
    }

    @PostMapping("/sign-in")
    @Log(title = "签到", businessType = BusinessType.INSERT)
    public Result<Long> signIn(@RequestBody SignInDTO dto) {
        return Result.ok(attendanceService.signIn(dto));
    }

    @GetMapping("/monthly")
    public Result<AttendanceMonthlyVO> monthlySummary(
            @RequestParam Long userId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        scopeHelper.assertCanView(userId);
        return Result.ok(attendanceService.getMonthlySummary(userId, year, month));
    }

    @GetMapping("/records")
    @SaCheckPermission("realty:attendance:list")
    public Result<List<AttendanceRecord>> records(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Long effectiveUserId = resolveQueryUserId(userId);
        Set<Long> allowed = effectiveUserId == null ? scopeHelper.visibleUserIds() : null;
        return Result.ok(attendanceService.listAttendanceRecords(effectiveUserId, allowed, startDate, endDate));
    }

    @GetMapping("/leave/list")
    public Result<List<LeaveRequest>> leaveList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        Long effectiveUserId = resolveQueryUserId(userId);
        Set<Long> allowed = effectiveUserId == null ? scopeHelper.visibleUserIds() : null;
        return Result.ok(attendanceService.listLeaveRequests(effectiveUserId, allowed, status));
    }

    @GetMapping("/compensate/list")
    public Result<List<CompensateRequest>> compensateList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        Long effectiveUserId = resolveQueryUserId(userId);
        Set<Long> allowed = effectiveUserId == null ? scopeHelper.visibleUserIds() : null;
        return Result.ok(attendanceService.listCompensateRequests(effectiveUserId, allowed, status));
    }

    /**
     * 当前登录用户在考勤数据上能看到的用户列表（用于前端"选择用户"下拉）
     * - HR/管理员 → 全部启用用户
     * - 部门主管 → 本部门 + 所有下级部门成员
     * - 普通员工 → 仅自己
     */
    @GetMapping("/visible-users")
    public Result<List<Map<String, Object>>> visibleUsers() {
        return Result.ok(scopeHelper.visibleUserOptions());
    }

    /**
     * 校验请求方传入的 userId 是否在可见范围内并返回最终查询用 userId：
     * - 传了 userId：先校验权限通过则返回 userId
     * - 未传 userId 且员工：强制返回当前登录用户 ID（避免越权）
     * - 未传 userId 且 HR / 主管：返回 null，走 scope 集合过滤
     */
    private Long resolveQueryUserId(Long requestedUserId) {
        if (requestedUserId != null) {
            scopeHelper.assertCanView(requestedUserId);
            return requestedUserId;
        }
        Set<Long> allowed = scopeHelper.visibleUserIds();
        if (allowed != null && allowed.size() == 1) {
            // 普通员工：未传 userId 时强制只看自己
            return allowed.iterator().next();
        }
        return null;
    }
}
