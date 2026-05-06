package com.pengcheng.ai.orchestration.router;

import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.ai.experiment.AiExperimentGuardService;
import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AgentRouteService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRouterServiceTest {

    @Test
    void ruleModeShouldUseRuleRouter() {
        AiProperties properties = new AiProperties();
        properties.setRouteMode("rule");

        AgentRouterService service = new AgentRouterService(
                new AgentRouteService(),
                message -> "GENERAL",
                properties,
                new AiExperimentGuardService(properties)
        );

        assertThat(service.route("帮我生成营销文案"))
                .isEqualTo(AgentIntent.COPYWRITING);
    }

    @Test
    void agentModeShouldUseAgentResult() {
        AiProperties properties = new AiProperties();
        properties.setRouteMode("agent");

        AgentRouterService service = new AgentRouterService(
                new AgentRouteService(),
                message -> "intent: CUSTOMER",
                properties,
                new AiExperimentGuardService(properties)
        );

        assertThat(service.route("查询客户手机号13800138000"))
                .isEqualTo(AgentIntent.CUSTOMER);
    }

    @Test
    void agentModeShouldFallbackToRuleWhenAgentResultInvalid() {
        AiProperties properties = new AiProperties();
        properties.setRouteMode("agent");
        properties.setRouteAgentFallbackToRule(true);

        AgentRouterService service = new AgentRouterService(
                new AgentRouteService(),
                message -> "UNKNOWN",
                properties,
                new AiExperimentGuardService(properties)
        );

        assertThat(service.route("本月业绩报表"))
                .isEqualTo(AgentIntent.REPORT);
    }

    @Test
    void agentModeShouldReturnGeneralWithoutFallback() {
        AiProperties properties = new AiProperties();
        properties.setRouteMode("agent");
        properties.setRouteAgentFallbackToRule(false);

        AgentRouterService service = new AgentRouterService(
                new AgentRouteService(),
                message -> "UNKNOWN",
                properties,
                new AiExperimentGuardService(properties)
        );

        assertThat(service.route("本月业绩报表"))
                .isEqualTo(AgentIntent.GENERAL);
    }

    @Test
    void hybridModeShouldFallbackToRuleOnAgentException() {
        AiProperties properties = new AiProperties();
        properties.setRouteMode("hybrid");
        properties.setRouteAgentFallbackToRule(true);

        AgentRouterService service = new AgentRouterService(
                new AgentRouteService(),
                message -> {
                    throw new IllegalStateException("agent unavailable");
                },
                properties,
                new AiExperimentGuardService(properties)
        );

        assertThat(service.route("知识库流程文档"))
                .isEqualTo(AgentIntent.KNOWLEDGE);
    }

    @Test
    void routeAbShouldUseControlModeWhenRolloutIsZero() {
        AiProperties properties = new AiProperties();
        properties.setRouteAbExperimentEnabled(true);
        properties.setRouteAbRolloutPercent(0);
        properties.setRouteAbControlMode("rule");
        properties.setRouteAbExperimentMode("agent");

        AgentRouterService service = new AgentRouterService(
                new AgentRouteService(),
                message -> "CUSTOMER",
                properties,
                new AiExperimentGuardService(properties)
        );

        assertThat(service.route("帮我生成营销文案", "conv-ab-0"))
                .isEqualTo(AgentIntent.COPYWRITING);
    }

    @Test
    void routeAbShouldUseExperimentModeWhenRolloutIsHundred() {
        AiProperties properties = new AiProperties();
        properties.setRouteAbExperimentEnabled(true);
        properties.setRouteAbRolloutPercent(100);
        properties.setRouteAbControlMode("rule");
        properties.setRouteAbExperimentMode("agent");

        AgentRouterService service = new AgentRouterService(
                new AgentRouteService(),
                message -> "CUSTOMER",
                properties,
                new AiExperimentGuardService(properties)
        );

        assertThat(service.route("帮我生成营销文案", "conv-ab-100"))
                .isEqualTo(AgentIntent.CUSTOMER);
    }

    @Test
    void routeAbShouldRollbackToControlGroupAfterFailureThresholdReached() {
        AiProperties properties = new AiProperties();
        properties.setRouteAbExperimentEnabled(true);
        properties.setRouteAbRolloutPercent(100);
        properties.setRouteAbControlMode("rule");
        properties.setRouteAbExperimentMode("agent");
        properties.setRouteAgentFallbackToRule(true);
        properties.setExperimentAutoRollbackEnabled(true);
        properties.setExperimentFailureThreshold(1);
        properties.setExperimentCooldownSeconds(300);

        AgentRouterService service = new AgentRouterService(
                new AgentRouteService(),
                message -> {
                    throw new IllegalStateException("route agent unavailable");
                },
                properties,
                new AiExperimentGuardService(properties)
        );

        AgentRouterService.RouteDecision first = service.routeDecision("知识库流程文档", "conv-rollback");
        AgentRouterService.RouteDecision second = service.routeDecision("知识库流程文档", "conv-rollback");

        assertThat(first.intent()).isEqualTo(AgentIntent.KNOWLEDGE);
        assertThat(first.experimentGroup()).isEqualTo("experiment");
        assertThat(second.intent()).isEqualTo(AgentIntent.KNOWLEDGE);
        assertThat(second.experimentGroup()).isEqualTo("rollback_control");
        assertThat(second.routeMode()).isEqualTo("RULE");
    }
}
