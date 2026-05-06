package com.pengcheng.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息表
 */
@Data
@TableName("sys_chat_message")
public class SysChatMessage implements Serializable {

    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 接收者ID（0表示群发/广播）
     */
    private Long receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型（1文本 2图片 3文件）
     */
    private Integer msgType;

    /**
     * 是否已撤回
     */
    private Integer recalled;

    /**
     * 引用的消息ID
     */
    private Long replyToId;

    /**
     * 消息序列号（排序与ACK）
     */
    private Long seq;

    /**
     * 是否已送达
     */
    private Integer delivered;

    /**
     * 是否已读
     */
    private Integer readFlag;

    /**
     * 是否已读（0未读 1已读）— 兼容旧字段
     */
    private Integer isRead;

    /**
     * 优先级 0=普通 1=重要 2=紧急
     */
    private Integer priority;

    /**
     * 是否置顶 0=否 1=是
     */
    private Integer pinned;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 引用的消息（非数据库字段，查询时填充）
     */
    @TableField(exist = false)
    private SysChatMessage replyMessage;
}
