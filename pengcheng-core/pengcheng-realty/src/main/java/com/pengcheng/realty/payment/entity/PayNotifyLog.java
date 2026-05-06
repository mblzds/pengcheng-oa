package com.pengcheng.realty.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调审计日志（幂等兜底）
 * 对应 V37__pay_gateway.sql 中的 pay_notify_log 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("pay_notify_log")
public class PayNotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 第三方通知ID（幂等键，唯一索引） */
    private String notifyId;

    /** 渠道：alipay / wechat */
    private String channel;

    /** 业务订单号 */
    private String orderNo;

    /** 第三方交易号 */
    private String thirdTradeNo;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 原始回调内容 */
    private String rawPayload;

    /** 处理结果：success / failure / duplicate */
    private String processResult;

    /** 失败原因 */
    private String errorMsg;

    /** 入库时间 */
    private LocalDateTime createTime;

    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_FAILURE = "failure";
    public static final String RESULT_DUPLICATE = "duplicate";
}
