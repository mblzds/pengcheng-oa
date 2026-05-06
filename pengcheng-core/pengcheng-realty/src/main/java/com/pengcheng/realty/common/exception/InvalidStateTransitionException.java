package com.pengcheng.realty.common.exception;

import com.pengcheng.common.exception.BusinessException;

/**
 * 客户状态流转不合法异常
 */
public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(String message) {
        super(400, message);
    }
}
