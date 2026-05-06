package com.pengcheng.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤打卡记录实体（公司级假勤）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("attendance_record")
public class AttendanceRecord extends BaseEntity {

    private Long userId;
    private LocalDate attendanceDate;
    private LocalDateTime clockInTime;
    private String clockInLocation;
    /** 1-正常 2-迟到 */
    private Integer clockInStatus;
    private LocalDateTime clockOutTime;
    private String clockOutLocation;
    /** 1-正常 2-早退 */
    private Integer clockOutStatus;
}
