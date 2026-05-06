package com.pengcheng.system.meeting.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeetingMinutesVO {

    private Long id;
    private String content;
    private String conclusions;
    private String actionItems;
    private Integer status;
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
