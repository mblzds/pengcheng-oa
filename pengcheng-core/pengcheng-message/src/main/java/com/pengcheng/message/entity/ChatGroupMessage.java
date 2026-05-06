package com.pengcheng.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群消息实体
 */
@Data
@TableName("sys_chat_group_message")
public class ChatGroupMessage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long groupId;
    
    private Long senderId;
    
    private String senderName;
    
    private String senderAvatar;
    
    private String content;
    
    /**
     * 消息类型：1-文本 2-图片 3-文件 4-系统消息
     */
    private Integer msgType;

    private Integer recalled;

    private Long replyToId;

    private Long seq;

    /**
     * @的用户ID列表，逗号分隔
     */
    private String atUserIds;
    
    private LocalDateTime sendTime;

    @TableField(exist = false)
    private ChatGroupMessage replyMessage;
}
