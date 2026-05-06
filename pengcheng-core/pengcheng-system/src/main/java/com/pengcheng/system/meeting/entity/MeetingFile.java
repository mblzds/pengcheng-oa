package com.pengcheng.system.meeting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meeting_file")
public class MeetingFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long meetingId;
    private Long fileId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Long uploaderId;
    private LocalDateTime createTime;
}
