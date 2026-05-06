package com.pengcheng.app.dto;

import lombok.Data;

/**
 * App / 小程序发起支付下单请求
 */
@Data
public class AppPayCreateDTO {

    /**
     * 已存在的付款申请 ID
     */
    private Long requestId;

    /**
     * 支付渠道，默认 wechat
     */
    private String payType;
}
