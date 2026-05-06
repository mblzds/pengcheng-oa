package com.pengcheng.system.meeting.dto;

import lombok.Data;

@Data
public class MeetingMinutesSaveRequest {

    private String content;
    private String conclusions;
    private String actionItems;
    private Integer status;
}
