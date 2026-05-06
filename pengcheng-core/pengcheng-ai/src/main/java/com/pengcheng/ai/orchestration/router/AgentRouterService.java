package com.pengcheng.ai.orchestration.router;

import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.ai.experiment.AiExperimentGuardService;
import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AgentRouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 路由服务（Rule Router + Router Agent）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRouterService {

    private static final Set<String> SUPPORTED_INTENTS = Set.of(
            "REPORT", "KNOWLEDGE", "COPYWRITING", "APPROVAL", "CUSTOMER", "GENERAL"
    );

    private final AgentRouteService ruleRouteService;
    private final RouteAgentClient routeAgentClient;
    private final AiProperties aiProperties;
    private final AiExperimentGuardService experimentGuardService;

    public record RouteDecision(AgentIntent intent, String routeMode, String experimentGroup) {}

    private record ResolvedRouteMode(String mode, String experimentGroup, boolean experimentalBranch) {}

    public AgentIntent route(String message) {
        return route(message, null);
    }

    public AgentIntent route(String message, String conversationId) {
        return routeDecision(message, conversationId).intent();
    }

    public RouteDecision routeDecision(String message, String conversationId) {
        ResolvedRouteMode resolved = resolveMode(message, conversationId);
        if ("RULE".equals(resolved.mode())) {
            return new RouteDecision(ruleRouteService.route(message), resolved.mode(), resolved.experimentGroup());
        }

        Optional<AgentIntent> agentIntent = classifyByAgent(message);
        if (agentIntent.isPresent()) {
            if (resolved.experimentalBranch()) {
                experimentGuardService.onRouteExperimentSuccess();
            }
            return new RouteDecision(agentIntent.get(), resolved.mode(), resolved.experimentGroup());
        }

        if (resolved.experimentalBranch()) {
            experimentGuardService.onRouteExperimentFailure();
        }
        if (aiProperties.isRouteAgentFallbackToRule()) {
            return new RouteDecision(ruleRouteService.route(message), resolved.mode(), resolved.experimentGroup());
        }
        return new RouteDecision(AgentIntent.GENERAL, resolved.mode(), resolved.experimentGroup());
    }

    private Optional<AgentIntent> classifyByAgent(String message) {
        if (!StringUtils.hasText(message)) {
            return Optional.of(AgentIntent.GENERAL);
        }
        try {
            String raw = routeAgentClient.classify(message);
            return parseIntent(raw);
        } catch (Exception e) {
            log.warn("Router Agent 分类失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<AgentIntent> parseIntent(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        String normalized = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z_]", " ");
        for (String token : normalized.split("\\s+")) {
            if (SUPPORTED_INTENTS.contains(token)) {
                return Optional.of(AgentIntent.valueOf(token));
            }
        }
        return Optional.empty();
    }

    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return "RULE";
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        if ("AGENT".equals(normalized) || "HYBRID".equals(normalized)) {
            return normalized;
        }
        return "RULE";
    }

    private ResolvedRouteMode resolveMode(String message, String conversationId) {
        if (!aiProperties.isRouteAbExperimentEnabled()) {
            return new ResolvedRouteMode(normalizeMode(aiProperties.getRouteMode()), "off", false);
        }
        if (!experimentGuardService.allowRouteExperiment()) {
            String mode = normalizeMode(aiProperties.getRouteAbControlMode());
            return new ResolvedRouteMode(mode, "rollback_control", false);
        }
        int rollout = normalizePercent(aiProperties.getRouteAbRolloutPercent());
        int bucket = bucket(conversationId, message);
        boolean experiment = bucket < rollout;
        String selected = experiment
                ? aiProperties.getRouteAbExperimentMode()
                : aiProperties.getRouteAbControlMode();
        String mode = normalizeMode(selected);
        String group = experiment ? "experiment" : "control";
        boolean experimentalBranch = experiment && !"RULE".equals(mode);
        if (log.isDebugEnabled()) {
            log.debug("Route A/B 决策: bucket={}, rollout={}, group={}, mode={}",
                    bucket, rollout, group, mode);
        }
        return new ResolvedRouteMode(mode, group, experimentalBranch);
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
}
