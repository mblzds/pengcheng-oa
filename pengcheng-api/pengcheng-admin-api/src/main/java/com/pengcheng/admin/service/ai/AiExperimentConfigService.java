package com.pengcheng.admin.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.admin.entity.ai.AiExperimentConfigAudit;
import com.pengcheng.admin.mapper.ai.AiExperimentConfigAuditMapper;
import com.pengcheng.system.entity.SysConfigGroup;
import com.pengcheng.system.mapper.SysConfigGroupMapper;
import com.pengcheng.system.service.SysConfigGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 实验配置服务（读取/更新/持久化/应用）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiExperimentConfigService {

    public static final String GROUP_CODE = "aiExperiment";
    private static final Set<String> ROUTE_MODES = Set.of("rule", "agent", "hybrid");
    private static final Set<String> PROMPT_VERSIONS = Set.of("v1", "v2");
    private static final Set<String> ALERT_CHANNELS = Set.of("notice", "email", "webhook");

    private final AiProperties aiProperties;
    private final SysConfigGroupService configGroupService;
    private final SysConfigGroupMapper configGroupMapper;
    private final AiExperimentConfigAuditMapper configAuditMapper;
    private final ObjectMapper objectMapper;

    public ExperimentConfigSnapshot currentSnapshot() {
        return new ExperimentConfigSnapshot(
                aiProperties.isRouteAbExperimentEnabled(),
                aiProperties.getRouteAbRolloutPercent(),
                aiProperties.getRouteAbControlMode(),
                aiProperties.getRouteAbExperimentMode(),
                aiProperties.isPromptAbExperimentEnabled(),
                aiProperties.getPromptAbRolloutPercent(),
                aiProperties.getPromptAbControlVersion(),
                aiProperties.getPromptAbExperimentVersion(),
                aiProperties.isExperimentAutoRollbackEnabled(),
                aiProperties.getExperimentFailureThreshold(),
                aiProperties.getExperimentCooldownSeconds(),
                aiProperties.isExperimentAlertEnabled(),
                aiProperties.getExperimentAlertSuppressSeconds(),
                aiProperties.isExperimentAlertEmailEnabled(),
                aiProperties.getExperimentAlertEmailRecipients(),
                aiProperties.isExperimentAlertWebhookEnabled(),
                aiProperties.getExperimentAlertWebhookUrls(),
                aiProperties.getExperimentAlertWebhookTimeoutSeconds(),
                aiProperties.getExperimentAlertCriticalChannels(),
                aiProperties.getExperimentAlertWarningChannels(),
                aiProperties.isExperimentAlertDeliveryRetryEnabled(),
                aiProperties.getExperimentAlertDeliveryMaxAttempts(),
                aiProperties.getExperimentAlertDeliveryRetryDelaySeconds(),
                aiProperties.isExperimentAlertDeliveryHealthCheckEnabled(),
                aiProperties.getExperimentAlertDeliveryHealthCheckDays(),
                aiProperties.getExperimentAlertDeliveryHealthDeadRateThreshold(),
                aiProperties.getExperimentAlertDeliveryHealthPendingRateThreshold(),
                aiProperties.getExperimentAlertDeliveryHealthEscalationCooldownSeconds()
        );
    }

    public ExperimentConfigSnapshot updateAndPersist(ExperimentConfigUpdateRequest request) {
        return updateAndPersist(request, null);
    }

    public ExperimentConfigSnapshot updateAndPersist(ExperimentConfigUpdateRequest request, OperatorInfo operatorInfo) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        ExperimentConfigSnapshot merged = merge(currentSnapshot(), request);
        persist(merged, "update", "api", operatorInfo, null, null);
        apply(merged);
        return currentSnapshot();
    }

    public ExperimentConfigSnapshot reloadFromSystemConfig() {
        try {
            SysConfigGroup group = configGroupService.getByGroupCode(GROUP_CODE);
            if (group == null || !StringUtils.hasText(group.getConfigValue())) {
                return currentSnapshot();
            }
            JsonNode root = objectMapper.readTree(group.getConfigValue());
            ExperimentConfigSnapshot merged = merge(currentSnapshot(), fromJson(root));
            apply(merged);
            return currentSnapshot();
        } catch (Exception ex) {
            log.warn("加载 AI 实验配置失败，继续使用当前内存配置: {}", ex.getMessage());
            return currentSnapshot();
        }
    }

    public List<ConfigAuditItem> listRecentAudits(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        QueryWrapper<AiExperimentConfigAudit> wrapper = new QueryWrapper<>();
        wrapper.eq("group_code", GROUP_CODE)
                .orderByDesc("id")
                .last("limit " + safeLimit);
        return configAuditMapper.selectList(wrapper).stream()
                .map(this::toConfigAuditItem)
                .toList();
    }

    public ExperimentConfigSnapshot rollbackTo(Long auditId, OperatorInfo operatorInfo) {
        if (auditId == null || auditId < 1) {
            throw new IllegalArgumentException("auditId 必须大于 0");
        }
        AiExperimentConfigAudit audit = configAuditMapper.selectById(auditId);
        if (audit == null || !GROUP_CODE.equals(audit.getGroupCode())) {
            throw new IllegalArgumentException("未找到对应审计记录");
        }
        if (!StringUtils.hasText(audit.getConfigValue())) {
            throw new IllegalArgumentException("审计记录配置内容为空，无法回滚");
        }
        try {
            JsonNode node = objectMapper.readTree(audit.getConfigValue());
            ExperimentConfigSnapshot rollbackSnapshot = merge(currentSnapshot(), fromJson(node));
            persist(rollbackSnapshot, "rollback", "api", operatorInfo, audit.getId(),
                    "rollback to auditId=" + audit.getId());
            apply(rollbackSnapshot);
            return currentSnapshot();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("回滚失败: " + ex.getMessage(), ex);
        }
    }

    private void persist(ExperimentConfigSnapshot snapshot,
                         String changeType,
                         String source,
                         OperatorInfo operatorInfo,
                         Long rollbackFromAuditId,
                         String remark) {
        ensureGroupExists();
        try {
            String configValue = objectMapper.writeValueAsString(snapshot);
            String previousConfigValue = readCurrentConfigValue();
            configGroupService.saveConfig(GROUP_CODE, configValue);
            insertAudit(changeType, source, operatorInfo, rollbackFromAuditId, previousConfigValue, configValue, remark);
        } catch (Exception ex) {
            throw new IllegalStateException("保存 AI 实验配置失败: " + ex.getMessage(), ex);
        }
    }

    private String readCurrentConfigValue() {
        SysConfigGroup group = configGroupService.getByGroupCode(GROUP_CODE);
        if (group == null || !StringUtils.hasText(group.getConfigValue())) {
            return null;
        }
        return group.getConfigValue();
    }

    private void insertAudit(String changeType,
                             String source,
                             OperatorInfo operatorInfo,
                             Long rollbackFromAuditId,
                             String previousConfigValue,
                             String configValue,
                             String remark) {
        if (Objects.equals(previousConfigValue, configValue)) {
            return;
        }
        AiExperimentConfigAudit audit = new AiExperimentConfigAudit();
        audit.setGroupCode(GROUP_CODE);
        audit.setChangeType(changeType);
        audit.setSource(StringUtils.hasText(source) ? source : "api");
        audit.setOperatorId(operatorInfo != null ? operatorInfo.operatorId() : null);
        audit.setOperatorName(operatorInfo != null ? operatorInfo.operatorName() : null);
        audit.setRollbackFromAuditId(rollbackFromAuditId);
        audit.setPreviousConfigValue(previousConfigValue);
        audit.setConfigValue(configValue);
        audit.setRemark(remark);
        audit.setCreateTime(LocalDateTime.now());
        configAuditMapper.insert(audit);
    }

    private void ensureGroupExists() {
        SysConfigGroup existing = configGroupService.getByGroupCode(GROUP_CODE);
        if (existing != null) {
            return;
        }
        SysConfigGroup created = new SysConfigGroup();
        created.setGroupCode(GROUP_CODE);
        created.setGroupName("AI实验配置");
        created.setGroupIcon(null);
        created.setConfigValue("{}");
        created.setSort(16);
        created.setStatus(1);
        created.setRemark("AI路由与提示词实验配置");
        configGroupMapper.insert(created);
        configGroupService.refreshCache();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        return Math.max(1, Math.min(limit, 100));
    }

    private ConfigAuditItem toConfigAuditItem(AiExperimentConfigAudit audit) {
        return new ConfigAuditItem(
                audit.getId(),
                audit.getChangeType(),
                audit.getSource(),
                audit.getOperatorId(),
                audit.getOperatorName(),
                audit.getRollbackFromAuditId(),
                audit.getPreviousConfigValue(),
                audit.getConfigValue(),
                audit.getRemark(),
                audit.getCreateTime()
        );
    }

    private void apply(ExperimentConfigSnapshot snapshot) {
        aiProperties.setRouteAbExperimentEnabled(snapshot.routeAbExperimentEnabled());
        aiProperties.setRouteAbRolloutPercent(snapshot.routeAbRolloutPercent());
        aiProperties.setRouteAbControlMode(snapshot.routeAbControlMode());
        aiProperties.setRouteAbExperimentMode(snapshot.routeAbExperimentMode());
        aiProperties.setPromptAbExperimentEnabled(snapshot.promptAbExperimentEnabled());
        aiProperties.setPromptAbRolloutPercent(snapshot.promptAbRolloutPercent());
        aiProperties.setPromptAbControlVersion(snapshot.promptAbControlVersion());
        aiProperties.setPromptAbExperimentVersion(snapshot.promptAbExperimentVersion());
        aiProperties.setExperimentAutoRollbackEnabled(snapshot.experimentAutoRollbackEnabled());
        aiProperties.setExperimentFailureThreshold(snapshot.experimentFailureThreshold());
        aiProperties.setExperimentCooldownSeconds(snapshot.experimentCooldownSeconds());
        aiProperties.setExperimentAlertEnabled(snapshot.experimentAlertEnabled());
        aiProperties.setExperimentAlertSuppressSeconds(snapshot.experimentAlertSuppressSeconds());
        aiProperties.setExperimentAlertEmailEnabled(snapshot.experimentAlertEmailEnabled());
        aiProperties.setExperimentAlertEmailRecipients(snapshot.experimentAlertEmailRecipients());
        aiProperties.setExperimentAlertWebhookEnabled(snapshot.experimentAlertWebhookEnabled());
        aiProperties.setExperimentAlertWebhookUrls(snapshot.experimentAlertWebhookUrls());
        aiProperties.setExperimentAlertWebhookTimeoutSeconds(snapshot.experimentAlertWebhookTimeoutSeconds());
        aiProperties.setExperimentAlertCriticalChannels(snapshot.experimentAlertCriticalChannels());
        aiProperties.setExperimentAlertWarningChannels(snapshot.experimentAlertWarningChannels());
        aiProperties.setExperimentAlertDeliveryRetryEnabled(snapshot.experimentAlertDeliveryRetryEnabled());
        aiProperties.setExperimentAlertDeliveryMaxAttempts(snapshot.experimentAlertDeliveryMaxAttempts());
        aiProperties.setExperimentAlertDeliveryRetryDelaySeconds(snapshot.experimentAlertDeliveryRetryDelaySeconds());
        aiProperties.setExperimentAlertDeliveryHealthCheckEnabled(snapshot.experimentAlertDeliveryHealthCheckEnabled());
        aiProperties.setExperimentAlertDeliveryHealthCheckDays(snapshot.experimentAlertDeliveryHealthCheckDays());
        aiProperties.setExperimentAlertDeliveryHealthDeadRateThreshold(snapshot.experimentAlertDeliveryHealthDeadRateThreshold());
        aiProperties.setExperimentAlertDeliveryHealthPendingRateThreshold(snapshot.experimentAlertDeliveryHealthPendingRateThreshold());
        aiProperties.setExperimentAlertDeliveryHealthEscalationCooldownSeconds(snapshot.experimentAlertDeliveryHealthEscalationCooldownSeconds());
    }

    private ExperimentConfigUpdateRequest fromJson(JsonNode root) {
        return new ExperimentConfigUpdateRequest(
                readBoolean(root, "routeAbExperimentEnabled"),
                readInt(root, "routeAbRolloutPercent"),
                readText(root, "routeAbControlMode"),
                readText(root, "routeAbExperimentMode"),
                readBoolean(root, "promptAbExperimentEnabled"),
                readInt(root, "promptAbRolloutPercent"),
                readText(root, "promptAbControlVersion"),
                readText(root, "promptAbExperimentVersion"),
                readBoolean(root, "experimentAutoRollbackEnabled"),
                readInt(root, "experimentFailureThreshold"),
                readInt(root, "experimentCooldownSeconds"),
                readBoolean(root, "experimentAlertEnabled"),
                readInt(root, "experimentAlertSuppressSeconds"),
                readBoolean(root, "experimentAlertEmailEnabled"),
                readText(root, "experimentAlertEmailRecipients"),
                readBoolean(root, "experimentAlertWebhookEnabled"),
                readText(root, "experimentAlertWebhookUrls"),
                readInt(root, "experimentAlertWebhookTimeoutSeconds"),
                readText(root, "experimentAlertCriticalChannels"),
                readText(root, "experimentAlertWarningChannels"),
                readBoolean(root, "experimentAlertDeliveryRetryEnabled"),
                readInt(root, "experimentAlertDeliveryMaxAttempts"),
                readInt(root, "experimentAlertDeliveryRetryDelaySeconds"),
                readBoolean(root, "experimentAlertDeliveryHealthCheckEnabled"),
                readInt(root, "experimentAlertDeliveryHealthCheckDays"),
                readDouble(root, "experimentAlertDeliveryHealthDeadRateThreshold"),
                readDouble(root, "experimentAlertDeliveryHealthPendingRateThreshold"),
                readInt(root, "experimentAlertDeliveryHealthEscalationCooldownSeconds")
        );
    }

    private Boolean readBoolean(JsonNode root, String key) {
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? null : node.asBoolean();
    }

    private Integer readInt(JsonNode root, String key) {
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? null : node.asInt();
    }

    private String readText(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return StringUtils.hasText(text) ? text : null;
    }

    private Double readDouble(JsonNode root, String key) {
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? null : node.asDouble();
    }

    private ExperimentConfigSnapshot merge(ExperimentConfigSnapshot current, ExperimentConfigUpdateRequest request) {
        boolean routeEnabled = request.routeAbExperimentEnabled() != null
                ? request.routeAbExperimentEnabled()
                : current.routeAbExperimentEnabled();
        int routeRollout = request.routeAbRolloutPercent() != null
                ? normalizePercent(request.routeAbRolloutPercent(), "routeAbRolloutPercent")
                : current.routeAbRolloutPercent();
        String routeControl = request.routeAbControlMode() != null
                ? normalizeRouteMode(request.routeAbControlMode(), "routeAbControlMode")
                : current.routeAbControlMode();
        String routeExperiment = request.routeAbExperimentMode() != null
                ? normalizeRouteMode(request.routeAbExperimentMode(), "routeAbExperimentMode")
                : current.routeAbExperimentMode();

        boolean promptEnabled = request.promptAbExperimentEnabled() != null
                ? request.promptAbExperimentEnabled()
                : current.promptAbExperimentEnabled();
        int promptRollout = request.promptAbRolloutPercent() != null
                ? normalizePercent(request.promptAbRolloutPercent(), "promptAbRolloutPercent")
                : current.promptAbRolloutPercent();
        String promptControl = request.promptAbControlVersion() != null
                ? normalizePromptVersion(request.promptAbControlVersion(), "promptAbControlVersion")
                : current.promptAbControlVersion();
        String promptExperimentVersion = request.promptAbExperimentVersion() != null
                ? normalizePromptVersion(request.promptAbExperimentVersion(), "promptAbExperimentVersion")
                : current.promptAbExperimentVersion();

        boolean rollbackEnabled = request.experimentAutoRollbackEnabled() != null
                ? request.experimentAutoRollbackEnabled()
                : current.experimentAutoRollbackEnabled();
        int failureThreshold = request.experimentFailureThreshold() != null
                ? normalizePositive(request.experimentFailureThreshold(), "experimentFailureThreshold")
                : current.experimentFailureThreshold();
        int cooldownSeconds = request.experimentCooldownSeconds() != null
                ? normalizePositive(request.experimentCooldownSeconds(), "experimentCooldownSeconds")
                : current.experimentCooldownSeconds();
        boolean alertEnabled = request.experimentAlertEnabled() != null
                ? request.experimentAlertEnabled()
                : current.experimentAlertEnabled();
        int alertSuppressSeconds = request.experimentAlertSuppressSeconds() != null
                ? normalizeNonNegative(request.experimentAlertSuppressSeconds(), "experimentAlertSuppressSeconds")
                : current.experimentAlertSuppressSeconds();
        boolean alertEmailEnabled = request.experimentAlertEmailEnabled() != null
                ? request.experimentAlertEmailEnabled()
                : current.experimentAlertEmailEnabled();
        String alertEmailRecipients = request.experimentAlertEmailRecipients() != null
                ? normalizeEmailRecipients(request.experimentAlertEmailRecipients(), "experimentAlertEmailRecipients")
                : current.experimentAlertEmailRecipients();
        boolean alertWebhookEnabled = request.experimentAlertWebhookEnabled() != null
                ? request.experimentAlertWebhookEnabled()
                : current.experimentAlertWebhookEnabled();
        String alertWebhookUrls = request.experimentAlertWebhookUrls() != null
                ? normalizeWebhookUrls(request.experimentAlertWebhookUrls(), "experimentAlertWebhookUrls")
                : current.experimentAlertWebhookUrls();
        int alertWebhookTimeoutSeconds = request.experimentAlertWebhookTimeoutSeconds() != null
                ? normalizeWebhookTimeoutSeconds(request.experimentAlertWebhookTimeoutSeconds(), "experimentAlertWebhookTimeoutSeconds")
                : current.experimentAlertWebhookTimeoutSeconds();
        String alertCriticalChannels = request.experimentAlertCriticalChannels() != null
                ? normalizeAlertChannels(request.experimentAlertCriticalChannels(), "experimentAlertCriticalChannels")
                : current.experimentAlertCriticalChannels();
        String alertWarningChannels = request.experimentAlertWarningChannels() != null
                ? normalizeAlertChannels(request.experimentAlertWarningChannels(), "experimentAlertWarningChannels")
                : current.experimentAlertWarningChannels();
        boolean deliveryRetryEnabled = request.experimentAlertDeliveryRetryEnabled() != null
                ? request.experimentAlertDeliveryRetryEnabled()
                : current.experimentAlertDeliveryRetryEnabled();
        int deliveryMaxAttempts = request.experimentAlertDeliveryMaxAttempts() != null
                ? normalizeRetryMaxAttempts(request.experimentAlertDeliveryMaxAttempts(), "experimentAlertDeliveryMaxAttempts")
                : current.experimentAlertDeliveryMaxAttempts();
        int deliveryRetryDelaySeconds = request.experimentAlertDeliveryRetryDelaySeconds() != null
                ? normalizeRetryDelaySeconds(request.experimentAlertDeliveryRetryDelaySeconds(), "experimentAlertDeliveryRetryDelaySeconds")
                : current.experimentAlertDeliveryRetryDelaySeconds();
        boolean deliveryHealthCheckEnabled = request.experimentAlertDeliveryHealthCheckEnabled() != null
                ? request.experimentAlertDeliveryHealthCheckEnabled()
                : current.experimentAlertDeliveryHealthCheckEnabled();
        int deliveryHealthCheckDays = request.experimentAlertDeliveryHealthCheckDays() != null
                ? normalizeHealthCheckDays(request.experimentAlertDeliveryHealthCheckDays(), "experimentAlertDeliveryHealthCheckDays")
                : current.experimentAlertDeliveryHealthCheckDays();
        double deliveryHealthDeadRateThreshold = request.experimentAlertDeliveryHealthDeadRateThreshold() != null
                ? normalizeRateThreshold(request.experimentAlertDeliveryHealthDeadRateThreshold(), "experimentAlertDeliveryHealthDeadRateThreshold")
                : current.experimentAlertDeliveryHealthDeadRateThreshold();
        double deliveryHealthPendingRateThreshold = request.experimentAlertDeliveryHealthPendingRateThreshold() != null
                ? normalizeRateThreshold(request.experimentAlertDeliveryHealthPendingRateThreshold(), "experimentAlertDeliveryHealthPendingRateThreshold")
                : current.experimentAlertDeliveryHealthPendingRateThreshold();
        int deliveryHealthEscalationCooldownSeconds = request.experimentAlertDeliveryHealthEscalationCooldownSeconds() != null
                ? normalizeHealthEscalationCooldownSeconds(request.experimentAlertDeliveryHealthEscalationCooldownSeconds(),
                "experimentAlertDeliveryHealthEscalationCooldownSeconds")
                : current.experimentAlertDeliveryHealthEscalationCooldownSeconds();

        return new ExperimentConfigSnapshot(
                routeEnabled,
                routeRollout,
                routeControl,
                routeExperiment,
                promptEnabled,
                promptRollout,
                promptControl,
                promptExperimentVersion,
                rollbackEnabled,
                failureThreshold,
                cooldownSeconds,
                alertEnabled,
                alertSuppressSeconds,
                alertEmailEnabled,
                alertEmailRecipients,
                alertWebhookEnabled,
                alertWebhookUrls,
                alertWebhookTimeoutSeconds,
                alertCriticalChannels,
                alertWarningChannels,
                deliveryRetryEnabled,
                deliveryMaxAttempts,
                deliveryRetryDelaySeconds,
                deliveryHealthCheckEnabled,
                deliveryHealthCheckDays,
                deliveryHealthDeadRateThreshold,
                deliveryHealthPendingRateThreshold,
                deliveryHealthEscalationCooldownSeconds
        );
    }

    private int normalizePercent(Integer percent, String field) {
        if (percent == null || percent < 0 || percent > 100) {
            throw new IllegalArgumentException(field + " 必须在 0-100 之间");
        }
        return percent;
    }

    private int normalizePositive(Integer value, String field) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(field + " 必须大于等于 1");
        }
        return value;
    }

    private int normalizeNonNegative(Integer value, String field) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(field + " 必须大于等于 0");
        }
        return value;
    }

    private String normalizeRouteMode(String mode, String field) {
        if (!StringUtils.hasText(mode)) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (!ROUTE_MODES.contains(normalized)) {
            throw new IllegalArgumentException(field + " 仅支持 rule/agent/hybrid");
        }
        return normalized;
    }

    private String normalizePromptVersion(String version, String field) {
        if (!StringUtils.hasText(version)) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        String normalized = version.trim().toLowerCase(Locale.ROOT);
        if (!PROMPT_VERSIONS.contains(normalized)) {
            throw new IllegalArgumentException(field + " 仅支持 v1/v2");
        }
        return normalized;
    }

    private String normalizeEmailRecipients(String recipients, String field) {
        if (!StringUtils.hasText(recipients)) {
            return "";
        }
        String normalized = recipients.trim()
                .replace('；', ',')
                .replace(';', ',');
        if (normalized.length() > 500) {
            throw new IllegalArgumentException(field + " 长度不能超过 500");
        }
        return normalized;
    }

    private String normalizeWebhookUrls(String urls, String field) {
        if (!StringUtils.hasText(urls)) {
            return "";
        }
        String normalized = urls.trim()
                .replace('\n', ',')
                .replace('\r', ',')
                .replace('；', ',')
                .replace(';', ',');
        if (normalized.length() > 2000) {
            throw new IllegalArgumentException(field + " 长度不能超过 2000");
        }
        List<String> urlList = Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (urlList.size() > 20) {
            throw new IllegalArgumentException(field + " 最多支持 20 个地址");
        }
        boolean hasInvalidUrl = urlList.stream()
                .anyMatch(url -> !(url.startsWith("http://") || url.startsWith("https://")));
        if (hasInvalidUrl) {
            throw new IllegalArgumentException(field + " 仅支持 http/https 地址");
        }
        return urlList.stream().collect(Collectors.joining(","));
    }

    private int normalizeWebhookTimeoutSeconds(Integer timeoutSeconds, String field) {
        if (timeoutSeconds == null || timeoutSeconds < 1 || timeoutSeconds > 30) {
            throw new IllegalArgumentException(field + " 必须在 1-30 之间");
        }
        return timeoutSeconds;
    }

    private String normalizeAlertChannels(String channels, String field) {
        if (!StringUtils.hasText(channels)) {
            return "";
        }
        String normalized = channels.trim()
                .replace('\n', ',')
                .replace('\r', ',')
                .replace('；', ',')
                .replace(';', ',')
                .toLowerCase(Locale.ROOT);
        List<String> channelList = Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (channelList.size() > 3) {
            throw new IllegalArgumentException(field + " 最多支持 notice/email/webhook 三类渠道");
        }
        boolean hasInvalidChannel = channelList.stream().anyMatch(channel -> !ALERT_CHANNELS.contains(channel));
        if (hasInvalidChannel) {
            throw new IllegalArgumentException(field + " 仅支持 notice/email/webhook");
        }
        return channelList.stream().collect(Collectors.joining(","));
    }

    private int normalizeRetryMaxAttempts(Integer maxAttempts, String field) {
        if (maxAttempts == null || maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException(field + " 必须在 1-10 之间");
        }
        return maxAttempts;
    }

    private int normalizeRetryDelaySeconds(Integer delaySeconds, String field) {
        if (delaySeconds == null || delaySeconds < 5 || delaySeconds > 3600) {
            throw new IllegalArgumentException(field + " 必须在 5-3600 之间");
        }
        return delaySeconds;
    }

    private int normalizeHealthCheckDays(Integer value, String field) {
        if (value == null || value < 1 || value > 90) {
            throw new IllegalArgumentException(field + " 必须在 1-90 之间");
        }
        return value;
    }

    private double normalizeRateThreshold(Double value, String field) {
        if (value == null || value.isNaN() || value < 0d || value > 1d) {
            throw new IllegalArgumentException(field + " 必须在 0-1 之间");
        }
        return value;
    }

    private int normalizeHealthEscalationCooldownSeconds(Integer value, String field) {
        if (value == null || value < 30 || value > 86400) {
            throw new IllegalArgumentException(field + " 必须在 30-86400 之间");
        }
        return value;
    }

    public record ExperimentConfigSnapshot(
            boolean routeAbExperimentEnabled,
            int routeAbRolloutPercent,
            String routeAbControlMode,
            String routeAbExperimentMode,
            boolean promptAbExperimentEnabled,
            int promptAbRolloutPercent,
            String promptAbControlVersion,
            String promptAbExperimentVersion,
            boolean experimentAutoRollbackEnabled,
            int experimentFailureThreshold,
            int experimentCooldownSeconds,
            boolean experimentAlertEnabled,
            int experimentAlertSuppressSeconds,
            boolean experimentAlertEmailEnabled,
            String experimentAlertEmailRecipients,
            boolean experimentAlertWebhookEnabled,
            String experimentAlertWebhookUrls,
            int experimentAlertWebhookTimeoutSeconds,
            String experimentAlertCriticalChannels,
            String experimentAlertWarningChannels,
            boolean experimentAlertDeliveryRetryEnabled,
            int experimentAlertDeliveryMaxAttempts,
            int experimentAlertDeliveryRetryDelaySeconds,
            boolean experimentAlertDeliveryHealthCheckEnabled,
            int experimentAlertDeliveryHealthCheckDays,
            double experimentAlertDeliveryHealthDeadRateThreshold,
            double experimentAlertDeliveryHealthPendingRateThreshold,
            int experimentAlertDeliveryHealthEscalationCooldownSeconds
    ) {}

    public record ExperimentConfigUpdateRequest(
            Boolean routeAbExperimentEnabled,
            Integer routeAbRolloutPercent,
            String routeAbControlMode,
            String routeAbExperimentMode,
            Boolean promptAbExperimentEnabled,
            Integer promptAbRolloutPercent,
            String promptAbControlVersion,
            String promptAbExperimentVersion,
            Boolean experimentAutoRollbackEnabled,
            Integer experimentFailureThreshold,
            Integer experimentCooldownSeconds,
            Boolean experimentAlertEnabled,
            Integer experimentAlertSuppressSeconds,
            Boolean experimentAlertEmailEnabled,
            String experimentAlertEmailRecipients,
            Boolean experimentAlertWebhookEnabled,
            String experimentAlertWebhookUrls,
            Integer experimentAlertWebhookTimeoutSeconds,
            String experimentAlertCriticalChannels,
            String experimentAlertWarningChannels,
            Boolean experimentAlertDeliveryRetryEnabled,
            Integer experimentAlertDeliveryMaxAttempts,
            Integer experimentAlertDeliveryRetryDelaySeconds,
            Boolean experimentAlertDeliveryHealthCheckEnabled,
            Integer experimentAlertDeliveryHealthCheckDays,
            Double experimentAlertDeliveryHealthDeadRateThreshold,
            Double experimentAlertDeliveryHealthPendingRateThreshold,
            Integer experimentAlertDeliveryHealthEscalationCooldownSeconds
    ) {}

    public record ConfigAuditItem(
            Long id,
            String changeType,
            String source,
            Long operatorId,
            String operatorName,
            Long rollbackFromAuditId,
            String previousConfigValue,
            String configValue,
            String remark,
            LocalDateTime createTime
    ) {}

    public record OperatorInfo(Long operatorId, String operatorName) {}
}
