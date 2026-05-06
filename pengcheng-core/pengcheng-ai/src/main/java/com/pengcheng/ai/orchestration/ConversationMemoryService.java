package com.pengcheng.ai.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 会话记忆服务（Redis）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private static final String KEY_PREFIX = "pengcheng:ai:conv:";
    private static final int SUMMARY_LINE_MAX = 24;
    private static final int SUMMARY_CONTENT_MAX = 80;

    private final RedisOperations<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    public String ensureConversationId(String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            return conversationId;
        }
        return UUID.randomUUID().toString();
    }

    public void appendUserMessage(String conversationId, String content) {
        append(conversationId, "user", content);
    }

    public void appendAssistantMessage(String conversationId, String content) {
        append(conversationId, "assistant", content);
    }

    private void append(String conversationId, String role, String content) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(content)) {
            return;
        }
        try {
            String key = key(conversationId);
            String summaryKey = summaryKey(conversationId);
            String value = objectMapper.writeValueAsString(new MessageItem(role, content, LocalDateTime.now()));
            redisTemplate.opsForList().rightPush(key, value);

            long ttlHours = Math.max(1, aiProperties.getConversationTtlHours());
            redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
            redisTemplate.expire(summaryKey, ttlHours, TimeUnit.HOURS);

            if (aiProperties.isConversationCompactionEnabled()) {
                compactIfNeeded(conversationId);
            } else {
                long limit = Math.max(10, aiProperties.getConversationMessageLimit());
                redisTemplate.opsForList().trim(key, -limit, -1);
            }
        } catch (Exception e) {
            log.warn("写入会话记忆失败, conversationId={}", conversationId, e);
        }
    }

    public String buildConversationContext(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return null;
        }
        try {
            String summary = redisTemplate.opsForValue().get(summaryKey(conversationId));
            int window = Math.max(2, aiProperties.getConversationContextWindow());
            List<MessageItem> recent = readRecentMessages(conversationId, window);
            if (!StringUtils.hasText(summary) && recent.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            if (StringUtils.hasText(summary)) {
                sb.append("历史摘要：\n").append(summary.trim());
            }
            if (!recent.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append("最近对话：");
                for (MessageItem item : recent) {
                    sb.append("\n- ")
                            .append(roleLabel(item.role()))
                            .append("：")
                            .append(sanitize(item.content(), SUMMARY_CONTENT_MAX));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("构建会话上下文失败, conversationId={}", conversationId, e);
            return null;
        }
    }

    private void compactIfNeeded(String conversationId) {
        String key = key(conversationId);
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        Long sizeValue = listOps.size(key);
        long size = sizeValue == null ? 0 : sizeValue;
        long threshold = Math.max(1, aiProperties.getConversationCompactionThreshold());
        if (size <= threshold) {
            return;
        }

        int keepRecent = Math.max(10, aiProperties.getConversationMessageLimit());
        int compactCount = (int) (size - keepRecent);
        if (compactCount <= 0) {
            return;
        }

        List<String> compactRaw = listOps.range(key, 0, compactCount - 1);
        String compactSummary = summarizeMessages(compactRaw);
        if (StringUtils.hasText(compactSummary)) {
            ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
            String summaryKey = summaryKey(conversationId);
            String existing = valueOps.get(summaryKey);
            String merged = mergeSummary(existing, compactSummary);
            valueOps.set(summaryKey, merged);
        }
        listOps.trim(key, -keepRecent, -1);
    }

    private List<MessageItem> readRecentMessages(String conversationId, int limit) {
        List<String> raw = redisTemplate.opsForList().range(key(conversationId), -limit, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<MessageItem> items = new ArrayList<>(raw.size());
        for (String item : raw) {
            MessageItem parsed = parse(item);
            if (parsed != null && StringUtils.hasText(parsed.content())) {
                items.add(parsed);
            }
        }
        return items;
    }

    private MessageItem parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, MessageItem.class);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String summarizeMessages(List<String> rawMessages) {
        if (rawMessages == null || rawMessages.isEmpty()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        for (String raw : rawMessages) {
            MessageItem item = parse(raw);
            if (item == null || !StringUtils.hasText(item.content())) {
                continue;
            }
            lines.add(roleLabel(item.role()) + "：" + sanitize(item.content(), SUMMARY_CONTENT_MAX));
            if (lines.size() >= SUMMARY_LINE_MAX) {
                break;
            }
        }
        if (lines.isEmpty()) {
            return null;
        }
        return String.join(" | ", lines);
    }

    private String mergeSummary(String existing, String delta) {
        if (!StringUtils.hasText(existing)) {
            return delta;
        }
        String merged = existing + " | " + delta;
        int maxLength = 4000;
        if (merged.length() <= maxLength) {
            return merged;
        }
        return merged.substring(merged.length() - maxLength);
    }

    private String roleLabel(String role) {
        if ("user".equalsIgnoreCase(role)) {
            return "用户";
        }
        if ("assistant".equalsIgnoreCase(role)) {
            return "助手";
        }
        return "系统";
    }

    private String sanitize(String content, int maxLen) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen) + "...";
    }

    private String key(String conversationId) {
        return KEY_PREFIX + conversationId + ":messages";
    }

    private String summaryKey(String conversationId) {
        return KEY_PREFIX + conversationId + ":summary";
    }

    public record MessageItem(String role, String content, LocalDateTime timestamp) {
    }
}
