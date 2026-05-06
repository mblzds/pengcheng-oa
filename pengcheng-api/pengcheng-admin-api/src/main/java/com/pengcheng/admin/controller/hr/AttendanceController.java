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

/**
 * 考勤/请假/调休管理（公司级假勤，归属人事模块）
 * 路径保持 /admin/attendance 以兼容前端与菜单
 */
@RestController
@RequestMapping("/admin/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

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
        return Result.ok(attendanceService.getMonthlySummary(userId, year, month));
    }

    @GetMapping("/records")
    @SaCheckPermission("hr:attendance:list")
    public Result<List<AttendanceRecord>> records(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.ok(attendanceService.listAttendanceRecords(userId, startDate, endDate));
    }

    @GetMapping("/leave/list")
    public Result<List<LeaveRequest>> leaveList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(attendanceService.listLeaveRequests(userId, status));
    }

    @GetMapping("/compensate/list")
    public Result<List<CompensateRequest>> compensateList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(attendanceService.listCompensateRequests(userId, status));
    }
}
