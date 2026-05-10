package com.pengcheng.hr.attendance.service;

import com.pengcheng.hr.attendance.entity.HolidayCalendar;

import java.time.LocalDate;
import java.util.List;

/**
 * 节假日 / 调休补班服务。仅在内存按年缓存，年初一次加载就够（同年节日数据稳定）。
 * 默认规则：周一~五 = 工作日；
 *   - 法定节假日（type=1）→ 当日改为休息
 *   - 调休补班（type=2）  → 当日改为工作
 */
public interface HolidayCalendarService {

    /**
     * 给定日期是否为应当工作的日期（用来算考勤月报"应出勤工作日"）
     */
    boolean isWorkday(LocalDate date);

    /**
     * 区间内（含端点）的工作日总数
     */
    int countWorkdays(LocalDate start, LocalDate endInclusive);

    /**
     * 列出某年的全部节假日 / 补班记录（管理后台用）
     */
    List<HolidayCalendar> listByYear(int year);

    /**
     * 新增/更新一条记录；按 holidayDate 唯一约束
     */
    Long save(HolidayCalendar item);

    /**
     * 删除一条
     */
    void delete(Long id);
}
