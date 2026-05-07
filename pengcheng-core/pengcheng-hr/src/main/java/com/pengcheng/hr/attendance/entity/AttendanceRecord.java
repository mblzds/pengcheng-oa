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
    private String clockInPhoto;
    /** 1-正常 2-迟到 */
    private Integer clockInStatus;
    private LocalDateTime clockOutTime;
    private String clockOutLocation;
    private String clockOutPhoto;
    /** 1-正常 2-早退 */
    private Integer clockOutStatus;
    /** 1-合规 2-超出允许范围 NULL-未启用位置校验 */
    private Integer locationStatus;

    /**
     * 考勤豁免原因（来自请假/调休审批通过的联动写入），如：leave-病假 / compensate
     * 非空时表示当日不参与缺卡判定
     */
    private String exemptReason;
}
