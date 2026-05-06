package com.pengcheng.realty.common.exception;

import com.pengcheng.common.exception.BusinessException;

/**
 * 审批流程异常
 */
public class ApprovalFlowException extends BusinessException {

    public ApprovalFlowException(String message) {
        super(400, message);
    }
}
