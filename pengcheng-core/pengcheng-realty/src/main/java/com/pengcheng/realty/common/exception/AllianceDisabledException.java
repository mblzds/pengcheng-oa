package com.pengcheng.realty.common.exception;

import com.pengcheng.common.exception.BusinessException;

/**
 * 已停用联盟商尝试操作异常
 */
public class AllianceDisabledException extends BusinessException {

    public AllianceDisabledException(String message) {
        super(403, message);
    }
}
