package com.pengcheng.ai.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pengcheng.ai.audit.entity.AiToolCallLog;
import com.pengcheng.ai.audit.mapper.AiToolCallLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A/B 实验统计服务（基于 AI 工具审计日志）
 */
@Service
@RequiredArgsConstructor
public class AiExperimentMetricsService {

    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 31;

    private final AiToolCallLogMapper auditMapper;

    public RouteExperimentStatsResponse queryRouteStats(LocalDate startDate, LocalDate endDate, String scene) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(DEFAULT_DAYS - 1L);
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        if (start.plusDays(MAX_DAYS - 1L).isBefore(end)) {
            start = end.minusDays(MAX_DAYS - 1L);
        }

        String normalizedScene = normalizeScene(scene);
        List<AiToolCallLog> logs = queryLogs(start, end, normalizedScene);
        Map<GroupKey, MutableStats> grouped = new LinkedHashMap<>();
        MutableStats summaryStats = new MutableStats();

        for (AiToolCallLog log : logs) {
            LocalDate date = log.getCreateTime() != null ? log.getCreateTime().toLocalDate() : end;
            String expGroup = parseTag(log.getCallChain(), "routeExp", "unknown");
            String routeMode = parseTag(log.getCallChain(), "routeMode", "UNKNOWN");
            String sceneValue = StringUtils.hasText(log.getScene()) ? log.getScene().toUpperCase(Locale.ROOT) : "UNKNOWN";
            GroupKey key = new GroupKey(date, sceneValue, expGroup, routeMode);
            MutableStats stats = grouped.computeIfAbsent(key, unused -> new MutableStats());
            stats.accept(log.getSuccess(), log.getLatencyMs());
            summaryStats.accept(log.getSuccess(), log.getLatencyMs());
        }

        List<RouteExperimentStat> items = grouped.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<GroupKey, MutableStats> e) -> e.getKey().date())
                        .thenComparing(e -> e.getKey().scene())
                        .thenComparing(e -> e.getKey().experimentGroup())
                        .thenComparing(e -> e.getKey().routeMode()))
                .map(entry -> {
                    GroupKey key = entry.getKey();
                    MutableStats stats = entry.getValue();
                    return new RouteExperimentStat(
                            key.date(),
                            key.scene(),
                            key.experimentGroup(),
                            key.routeMode(),
                            stats.total,
                            stats.success,
                            stats.failure(),
                            ratio(stats.success, stats.total),
                            stats.averageLatencyMs()
                    );
                })
                .toList();

        RouteExperimentSummary summary = new RouteExperimentSummary(
                summaryStats.total,
                summaryStats.success,
                summaryStats.failure(),
                ratio(summaryStats.success, summaryStats.total),
                summaryStats.averageLatencyMs()
        );
        return new RouteExperimentStatsResponse(start, end, items, summary);
    }

    public PromptExperimentStatsResponse queryPromptStats(LocalDate startDate, LocalDate endDate, String scene) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(DEFAULT_DAYS - 1L);
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        if (start.plusDays(MAX_DAYS - 1L).isBefore(end)) {
            start = end.minusDays(MAX_DAYS - 1L);
        }

        String normalizedScene = normalizeScene(scene);
        List<AiToolCallLog> logs = queryPromptLogs(start, end, normalizedScene);
        Map<PromptGroupKey, MutableStats> grouped = new LinkedHashMap<>();
        MutableStats summaryStats = new MutableStats();

        for (AiToolCallLog log : logs) {
            LocalDate date = log.getCreateTime() != null ? log.getCreateTime().toLocalDate() : end;
            String expGroup = parseTag(log.getCallChain(), "promptExp", "unknown");
            String version = parseTag(log.getCallChain(), "promptVersion", "unknown");
            String sceneValue = StringUtils.hasText(log.getScene()) ? log.getScene().toUpperCase(Locale.ROOT) : "UNKNOWN";
            PromptGroupKey key = new PromptGroupKey(date, sceneValue, expGroup, version);
            MutableStats stats = grouped.computeIfAbsent(key, unused -> new MutableStats());
            stats.accept(log.getSuccess(), log.getLatencyMs());
            summaryStats.accept(log.getSuccess(), log.getLatencyMs());
        }

        List<PromptExperimentStat> items = grouped.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<PromptGroupKey, MutableStats> e) -> e.getKey().date())
                        .thenComparing(e -> e.getKey().scene())
                        .thenComparing(e -> e.getKey().experimentGroup())
                        .thenComparing(e -> e.getKey().promptVersion()))
                .map(entry -> {
                    PromptGroupKey key = entry.getKey();
                    MutableStats stats = entry.getValue();
                    return new PromptExperimentStat(
                            key.date(),
                            key.scene(),
                            key.experimentGroup(),
                            key.promptVersion(),
                            stats.total,
                            stats.success,
                            stats.failure(),
                            ratio(stats.success, stats.total),
                            stats.averageLatencyMs()
                    );
                })
                .toList();

        PromptExperimentSummary summary = new PromptExperimentSummary(
                summaryStats.total,
                summaryStats.success,
                summaryStats.failure(),
                ratio(summaryStats.success, summaryStats.total),
                summaryStats.averageLatencyMs()
        );
        return new PromptExperimentStatsResponse(start, end, items, summary);
    }

    private List<AiToolCallLog> queryLogs(LocalDate start, LocalDate end, String scene) {
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTimeExclusive = end.plusDays(1).atStartOfDay();
        QueryWrapper<AiToolCallLog> wrapper = new QueryWrapper<>();
        wrapper.select(
                "scene",
                "success",
                "latency_ms",
                "call_chain",
                "create_time"
        );
        wrapper.ge("create_time", startTime)
                .lt("create_time", endTimeExclusive)
                .like("call_chain", "routeExp:");
        if (StringUtils.hasText(scene)) {
            wrapper.eq("scene", scene);
        }
        return new ArrayList<>(auditMapper.selectList(wrapper));
    }

    private List<AiToolCallLog> queryPromptLogs(LocalDate start, LocalDate end, String scene) {
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTimeExclusive = end.plusDays(1).atStartOfDay();
        QueryWrapper<AiToolCallLog> wrapper = new QueryWrapper<>();
        wrapper.select(
                "scene",
                "success",
                "latency_ms",
                "call_chain",
                "create_time"
        );
        wrapper.ge("create_time", startTime)
                .lt("create_time", endTimeExclusive)
                .like("call_chain", "promptExp:");
        if (StringUtils.hasText(scene)) {
            wrapper.eq("scene", scene);
        }
        return new ArrayList<>(auditMapper.selectList(wrapper));
    }

    private String parseTag(String callChain, String key, String defaultValue) {
        if (!StringUtils.hasText(callChain)) {
            return defaultValue;
        }
        String prefix = key + ":";
        for (String token : callChain.split("\\|")) {
            if (token.startsWith(prefix)) {
                String value = token.substring(prefix.length());
                return StringUtils.hasText(value) ? value : defaultValue;
            }
        }
        return defaultValue;
    }

    private String normalizeScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            return null;
        }
        return scene.trim().toUpperCase(Locale.ROOT);
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record GroupKey(LocalDate date, String scene, String experimentGroup, String routeMode) {}

    private record PromptGroupKey(LocalDate date, String scene, String experimentGroup, String promptVersion) {}

    private static final class MutableStats {
        private long total = 0;
        private long success = 0;
        private long latencySum = 0;

        void accept(Integer successFlag, Long latencyMs) {
            total++;
            if (Integer.valueOf(1).equals(successFlag)) {
                success++;
            }
            latencySum += (latencyMs != null ? latencyMs : 0L);
        }

        long failure() {
            return total - success;
        }

        long averageLatencyMs() {
            return total <= 0 ? 0 : latencySum / total;
        }
    }

    public record RouteExperimentStatsResponse(
            LocalDate startDate,
            LocalDate endDate,
            List<RouteExperimentStat> items,
            RouteExperimentSummary summary
    ) {}

    public record RouteExperimentStat(
            LocalDate date,
            String scene,
            String experimentGroup,
            String routeMode,
            long total,
            long success,
            long failure,
            double successRate,
            long avgLatencyMs
    ) {}

    public record RouteExperimentSummary(
            long total,
            long success,
            long failure,
            double successRate,
            long avgLatencyMs
    ) {}

    public record PromptExperimentStatsResponse(
            LocalDate startDate,
            LocalDate endDate,
            List<PromptExperimentStat> items,
            PromptExperimentSummary summary
    ) {}

    public record PromptExperimentStat(
            LocalDate date,
            String scene,
            String experimentGroup,
            String promptVersion,
            long total,
            long success,
            long failure,
            double successRate,
            long avgLatencyMs
    ) {}

    public record PromptExperimentSummary(
            long total,
            long success,
            long failure,
            double successRate,
            long avgLatencyMs
    ) {}
}
