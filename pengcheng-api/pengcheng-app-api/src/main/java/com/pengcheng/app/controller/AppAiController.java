package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.ai.orchestration.AgentOrchestratorService;
import com.pengcheng.ai.orchestration.AgentScene;
import com.pengcheng.ai.orchestration.OrchestratedChatResult;
import com.pengcheng.ai.service.AiContentService;
import com.pengcheng.app.dto.AiChatVO;
import com.pengcheng.app.dto.AiCopywritingVO;
import com.pengcheng.app.dto.AppChatDTO;
import com.pengcheng.app.dto.AppCopywritingDTO;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.project.dto.ProjectVO;
import com.pengcheng.realty.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * App端AI助手控制器
 * 提供AI对话和营销文案生成接口
 */
@Slf4j
@RestController
@RequestMapping("/app/ai")
@RequiredArgsConstructor
@SaCheckLogin
public class AppAiController {

    private final AgentOrchestratorService agentOrchestratorService;
    private final AiContentService aiContentService;
    private final ProjectService projectService;
    @Qualifier("aiChatExecutor")
    private final Executor aiChatExecutor;
    private final AiProperties aiProperties;

    /**
     * AI对话
     * 请求体含 message/conversationId/projectId，含超时处理
     */
    @PostMapping("/chat")
    public Result<AiChatVO> chat(@RequestBody AppChatDTO dto) {
        if (dto.getMessage() == null || dto.getMessage().isBlank()) {
            return Result.fail(400, "消息内容不能为空");
        }

        try {
            OrchestratedChatResult chatResult = CompletableFuture
                    .supplyAsync(() -> agentOrchestratorService.orchestrate(
                            dto.getConversationId(),
                            dto.getMessage(),
                            AgentScene.APP,
                            dto.getProjectId()
                    ), aiChatExecutor)
                    .orTimeout(Math.max(1, aiProperties.getChatTimeoutSeconds()), TimeUnit.SECONDS)
                    .join();

            AiChatVO vo = AiChatVO.builder()
                    .reply(chatResult.content())
                    .conversationId(chatResult.conversationId())
                    .displayType(chatResult.displayType())
                    .routedAgent(chatResult.routedAgent())
                    .structuredData(chatResult.structuredData())
                    .build();
            return Result.ok(vo);
        } catch (CompletionException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.warn("AI对话超时: message={}", dto.getMessage());
                return Result.fail("AI服务响应超时，请稍后重试");
            }
            log.warn("AI对话异常: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return Result.fail("AI服务暂时不可用，请稍后重试");
        } catch (Exception e) {
            log.warn("AI对话失败: message={}", dto.getMessage(), e);
            return Result.fail("AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 营销文案生成
     * 请求体含 projectId/type
     */
    @PostMapping("/copywriting")
    public Result<AiCopywritingVO> copywriting(@RequestBody AppCopywritingDTO dto) {
        if (dto.getProjectId() == null) {
            return Result.fail(400, "项目ID不能为空");
        }

        ProjectVO project = projectService.getProject(dto.getProjectId());
        if (project == null) {
            return Result.fail(400, "项目不存在");
        }

        // 用项目信息构建关键词
        String keywords = buildKeywords(project);
        String channel = dto.getType() != null ? dto.getType() : "general";

        String content = aiContentService.generateMarketingContent(keywords, channel);
        AiCopywritingVO vo = AiCopywritingVO.builder()
                .content(content)
                .build();
        return Result.ok(vo);
    }

    private String buildKeywords(ProjectVO project) {
        StringBuilder sb = new StringBuilder();
        sb.append("项目名称：").append(project.getProjectName());
        if (project.getAddress() != null) {
            sb.append("，地址：").append(project.getAddress());
        }
        if (project.getDescription() != null) {
            sb.append("，项目介绍：").append(project.getDescription());
        }
        if (project.getDeveloperName() != null) {
            sb.append("，开发商：").append(project.getDeveloperName());
        }
        return sb.toString();
    }
}
