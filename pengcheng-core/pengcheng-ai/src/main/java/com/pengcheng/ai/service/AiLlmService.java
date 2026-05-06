package com.pengcheng.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 通用 LLM 文本生成服务
 * 提供不绑定特定业务的 AI 文本生成能力，供各业务模块调用
 */
@Slf4j
@Service
public class AiLlmService {

    private final ChatClient chatClient;

    public AiLlmService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 通用文本生成：基于系统提示词和用户输入生成回复
     */
    public String generate(String systemPrompt, String userInput) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userInput)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("[AiLlm] 文本生成失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 拜访内容 AI 分析：提取客户需求、异议点、承诺事项、竞品信息
     */
    public String analyzeVisit(String visitContent, String customerName, String visitType) {
        String systemPrompt = """
            你是一名专业的房地产销售分析师。请分析以下拜访记录，按结构化格式提取关键信息。
            输出格式（Markdown）：
            ## 客户需求
            - （列出识别到的需求点）
            ## 异议与顾虑
            - （列出客户提出的异议或顾虑）
            ## 承诺事项
            - （列出销售人员或客户做出的承诺，标注责任方和时间）
            ## 竞品信息
            - （如有提及竞品楼盘或中介，列出）
            ## 跟进建议
            - （给出 1-3 条具体可执行的跟进建议）
            ## 综合评分
            - 客户意向度：X/10
            - 拜访质量：X/10
            请用中文回答，简洁专业。
            """;
        String userInput = "客户：" + customerName + "\n拜访类型：" + visitType + "\n拜访内容：\n" + visitContent;
        return generate(systemPrompt, userInput);
    }

    /**
     * 日报自然语言摘要生成
     */
    public String generateDailyReportSummary(int followUp, int newCustomers, int dealCount,
                                              String dealAmount, int todoCompleted, int todoPending) {
        String systemPrompt = """
            你是一名房地产销售团队的 AI 助手。根据以下工作数据，生成一份简洁的日报摘要。
            要求：
            1. 用自然语言概述今日工作成果
            2. 基于数据给出 1-2 条改进建议
            3. 语气积极但实事求是
            4. 控制在 200 字以内
            """;
        String userInput = String.format(
                "今日跟进客户 %d 位，新增客户 %d 位，签约 %d 单（金额 %s 元），完成待办 %d 项，剩余待办 %d 项。",
                followUp, newCustomers, dealCount, dealAmount, todoCompleted, todoPending);
        return generate(systemPrompt, userInput);
    }

    /**
     * 从聊天消息中智能提取待办事项
     */
    public String extractTodosFromMessage(String messageContent) {
        String systemPrompt = """
            你是一名任务管理助手。请从以下聊天消息中提取待办事项。
            输出 JSON 数组格式，每个待办包含：
            - title: 待办标题（简短描述任务）
            - priority: 优先级（0=普通, 1=重要, 2=紧急）
            - dueHint: 时间提示（如"明天"、"下周一"、"3月15日"，无则为null）
            如果消息中没有可提取的待办事项，返回空数组 []。
            仅输出 JSON，不要其他文字。
            """;
        return generate(systemPrompt, messageContent);
    }

    /**
     * 根据客户画像智能预填模板字段
     */
    public String smartFillTemplate(String templateFields, String customerProfile) {
        String systemPrompt = """
            你是房地产销售文案助手。根据客户画像信息，为销售场景模板预填字段值。
            输出 JSON 对象格式，key 为字段名，value 为预填值。
            要求：
            1. 根据客户信息推断合理值
            2. 无法推断的字段不要填写（不要编造）
            3. 仅输出 JSON，不要其他文字
            """;
        String userInput = "模板字段：\n" + templateFields + "\n\n客户画像：\n" + customerProfile;
        return generate(systemPrompt, userInput);
    }

    /**
     * AI 评分：给拜访内容打分（0-100）
     */
    public Integer scoreVisit(String analysisResult) {
        String systemPrompt = "根据以下销售拜访分析报告，给出一个 0-100 的整数评分。仅输出数字，不要其他文字。";
        String result = generate(systemPrompt, analysisResult);
        if (result == null) return null;
        try {
            return Integer.parseInt(result.trim().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
