package com.pengcheng.ai.experiment;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.ai.experiment.entity.AiExperimentAlertLog;
import com.pengcheng.ai.experiment.mapper.AiExperimentAlertLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AiExperimentAlertServiceTest {

    private final List<AiExperimentAlertLog> savedLogs = new ArrayList<>();
    private final AtomicInteger insertCount = new AtomicInteger(0);

    private AiProperties aiProperties;
    private AiExperimentAlertService alertService;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.setExperimentAlertEnabled(true);
        aiProperties.setExperimentAlertSuppressSeconds(300);
        alertService = new AiExperimentAlertService(
                aiProperties,
                mapperProxy(savedLogs, insertCount),
                new ObjectMapper()
        );
        savedLogs.clear();
        insertCount.set(0);
    }

    @Test
    void shouldPersistAutoRollbackAlert() {
        alertService.notifyAutoRollback("route", 3, 120, 1700000000000L);

        assertThat(insertCount.get()).isEqualTo(1);
        assertThat(savedLogs).hasSize(1);
        AiExperimentAlertLog item = savedLogs.get(0);
        assertThat(item.getAlertType()).isEqualTo("auto_rollback");
        assertThat(item.getExperimentType()).isEqualTo("route");
        assertThat(item.getSuppressed()).isEqualTo(0);
        assertThat(item.getSuppressedUntilEpochMs()).isNotNull();
    }

    @Test
    void shouldSuppressDuplicateAlertWithinSuppressionWindow() {
        alertService.notifyManualBlock("prompt", 60, 1700000000000L);
        alertService.notifyManualBlock("prompt", 60, 1700000001000L);

        assertThat(insertCount.get()).isEqualTo(2);
        assertThat(savedLogs).hasSize(2);
        assertThat(savedLogs.get(0).getSuppressed()).isEqualTo(0);
        assertThat(savedLogs.get(1).getSuppressed()).isEqualTo(1);
        assertThat(savedLogs.get(1).getSuppressedUntilEpochMs()).isNotNull();
    }

    @Test
    void shouldResetSuppressionState() {
        alertService.notifyManualBlock("route", 30, 1700000000000L);

        AiExperimentAlertService.AlertSuppressionState state = alertService.resetSuppressionState();

        assertThat(state.clearedKeyCount()).isEqualTo(1);
        assertThat(state.suppressSeconds()).isEqualTo(300);
    }

    @Test
    void shouldSkipAlertWhenDisabled() {
        aiProperties.setExperimentAlertEnabled(false);

        alertService.notifyAutoRollback("route", 2, 60, 1700000000000L);

        assertThat(insertCount.get()).isZero();
        assertThat(savedLogs).isEmpty();
    }

    private static AiExperimentAlertLogMapper mapperProxy(List<AiExperimentAlertLog> savedLogs, AtomicInteger insertCount) {
        return (AiExperimentAlertLogMapper) Proxy.newProxyInstance(
                AiExperimentAlertLogMapper.class.getClassLoader(),
                new Class[]{AiExperimentAlertLogMapper.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if (Objects.equals(methodName, "insert")) {
                        AiExperimentAlertLog entity = (AiExperimentAlertLog) args[0];
                        savedLogs.add(entity);
                        insertCount.incrementAndGet();
                        return 1;
                    }
                    if (Objects.equals(methodName, "selectList")) {
                        return new ArrayList<>(savedLogs);
                    }
                    if (Objects.equals(methodName, "equals")) {
                        return proxy == args[0];
                    }
                    if (Objects.equals(methodName, "hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (Objects.equals(methodName, "toString")) {
                        return "AiExperimentAlertLogMapperProxy";
                    }
                    if (Objects.equals(methodName, "selectCount")) {
                        return Long.valueOf(savedLogs.size());
                    }
                    if (Objects.equals(methodName, "selectOne")) {
                        return savedLogs.isEmpty() ? null : savedLogs.get(savedLogs.size() - 1);
                    }
                    if (Objects.equals(methodName, "delete")) {
                        return 0;
                    }
                    if (Objects.equals(methodName, "update")) {
                        return 0;
                    }
                    if (Objects.equals(methodName, "selectById")) {
                        return null;
                    }
                    if (Objects.equals(methodName, "selectObjs")) {
                        return List.of();
                    }
                    if (Objects.equals(methodName, "selectMaps")) {
                        return List.of();
                    }
                    if (Objects.equals(methodName, "selectBatchIds")) {
                        return List.of();
                    }
                    if (Objects.equals(methodName, "selectPage")) {
                        return args[0];
                    }
                    if (Objects.equals(methodName, "deleteById")) {
                        return 0;
                    }
                    if (Objects.equals(methodName, "deleteBatchIds")) {
                        return 0;
                    }
                    if (Objects.equals(methodName, "deleteByMap")) {
                        return 0;
                    }
                    if (Objects.equals(methodName, "updateById")) {
                        return 0;
                    }
                    if (Objects.equals(methodName, "selectMapsPage")) {
                        return args[0];
                    }
                    if (method.getParameterCount() == 1 && Wrapper.class.isAssignableFrom(method.getParameterTypes()[0])) {
                        return null;
                    }
                    return null;
                }
        );
    }
}
