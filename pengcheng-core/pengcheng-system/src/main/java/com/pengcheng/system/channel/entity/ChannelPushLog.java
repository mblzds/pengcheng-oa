package com.pengcheng.system.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_channel_push_log")
public class ChannelPushLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long channelId;
    private String messageType;
    private String content;
    private String target;
    /** 0=待发 1=成功 2=失败 */
    private Integer status;
    private String errorMsg;
    private LocalDateTime sentAt;
}
