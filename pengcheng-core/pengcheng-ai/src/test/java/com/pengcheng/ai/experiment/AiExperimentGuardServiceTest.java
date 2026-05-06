package com.pengcheng.ai.experiment;

import com.pengcheng.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiExperimentGuardServiceTest {

    @Test
    void shouldBlockRouteExperimentAfterThresholdReached() {
        AiProperties properties = new AiProperties();
        properties.setExperimentAutoRollbackEnabled(true);
        properties.setExperimentFailureThreshold(2);
        properties.setExperimentCooldownSeconds(300);

        AiExperimentGuardService service = new AiExperimentGuardService(properties);

        assertThat(service.allowRouteExperiment()).isTrue();
        service.onRouteExperimentFailure();
        assertThat(service.allowRouteExperiment()).isTrue();
        service.onRouteExperimentFailure();
        assertThat(service.allowRouteExperiment()).isFalse();
    }

    @Test
    void shouldBlockPromptExperimentAfterThresholdReached() {
        AiProperties properties = new AiProperties();
        properties.setExperimentAutoRollbackEnabled(true);
        properties.setExperimentFailureThreshold(1);
        properties.setExperimentCooldownSeconds(300);

        AiExperimentGuardService service = new AiExperimentGuardService(properties);

        assertThat(service.allowPromptExperiment()).isTrue();
        service.onPromptExperimentFailure();
        assertThat(service.allowPromptExperiment()).isFalse();
    }

    @Test
    void shouldResetRouteGuardState() {
        AiProperties properties = new AiProperties();
        properties.setExperimentAutoRollbackEnabled(true);
        properties.setExperimentFailureThreshold(1);
        properties.setExperimentCooldownSeconds(300);

        AiExperimentGuardService service = new AiExperimentGuardService(properties);
        service.onRouteExperimentFailure();
        assertThat(service.allowRouteExperiment()).isFalse();

        service.resetRouteGuard();
        AiExperimentGuardService.ExperimentGuardStatus status = service.currentStatus();

        assertThat(service.allowRouteExperiment()).isTrue();
        assertThat(status.routeFailureCount()).isZero();
        assertThat(status.routeBlockedUntilEpochMs()).isZero();
    }

    @Test
    void shouldSupportManualBlockForPromptExperiment() {
        AiProperties properties = new AiProperties();
        properties.setExperimentCooldownSeconds(300);

        RecordingAlertService alertService = new RecordingAlertService();
        AiExperimentGuardService service = new AiExperimentGuardService(properties, alertService);
        long now = System.currentTimeMillis();
        service.blockPromptExperiment(120);
        AiExperimentGuardService.ExperimentGuardStatus status = service.currentStatus();

        assertThat(service.allowPromptExperiment()).isFalse();
        assertThat(status.promptBlockedUntilEpochMs()).isGreaterThan(now);
        assertThat(alertService.manualBlockType).isEqualTo("prompt");
        assertThat(alertService.manualBlockCooldownSeconds).isEqualTo(120);
        assertThat(alertService.manualBlockBlockedUntilEpochMs).isEqualTo(status.promptBlockedUntilEpochMs());
    }

    @Test
    void shouldAlertWhenRouteAutoRollbackTriggered() {
        AiProperties properties = new AiProperties();
        properties.setExperimentAutoRollbackEnabled(true);
        properties.setExperimentFailureThreshold(2);
        properties.setExperimentCooldownSeconds(300);

        RecordingAlertService alertService = new RecordingAlertService();
        AiExperimentGuardService service = new AiExperimentGuardService(properties, alertService);
        service.onRouteExperimentFailure();
        service.onRouteExperimentFailure();

        AiExperimentGuardService.ExperimentGuardStatus status = service.currentStatus();
        assertThat(alertService.autoRollbackType).isEqualTo("route");
        assertThat(alertService.autoRollbackThreshold).isEqualTo(2);
        assertThat(alertService.autoRollbackCooldownSeconds).isEqualTo(300);
        assertThat(alertService.autoRollbackBlockedUntilEpochMs).isEqualTo(status.routeBlockedUntilEpochMs());
    }

    private static final class RecordingAlertService extends AiExperimentAlertService {

        private String autoRollbackType;
        private Integer autoRollbackThreshold;
        private Integer autoRollbackCooldownSeconds;
        private Long autoRollbackBlockedUntilEpochMs;

        private String manualBlockType;
        private Integer manualBlockCooldownSeconds;
        private Long manualBlockBlockedUntilEpochMs;

        private RecordingAlertService() {
            super(new AiProperties(), null, new ObjectMapper());
        }

        @Override
        public void notifyAutoRollback(String experimentType, int failureThreshold, int cooldownSeconds, long blockedUntilEpochMs) {
            this.autoRollbackType = experimentType;
            this.autoRollbackThreshold = failureThreshold;
            this.autoRollbackCooldownSeconds = cooldownSeconds;
            this.autoRollbackBlockedUntilEpochMs = blockedUntilEpochMs;
        }

        @Override
        public void notifyManualBlock(String experimentType, int cooldownSeconds, long blockedUntilEpochMs) {
            this.manualBlockType = experimentType;
            this.manualBlockCooldownSeconds = cooldownSeconds;
            this.manualBlockBlockedUntilEpochMs = blockedUntilEpochMs;
        }
    }
}
