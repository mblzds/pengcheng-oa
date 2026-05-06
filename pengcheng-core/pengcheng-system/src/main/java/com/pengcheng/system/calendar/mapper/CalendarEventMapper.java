package com.pengcheng.system.calendar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.calendar.entity.CalendarEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日历事件 Mapper
 */
@Mapper
public interface CalendarEventMapper extends BaseMapper<CalendarEvent> {

    @Select("SELECT * FROM sys_calendar_event WHERE user_id = #{userId} AND status = 1 " +
            "AND start_time >= #{rangeStart} AND start_time < #{rangeEnd} ORDER BY start_time")
    List<CalendarEvent> findByUserAndRange(Long userId, LocalDateTime rangeStart, LocalDateTime rangeEnd);

    @Select("SELECT * FROM sys_calendar_event WHERE reminder_sent = 0 AND status = 1 " +
            "AND start_time <= DATE_ADD(NOW(), INTERVAL reminder_minutes MINUTE) AND start_time > NOW()")
    List<CalendarEvent> findPendingReminders();
}
