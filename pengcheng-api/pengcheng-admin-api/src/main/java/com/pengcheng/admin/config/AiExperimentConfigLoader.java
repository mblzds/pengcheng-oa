package com.pengcheng.admin.config;

import com.pengcheng.admin.service.ai.AiExperimentConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * AI 实验配置加载器：启动时从系统配置中心加载并覆盖 yml 默认值。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiExperimentConfigLoader implements ApplicationRunner {

    private final AiExperimentConfigService configService;

    @Override
    public void run(ApplicationArguments args) {
        AiExperimentConfigService.ExperimentConfigSnapshot snapshot = configService.reloadFromSystemConfig();
        log.info("AI实验配置已加载: routeAb={}, routeRollout={}, promptAb={}, promptRollout={}, rollback={}, threshold={}, cooldown={}s",
                snapshot.routeAbExperimentEnabled(),
                snapshot.routeAbRolloutPercent(),
                snapshot.promptAbExperimentEnabled(),
                snapshot.promptAbRolloutPercent(),
                snapshot.experimentAutoRollbackEnabled(),
                snapshot.experimentFailureThreshold(),
                snapshot.experimentCooldownSeconds());
    }
}
