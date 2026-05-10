package com.pengcheng.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.pengcheng.common.event.DataChangeEvent;
import com.pengcheng.hr.approval.constant.ApprovalConstants;
import com.pengcheng.hr.approval.service.ApprovalFlowService;
import com.pengcheng.hr.attendance.dto.*;
import com.pengcheng.hr.attendance.entity.AttendanceRecord;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.HolidayCalendar;
import com.pengcheng.hr.attendance.entity.LeaveRequest;
import com.pengcheng.hr.attendance.entity.SignInRecord;
import com.pengcheng.hr.attendance.mapper.AttendanceRecordMapper;
import com.pengcheng.hr.attendance.mapper.CompensateRequestMapper;
import com.pengcheng.hr.attendance.mapper.LeaveRequestMapper;
import com.pengcheng.hr.attendance.mapper.SignInRecordMapper;
import com.pengcheng.hr.attendance.service.AttendanceService;
import com.pengcheng.hr.attendance.service.HolidayCalendarService;
import com.pengcheng.hr.attendance.util.LeaveDaysUtil;
import com.pengcheng.hr.employee.entity.EmployeeProfile;
import com.pengcheng.hr.employee.mapper.EmployeeProfileMapper;
import com.pengcheng.system.entity.SysDept;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.helper.SystemConfigHelper;
import com.pengcheng.system.mapper.SysDeptMapper;
import com.pengcheng.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 考勤/请假/调休/签到服务实现（公司级假勤）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final SignInRecordMapper signInRecordMapper;
    private final CompensateRequestMapper compensateRequestMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ApprovalFlowService approvalFlowService;
    private final SystemConfigHelper systemConfigHelper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final EmployeeProfileMapper employeeProfileMapper;
    private final HolidayCalendarService holidayCalendarService;

    public static final int CLOCK_IN_NORMAL = 1;
    public static final int CLOCK_IN_LATE = 2;
    public static final int CLOCK_OUT_NORMAL = 1;
    public static final int CLOCK_OUT_EARLY = 2;
    public static final int LOCATION_COMPLIANT = 1;
    public static final int LOCATION_OUT_OF_RANGE = 2;
    /** 默认上下班时间，仅在配置缺失时兜底 */
    public static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    public static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long clockIn(ClockInDTO dto) {
        if (dto.getUserId() == null) throw new IllegalArgumentException("员工ID不能为空");
        if (dto.getClockTime() == null) throw new IllegalArgumentException("打卡时间不能为空");
        // 位置硬校验：开启位置校验且不在合规范围 → 直接拒绝，时间不入库
        Integer locationStatus = checkLocation(dto.getLatitude(), dto.getLongitude());
        if (locationStatus != null && locationStatus == LOCATION_OUT_OF_RANGE) {
            throw new IllegalArgumentException("不在允许打卡范围，无法打卡");
        }
        LocalDate today = dto.getClockTime().toLocalDate();
        AttendanceRecord record = getOrCreateRecord(dto.getUserId(), today);
        record.setClockInTime(dto.getClockTime());
        record.setClockInLocation(dto.getLocation());
        record.setClockInPhoto(dto.getPhotoUrl());
        record.setClockInStatus(determineClockInStatus(dto.getClockTime().toLocalTime()));
        record.setLocationStatus(locationStatus);
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
        // 位置硬校验：开启位置校验且不在合规范围 → 直接拒绝，时间不入库
        Integer locationStatus = checkLocation(dto.getLatitude(), dto.getLongitude());
        if (locationStatus != null && locationStatus == LOCATION_OUT_OF_RANGE) {
            throw new IllegalArgumentException("不在允许打卡范围，无法打卡");
        }
        LocalDate today = dto.getClockTime().toLocalDate();
        AttendanceRecord record = getOrCreateRecord(dto.getUserId(), today);
        record.setClockOutTime(dto.getClockTime());
        record.setClockOutLocation(dto.getLocation());
        record.setClockOutPhoto(dto.getPhotoUrl());
        record.setClockOutStatus(determineClockOutStatus(dto.getClockTime().toLocalTime()));
        if (locationStatus != null) {
            record.setLocationStatus(locationStatus);
        }
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
        approvalFlowService.start(ApprovalConstants.BUSINESS_TYPE_LEAVE, request.getId(), request.getUserId());
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
        approvalFlowService.start(ApprovalConstants.BUSINESS_TYPE_COMPENSATE, request.getId(), userId);
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
        LocalDate today = LocalDate.now();
        LocalDate scopeEnd = today.isAfter(end) ? end : (today.isBefore(start) ? start.minusDays(1) : today);

        // 考勤起算日：优先员工 joinDate，缺失则回退到系统级"考勤启用日期"，再缺失则不设
        // 之前的日期不算应当出勤、不算缺勤——刚上线/新入职都不要被历史空白天数误伤
        LocalDate cutoff = null;
        EmployeeProfile profile = employeeProfileMapper.selectOne(new LambdaQueryWrapper<EmployeeProfile>()
                .eq(EmployeeProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile != null && profile.getJoinDate() != null) {
            cutoff = profile.getJoinDate();
        } else {
            cutoff = systemConfigHelper.getAttendanceStartDate();
        }
        // 应当出勤的起点 = max(月初, cutoff)；若 cutoff 在本月之内则 expectedWorkdays 应该跳过 cutoff 之前的日子
        LocalDate effectiveStart = (cutoff != null && cutoff.isAfter(start)) ? cutoff : start;

        // attendance_record 的 attendanceDays / lateTimes / earlyLeaveTimes
        // attendanceDays 仅计「应当出勤的工作日」上的打卡——周末/节假日加班打卡不抵缺勤
        List<AttendanceRecord> records = attendanceRecordMapper.selectList(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getUserId, userId)
                .ge(AttendanceRecord::getAttendanceDate, start)
                .le(AttendanceRecord::getAttendanceDate, end));
        int attendanceDays = 0, lateTimes = 0, earlyLeaveTimes = 0;
        for (AttendanceRecord r : records) {
            boolean isOnWorkday = r.getAttendanceDate() != null && holidayCalendarService.isWorkday(r.getAttendanceDate());
            if (isOnWorkday && (r.getClockInTime() != null || r.getClockOutTime() != null)) attendanceDays++;
            if (r.getClockInStatus() != null && r.getClockInStatus() == CLOCK_IN_LATE) lateTimes++;
            if (r.getClockOutStatus() != null && r.getClockOutStatus() == CLOCK_OUT_EARLY) earlyLeaveTimes++;
        }

        // 已通过请假覆盖到本月的真实天数（按上下班时段算，区别于历史"条数"）
        LocalTime workStart = parseHHmmOrDefault(systemConfigHelper.getAttendanceWorkStartTime(), WORK_START_TIME);
        LocalTime workEnd = parseHHmmOrDefault(systemConfigHelper.getAttendanceWorkEndTime(), WORK_END_TIME);
        List<LeaveRequest> approvedLeaves = leaveRequestMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getUserId, userId)
                .eq(LeaveRequest::getStatus, ApprovalConstants.STATUS_APPROVED)
                .le(LeaveRequest::getStartTime, end.atTime(LocalTime.MAX))
                .ge(LeaveRequest::getEndTime, start.atStartOfDay()));
        double leaveDays = 0;
        // 收集本月每一天是否被请假覆盖；前端日历用这个清单把请假天的格子渲染成"请假"而不是"缺勤"
        java.util.Set<String> leaveDateSet = new java.util.LinkedHashSet<>();
        for (LeaveRequest l : approvedLeaves) {
            // 截到本月窗口里再算天数，跨月请假只计本月那段
            java.time.LocalDateTime s = l.getStartTime().isBefore(start.atStartOfDay()) ? start.atStartOfDay() : l.getStartTime();
            java.time.LocalDateTime e = l.getEndTime().isAfter(end.atTime(LocalTime.MAX)) ? end.atTime(LocalTime.MAX) : l.getEndTime();
            Double d = LeaveDaysUtil.calcDays(s, e, workStart, workEnd);
            if (d != null) leaveDays += d;
            // 把覆盖到的每一个本月日期加入集合（即使是当日的半天请假也算这天被覆盖）
            LocalDate cur = s.toLocalDate();
            LocalDate endDay = e.toLocalDate();
            while (!cur.isAfter(endDay)) {
                if (!cur.isBefore(start) && !cur.isAfter(end)) {
                    leaveDateSet.add(cur.toString());
                }
                cur = cur.plusDays(1);
            }
        }

        // 已通过调休覆盖到本月、且已经过去的天数（compensate 按整天计；未来日不能提前抵缺勤）
        LocalDate compensateScopeEnd = scopeEnd.isBefore(start) ? start.minusDays(1) : scopeEnd;
        List<CompensateRequest> approvedCompensates = compensateRequestMapper.selectList(new LambdaQueryWrapper<CompensateRequest>()
                .eq(CompensateRequest::getUserId, userId)
                .eq(CompensateRequest::getStatus, ApprovalConstants.STATUS_APPROVED)
                .ge(CompensateRequest::getCompensateDate, start)
                .le(CompensateRequest::getCompensateDate, compensateScopeEnd));
        java.util.Set<String> compensateDateSet = new java.util.LinkedHashSet<>();
        for (CompensateRequest c : approvedCompensates) {
            if (c.getCompensateDate() != null) compensateDateSet.add(c.getCompensateDate().toString());
        }
        long compensateDays = approvedCompensates.size();

        // 应当出勤的工作日：从 max(月初, cutoff) 到 min(月末, 今日) 之间的工作日
        // 当 effectiveStart 已越过 scopeEnd（cutoff 在未来 / 整月在 cutoff 之前），expectedWorkdays = 0
        int expectedWorkdays = (scopeEnd.isBefore(effectiveStart)) ? 0 : holidayCalendarService.countWorkdays(effectiveStart, scopeEnd);

        int leaveDaysRounded = (int) Math.round(leaveDays);
        int compensateDaysInt = (int) compensateDays;
        int absentDays = Math.max(expectedWorkdays - attendanceDays - leaveDaysRounded - compensateDaysInt, 0);

        // 把本月内的法定节假日 + 调休补班按 YYYY-MM-DD 字符串列出，方便小程序日历视图就地判定
        List<HolidayCalendar> yearItems = holidayCalendarService.listByYear(year);
        List<String> holidays = new java.util.ArrayList<>();
        List<String> makeupWorkdays = new java.util.ArrayList<>();
        for (HolidayCalendar h : yearItems) {
            if (h.getHolidayDate() == null) continue;
            if (h.getHolidayDate().isBefore(start) || h.getHolidayDate().isAfter(end)) continue;
            String iso = h.getHolidayDate().toString();
            if (h.getType() != null && h.getType() == HolidayCalendar.TYPE_HOLIDAY) holidays.add(iso);
            else if (h.getType() != null && h.getType() == HolidayCalendar.TYPE_MAKEUP_WORKDAY) makeupWorkdays.add(iso);
        }

        return AttendanceMonthlyVO.builder()
                .userId(userId)
                .year(year)
                .month(month)
                .attendanceDays(attendanceDays)
                .lateTimes(lateTimes)
                .earlyLeaveTimes(earlyLeaveTimes)
                .leaveDays(leaveDaysRounded)
                .compensateDays(compensateDaysInt)
                .expectedWorkdays(expectedWorkdays)
                .absentDays(absentDays)
                .holidays(holidays)
                .makeupWorkdays(makeupWorkdays)
                .leaveDates(new java.util.ArrayList<>(leaveDateSet))
                .compensateDates(new java.util.ArrayList<>(compensateDateSet))
                .cutoffDate(cutoff != null ? cutoff.toString() : null)
                .overtimeHours(0.0)
                .build();
    }

    private static LocalTime parseHHmmOrDefault(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public List<AttendanceRecord> listAttendanceRecords(Long userId, LocalDate startDate, LocalDate endDate) {
        return listAttendanceRecords(userId, null, startDate, endDate);
    }

    @Override
    public List<AttendanceRecord> listAttendanceRecords(Long userId, Set<Long> allowedUserIds, LocalDate startDate, LocalDate endDate) {
        if (allowedUserIds != null && allowedUserIds.isEmpty()) return Collections.emptyList();
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(AttendanceRecord::getUserId, userId);
        } else if (allowedUserIds != null) {
            wrapper.in(AttendanceRecord::getUserId, allowedUserIds);
        }
        if (startDate != null) wrapper.ge(AttendanceRecord::getAttendanceDate, startDate);
        if (endDate != null) wrapper.le(AttendanceRecord::getAttendanceDate, endDate);
        wrapper.orderByDesc(AttendanceRecord::getAttendanceDate);
        List<AttendanceRecord> list = attendanceRecordMapper.selectList(wrapper);
        enrichEmployeeInfo(list, AttendanceRecord::getUserId, (r, info) -> {
            r.setUserName(info.userName);
            r.setEmployeeNo(info.employeeNo);
            r.setDeptName(info.deptName);
        });
        return list;
    }

    @Override
    public List<LeaveRequest> listLeaveRequests(Long userId, Integer status) {
        return listLeaveRequests(userId, null, status);
    }

    @Override
    public List<LeaveRequest> listLeaveRequests(Long userId, Set<Long> allowedUserIds, Integer status) {
        if (allowedUserIds != null && allowedUserIds.isEmpty()) return Collections.emptyList();
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(LeaveRequest::getUserId, userId);
        } else if (allowedUserIds != null) {
            wrapper.in(LeaveRequest::getUserId, allowedUserIds);
        }
        if (status != null) wrapper.eq(LeaveRequest::getStatus, status);
        wrapper.orderByDesc(LeaveRequest::getCreateTime);
        List<LeaveRequest> list = leaveRequestMapper.selectList(wrapper);
        enrichEmployeeInfo(list, LeaveRequest::getUserId, (r, info) -> {
            r.setUserName(info.userName);
            r.setEmployeeNo(info.employeeNo);
            r.setDeptName(info.deptName);
        });
        return list;
    }

    @Override
    public List<CompensateRequest> listCompensateRequests(Long userId, Integer status) {
        return listCompensateRequests(userId, null, status);
    }

    @Override
    public List<CompensateRequest> listCompensateRequests(Long userId, Set<Long> allowedUserIds, Integer status) {
        if (allowedUserIds != null && allowedUserIds.isEmpty()) return Collections.emptyList();
        LambdaQueryWrapper<CompensateRequest> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(CompensateRequest::getUserId, userId);
        } else if (allowedUserIds != null) {
            wrapper.in(CompensateRequest::getUserId, allowedUserIds);
        }
        if (status != null) wrapper.eq(CompensateRequest::getStatus, status);
        wrapper.orderByDesc(CompensateRequest::getCreateTime);
        List<CompensateRequest> list = compensateRequestMapper.selectList(wrapper);
        enrichEmployeeInfo(list, CompensateRequest::getUserId, (r, info) -> {
            r.setUserName(info.userName);
            r.setEmployeeNo(info.employeeNo);
            r.setDeptName(info.deptName);
        });
        return list;
    }

    /**
     * 批量回填姓名 / 工号 / 部门名。三表查询：sys_user、sys_dept、hr_employee_profile。
     */
    private <T> void enrichEmployeeInfo(List<T> list,
                                        Function<T, Long> userIdGetter,
                                        BiConsumer<T, EmployeeInfo> applier) {
        if (list == null || list.isEmpty()) return;
        Set<Long> userIds = list.stream()
                .map(userIdGetter)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (userIds.isEmpty()) return;

        Map<Long, SysUser> userMap = batchSelectByIds(sysUserMapper.selectBatchIds(userIds), SysUser::getId);
        Set<Long> deptIds = userMap.values().stream()
                .map(SysUser::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> deptNameMap = deptIds.isEmpty() ? Collections.emptyMap()
                : sysDeptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));

        LambdaQueryWrapper<EmployeeProfile> profileWrapper = new LambdaQueryWrapper<>();
        profileWrapper.in(EmployeeProfile::getUserId, userIds);
        Map<Long, String> employeeNoMap = employeeProfileMapper.selectList(profileWrapper).stream()
                .filter(p -> p.getUserId() != null && p.getEmployeeNo() != null)
                .collect(Collectors.toMap(EmployeeProfile::getUserId, EmployeeProfile::getEmployeeNo, (a, b) -> a));

        for (T row : list) {
            Long uid = userIdGetter.apply(row);
            if (uid == null) continue;
            SysUser user = userMap.get(uid);
            EmployeeInfo info = new EmployeeInfo();
            if (user != null) {
                info.userName = user.getNickname();
                info.deptName = user.getDeptId() == null ? null : deptNameMap.get(user.getDeptId());
            }
            info.employeeNo = employeeNoMap.get(uid);
            applier.accept(row, info);
        }
    }

    private static <K, V> Map<K, V> batchSelectByIds(Collection<V> rows, Function<V, K> keyGetter) {
        if (rows == null || rows.isEmpty()) return Collections.emptyMap();
        Map<K, V> map = new HashMap<>(rows.size());
        for (V row : rows) {
            K key = keyGetter.apply(row);
            if (key != null) map.put(key, row);
        }
        return map;
    }

    private static class EmployeeInfo {
        String userName;
        String employeeNo;
        String deptName;
    }

    @Override
    public int determineClockInStatus(LocalTime clockInTime) {
        if (!systemConfigHelper.isAttendanceEnforceTime()) return CLOCK_IN_NORMAL;
        return clockInTime.isAfter(workStartTime()) ? CLOCK_IN_LATE : CLOCK_IN_NORMAL;
    }

    @Override
    public int determineClockOutStatus(LocalTime clockOutTime) {
        if (!systemConfigHelper.isAttendanceEnforceTime()) return CLOCK_OUT_NORMAL;
        return clockOutTime.isBefore(workEndTime()) ? CLOCK_OUT_EARLY : CLOCK_OUT_NORMAL;
    }

    private LocalTime workStartTime() {
        return parseHHmm(systemConfigHelper.getAttendanceWorkStartTime(), WORK_START_TIME);
    }

    private LocalTime workEndTime() {
        return parseHHmm(systemConfigHelper.getAttendanceWorkEndTime(), WORK_END_TIME);
    }

    private LocalTime parseHHmm(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return LocalTime.parse(value, HHMM);
        } catch (Exception e) {
            log.warn("考勤时间配置 {} 解析失败，使用默认值 {}", value, fallback);
            return fallback;
        }
    }

    /**
     * 与所有合规位置比对，返回最近距离的合规判定。
     * 返回 null 表示未启用位置校验、或本次打卡未提供经纬度（视为不可判定）。
     */
    Integer checkLocation(Double lat, Double lng) {
        if (!systemConfigHelper.isAttendanceEnforceLocation()) return null;
        if (lat == null || lng == null) return LOCATION_OUT_OF_RANGE;
        JsonNode locations = systemConfigHelper.getAttendanceAllowedLocations();
        if (locations == null || !locations.isArray() || locations.size() == 0) {
            return LOCATION_OUT_OF_RANGE;
        }
        for (JsonNode loc : locations) {
            JsonNode latNode = loc.get("lat");
            JsonNode lngNode = loc.get("lng");
            JsonNode radiusNode = loc.get("radius");
            if (latNode == null || lngNode == null || radiusNode == null) continue;
            double distance = haversineMeters(lat, lng, latNode.asDouble(), lngNode.asDouble());
            if (distance <= radiusNode.asDouble()) return LOCATION_COMPLIANT;
        }
        return LOCATION_OUT_OF_RANGE;
    }

    private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
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
