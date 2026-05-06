package com.pengcheng.ai.orchestration.router;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.ai.experiment.AiExperimentGuardService;
import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AgentRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class RouteOfflineEvaluationTest {

    private static final String DATASET_PATH = "evals/route-offline-eval.jsonl";
    private static final double RULE_ROUTER_BASELINE = 0.90;
    private static final double ORCHESTRATOR_ROUTER_BASELINE = 0.90;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ruleRouterAccuracyShouldMeetBaseline() throws Exception {
        List<EvalSample> samples = loadSamples();
        AgentRouteService routeService = new AgentRouteService();

        long correct = samples.stream()
                .filter(sample -> routeService.route(sample.message()) == sample.expectedIntent())
                .count();
        double accuracy = (double) correct / samples.size();

        assertThat(accuracy)
                .withFailMessage("Rule router accuracy %.2f below baseline %.2f", accuracy, RULE_ROUTER_BASELINE)
                .isGreaterThanOrEqualTo(RULE_ROUTER_BASELINE);
    }

    @Test
    void agentRouterRuleModeAccuracyShouldMeetBaseline() throws Exception {
        List<EvalSample> samples = loadSamples();
        AiProperties properties = new AiProperties();
        properties.setRouteMode("rule");
        AgentRouterService routerService = new AgentRouterService(
                new AgentRouteService(),
                message -> "GENERAL",
                properties,
                new AiExperimentGuardService(properties)
        );

        long correct = samples.stream()
                .filter(sample -> routerService.route(sample.message(), "offline-eval") == sample.expectedIntent())
                .count();
        double accuracy = (double) correct / samples.size();

        assertThat(accuracy)
                .withFailMessage("AgentRouter(rule mode) accuracy %.2f below baseline %.2f", accuracy,
                        ORCHESTRATOR_ROUTER_BASELINE)
                .isGreaterThanOrEqualTo(ORCHESTRATOR_ROUTER_BASELINE);
    }

    private List<EvalSample> loadSamples() throws Exception {
        ClassPathResource resource = new ClassPathResource(DATASET_PATH);
        List<EvalSample> samples = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(line);
                String message = node.path("message").asText(null);
                String expected = node.path("expected_intent").asText(null);
                if (!StringUtils.hasText(message) || !StringUtils.hasText(expected)) {
                    continue;
                }
                samples.add(new EvalSample(
                        message,
                        AgentIntent.valueOf(expected.toUpperCase(Locale.ROOT))
                ));
            }
        }
        assertThat(samples).isNotEmpty();
        return samples;
    }

    private record EvalSample(String message, AgentIntent expectedIntent) {}
}
