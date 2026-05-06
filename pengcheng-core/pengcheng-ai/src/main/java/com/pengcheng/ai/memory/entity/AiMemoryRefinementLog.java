package com.pengcheng.ai.memory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 记忆精炼日志（合并/压缩/淘汰记录）
 */
@Data
@TableName("ai_memory_refinement_log")
public class AiMemoryRefinementLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作：merge/compress/evict/promote/demote
     */
    private String action;

    private String sourceIds;

    private Long targetId;

    private String reason;

    private LocalDateTime createdAt;
}
