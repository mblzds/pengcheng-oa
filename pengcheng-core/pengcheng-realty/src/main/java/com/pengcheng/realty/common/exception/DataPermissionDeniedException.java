package com.pengcheng.realty.common.exception;

import com.pengcheng.common.exception.BusinessException;

/**
 * 数据权限越权访问异常
 */
public class DataPermissionDeniedException extends BusinessException {

    public DataPermissionDeniedException(String message) {
        super(403, message);
    }
}
