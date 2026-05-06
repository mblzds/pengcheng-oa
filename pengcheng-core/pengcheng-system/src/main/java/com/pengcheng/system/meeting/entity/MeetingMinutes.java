package com.pengcheng.system.meeting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meeting_minutes")
public class MeetingMinutes {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long meetingId;
    private String content;
    private String conclusions;
    private String actionItems;
    private Long creatorId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
