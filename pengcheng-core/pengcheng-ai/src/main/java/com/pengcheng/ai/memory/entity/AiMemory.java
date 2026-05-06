package com.pengcheng.ai.memory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 记忆
 */
@Data
@TableName("ai_memory")
public class AiMemory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long customerId;

    /**
     * 记忆类型：fact/preference/decision/event/profile
     */
    private String memoryType;

    /**
     * 记忆层级：L1-短期 L2-长期
     */
    private String memoryLevel;

    private String content;

    /**
     * 来源：chat/manual/system/extraction
     */
    private String source;

    /**
     * 重要度评分（0.00-1.00）
     */
    private BigDecimal importance;

    private Integer accessCount;

    private LocalDateTime lastAccessedAt;

    private String tags;

    /**
     * PGVector 中的向量 ID
     */
    private String embeddingId;

    /**
     * L1 短期记忆到期时间
     */
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
