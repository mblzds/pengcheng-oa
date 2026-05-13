package com.pengcheng.hr.attendance.service;

import com.pengcheng.hr.attendance.dto.*;
import com.pengcheng.hr.attendance.entity.AttendanceRecord;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.LeaveRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 考勤/请假/调休/签到服务（公司级假勤）
 */
public interface AttendanceService {

    Long clockIn(ClockInDTO dto);

    Long clockOut(ClockInDTO dto);

    Long submitLeaveRequest(LeaveRequestDTO dto);

    Long submitCompensateRequest(Long userId, LocalDate compensateDate, String reason);

    Long signIn(SignInDTO dto);

    AttendanceMonthlyVO getMonthlySummary(Long userId, int year, int month);

    /**
     * 批量取多用户月度汇总（用于 HR / 部门主管"全员"列表视图）。
     * @param userIds null = 全公司在职启用用户；非空集合 = 仅这些 userId；空集合 = 直接返回空 list
     */
    List<AttendanceMonthlyVO> getMonthlySummaryBatch(Set<Long> userIds, int year, int month);

    List<AttendanceRecord> listAttendanceRecords(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 带数据范围的考勤记录查询
     * @param userId         指定查某个用户；为 null 时按 allowedUserIds 限制范围
     * @param allowedUserIds null = 不限（全公司）；非空集合 = 仅这些用户；空集合 = 直接返回空
     */
    List<AttendanceRecord> listAttendanceRecords(Long userId, Set<Long> allowedUserIds, LocalDate startDate, LocalDate endDate);

    /**
     * 带"缺勤补全"的考勤记录查询（用于前端列表视图，让整天没打卡的工作日也能呈现一条"缺勤"行）。
     * <p>
     * 规则：
     *  - 仅当 userId 明确（单人查询）才补全。allowedUserIds 集合模式不补，避免数据爆炸
     *  - 跳过周末/法定节假日（HolidayCalendarService.isWorkday=false）
     *  - 跳过 cutoff 之前的日期（员工 joinDate / 系统级考勤启用日期）
     *  - 跳过已有 attendance_record 的日期（请假/调休等 exempt 已由 listener 落表）
     *  - 跳过未来日期
     *  - 补出的行只填 userId + attendanceDate + 显示信息（nickname/deptName/employeeNo），
     *    clockInTime / clockOutTime / exemptReason 都为 null，前端 deriveDayStatus 即识别为"缺勤"
     */
    List<AttendanceRecord> listAttendanceRecordsWithAbsent(Long userId, Set<Long> allowedUserIds, LocalDate startDate, LocalDate endDate);

    List<LeaveRequest> listLeaveRequests(Long userId, Integer status);

    List<LeaveRequest> listLeaveRequests(Long userId, Set<Long> allowedUserIds, Integer status);

    List<CompensateRequest> listCompensateRequests(Long userId, Integer status);

    List<CompensateRequest> listCompensateRequests(Long userId, Set<Long> allowedUserIds, Integer status);

    /** 判定上班是否迟到 */
    int determineClockInStatus(java.time.LocalTime clockInTime);

    /** 判定下班是否早退 */
    int determineClockOutStatus(java.time.LocalTime clockOutTime);
}
