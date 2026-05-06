package com.pengcheng.ai.exception;

import com.pengcheng.common.exception.BusinessException;

/**
 * AI服务调用失败异常
 */
public class AiServiceException extends BusinessException {

    public AiServiceException(String message) {
        super(503, message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(503, message);
        initCause(cause);
    }
}
