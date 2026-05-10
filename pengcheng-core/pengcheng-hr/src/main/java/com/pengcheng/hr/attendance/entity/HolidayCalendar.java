package com.pengcheng.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 节假日 / 调休补班日历项。
 * type=1 法定节假日（休）；type=2 调休补班（上）。
 * 默认规则：周一~五 = 工作日；本表两类条目改写默认。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("holiday_calendar")
public class HolidayCalendar extends BaseEntity {

    /** 类型：1 法定节假日（休）/ 2 调休补班（上） */
    public static final int TYPE_HOLIDAY = 1;
    public static final int TYPE_MAKEUP_WORKDAY = 2;

    private LocalDate holidayDate;
    private Integer type;
    private String label;
    private String note;
}
