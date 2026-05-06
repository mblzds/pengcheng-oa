package com.pengcheng.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 基础业务错误码
 */
@Getter
@RequiredArgsConstructor
public enum BizErrorCode {

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "没有权限访问"),
    BUSINESS_ERROR(500, "业务处理失败"),
    INTERNAL_ERROR(500, "系统繁忙，请稍后再试");

    private final int code;
    private final String defaultMessage;
}
