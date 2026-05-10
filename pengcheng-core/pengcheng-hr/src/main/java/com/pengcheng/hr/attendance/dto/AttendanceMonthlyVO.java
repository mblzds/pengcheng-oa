package com.pengcheng.hr.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceMonthlyVO {
    private Long userId;
    private Integer year;
    private Integer month;

    /** 已打卡（in/out 任意一边非空）的天数 */
    private Integer attendanceDays;
    private Integer lateTimes;
    private Integer earlyLeaveTimes;

    /** 已通过请假覆盖到本月的真实天数（按 LeaveDaysUtil 估算，区别于历史"条数") */
    private Integer leaveDays;

    /** 已通过调休覆盖到本月的天数 */
    private Integer compensateDays;

    /** 截至今日的应当出勤工作日数（扣周末/法定节假日，加调休补班） */
    private Integer expectedWorkdays;

    /** 缺勤天数 = max(expectedWorkdays - attendanceDays - leaveDays - compensateDays, 0)；后端算好直接返回 */
    private Integer absentDays;

    /** 本月内法定节假日的日期清单（YYYY-MM-DD），给小程序日历视图用，避免把节日格当缺勤渲染 */
    private List<String> holidays;

    /** 本月内调休补班的日期清单（YYYY-MM-DD），日历应当把这些周末日反过来当工作日 */
    private List<String> makeupWorkdays;

    /**
     * 考勤起算日（YYYY-MM-DD），优先取员工 joinDate，缺失时回退到系统级"考勤启用日期"。
     * 早于该日期的格子前端不应渲染为缺勤。null 表示无截止线（老员工 + 系统未配）。
     */
    private String cutoffDate;

    private Double overtimeHours;
}
