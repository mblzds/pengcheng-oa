package com.pengcheng.ai.orchestration;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.ai.audit.service.AiToolAuditService;
import com.pengcheng.ai.orchestration.permission.ToolPermissionDecision;
import com.pengcheng.ai.orchestration.permission.ToolPermissionGuardService;
import com.pengcheng.ai.orchestration.router.AgentRouterService;
import com.pengcheng.ai.orchestration.tool.AiAgentTool;
import com.pengcheng.ai.service.AiChatService;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 多智能体编排服务
 * <p>
 * 意图路由与工具执行均在 Spring/Spring AI 生态内完成（规则 + ChatClient 路由，AiAgentTool 执行）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestratorService {

    private final AgentRouterService routeService;
    private final ConversationMemoryService memoryService;
    private final AiChatService aiChatService;
    private final ToolPermissionGuardService permissionGuardService;
    private final AiToolAuditService auditService;
    private final SysUserService userService;
    private final List<AiAgentTool> tools;
    private final SkillEnableRegistry skillEnableRegistry;

    public OrchestratedChatResult orchestrate(String conversationId,
                                              String message,
                                              AgentScene scene,
                                              Long projectIdHint) {
        String convId = memoryService.ensureConversationId(conversationId);
        memoryService.appendUserMessage(convId, message);
        String conversationContext = memoryService.buildConversationContext(convId);

        Long userId = currentUserId();
        List<String> roleCodes = getRoleCodes(userId);
        AgentRouterService.RouteDecision routeDecision = routeService.routeDecision(message, convId);
        AgentIntent intent = routeDecision.intent();
        AiChatService.PromptMeta promptMeta = resolvePromptMeta(intent, convId, message);
        ToolPermissionDecision decision = permissionGuardService.authorize(scene, intent, projectIdHint, roleCodes);
        String callChain = buildCallChain(routeDecision, promptMeta, decision, "sync");

        long start = System.currentTimeMillis();
        if (!decision.allowed()) {
            String denied = "无权限调用该AI能力: " + decision.reason();
            memoryService.appendAssistantMessage(convId, denied);
            auditService.recordFailure(
                    "permission-guard",
                    scene,
                    intent,
                    convId,
                    userId,
                    roleCodes,
                    message,
                    System.currentTimeMillis() - start,
                    callChain + "|guard:deny",
                    new IllegalStateException(decision.reason())
            );
            return new OrchestratedChatResult(denied, "text", convId, "permission-guard");
        }

        Optional<AiAgentTool> toolOpt = resolveTool(intent);
        if (toolOpt.isEmpty()) {
            String noToolMsg = "该能力暂未开放，请在「AI → Skill 管理」中启用对应能力，或联系管理员。";
            memoryService.appendAssistantMessage(convId, noToolMsg);
            return new OrchestratedChatResult(noToolMsg, "text", convId, "no-tool");
        }
        AiAgentTool tool = toolOpt.get();
        AiToolContext context = new AiToolContext(
                convId,
                message,
                conversationContext,
                scene,
                intent,
                projectIdHint,
                userId,
                roleCodes,
                decision.dataScope(),
                decision.projectScope(),
                promptMeta.experimentGroup(),
                promptMeta.version()
        );
        OrchestratedChatResult result;
        try {
            result = tool.execute(context);
            auditService.recordSuccess(
                    tool.toolName(),
                    scene,
                    intent,
                    convId,
                    userId,
                    roleCodes,
                    message,
                    result.content(),
                    System.currentTimeMillis() - start,
                    callChain + "|tool:" + tool.toolName()
            );
        } catch (Exception ex) {
            String fallback = "AI服务暂时不可用，请稍后重试。";
            memoryService.appendAssistantMessage(convId, fallback);
            auditService.recordFailure(
                    tool.toolName(),
                    scene,
                    intent,
                    convId,
                    userId,
                    roleCodes,
                    message,
                    System.currentTimeMillis() - start,
                    callChain + "|tool:" + tool.toolName(),
                    ex
            );
            return new OrchestratedChatResult(fallback, "text", convId, tool.toolName());
        }

        memoryService.appendAssistantMessage(convId, result.content());
        return result;
    }

    public Flux<String> streamOrchestrate(String conversationId,
                                          String message,
                                          AgentScene scene,
                                          Long projectIdHint) {
        String convId = memoryService.ensureConversationId(conversationId);
        memoryService.appendUserMessage(convId, message);
        String conversationContext = memoryService.buildConversationContext(convId);

        Long userId = currentUserId();
        List<String> roleCodes = getRoleCodes(userId);
        AgentRouterService.RouteDecision routeDecision = routeService.routeDecision(message, convId);
        AgentIntent intent = routeDecision.intent();
        AiChatService.PromptMeta promptMeta = resolvePromptMeta(intent, convId, message);
        ToolPermissionDecision decision = permissionGuardService.authorize(scene, intent, projectIdHint, roleCodes);
        String callChain = buildCallChain(routeDecision, promptMeta, decision, "stream");
        long start = System.currentTimeMillis();

        if (!decision.allowed()) {
            String denied = "无权限调用该AI能力: " + decision.reason();
            memoryService.appendAssistantMessage(convId, denied);
            auditService.recordFailure(
                    "permission-guard",
                    scene,
                    intent,
                    convId,
                    userId,
                    roleCodes,
                    message,
                    System.currentTimeMillis() - start,
                    callChain + "|guard:deny",
                    new IllegalStateException(decision.reason())
            );
            return Flux.just(denied);
        }

        Optional<AiAgentTool> toolOpt = resolveTool(intent);
        if (toolOpt.isEmpty()) {
            String noToolMsg = "该能力暂未开放，请在「AI → Skill 管理」中启用对应能力，或联系管理员。";
            memoryService.appendAssistantMessage(convId, noToolMsg);
            return Flux.just(noToolMsg);
        }
        AiAgentTool tool = toolOpt.get();
        if (intent == AgentIntent.KNOWLEDGE
                || intent == AgentIntent.COPYWRITING
                || intent == AgentIntent.APPROVAL
                || intent == AgentIntent.CUSTOMER) {
            try {
                OrchestratedChatResult result = tool.execute(new AiToolContext(
                        convId, message, conversationContext, scene, intent, projectIdHint, userId, roleCodes,
                        decision.dataScope(), decision.projectScope(),
                        promptMeta.experimentGroup(), promptMeta.version()
                ));
                memoryService.appendAssistantMessage(convId, result.content());
                auditService.recordSuccess(
                        tool.toolName(),
                        scene,
                        intent,
                        convId,
                        userId,
                        roleCodes,
                        message,
                        result.content(),
                        System.currentTimeMillis() - start,
                        callChain + "|tool:" + tool.toolName()
                );
                return Flux.just(result.content());
            } catch (Exception ex) {
                String fallback = "AI服务暂时不可用，请稍后重试。";
                memoryService.appendAssistantMessage(convId, fallback);
                auditService.recordFailure(
                        tool.toolName(),
                        scene,
                        intent,
                        convId,
                        userId,
                        roleCodes,
                        message,
                        System.currentTimeMillis() - start,
                        callChain + "|tool:" + tool.toolName(),
                        ex
                );
                return Flux.just(fallback);
            }
        }

        AtomicReference<StringBuilder> full = new AtomicReference<>(new StringBuilder());
        return aiChatService.streamChat(message, conversationContext, convId, promptMeta)
                .doOnNext(chunk -> full.get().append(chunk))
                .doOnComplete(() -> {
                    String content = full.get().toString();
                    memoryService.appendAssistantMessage(convId, content);
                    auditService.recordSuccess(
                            tool.toolName(),
                            scene,
                            intent,
                            convId,
                            userId,
                            roleCodes,
                            message,
                            content,
                            System.currentTimeMillis() - start,
                            callChain + "|tool:" + tool.toolName()
                    );
                })
                .onErrorResume(ex -> {
                    log.warn("流式编排失败, conversationId={}", convId, ex);
                    String fallback = "AI服务暂时不可用，请稍后重试。";
                    memoryService.appendAssistantMessage(convId, fallback);
                    auditService.recordFailure(
                            tool.toolName(),
                            scene,
                            intent,
                            convId,
                            userId,
                            roleCodes,
                            message,
                            System.currentTimeMillis() - start,
                            callChain + "|tool:" + tool.toolName(),
                            ex
                    );
                    return Flux.just(fallback);
                });
    }

    private Optional<AiAgentTool> resolveTool(AgentIntent intent) {
        Optional<AiAgentTool> match = tools.stream()
                .filter(tool -> !skillEnableRegistry.isDisabled(tool.toolName()))
                .filter(tool -> tool.supportedIntent() == intent)
                .findFirst();
        if (match.isPresent()) {
            return match;
        }
        return tools.stream()
                .filter(tool -> !skillEnableRegistry.isDisabled(tool.toolName()))
                .filter(tool -> tool.supportedIntent() == AgentIntent.GENERAL)
                .findFirst();
    }

    private Long currentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception ignore) {
            return null;
        }
    }

    private List<String> getRoleCodes(Long userId) {
        if (userId == null) {
            return List.of();
        }
        try {
            List<String> roleCodes = userService.getRoleCodes(userId);
            return roleCodes != null ? roleCodes : List.of();
        } catch (Exception e) {
            log.warn("读取用户角色失败: userId={}", userId, e);
            return List.of();
        }
    }

    private String buildCallChain(AgentRouterService.RouteDecision routeDecision,
                                  AiChatService.PromptMeta promptMeta,
                                  ToolPermissionDecision decision,
                                  String mode) {
        return "mode:" + mode
                + "|route:" + routeDecision.intent().name()
                + "|routeMode:" + routeDecision.routeMode()
                + "|routeExp:" + routeDecision.experimentGroup()
                + "|promptExp:" + promptMeta.experimentGroup()
                + "|promptVersion:" + promptMeta.version()
                + "|dataScope:" + decision.dataScope()
                + "|projectScope:" + decision.projectScope();
    }

    private AiChatService.PromptMeta resolvePromptMeta(AgentIntent intent, String conversationId, String message) {
        if (intent == AgentIntent.REPORT || intent == AgentIntent.GENERAL) {
            return aiChatService.resolvePromptMeta(conversationId, message);
        }
        return new AiChatService.PromptMeta("not_applicable", "not_applicable");
    }
}
