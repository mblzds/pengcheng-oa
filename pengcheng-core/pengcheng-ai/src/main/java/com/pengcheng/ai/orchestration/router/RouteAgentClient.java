package com.pengcheng.ai.orchestration.router;

/**
 * Router Agent 客户端
 */
public interface RouteAgentClient {

    /**
     * 对用户输入进行路由意图分类，返回模型原始文本
     */
    String classify(String message);
}
