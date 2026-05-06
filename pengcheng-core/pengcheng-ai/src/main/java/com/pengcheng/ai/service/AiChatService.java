package com.pengcheng.ai.service;

import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.ai.experiment.AiExperimentGuardService;
import com.pengcheng.ai.function.ReportQueryFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Locale;
import java.util.Objects;

/**
 * AI 对话服务
 * <p>
 * 支持自然语言问答，通过 Function Calling 将自然语言转换为数据查询。
 * 支持同步对话和流式对话（SSE）两种模式。
 * 支持表格和图表两种结果展示形式。
 * 超出能力范围的查询返回明确提示信息并建议替代查询方式。
 */
@Slf4j
@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final ReportQueryFunction reportQueryFunction;
    private final AiFallbackHandler fallbackHandler;
    private final ToolCallback reportQueryFunctionCallback;
    private final AiProperties aiProperties;
    private final AiExperimentGuardService experimentGuardService;

    public record PromptMeta(String experimentGroup, String version) {}

    private record PromptDecision(String version, String experimentGroup, boolean experimentalBranch) {}

    public AiChatService(ChatClient chatClient,
                         ReportQueryFunction reportQueryFunction,
                         AiFallbackHandler fallbackHandler,
                         @Qualifier("reportQueryFunctionCallback") ToolCallback reportQueryFunctionCallback,
                         AiProperties aiProperties,
                         AiExperimentGuardService experimentGuardService) {
        this.chatClient = chatClient;
        this.reportQueryFunction = reportQueryFunction;
        this.fallbackHandler = fallbackHandler;
        this.reportQueryFunctionCallback = reportQueryFunctionCallback;
        this.aiProperties = aiProperties;
        this.experimentGuardService = experimentGuardService;
    }

    private static final String REPORT_SYSTEM_PROMPT_V1 =
            "你是一个房产销售管理系统的AI报表助手。你可以帮助用户查询业务数据和生成报表。\n"
            + "你可以查询以下类型的数据：\n"
            + "1. 业务概览(overview)：报备数、到访数、成交数、成交金额、应收佣金、已结佣金\n"
            + "2. 项目业绩排行(project_ranking)：按成交数量和成交金额排序\n"
            + "3. 联盟商业绩排行(alliance_ranking)：按上客数量和成交数量排序\n"
            + "4. 转化漏斗(funnel)：报备到到访到成交转化率\n"
            + "当用户提出查询请求时，请分析用户意图，确定查询类型和时间范围，然后调用reportQuery函数获取数据。\n"
            + "如果用户的查询超出你的能力范围，请明确告知并建议使用上述支持的查询方式。\n"
            + "请用中文回答，回答要简洁明了，包含数据支撑。";

    private static final String REPORT_SYSTEM_PROMPT_V2 =
            "你是房产销售管理系统的AI数据分析助手，目标是给出可执行的业务结论。\n"
            + "可调用 reportQuery 获取以下数据：\n"
            + "1. overview（业务概览）\n"
            + "2. project_ranking（项目业绩排行）\n"
            + "3. alliance_ranking（联盟商业绩排行）\n"
            + "4. funnel（转化漏斗）\n"
            + "请先识别用户意图与时间范围，再调用 reportQuery；若信息不足，先做最小必要澄清。\n"
            + "输出要求：\n"
            + "1. 先给结论，再给关键指标；\n"
            + "2. 指标需带时间范围与口径；\n"
            + "3. 若查询超出能力范围，明确说明并给出可替代问题。\n"
            + "请使用中文，表达简洁、结构清晰。";

    /**
     * 同步对话：自然语言问答，支持 Function Calling
     */
    public ChatResult chat(String userMessage) {
        return chat(userMessage, null, (String) null);
    }

    public ChatResult chat(String userMessage, String conversationContext) {
        return chat(userMessage, conversationContext, (String) null);
    }

    public ChatResult chat(String userMessage, String conversationContext, String conversationId) {
        PromptDecision promptDecision = resolvePromptDecision(conversationId, userMessage);
        return chat(userMessage, conversationContext, promptDecision);
    }

    public ChatResult chat(String userMessage,
                           String conversationContext,
                           String conversationId,
                           PromptMeta promptMeta) {
        PromptDecision promptDecision = fromPromptMeta(promptMeta);
        return chat(userMessage, conversationContext, promptDecision);
    }

    private ChatResult chat(String userMessage, String conversationContext, PromptDecision promptDecision) {
        return fallbackHandler.executeWithFallback(
                () -> doChat(userMessage, conversationContext, promptDecision),
                this::fallbackResult,
                "AI智能报表问答"
        );
    }

    /**
     * 流式对话：SSE 方式返回
     */
    public Flux<String> streamChat(String userMessage) {
        return streamChat(userMessage, null, (String) null);
    }

    public Flux<String> streamChat(String userMessage, String conversationContext) {
        return streamChat(userMessage, conversationContext, (String) null);
    }

    public Flux<String> streamChat(String userMessage, String conversationContext, String conversationId) {
        PromptDecision promptDecision = resolvePromptDecision(conversationId, userMessage);
        return streamChat(userMessage, conversationContext, promptDecision);
    }

    public Flux<String> streamChat(String userMessage,
                                   String conversationContext,
                                   String conversationId,
                                   PromptMeta promptMeta) {
        PromptDecision promptDecision = fromPromptMeta(promptMeta);
        return streamChat(userMessage, conversationContext, promptDecision);
    }

    private Flux<String> streamChat(String userMessage,
                                    String conversationContext,
                                    PromptDecision promptDecision) {
        try {
            return chatClient.prompt()
                    .system(buildSystemPrompt(conversationContext, promptDecision.version()))
                    .user(userMessage)
                    .toolCallbacks(reportQueryFunctionCallback)
                    .stream()
                    .content()
                    .doOnComplete(() -> markPromptExperimentSuccess(promptDecision))
                    .doOnError(ex -> markPromptExperimentFailure(promptDecision, ex));
        } catch (Exception e) {
            markPromptExperimentFailure(promptDecision, e);
            log.warn("AI 流式对话失败: {}", e.getMessage());
            return Flux.just(fallbackHandler.ragFallbackMessage());
        }
    }

    /**
     * 直接查询报表数据（不经过 AI）
     */
    public ReportQueryFunction.Response queryReport(String queryType, String startDate, String endDate) {
        return reportQueryFunction.apply(new ReportQueryFunction.Request(queryType, startDate, endDate));
    }

    public PromptMeta resolvePromptMeta(String conversationId, String userMessage) {
        PromptDecision promptDecision = resolvePromptDecision(conversationId, userMessage);
        return new PromptMeta(promptDecision.experimentGroup(), promptDecision.version());
    }

    private ChatResult doChat(String userMessage, String conversationContext, PromptDecision promptDecision) {
        try {
            String content = chatClient.prompt()
                    .system(buildSystemPrompt(conversationContext, promptDecision.version()))
                    .user(userMessage)
                    .toolCallbacks(reportQueryFunctionCallback)
                    .call()
                    .content();
            markPromptExperimentSuccess(promptDecision);
            return new ChatResult(content, "table");
        } catch (Exception ex) {
            markPromptExperimentFailure(promptDecision, ex);
            throw ex;
        }
    }

    private String buildSystemPrompt(String conversationContext, String promptVersion) {
        String basePrompt = "v2".equals(promptVersion) ? REPORT_SYSTEM_PROMPT_V2 : REPORT_SYSTEM_PROMPT_V1;
        if (!StringUtils.hasText(conversationContext)) {
            return basePrompt;
        }
        return basePrompt
                + "\n\n以下是会话记忆（历史摘要与最近对话），仅在与当前问题相关时使用：\n"
                + conversationContext;
    }

    private PromptDecision resolvePromptDecision(String conversationId, String userMessage) {
        if (!aiProperties.isPromptAbExperimentEnabled()) {
            return new PromptDecision(normalizePromptVersion(aiProperties.getPromptAbControlVersion()), "off", false);
        }
        if (!experimentGuardService.allowPromptExperiment()) {
            return new PromptDecision(normalizePromptVersion(aiProperties.getPromptAbControlVersion()),
                    "rollback_control", false);
        }
        int rollout = normalizePercent(aiProperties.getPromptAbRolloutPercent());
        int bucket = bucket(conversationId, userMessage);
        boolean experiment = bucket < rollout;
        String selected = experiment
                ? aiProperties.getPromptAbExperimentVersion()
                : aiProperties.getPromptAbControlVersion();
        String version = normalizePromptVersion(selected);
        String group = experiment ? "experiment" : "control";
        if (log.isDebugEnabled()) {
            log.debug("Prompt A/B 决策: bucket={}, rollout={}, group={}, version={}",
                    bucket, rollout, group, version);
        }
        return new PromptDecision(version, group, experiment);
    }

    private PromptDecision fromPromptMeta(PromptMeta promptMeta) {
        if (promptMeta == null) {
            return new PromptDecision("v1", "off", false);
        }
        String version = normalizePromptVersion(promptMeta.version());
        String group = StringUtils.hasText(promptMeta.experimentGroup())
                ? promptMeta.experimentGroup()
                : "off";
        boolean experimentalBranch = "experiment".equals(group);
        return new PromptDecision(version, group, experimentalBranch);
    }

    private String normalizePromptVersion(String version) {
        if (!StringUtils.hasText(version)) {
            return "v1";
        }
        String normalized = version.trim().toLowerCase(Locale.ROOT);
        if ("v2".equals(normalized)) {
            return "v2";
        }
        return "v1";
    }

    private int normalizePercent(int percent) {
        if (percent < 0) {
            return 0;
        }
        return Math.min(percent, 100);
    }

    private int bucket(String conversationId, String message) {
        String seed = StringUtils.hasText(conversationId) ? conversationId : message;
        if (!StringUtils.hasText(seed)) {
            return 0;
        }
        return Math.floorMod(Objects.hash(seed), 100);
    }

    private void markPromptExperimentSuccess(PromptDecision promptDecision) {
        if (promptDecision.experimentalBranch()) {
            experimentGuardService.onPromptExperimentSuccess();
        }
    }

    private void markPromptExperimentFailure(PromptDecision promptDecision, Throwable throwable) {
        if (!promptDecision.experimentalBranch()) {
            return;
        }
        experimentGuardService.onPromptExperimentFailure();
        if (throwable != null) {
            log.warn("Prompt 实验分支失败，触发保护计数: group={}, version={}, message={}",
                    promptDecision.experimentGroup(), promptDecision.version(), throwable.getMessage());
        }
    }

    private ChatResult fallbackResult() {
        return new ChatResult(fallbackHandler.ragFallbackMessage(), "text");
    }

    /**
     * 对话结果
     *
     * @param content     回答内容
     * @param displayType 展示类型: table(表格), chart(图表), text(纯文本)
     */
    public record ChatResult(String content, String displayType) {}
}
