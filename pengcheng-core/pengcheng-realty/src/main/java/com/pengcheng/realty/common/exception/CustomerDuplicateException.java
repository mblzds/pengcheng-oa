package com.pengcheng.realty.common.exception;

import com.pengcheng.common.exception.BusinessException;

/**
 * 客户保护期内重复报备异常
 */
public class CustomerDuplicateException extends BusinessException {

    public CustomerDuplicateException(String message) {
        super(409, message);
    }
}
