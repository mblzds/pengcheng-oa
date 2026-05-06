package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI对话响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatVO {

    /** AI回复内容 */
    private String reply;

    /** 会话ID */
    private String conversationId;

    /** 展示类型 */
    private String displayType;

    /** 路由智能体 */
    private String routedAgent;

    /** 结构化数据 */
    private Map<String, Object> structuredData;
}
