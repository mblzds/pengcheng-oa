package com.pengcheng.ai.orchestration.router;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 基于 Spring AI ChatClient 的意图路由客户端（唯一实现，路由 100% Spring 生态）
 */
@Service("chatRouteAgentClient")
@Primary
@RequiredArgsConstructor
public class ChatClientRouteAgentClient implements RouteAgentClient {

    private static final String ROUTER_SYSTEM_PROMPT =
            "你是一个路由分类器。"
                    + "请根据用户输入，仅返回一个枚举值，不要输出其他内容。"
                    + "可选枚举：REPORT, KNOWLEDGE, COPYWRITING, APPROVAL, CUSTOMER, GENERAL。"
                    + "分类规则："
                    + "报表统计类返回REPORT；制度文档类返回KNOWLEDGE；营销文案类返回COPYWRITING；"
                    + "审批待办类返回APPROVAL；客户查询/判客类返回CUSTOMER；无法判断返回GENERAL。";

    private final ChatClient chatClient;

    @Override
    public String classify(String message) {
        return chatClient.prompt()
                .system(ROUTER_SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }
}
