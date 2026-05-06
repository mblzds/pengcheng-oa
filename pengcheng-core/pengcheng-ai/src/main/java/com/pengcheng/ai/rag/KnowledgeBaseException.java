package com.pengcheng.ai.rag;

import com.pengcheng.common.exception.BusinessException;

/**
 * 知识库处理异常
 */
public class KnowledgeBaseException extends BusinessException {

    public KnowledgeBaseException(String message) {
        super(500, message);
    }

    public KnowledgeBaseException(String message, Throwable cause) {
        super(500, message);
        initCause(cause);
    }
}
