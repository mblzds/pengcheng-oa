package com.pengcheng.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.event.DataChangeEvent;
import com.pengcheng.hr.attendance.dto.*;
import com.pengcheng.hr.attendance.entity.AttendanceRecord;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.LeaveRequest;
import com.pengcheng.hr.attendance.entity.SignInRecord;
import com.pengcheng.hr.attendance.mapper.AttendanceRecordMapper;
import com.pengcheng.hr.attendance.mapper.CompensateRequestMapper;
import com.pengcheng.hr.attendance.mapper.LeaveRequestMapper;
import com.pengcheng.hr.attendance.mapper.SignInRecordMapper;
import com.pengcheng.hr.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 考勤/请假/调休/签到服务实现（公司级假勤）
 */
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final SignInRecordMapper signInRecordMapper;
    private final CompensateRequestMapper compensateRequestMapper;
    private final ApplicationEventPublisher eventPublisher;

    public static final int CLOCK_IN_NORMAL = 1;
    public static final int CLOCK_IN_LATE = 2;
    public static final int CLOCK_OUT_NORMAL = 1;
    public static final int CLOCK_OUT_EARLY = 2;
    public static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    public static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long clockIn(ClockInDTO dto) {
        if (dto.getUserId() == null) throw new IllegalArgumentException("员工ID不能为空");
        if (dto.getClockTime() == null) throw new IllegalArgumentException("打卡时间不能为空");
        LocalDate today = dto.getClockTime().toLocalDate();
        AttendanceRecord record = getOrCreateRecord(dto.getUserId(), today);
        int status = dto.getClockTime().toLocalTime().isAfter(WORK_START_TIME) ? CLOCK_IN_LATE : CLOCK_IN_NORMAL;
        record.setClockInTime(dto.getClockTime());
        record.setClockInLocation(dto.getLocation());
        record.setClockInStatus(status);
        String changeType = record.getId() == null ? "create" : "update";
        if (record.getId() == null) attendanceRecordMapper.insert(record);
        else attendanceRecordMapper.updateById(record);
        eventPublisher.publishEvent(new DataChangeEvent(this, changeType, "attendance", record.getId()));
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long clockOut(ClockInDTO dto) {
        if (dto.getUserId() == null) throw new IllegalArgumentException("员工ID不能为空");
        if (dto.getClockTime() == null) throw new IllegalArgumentException("打卡时间不能为空");
        LocalDate today = dto.getClockTime().toLocalDate();
        AttendanceRecord record = getOrCreateRecord(dto.getUserId(), today);
        int status = dto.getClockTime().toLocalTime().isBefore(WORK_END_TIME) ? CLOCK_OUT_EARLY : CLOCK_OUT_NORMAL;
        record.setClockOutTime(dto.getClockTime());
        record.setClockOutLocation(dto.getLocation());
        record.setClockOutStatus(status);
        String changeType = record.getId() == null ? "create" : "update";
        if (record.getId() == null) attendanceRecordMapper.insert(record);
        else attendanceRecordMapper.updateById(record);
        eventPublisher.publishEvent(new DataChangeEvent(this, changeType, "attendance", record.getId()));
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitLeaveRequest(LeaveRequestDTO dto) {
        if (dto.getUserId() == null) throw new IllegalArgumentException("申请人ID不能为空");
        if (dto.getLeaveType() == null) throw new IllegalArgumentException("请假类型不能为空");
        if (dto.getStartTime() == null || dto.getEndTime() == null) throw new IllegalArgumentException("起止时间不能为空");
        if (!dto.getStartTime().isBefore(dto.getEndTime())) throw new IllegalArgumentException("开始时间必须早于结束时间");
        LeaveRequest request = LeaveRequest.builder()
                .userId(dto.getUserId())
                .leaveType(dto.getLeaveType())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .reason(dto.getReason())
                .status(1)
                .build();
        leaveRequestMapper.insert(request);
        eventPublisher.publishEvent(new DataChangeEvent(this, "create", "attendance", request.getId()));
        return request.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitCompensateRequest(Long userId, LocalDate compensateDate, String reason) {
        if (userId == null) throw new IllegalArgumentException("申请人ID不能为空");
        if (compensateDate == null) throw new IllegalArgumentException("调休日期不能为空");
        CompensateRequest request = CompensateRequest.builder()
                .userId(userId)
                .compensateDate(compensateDate)
                .reason(reason)
                .status(1)
                .build();
        compensateRequestMapper.insert(request);
        eventPublisher.publishEvent(new DataChangeEvent(this, "create", "attendance", request.getId()));
        return request.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long signIn(SignInDTO dto) {
        if (dto.getUserId() == null) throw new IllegalArgumentException("员工ID不能为空");
        if (dto.getSignInTime() == null) throw new IllegalArgumentException("签到时间不能为空");
        SignInRecord record = SignInRecord.builder()
                .userId(dto.getUserId())
                .signInTime(dto.getSignInTime())
                .location(dto.getLocation())
                .remark(dto.getRemark())
                .build();
        signInRecordMapper.insert(record);
        eventPublisher.publishEvent(new DataChangeEvent(this, "create", "attendance", record.getId()));
        return record.getId();
    }

    @Override
    public AttendanceMonthlyVO getMonthlySummary(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceRecord::getUserId, userId)
                .ge(AttendanceRecord::getAttendanceDate, start)
                .le(AttendanceRecord::getAttendanceDate, end);
        List<AttendanceRecord> records = attendanceRecordMapper.selectList(wrapper);
        int attendanceDays = 0, lateTimes = 0, earlyLeaveTimes = 0;
        for (AttendanceRecord r : records) {
            if (r.getClockInTime() != null || r.getClockOutTime() != null) attendanceDays++;
            if (r.getClockInStatus() != null && r.getClockInStatus() == CLOCK_IN_LATE) lateTimes++;
            if (r.getClockOutStatus() != null && r.getClockOutStatus() == CLOCK_OUT_EARLY) earlyLeaveTimes++;
        }
        LambdaQueryWrapper<LeaveRequest> leaveWrapper = new LambdaQueryWrapper<>();
        leaveWrapper.eq(LeaveRequest::getUserId, userId)
                .eq(LeaveRequest::getStatus, 2)
                .le(LeaveRequest::getStartTime, end.atTime(LocalTime.MAX))
                .ge(LeaveRequest::getEndTime, start.atStartOfDay());
        List<LeaveRequest> leaves = leaveRequestMapper.selectList(leaveWrapper);
        int leaveDays = leaves.size();
        return AttendanceMonthlyVO.builder()
                .userId(userId)
                .year(year)
                .month(month)
                .attendanceDays(attendanceDays)
                .lateTimes(lateTimes)
                .earlyLeaveTimes(earlyLeaveTimes)
                .leaveDays(leaveDays)
                .overtimeHours(0.0)
                .build();
    }

    @Override
    public List<AttendanceRecord> listAttendanceRecords(Long userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(AttendanceRecord::getUserId, userId);
        if (startDate != null) wrapper.ge(AttendanceRecord::getAttendanceDate, startDate);
        if (endDate != null) wrapper.le(AttendanceRecord::getAttendanceDate, endDate);
        wrapper.orderByDesc(AttendanceRecord::getAttendanceDate);
        return attendanceRecordMapper.selectList(wrapper);
    }

    @Override
    public List<LeaveRequest> listLeaveRequests(Long userId, Integer status) {
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(LeaveRequest::getUserId, userId);
        if (status != null) wrapper.eq(LeaveRequest::getStatus, status);
        wrapper.orderByDesc(LeaveRequest::getCreateTime);
        return leaveRequestMapper.selectList(wrapper);
    }

    @Override
    public List<CompensateRequest> listCompensateRequests(Long userId, Integer status) {
        LambdaQueryWrapper<CompensateRequest> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(CompensateRequest::getUserId, userId);
        if (status != null) wrapper.eq(CompensateRequest::getStatus, status);
        wrapper.orderByDesc(CompensateRequest::getCreateTime);
        return compensateRequestMapper.selectList(wrapper);
    }

    @Override
    public int determineClockInStatus(LocalTime clockInTime) {
        return clockInTime.isAfter(WORK_START_TIME) ? CLOCK_IN_LATE : CLOCK_IN_NORMAL;
    }

    @Override
    public int determineClockOutStatus(LocalTime clockOutTime) {
        return clockOutTime.isBefore(WORK_END_TIME) ? CLOCK_OUT_EARLY : CLOCK_OUT_NORMAL;
    }

    private AttendanceRecord getOrCreateRecord(Long userId, LocalDate date) {
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceRecord::getUserId, userId).eq(AttendanceRecord::getAttendanceDate, date);
        AttendanceRecord record = attendanceRecordMapper.selectOne(wrapper);
        if (record == null) {
            record = AttendanceRecord.builder().userId(userId).attendanceDate(date).build();
        }
        return record;
    }
}
