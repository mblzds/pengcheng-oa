package com.pengcheng.realty.common.exception;

import com.pengcheng.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 房产业务异常处理器
 * 优先级高于全局 BusinessException 兜底处理，提供更精确的日志分类和响应
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RealtyExceptionHandler {

    /**
     * 客户保护期内重复报备（409）
     */
    @ExceptionHandler(CustomerDuplicateException.class)
    public Result<Void> handleCustomerDuplicate(CustomerDuplicateException e) {
        log.warn("客户重复报备: {}", e.getMessage());
        return Result.fail(409, e.getMessage());
    }

    /**
     * 客户状态流转不合法（400）
     */
    @ExceptionHandler(InvalidStateTransitionException.class)
    public Result<Void> handleInvalidStateTransition(InvalidStateTransitionException e) {
        log.warn("客户状态流转异常: {}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    /**
     * 佣金等式校验失败（400）
     */
    @ExceptionHandler(CommissionValidationException.class)
    public Result<Void> handleCommissionValidation(CommissionValidationException e) {
        log.warn("佣金校验失败: {}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    /**
     * 数据权限越权访问（403）
     */
    @ExceptionHandler(DataPermissionDeniedException.class)
    public Result<Void> handleDataPermissionDenied(DataPermissionDeniedException e) {
        log.error("数据权限越权访问: {}", e.getMessage());
        return Result.fail(403, e.getMessage());
    }

    /**
     * 已停用联盟商尝试操作（403）
     */
    @ExceptionHandler(AllianceDisabledException.class)
    public Result<Void> handleAllianceDisabled(AllianceDisabledException e) {
        log.warn("已停用联盟商操作: {}", e.getMessage());
        return Result.fail(403, e.getMessage());
    }

    /**
     * 审批流程异常（400）
     */
    @ExceptionHandler(ApprovalFlowException.class)
    public Result<Void> handleApprovalFlow(ApprovalFlowException e) {
        log.warn("审批流程异常: {}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }
}
