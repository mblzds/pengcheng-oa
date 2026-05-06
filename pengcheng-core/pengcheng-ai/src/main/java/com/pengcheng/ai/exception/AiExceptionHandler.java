package com.pengcheng.ai.exception;

import com.pengcheng.ai.rag.KnowledgeBaseException;
import com.pengcheng.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * AI服务异常处理器
 * 优先级高于全局 BusinessException 兜底处理，提供更精确的日志分类和响应
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiExceptionHandler {

    /**
     * AI服务调用失败（503）
     */
    @ExceptionHandler(AiServiceException.class)
    public Result<Void> handleAiService(AiServiceException e) {
        log.error("AI服务调用失败: {}", e.getMessage(), e);
        return Result.fail(503, e.getMessage());
    }

    /**
     * 知识库处理异常（500）
     */
    @ExceptionHandler(KnowledgeBaseException.class)
    public Result<Void> handleKnowledgeBase(KnowledgeBaseException e) {
        log.error("知识库处理异常: {}", e.getMessage(), e);
        return Result.fail(500, e.getMessage());
    }
}
