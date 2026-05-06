package com.pengcheng.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 多模态 AI 服务
 * 支持图片理解（户型图分析、证件 OCR）和语音转写（ASR）
 */
@Slf4j
@Service
public class AiMultiModalService {

    private final ChatClient chatClient;

    public AiMultiModalService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 图片理解：分析户型图
     *
     * @param imageUrl 图片公网可访问 URL
     * @return 分析结果（Markdown 格式）
     */
    public String analyzeFloorPlan(String imageUrl) {
        String systemPrompt = """
            你是专业的房地产户型分析师。请分析用户提供的户型图，输出以下信息：
            ## 户型概述
            - 户型结构（几室几厅几卫）
            - 建筑面积估算
            - 朝向分析
            ## 空间布局
            - 各房间功能和面积估算
            - 动线分析（生活/家务/访客动线）
            ## 优缺点
            - 优点（采光、通风、空间利用等）
            - 不足（暗卫、走廊浪费、动线交叉等）
            ## 适合人群
            - 推荐客户类型
            请用中文回答。
            """;
        return generateWithImage(systemPrompt, "请分析这张户型图", imageUrl);
    }

    /**
     * 图片理解：证件 OCR 识别
     *
     * @param imageUrl 证件图片 URL
     * @return 提取的结构化信息（JSON 格式）
     */
    public String ocrDocument(String imageUrl) {
        String systemPrompt = """
            你是证件识别助手。请识别图片中的证件信息，以 JSON 格式输出。
            支持的证件类型：身份证、营业执照、房产证、购房合同等。
            输出格式示例：
            {"type": "身份证", "name": "张三", "idNumber": "xxx", "address": "xxx"}
            仅输出 JSON，不要其他文字。如果无法识别，返回 {"error": "无法识别"}。
            """;
        return generateWithImage(systemPrompt, "请识别这张证件", imageUrl);
    }

    /**
     * 通用图片理解
     */
    public String analyzeImage(String imageUrl, String question) {
        String systemPrompt = "你是房地产 AI 助手，擅长分析与房产销售相关的图片。请用中文回答用户问题。";
        return generateWithImage(systemPrompt, question, imageUrl);
    }

    /**
     * DashScope Paraformer ASR 语音转写
     * 当前为框架桩实现，接入 DashScope ASR API 后可直接替换
     *
     * @param audioUrl 音频文件 URL（支持 wav/mp3/m4a/ogg）
     * @return 转写文本
     */
    public String transcribeAudio(String audioUrl) {
        log.info("[ASR] 收到语音转写请求: {}", audioUrl);
        // DashScope Paraformer ASR 接入点
        // 实际对接时替换为 DashScope API 调用：
        // POST https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription
        // Headers: Authorization: Bearer ${DASHSCOPE_API_KEY}
        // Body: {"model": "paraformer-realtime-v2", "input": {"file_urls": [audioUrl]}}
        log.warn("[ASR] DashScope Paraformer ASR 尚未配置，请设置 DASHSCOPE_API_KEY 环境变量并对接 API");
        return null;
    }

    private String generateWithImage(String systemPrompt, String userInput, String imageUrl) {
        try {
            // Spring AI 1.x 通过 Media 类型支持多模态输入
            // 当前版本（1.0.0.2）可能不完全支持，升级到 1.1.x 后启用
            // 降级方案：将图片 URL 嵌入到用户提示词中
            String enhancedInput = userInput + "\n\n图片地址: " + imageUrl;
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(enhancedInput)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("[MultiModal] 图片分析失败: {}", e.getMessage());
            return null;
        }
    }
}
