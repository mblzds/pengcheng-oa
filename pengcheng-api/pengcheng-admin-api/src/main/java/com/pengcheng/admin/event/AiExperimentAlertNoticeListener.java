package com.pengcheng.admin.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.admin.service.ai.AiExperimentAlertDeliveryService;
import com.pengcheng.admin.websocket.MessageWebSocketHandler;
import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.ai.experiment.event.AiExperimentAlertEvent;
import com.pengcheng.message.entity.SysNotice;
import com.pengcheng.message.entity.SysUserNotice;
import com.pengcheng.message.mapper.SysNoticeMapper;
import com.pengcheng.message.mapper.SysUserNoticeMapper;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 实验异常告警 -> 站内通知桥接监听器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiExperimentAlertNoticeListener {

    private static final String CHANNEL_NOTICE = "notice";
    private static final String CHANNEL_EMAIL = "email";
    private static final String CHANNEL_WEBHOOK = "webhook";

    private final SysNoticeMapper noticeMapper;
    private final SysUserMapper userMapper;
    private final SysUserNoticeMapper userNoticeMapper;
    private final MessageWebSocketHandler webSocketHandler;
    private final AiExperimentAlertDeliveryService deliveryService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onAiExperimentAlert(AiExperimentAlertEvent event) {
        if (event == null) {
            return;
        }
        AlertLevel alertLevel = resolveAlertLevel(event.alertType());
        Set<String> channels = resolveChannels(alertLevel);
        String title = buildTitle(event, alertLevel);
        String content = buildContent(event, alertLevel);

        boolean shouldSendNotice = channels.contains(CHANNEL_NOTICE);
        boolean shouldSendEmail = channels.contains(CHANNEL_EMAIL) && aiProperties.isExperimentAlertEmailEnabled();
        boolean shouldSendWebhook = channels.contains(CHANNEL_WEBHOOK) && aiProperties.isExperimentAlertWebhookEnabled();
        if (!shouldSendNotice && !shouldSendEmail && !shouldSendWebhook) {
            shouldSendNotice = true;
        }

        DispatchNoticeResult noticeResult = null;
        if (shouldSendNotice) {
            noticeResult = dispatchNotice(title, content, event);
        }
        if (shouldSendEmail) {
            dispatchEmails(event, alertLevel, title, content);
        }
        if (shouldSendWebhook) {
            dispatchWebhooks(event, alertLevel, title, content);
        }
        log.warn("AI实验告警已分发: level={}, channels={}, noticeId={}, alertType={}, experimentType={}, userCount={}",
                alertLevel.name(), String.join(",", channels),
                noticeResult != null ? noticeResult.noticeId() : null,
                event.alertType(), event.experimentType(),
                noticeResult != null ? noticeResult.userCount() : 0);
    }

    private DispatchNoticeResult dispatchNotice(String title, String content, AiExperimentAlertEvent event) {
        SysNotice notice = new SysNotice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setNoticeType(1);
        notice.setStatus(1);
        notice.setCreateBy(0L);
        notice.setCreateName("ai-guard");
        notice.setCreateTime(event.createTime() != null ? event.createTime() : LocalDateTime.now());
        notice.setDeleted(0);
        noticeMapper.insert(notice);
        List<SysUser> users = userMapper.selectList(null);
        for (SysUser user : users) {
            SysUserNotice link = new SysUserNotice();
            link.setUserId(user.getId());
            link.setNoticeId(notice.getId());
            link.setIsRead(0);
            userNoticeMapper.insert(link);
        }
        webSocketHandler.sendNotice(null, notice.getTitle(), notice.getContent());
        return new DispatchNoticeResult(notice.getId(), users.size());
    }

    private String buildTitle(AiExperimentAlertEvent event, AlertLevel alertLevel) {
        String experimentType = event.experimentType() == null ? "unknown" : event.experimentType().toUpperCase(Locale.ROOT);
        return "[AI实验告警][" + alertLevel.name() + "] " + experimentType + " - " + event.title();
    }

    private String buildContent(AiExperimentAlertEvent event, AlertLevel alertLevel) {
        String source = event.triggerSource() == null ? "system" : event.triggerSource();
        return "level=" + alertLevel.name() + "\nsource=" + source + "\n" + event.content();
    }

    private AlertLevel resolveAlertLevel(String alertType) {
        String normalized = alertType == null ? "" : alertType.trim().toLowerCase(Locale.ROOT);
        if ("auto_rollback".equals(normalized)) {
            return AlertLevel.CRITICAL;
        }
        return AlertLevel.WARNING;
    }

    private Set<String> resolveChannels(AlertLevel alertLevel) {
        String configured = alertLevel == AlertLevel.CRITICAL
                ? aiProperties.getExperimentAlertCriticalChannels()
                : aiProperties.getExperimentAlertWarningChannels();
        Set<String> parsed = parseChannels(configured);
        if (parsed.isEmpty()) {
            return new LinkedHashSet<>(List.of(CHANNEL_NOTICE));
        }
        return parsed;
    }

    private Set<String> parseChannels(String channelsValue) {
        if (!StringUtils.hasText(channelsValue)) {
            return Set.of();
        }
        String normalized = channelsValue.replace('\n', ',')
                .replace('\r', ',')
                .replace('；', ',')
                .replace(';', ',')
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(channel -> CHANNEL_NOTICE.equals(channel)
                        || CHANNEL_EMAIL.equals(channel)
                        || CHANNEL_WEBHOOK.equals(channel))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void dispatchEmails(AiExperimentAlertEvent event, AlertLevel alertLevel, String subject, String content) {
        List<String> recipients = parseRecipients(aiProperties.getExperimentAlertEmailRecipients());
        if (recipients.isEmpty()) {
            return;
        }
        for (String recipient : recipients) {
            try {
                deliveryService.dispatchEmail(event.alertLogId(), event.alertType(), alertLevel.name(),
                        recipient, subject, content);
            } catch (Exception ex) {
                log.error("AI实验告警邮件投递异常: recipient={}, alertLogId={}", recipient, event.alertLogId(), ex);
            }
        }
    }

    private void dispatchWebhooks(AiExperimentAlertEvent event, AlertLevel alertLevel, String subject, String content) {
        List<String> urls = parseWebhookUrls(aiProperties.getExperimentAlertWebhookUrls());
        if (urls.isEmpty()) {
            return;
        }
        int timeoutSeconds = Math.max(1, Math.min(aiProperties.getExperimentAlertWebhookTimeoutSeconds(), 30));
        String payload = buildWebhookPayload(event, alertLevel, subject, content);
        if (payload == null) {
            return;
        }
        for (String url : urls) {
            try {
                deliveryService.dispatchWebhook(event.alertLogId(), event.alertType(), alertLevel.name(),
                        url, payload, timeoutSeconds);
            } catch (Exception ex) {
                log.error("AI实验告警Webhook投递异常: url={}, alertLogId={}", url, event.alertLogId(), ex);
            }
        }
    }

    private String buildWebhookPayload(AiExperimentAlertEvent event, AlertLevel alertLevel, String subject, String content) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventType", "aiExperimentAlert");
            payload.put("alertLogId", event.alertLogId());
            payload.put("alertLevel", alertLevel.name());
            payload.put("title", subject);
            payload.put("content", content);
            payload.put("alertType", event.alertType());
            payload.put("experimentType", event.experimentType());
            payload.put("triggerSource", event.triggerSource());
            payload.put("dedupeKey", event.dedupeKey());
            payload.put("metadataJson", event.metadataJson());
            payload.put("createTime", event.createTime() != null ? event.createTime().toString() : null);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.error("AI实验告警Webhook序列化失败: subject={}", subject, ex);
            return null;
        }
    }

    private List<String> parseRecipients(String recipientsValue) {
        if (!StringUtils.hasText(recipientsValue)) {
            return List.of();
        }
        String normalized = recipientsValue.replace('；', ',').replace(';', ',');
        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(20)
                .collect(Collectors.toList());
    }

    private List<String> parseWebhookUrls(String urlsValue) {
        if (!StringUtils.hasText(urlsValue)) {
            return List.of();
        }
        String normalized = urlsValue.replace('\n', ',')
                .replace('\r', ',')
                .replace('；', ',')
                .replace(';', ',');
        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(url -> url.startsWith("http://") || url.startsWith("https://"))
                .distinct()
                .limit(20)
                .collect(Collectors.toList());
    }

    private enum AlertLevel {
        CRITICAL,
        WARNING
    }

    private record DispatchNoticeResult(Long noticeId, int userCount) {}
}
