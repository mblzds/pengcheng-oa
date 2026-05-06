package com.pengcheng.admin.controller.ai;

import com.pengcheng.ai.orchestration.AgentOrchestratorService;
import com.pengcheng.ai.orchestration.AgentScene;
import com.pengcheng.ai.orchestration.OrchestratedChatResult;
import com.pengcheng.ai.service.AiMultiModalService;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI 智能报表与问答控制器
 * <p>
 * 提供自然语言问答接口，支持同步和流式（SSE）两种模式。
 * 通过 Function Calling 将自然语言转换为数据查询，
 * 支持表格和图表两种结果展示形式。
 */
@RestController
@RequestMapping("/admin/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AgentOrchestratorService agentOrchestratorService;
    @Autowired(required = false)
    private AiMultiModalService multiModalService;

    /**
     * 同步对话接口
     *
     * @param request 对话请求
     * @return 对话结果（包含内容和展示类型）
     */
    @PostMapping
    @Log(title = "AI对话", businessType = BusinessType.OTHER)
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            return Result.fail("消息内容不能为空");
        }
        OrchestratedChatResult result = agentOrchestratorService.orchestrate(
                request.conversationId(),
                request.message(),
                AgentScene.ADMIN,
                request.projectId()
        );
        return Result.ok(new ChatResponse(
                result.content(),
                result.displayType(),
                result.conversationId(),
                result.routedAgent(),
                result.structuredData()
        ));
    }

    /**
     * 流式对话接口（SSE）
     *
     * @param request 对话请求
     * @return SSE 流式响应
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            return Flux.just("消息内容不能为空");
        }
        return agentOrchestratorService.streamOrchestrate(
                request.conversationId(),
                request.message(),
                AgentScene.ADMIN,
                request.projectId()
        );
    }

    /** 户型图分析 */
    @PostMapping("/analyze-floor-plan")
    public Result<java.util.Map<String, String>> analyzeFloorPlan(@RequestBody java.util.Map<String, String> req) {
        if (multiModalService == null) return Result.fail("多模态 AI 服务未启用");
        String imageUrl = req.get("imageUrl");
        if (imageUrl == null || imageUrl.isBlank()) return Result.fail("图片 URL 不能为空");
        String result = multiModalService.analyzeFloorPlan(imageUrl);
        return Result.ok(java.util.Map.of("analysis", result != null ? result : "分析失败"));
    }

    /** 证件 OCR */
    @PostMapping("/ocr")
    public Result<java.util.Map<String, String>> ocr(@RequestBody java.util.Map<String, String> req) {
        if (multiModalService == null) return Result.fail("多模态 AI 服务未启用");
        String imageUrl = req.get("imageUrl");
        if (imageUrl == null || imageUrl.isBlank()) return Result.fail("图片 URL 不能为空");
        String result = multiModalService.ocrDocument(imageUrl);
        return Result.ok(java.util.Map.of("result", result != null ? result : "{}"));
    }

    /** 语音转写 */
    @PostMapping("/transcribe")
    public Result<java.util.Map<String, String>> transcribe(@RequestBody java.util.Map<String, String> req) {
        if (multiModalService == null) return Result.fail("多模态 AI 服务未启用");
        String audioUrl = req.get("audioUrl");
        if (audioUrl == null || audioUrl.isBlank()) return Result.fail("音频 URL 不能为空");
        String transcript = multiModalService.transcribeAudio(audioUrl);
        if (transcript == null) return Result.fail("ASR 服务尚未配置，请设置 DashScope API Key");
        return Result.ok(java.util.Map.of("transcript", transcript));
    }

    /**
     * 对话请求
     */
    public record ChatRequest(String message, String conversationId, Long projectId) {}

    /**
     * 对话响应
     */
    public record ChatResponse(
            String content,
            String displayType,
            String conversationId,
            String routedAgent,
            java.util.Map<String, Object> structuredData
    ) {
    }
}
