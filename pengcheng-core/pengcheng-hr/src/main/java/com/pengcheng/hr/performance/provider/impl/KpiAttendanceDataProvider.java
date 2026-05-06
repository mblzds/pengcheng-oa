package com.pengcheng.hr.performance.provider.impl;

import com.pengcheng.hr.attendance.dto.AttendanceMonthlyVO;
import com.pengcheng.hr.attendance.service.AttendanceService;
import com.pengcheng.hr.performance.provider.KpiDataSourceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 考勤数据源：按周期内月份汇总出勤天数/迟到早退等，供 auto_attendance 指标建议值
 */
@Component
@RequiredArgsConstructor
public class KpiAttendanceDataProvider implements KpiDataSourceProvider {

    public static final String DATA_SOURCE = "auto_attendance";

    private final AttendanceService attendanceService;

    @Override
    public String getDataSource() {
        return DATA_SOURCE;
    }

    @Override
    public BigDecimal getActualValue(LocalDate startDate, LocalDate endDate, Long userId, String templateCode) {
        if (userId == null || startDate == null || endDate == null) return null;
        List<AttendanceMonthlyVO> summaries = new ArrayList<>();
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            AttendanceMonthlyVO vo = attendanceService.getMonthlySummary(userId, ym.getYear(), ym.getMonthValue());
            if (vo != null) summaries.add(vo);
        }
        if (summaries.isEmpty()) return null;
        // 按指标编码区分：attendance_days / attendance_rate / late_times 等
        if ("late_times".equalsIgnoreCase(templateCode)) {
            int total = summaries.stream().mapToInt(v -> v.getLateTimes() != null ? v.getLateTimes() : 0).sum();
            return BigDecimal.valueOf(total);
        }
        if ("early_leave_times".equalsIgnoreCase(templateCode)) {
            int total = summaries.stream().mapToInt(v -> v.getEarlyLeaveTimes() != null ? v.getEarlyLeaveTimes() : 0).sum();
            return BigDecimal.valueOf(total);
        }
        // 默认：出勤天数合计（或首月出勤天数）
        int totalDays = summaries.stream().mapToInt(v -> v.getAttendanceDays() != null ? v.getAttendanceDays() : 0).sum();
        return BigDecimal.valueOf(totalDays);
    }
}
