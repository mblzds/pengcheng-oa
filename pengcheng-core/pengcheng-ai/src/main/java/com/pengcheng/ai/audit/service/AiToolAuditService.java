package com.pengcheng.ai.audit.service;

import com.pengcheng.ai.audit.entity.AiToolCallLog;
import com.pengcheng.ai.audit.mapper.AiToolCallLogMapper;
import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AgentScene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 工具调用审计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiToolAuditService {

    private static final int MAX_SUMMARY_LEN = 500;
    private static final int MAX_ERROR_LEN = 500;
    private static final int MAX_ROLE_LEN = 255;
    private static final int MAX_CHAIN_LEN = 255;

    private final AiToolCallLogMapper auditMapper;

    public void recordSuccess(String toolName,
                              AgentScene scene,
                              AgentIntent intent,
                              String conversationId,
                              Long userId,
                              List<String> roleCodes,
                              String requestSummary,
                              String responseSummary,
                              long latencyMs,
                              String callChain) {
        save(AiToolCallLog.builder()
                .toolName(toolName)
                .scene(scene.name())
                .intent(intent.name())
                .conversationId(truncate(conversationId, 64))
                .userId(userId)
                .roleCodes(truncate(String.join(",", roleCodes != null ? roleCodes : List.of()), MAX_ROLE_LEN))
                .requestSummary(truncate(requestSummary, MAX_SUMMARY_LEN))
                .responseSummary(truncate(responseSummary, MAX_SUMMARY_LEN))
                .success(1)
                .latencyMs(latencyMs)
                .callChain(truncate(callChain, MAX_CHAIN_LEN))
                .errorMessage(null)
                .createTime(LocalDateTime.now())
                .build());
    }

    public void recordFailure(String toolName,
                              AgentScene scene,
                              AgentIntent intent,
                              String conversationId,
                              Long userId,
                              List<String> roleCodes,
                              String requestSummary,
                              long latencyMs,
                              String callChain,
                              Throwable throwable) {
        String error = throwable != null ? throwable.getMessage() : "unknown error";
        save(AiToolCallLog.builder()
                .toolName(toolName)
                .scene(scene.name())
                .intent(intent.name())
                .conversationId(truncate(conversationId, 64))
                .userId(userId)
                .roleCodes(truncate(String.join(",", roleCodes != null ? roleCodes : List.of()), MAX_ROLE_LEN))
                .requestSummary(truncate(requestSummary, MAX_SUMMARY_LEN))
                .responseSummary(null)
                .success(0)
                .latencyMs(latencyMs)
                .callChain(truncate(callChain, MAX_CHAIN_LEN))
                .errorMessage(truncate(error, MAX_ERROR_LEN))
                .createTime(LocalDateTime.now())
                .build());
    }

    private void save(AiToolCallLog logRecord) {
        try {
            auditMapper.insert(logRecord);
        } catch (Exception e) {
            log.warn("写入 AI 工具审计日志失败: {}", e.getMessage());
        }
    }

    private String truncate(String text, int max) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}

