package com.pengcheng.ai.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 工具调用审计日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_tool_call_log")
public class AiToolCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String toolName;

    private String scene;

    private String intent;

    private String conversationId;

    private Long userId;

    private String roleCodes;

    private String requestSummary;

    private String responseSummary;

    private Integer success;

    private Long latencyMs;

    private String callChain;

    private String errorMessage;

    private LocalDateTime createTime;
}

