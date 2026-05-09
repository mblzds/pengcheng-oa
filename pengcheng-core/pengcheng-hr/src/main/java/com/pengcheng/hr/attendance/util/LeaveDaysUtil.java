package com.pengcheng.hr.attendance.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 请假天数计算（按"考勤工时"折算，不是 wall-clock）。
 *
 * 规则：
 * - 工作时段由调用方传入（来自 sys_config_group(attendance) 的 workStartTime / workEndTime）
 * - 跨日累加每个自然日的"工作时段交集"
 * - 自动扣除 12:00~13:00 午休 1 小时（约定俗成；若工作时段不覆盖午休则不扣）
 * - 1 天 = 8 小时，结果四舍五入到 0.5 天精度，最小 0.5 天
 *
 * 示例（默认 09:00~18:00 + 12:00~13:00 午休）：
 *   09:00~12:00 → 3h → 0.5 天
 *   14:00~18:00 → 4h → 0.5 天
 *   09:00~18:00 → 8h（扣午休） → 1.0 天
 *   00:00~18:30 → 9h（clip 到 9~18 + 扣午休） → 1.0 天
 *   5/9 09:00 ~ 5/11 18:00 → 24h → 3.0 天
 */
public final class LeaveDaysUtil {

    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    private static final double FULL_DAY_HOURS = 8.0;

    private LeaveDaysUtil() {}

    public static Double calcDays(LocalDateTime start, LocalDateTime end, LocalTime workStart, LocalTime workEnd) {
        if (start == null || end == null || workStart == null || workEnd == null) return null;
        if (!end.isAfter(start)) return 0.5;
        if (!workEnd.isAfter(workStart)) return 0.5;

        LocalDate cursor = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        double totalMinutes = 0;
        while (!cursor.isAfter(endDate)) {
            LocalDateTime dayWorkStart = LocalDateTime.of(cursor, workStart);
            LocalDateTime dayWorkEnd = LocalDateTime.of(cursor, workEnd);
            LocalDateTime overlapStart = maxDt(start, dayWorkStart);
            LocalDateTime overlapEnd = minDt(end, dayWorkEnd);
            if (overlapEnd.isAfter(overlapStart)) {
                long minutes = Duration.between(overlapStart, overlapEnd).toMinutes();
                LocalDateTime lunchStart = LocalDateTime.of(cursor, LUNCH_START);
                LocalDateTime lunchEnd = LocalDateTime.of(cursor, LUNCH_END);
                LocalDateTime lunchOverlapStart = maxDt(overlapStart, lunchStart);
                LocalDateTime lunchOverlapEnd = minDt(overlapEnd, lunchEnd);
                if (lunchOverlapEnd.isAfter(lunchOverlapStart)) {
                    minutes -= Duration.between(lunchOverlapStart, lunchOverlapEnd).toMinutes();
                }
                totalMinutes += minutes;
            }
            cursor = cursor.plusDays(1);
        }

        double rawDays = totalMinutes / 60.0 / FULL_DAY_HOURS;
        double rounded = Math.round(rawDays * 2.0) / 2.0;
        return rounded < 0.5 ? 0.5 : rounded;
    }

    private static LocalDateTime maxDt(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDateTime minDt(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
