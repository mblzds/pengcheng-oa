package com.pengcheng.system.meeting.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MeetingCalendarSaveRequest {

    private String title;
    private String description;
    private Integer type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String meetingUrl;
    private Integer reminderMinutes;
    private List<Long> participantIds;
}
