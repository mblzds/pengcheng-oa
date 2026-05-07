package com.pengcheng.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.pengcheng.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern DUPLICATE_ENTRY_PATTERN =
            Pattern.compile("Duplicate entry '([^']*)' for key '(?:[^.]+\\.)?([^']+)'");

    private static final Map<String, String> UNIQUE_KEY_LABELS = Map.of(
            "uk_username", "用户名",
            "uk_phone", "手机号",
            "uk_open_id", "微信账号",
            "uk_email", "邮箱"
    );

    /**
     * 业务异常（兜底处理所有 BusinessException 子类）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常 [{}]: {}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 非法状态异常（客户状态流转等）
     */
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalStateException(IllegalStateException e) {
        log.warn("非法状态异常: {}", e.getMessage());
        return Result.fail(BizErrorCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.error("未登录异常: {}", e.getMessage());
        return Result.fail(BizErrorCode.UNAUTHORIZED.getCode(), BizErrorCode.UNAUTHORIZED.getDefaultMessage());
    }

    /**
     * 无权限异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.error("无权限异常: {}", e.getMessage());
        return Result.fail(BizErrorCode.FORBIDDEN.getCode(), BizErrorCode.FORBIDDEN.getDefaultMessage());
    }

    /**
     * 无角色异常
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.error("无角色异常: {}", e.getMessage());
        return Result.fail(BizErrorCode.FORBIDDEN.getCode(), "没有角色权限");
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.error("参数校验异常: {}", message);
        return Result.fail(BizErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        log.error("参数绑定异常: {}", message);
        return Result.fail(BizErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 约束校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "参数校验失败";
        }
        log.error("约束校验异常: {}", message);
        return Result.fail(BizErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 数据完整性异常（唯一索引、外键、非空约束等）
     * 把 MySQL 原始报错翻译成可读提示，避免 SQL 详情泄漏给前端。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        Throwable root = e.getMostSpecificCause();
        String rootMsg = root != null ? root.getMessage() : e.getMessage();
        log.warn("数据完整性异常: {}", rootMsg);
        String friendly = translateIntegrityError(rootMsg);
        return Result.fail(BizErrorCode.BAD_REQUEST.getCode(), friendly);
    }

    private String translateIntegrityError(String rootMsg) {
        if (rootMsg == null) {
            return "数据完整性约束失败";
        }
        Matcher m = DUPLICATE_ENTRY_PATTERN.matcher(rootMsg);
        if (m.find()) {
            String value = m.group(1);
            String key = m.group(2);
            String label = UNIQUE_KEY_LABELS.get(key);
            return label != null
                    ? String.format("%s「%s」已被占用", label, value)
                    : String.format("「%s」已存在，不能重复", value);
        }
        return "数据完整性约束失败";
    }

    /**
     * 运行时异常（业务逻辑抛出的异常）
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.warn("运行时异常: {}", e.getMessage());
        return Result.fail(BizErrorCode.BUSINESS_ERROR.getCode(), e.getMessage());
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数异常: {}", e.getMessage());
        return Result.fail(BizErrorCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(BizErrorCode.INTERNAL_ERROR.getCode(), BizErrorCode.INTERNAL_ERROR.getDefaultMessage());
    }
}
