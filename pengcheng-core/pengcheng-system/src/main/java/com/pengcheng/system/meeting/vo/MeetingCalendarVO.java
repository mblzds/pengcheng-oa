package com.pengcheng.system.meeting.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MeetingCalendarVO {

    private Long id;
    private String title;
    private String description;
    private Integer type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String meetingUrl;
    private Integer reminderMinutes;
    private Integer status;
    private Long organizerId;
    private String organizerName;
    private List<Long> participantIds = new ArrayList<>();
    private List<MeetingParticipantVO> participants = new ArrayList<>();
    private MeetingMinutesVO minutes;
    private List<MeetingFileVO> files = new ArrayList<>();
}
