package com.pengcheng.system.calendar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日历事件实体
 */
@Data
@TableName("sys_calendar_event")
public class CalendarEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String description;

    /**
     * visit / sign / payment / meeting / reminder / custom
     */
    private String eventType;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allDay;
    private String color;
    private Long userId;
    private Long customerId;
    private Long projectId;
    private String location;
    private Integer meetingType;
    private String meetingUrl;
    private Long organizerId;
    private Integer reminderMinutes;
    private Boolean reminderSent;

    /**
     * daily / weekly / monthly / none
     */
    private String recurrence;

    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
