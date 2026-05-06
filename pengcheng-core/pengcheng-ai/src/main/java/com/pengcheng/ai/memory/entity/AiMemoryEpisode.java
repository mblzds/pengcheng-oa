package com.pengcheng.ai.memory.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 记忆片段（对话摘要和关键事件）
 */
@Data
@TableName(value = "ai_memory_episode", autoResultMap = true)
public class AiMemoryEpisode implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String sessionId;

    private String summary;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> keyFacts;

    private String participants;

    /**
     * 情感基调：positive/neutral/negative
     */
    private String emotion;

    private LocalDateTime createdAt;
}
