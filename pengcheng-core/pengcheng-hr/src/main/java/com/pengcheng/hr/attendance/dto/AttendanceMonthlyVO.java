package com.pengcheng.hr.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceMonthlyVO {
    private Long userId;
    private Integer year;
    private Integer month;
    private Integer attendanceDays;
    private Integer lateTimes;
    private Integer earlyLeaveTimes;
    private Integer leaveDays;
    private Double overtimeHours;
}
