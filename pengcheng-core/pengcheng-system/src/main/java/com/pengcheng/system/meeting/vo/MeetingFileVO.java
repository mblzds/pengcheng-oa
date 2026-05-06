package com.pengcheng.system.meeting.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeetingFileVO {

    private Long id;
    private Long fileId;
    private String name;
    private String type;
    private Long size;
    private String url;
    private LocalDateTime createTime;
}
