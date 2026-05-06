package com.pengcheng.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息分类管理（用户对会话的分组：关注/星标/静音/普通）
 */
@Data
@TableName("sys_message_category")
public class MessageCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * private / group
     */
    private String chatType;

    /**
     * 对方用户ID 或群ID
     */
    private Long targetId;

    /**
     * focus / starred / muted / normal
     */
    private String category;

    private LocalDateTime updatedAt;
}
