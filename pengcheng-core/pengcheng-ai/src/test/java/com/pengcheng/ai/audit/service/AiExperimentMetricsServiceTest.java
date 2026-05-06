package com.pengcheng.ai.audit.service;

import com.pengcheng.ai.audit.entity.AiToolCallLog;
import com.pengcheng.ai.audit.mapper.AiToolCallLogMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiExperimentMetricsServiceTest {

    @Test
    void shouldAggregateRouteExperimentStatsByDayAndGroup() {
        AiToolCallLogMapper mapper = mock(AiToolCallLogMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                log("ADMIN", 1, 120L,
                        "mode:sync|route:REPORT|routeMode:HYBRID|routeExp:experiment|promptExp:experiment|promptVersion:v2",
                        "2026-02-20T10:00:00"),
                log("ADMIN", 0, 300L,
                        "mode:sync|route:REPORT|routeMode:HYBRID|routeExp:experiment|promptExp:experiment|promptVersion:v2",
                        "2026-02-20T11:00:00"),
                log("ADMIN", 1, 200L,
                        "mode:sync|route:REPORT|routeMode:RULE|routeExp:control|promptExp:control|promptVersion:v1",
                        "2026-02-20T12:00:00"),
                log("APP", 1, 80L,
                        "mode:sync|route:GENERAL|routeMode:RULE|routeExp:rollback_control|promptExp:not_applicable|promptVersion:not_applicable",
                        "2026-02-21T08:00:00")
        ));
        AiExperimentMetricsService service = new AiExperimentMetricsService(mapper);

        AiExperimentMetricsService.RouteExperimentStatsResponse response = service.queryRouteStats(
                LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 2, 21),
                null
        );

        assertThat(response.items()).hasSize(3);

        AiExperimentMetricsService.RouteExperimentStat experiment = response.items().stream()
                .filter(item -> item.date().equals(LocalDate.of(2026, 2, 20))
                        && "ADMIN".equals(item.scene())
                        && "experiment".equals(item.experimentGroup()))
                .findFirst()
                .orElseThrow();
        assertThat(experiment.total()).isEqualTo(2);
        assertThat(experiment.success()).isEqualTo(1);
        assertThat(experiment.failure()).isEqualTo(1);
        assertThat(experiment.successRate()).isEqualTo(0.5D);
        assertThat(experiment.avgLatencyMs()).isEqualTo(210L);

        assertThat(response.summary().total()).isEqualTo(4);
        assertThat(response.summary().success()).isEqualTo(3);
        assertThat(response.summary().failure()).isEqualTo(1);
        assertThat(response.summary().successRate()).isEqualTo(0.75D);
        assertThat(response.summary().avgLatencyMs()).isEqualTo(175L);

        AiExperimentMetricsService.PromptExperimentStatsResponse promptResponse = service.queryPromptStats(
                LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 2, 21),
                null
        );
        assertThat(promptResponse.items()).hasSize(3);
        AiExperimentMetricsService.PromptExperimentStat promptExperiment = promptResponse.items().stream()
                .filter(item -> item.date().equals(LocalDate.of(2026, 2, 20))
                        && "ADMIN".equals(item.scene())
                        && "experiment".equals(item.experimentGroup())
                        && "v2".equals(item.promptVersion()))
                .findFirst()
                .orElseThrow();
        assertThat(promptExperiment.total()).isEqualTo(2);
        assertThat(promptExperiment.success()).isEqualTo(1);
        assertThat(promptExperiment.successRate()).isEqualTo(0.5D);
        assertThat(promptResponse.summary().total()).isEqualTo(4);
        assertThat(promptResponse.summary().successRate()).isEqualTo(0.75D);
    }

    private AiToolCallLog log(String scene, Integer success, Long latency, String callChain, String createTime) {
        AiToolCallLog log = new AiToolCallLog();
        log.setScene(scene);
        log.setSuccess(success);
        log.setLatencyMs(latency);
        log.setCallChain(callChain);
        log.setCreateTime(LocalDateTime.parse(createTime));
        return log;
    }
}
