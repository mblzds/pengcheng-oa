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
import java.time.LocalDateTime;
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
        // 拒绝时段重叠：当前用户已有 PENDING/APPROVED 的请假在 [start,end) 内 → 必须先撤销原单
        // REJECTED/CANCELLED 单子允许重新提交（不阻塞）
        List<LeaveRequest> overlapping = leaveRequestMapper.selectList(
                new LambdaQueryWrapper<LeaveRequest>()
                        .eq(LeaveRequest::getUserId, dto.getUserId())
                        .in(LeaveRequest::getStatus,
                                ApprovalConstants.STATUS_PENDING,
                                ApprovalConstants.STATUS_APPROVED)
                        // (a.start < b.end) AND (a.end > b.start) 表示两个区间重叠
                        .lt(LeaveRequest::getStartTime, dto.getEndTime())
                        .gt(LeaveRequest::getEndTime, dto.getStartTime()));
        if (!overlapping.isEmpty()) {
            LeaveRequest first = overlapping.get(0);
            String statusLabel = Integer.valueOf(ApprovalConstants.STATUS_PENDING).equals(first.getStatus())
                    ? "待审批" : "已通过";
            throw new IllegalArgumentException(
                    "该时段已存在请假申请（单号 " + first.getId() + "，状态：" + statusLabel + "），请先撤销原申请");
        }
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
        // 拒绝同日重复：同一用户该日已有 PENDING/APPROVED 的调休 → 必须先撤销
        List<CompensateRequest> overlapping = compensateRequestMapper.selectList(
                new LambdaQueryWrapper<CompensateRequest>()
                        .eq(CompensateRequest::getUserId, userId)
                        .eq(CompensateRequest::getCompensateDate, compensateDate)
                        .in(CompensateRequest::getStatus,
                                ApprovalConstants.STATUS_PENDING,
                                ApprovalConstants.STATUS_APPROVED));
        if (!overlapping.isEmpty()) {
            CompensateRequest first = overlapping.get(0);
            String statusLabel = Integer.valueOf(ApprovalConstants.STATUS_PENDING).equals(first.getStatus())
                    ? "待审批" : "已通过";
            throw new IllegalArgumentException(
                    "该日期已存在调休申请（单号 " + first.getId() + "，状态：" + statusLabel + "），请先撤销原申请");
        }
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
            // 当天有 exempt_reason（请假/调休审批通过后由 AttendanceExemptListener 写入）
            // → 整条记录跳过所有打卡相关 counter：那天本质属于请假/调休，归 leave/compensate
            // bucket 处理；absentDays = expectedWorkdays - attendance - leave - compensate 自然
            // 在 expected 里把这天扣给 leave，不应再让它给 attendanceDays +1（否则
            // "请假 + 迟到打卡" 那天既算出勤又算请假，挤掉真正缺勤天的份额）
            if (r.getExemptReason() != null && !r.getExemptReason().isBlank()) {
                continue;
            }
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
        // 先把所有请假时段截到本月窗口里，再做并集合并（sweep line）—— 防止
        // 同一天重复提单（即便提交端已有重叠拦截，历史脏数据也可能存在）导致天数加倍。
        // 合并后用不相交时段集累加 calcDays。
        List<java.time.LocalDateTime[]> intervals = new java.util.ArrayList<>();
        for (LeaveRequest l : approvedLeaves) {
            java.time.LocalDateTime s = l.getStartTime().isBefore(start.atStartOfDay()) ? start.atStartOfDay() : l.getStartTime();
            java.time.LocalDateTime e = l.getEndTime().isAfter(end.atTime(LocalTime.MAX)) ? end.atTime(LocalTime.MAX) : l.getEndTime();
            if (s.isBefore(e)) intervals.add(new java.time.LocalDateTime[]{s, e});
        }
        intervals.sort((a, b) -> a[0].compareTo(b[0]));
        List<java.time.LocalDateTime[]> merged = new java.util.ArrayList<>();
        for (java.time.LocalDateTime[] iv : intervals) {
            if (!merged.isEmpty() && !iv[0].isAfter(merged.get(merged.size() - 1)[1])) {
                // 端点重叠或交叠 → 合并：扩展尾端到 max(当前尾, 新尾)
                java.time.LocalDateTime[] last = merged.get(merged.size() - 1);
                if (iv[1].isAfter(last[1])) last[1] = iv[1];
            } else {
                merged.add(new java.time.LocalDateTime[]{iv[0], iv[1]});
            }
        }
        double leaveDays = 0;
        // 收集本月每一天是否被请假覆盖；前端日历用这个清单把请假天的格子渲染成"请假"而不是"缺勤"
        java.util.Set<String> leaveDateSet = new java.util.LinkedHashSet<>();
        for (java.time.LocalDateTime[] iv : merged) {
            Double d = LeaveDaysUtil.calcDays(iv[0], iv[1], workStart, workEnd);
            if (d != null) leaveDays += d;
            LocalDate cur = iv[0].toLocalDate();
            LocalDate endDay = iv[1].toLocalDate();
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
    public List<AttendanceMonthlyVO> getMonthlySummaryBatch(Set<Long> userIds, int year, int month) {
        Set<Long> targetIds;
        if (userIds == null) {
            // 全员：所有 status=1 的启用用户
            targetIds = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1))
                    .stream().map(SysUser::getId).collect(Collectors.toSet());
        } else if (userIds.isEmpty()) {
            return Collections.emptyList();
        } else {
            targetIds = userIds;
        }
        if (targetIds.isEmpty()) return Collections.emptyList();

        // 一次性把 nickname + deptName 缓存好，避免 N+1
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().in(SysUser::getId, targetIds));
        Map<Long, String> deptNameMap = sysDeptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));

        // 逐人调单人汇总，回填 nickname / deptName
        // N 增大后可优化为批量 SQL；当前公司规模够用
        List<AttendanceMonthlyVO> result = new java.util.ArrayList<>(users.size());
        for (SysUser u : users) {
            AttendanceMonthlyVO vo = getMonthlySummary(u.getId(), year, month);
            vo.setNickname(u.getNickname());
            vo.setDeptName(u.getDeptId() != null ? deptNameMap.get(u.getDeptId()) : null);
            result.add(vo);
        }
        // 按部门 + 用户 id 稳定排序，方便前端表格展示
        result.sort((a, b) -> {
            String da = a.getDeptName() == null ? "" : a.getDeptName();
            String db = b.getDeptName() == null ? "" : b.getDeptName();
            int c = da.compareTo(db);
            if (c != 0) return c;
            return Long.compare(a.getUserId(), b.getUserId());
        });
        return result;
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
    public List<AttendanceRecord> listAttendanceRecordsWithAbsent(Long userId, Set<Long> allowedUserIds,
                                                                  LocalDate startDate, LocalDate endDate) {
        List<AttendanceRecord> existing = listAttendanceRecords(userId, allowedUserIds, startDate, endDate);
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return existing;
        }
        LocalDate today = LocalDate.now();
        // 今天的下班时刻未过 → 今天不补缺勤行（员工可能还没到岗或仍在岗中）；
        // 未来日期同样不算缺勤。最远可补到 latestAbsentDate。
        LocalDate latestAbsentDate = LocalDateTime.now().isBefore(LocalDateTime.of(today, workEndTime()))
                ? today.minusDays(1)
                : today;
        LocalDate effectiveEnd = latestAbsentDate.isBefore(endDate) ? latestAbsentDate : endDate;
        if (effectiveEnd.isBefore(startDate)) {
            return existing;
        }

        // 目标用户集合：单人 → 该 userId；多人 → allowedUserIds，null 时退化为全员启用账号
        List<Long> targetUserIds;
        if (userId != null) {
            targetUserIds = java.util.List.of(userId);
        } else if (allowedUserIds != null) {
            if (allowedUserIds.isEmpty()) return existing;
            targetUserIds = new java.util.ArrayList<>(allowedUserIds);
        } else {
            // admin / HR 全员可见：拉所有启用用户
            targetUserIds = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1)
            ).stream().map(SysUser::getId).collect(Collectors.toList());
            if (targetUserIds.isEmpty()) return existing;
        }

        // 防爆量：多人视图限制总单元格规模（用户数 × 天数）
        if (userId == null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, effectiveEnd) + 1;
            long capacity = (long) targetUserIds.size() * days;
            final long MAX_CELLS = 1000L;
            if (capacity > MAX_CELLS) {
                return existing;
            }
        }

        // 系统启动日期（兜底 cutoff），员工 profile 中的入职日期优先
        LocalDate systemStart = systemConfigHelper.getAttendanceStartDate();

        // 批量预加载，避免 N+1
        List<EmployeeProfile> profiles = employeeProfileMapper.selectList(
                new LambdaQueryWrapper<EmployeeProfile>().in(EmployeeProfile::getUserId, targetUserIds)
        );
        Map<Long, EmployeeProfile> profileByUser = profiles.stream()
                .collect(Collectors.toMap(EmployeeProfile::getUserId, p -> p, (a, b) -> a));
        List<SysUser> users = sysUserMapper.selectBatchIds(targetUserIds);
        Map<Long, SysUser> userById = users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        Set<Long> deptIds = users.stream().map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> deptNameById = deptIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : sysDeptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));

        // 已有 (userId, date) 记录的集合，跳过补全
        Set<String> existingKeys = existing.stream()
                .filter(r -> r.getUserId() != null && r.getAttendanceDate() != null)
                .map(r -> r.getUserId() + "|" + r.getAttendanceDate())
                .collect(Collectors.toSet());

        List<AttendanceRecord> absentRows = new java.util.ArrayList<>();
        for (Long uid : targetUserIds) {
            EmployeeProfile profile = profileByUser.get(uid);
            LocalDate cutoff = (profile != null && profile.getJoinDate() != null) ? profile.getJoinDate() : systemStart;
            LocalDate effectiveStart = (cutoff != null && cutoff.isAfter(startDate)) ? cutoff : startDate;
            if (effectiveStart.isAfter(effectiveEnd)) continue;
            SysUser u = userById.get(uid);
            String userName = u != null ? u.getNickname() : null;
            String employeeNo = profile != null ? profile.getEmployeeNo() : null;
            String deptName = (u != null && u.getDeptId() != null) ? deptNameById.get(u.getDeptId()) : null;
            for (LocalDate d = effectiveStart; !d.isAfter(effectiveEnd); d = d.plusDays(1)) {
                if (existingKeys.contains(uid + "|" + d)) continue;
                if (!holidayCalendarService.isWorkday(d)) continue;
                AttendanceRecord absent = AttendanceRecord.builder()
                        .userId(uid)
                        .attendanceDate(d)
                        .build();
                absent.setUserName(userName);
                absent.setEmployeeNo(employeeNo);
                absent.setDeptName(deptName);
                absentRows.add(absent);
            }
        }
        if (absentRows.isEmpty()) {
            return existing;
        }
        // 合并并按日期倒序（与原列表口径一致）
        List<AttendanceRecord> merged = new java.util.ArrayList<>(existing.size() + absentRows.size());
        merged.addAll(existing);
        merged.addAll(absentRows);
        merged.sort((a, b) -> {
            LocalDate da = a.getAttendanceDate();
            LocalDate db = b.getAttendanceDate();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });
        return merged;
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
        enrichCurrentNodeName(list, ApprovalConstants.BUSINESS_TYPE_LEAVE,
                LeaveRequest::getId, LeaveRequest::setCurrentNodeName);
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
        enrichCurrentNodeName(list, ApprovalConstants.BUSINESS_TYPE_COMPENSATE,
                CompensateRequest::getId, CompensateRequest::setCurrentNodeName);
        return list;
    }

    /**
     * 批量回填当前活跃节点名。终态业务单（已通过/驳回/撤销）无活跃节点，map 无对应 key，
     * 此时 setter 写入 null，前端按需展示占位符。
     */
    private <T> void enrichCurrentNodeName(List<T> list,
                                           String businessType,
                                           Function<T, Long> idGetter,
                                           BiConsumer<T, String> setter) {
        if (list == null || list.isEmpty()) return;
        List<Long> ids = list.stream()
                .map(idGetter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<Long, String> nodeNameMap = approvalFlowService.getCurrentNodeNames(businessType, ids);
        for (T row : list) {
            Long id = idGetter.apply(row);
            if (id != null) setter.accept(row, nodeNameMap.get(id));
        }
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
