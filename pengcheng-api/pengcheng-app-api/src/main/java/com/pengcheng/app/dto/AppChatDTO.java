package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI对话请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppChatDTO {

    /** 用户消息内容 */
    private String message;

    /** 会话ID（可选，首次对话为空） */
    private String conversationId;

    /** 项目ID（可选，用于知识库范围过滤） */
    private Long projectId;
}
