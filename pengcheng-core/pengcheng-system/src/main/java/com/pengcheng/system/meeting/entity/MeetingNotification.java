package com.pengcheng.system.meeting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meeting_notification")
public class MeetingNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long meetingId;
    private Long userId;
    private Integer notifyType;
    private LocalDateTime notifyTime;
    private Integer status;
    private LocalDateTime createTime;
}
