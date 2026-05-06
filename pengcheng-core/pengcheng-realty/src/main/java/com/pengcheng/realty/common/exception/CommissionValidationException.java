package com.pengcheng.realty.common.exception;

import com.pengcheng.common.exception.BusinessException;

/**
 * 佣金等式校验失败异常
 */
public class CommissionValidationException extends BusinessException {

    public CommissionValidationException(String message) {
        super(400, message);
    }
}
