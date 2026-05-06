package com.pengcheng.hr.attendance.service;

import com.pengcheng.hr.attendance.dto.*;
import com.pengcheng.hr.attendance.entity.AttendanceRecord;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.LeaveRequest;

import java.time.LocalDate;
import java.util.List;

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

    List<AttendanceRecord> listAttendanceRecords(Long userId, LocalDate startDate, LocalDate endDate);

    List<LeaveRequest> listLeaveRequests(Long userId, Integer status);

    List<CompensateRequest> listCompensateRequests(Long userId, Integer status);

    /** 判定上班是否迟到 */
    int determineClockInStatus(java.time.LocalTime clockInTime);

    /** 判定下班是否早退 */
    int determineClockOutStatus(java.time.LocalTime clockOutTime);
}
