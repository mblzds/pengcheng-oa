package com.pengcheng.ai.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pengcheng.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversationMemoryServiceTest {

    @Mock
    private RedisOperations<String, String> redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final Map<String, List<String>> listStore = new HashMap<>();
    private final Map<String, String> valueStore = new HashMap<>();

    private ConversationMemoryService memoryService;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.setConversationMessageLimit(10);
        properties.setConversationContextWindow(3);
        properties.setConversationCompactionEnabled(true);
        properties.setConversationCompactionThreshold(12);
        properties.setConversationTtlHours(24);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        memoryService = new ConversationMemoryService(redisTemplate, objectMapper, properties);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        when(listOperations.rightPush(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            String value = invocation.getArgument(1, String.class);
            List<String> list = listStore.computeIfAbsent(key, k -> new ArrayList<>());
            list.add(value);
            return (long) list.size();
        });
        when(listOperations.size(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            return (long) listStore.getOrDefault(key, List.of()).size();
        });
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            long start = invocation.getArgument(1, Long.class);
            long end = invocation.getArgument(2, Long.class);
            return slice(listStore.getOrDefault(key, List.of()), start, end);
        });
        doAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            long start = invocation.getArgument(1, Long.class);
            long end = invocation.getArgument(2, Long.class);
            List<String> current = listStore.getOrDefault(key, List.of());
            listStore.put(key, new ArrayList<>(slice(current, start, end)));
            return null;
        }).when(listOperations).trim(anyString(), anyLong(), anyLong());

        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            return valueStore.get(key);
        });
        doAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            String value = invocation.getArgument(1, String.class);
            valueStore.put(key, value);
            return null;
        }).when(valueOperations).set(anyString(), anyString());
    }

    @Test
    void shouldCompactAndKeepSummaryWhenThresholdExceeded() {
        String conversationId = "conv-compact";
        for (int i = 1; i <= 14; i++) {
            memoryService.appendUserMessage(conversationId, "用户消息#" + i);
        }

        String messageKey = "pengcheng:ai:conv:" + conversationId + ":messages";
        String summaryKey = "pengcheng:ai:conv:" + conversationId + ":summary";
        assertThat(listStore.get(messageKey)).hasSizeLessThanOrEqualTo(11);
        assertThat(valueStore.get(summaryKey)).isNotBlank();

        String context = memoryService.buildConversationContext(conversationId);
        assertThat(context).contains("历史摘要");
        assertThat(context).contains("最近对话");
    }

    @Test
    void shouldBuildContextFromRecentMessagesWhenCompactionNotTriggered() {
        String conversationId = "conv-recent";
        memoryService.appendUserMessage(conversationId, "生成今日简报");
        memoryService.appendAssistantMessage(conversationId, "已生成今日简报草稿。");

        String context = memoryService.buildConversationContext(conversationId);
        assertThat(context).contains("最近对话");
        assertThat(context).contains("用户：生成今日简报");
        assertThat(context).contains("助手：已生成今日简报草稿。");
    }

    private List<String> slice(List<String> list, long start, long end) {
        int size = list.size();
        if (size == 0) {
            return List.of();
        }
        int from = resolveIndex(start, size);
        int to = resolveIndex(end, size);
        if (from < 0) {
            from = 0;
        }
        if (to >= size) {
            to = size - 1;
        }
        if (from > to || from >= size) {
            return List.of();
        }
        return list.subList(from, to + 1);
    }

    private int resolveIndex(long index, int size) {
        if (index < 0) {
            return (int) (size + index);
        }
        return (int) index;
    }
}
