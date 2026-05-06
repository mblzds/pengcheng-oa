package com.pengcheng.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * AI 服务配置属性
 */
@Data
@ConfigurationProperties(prefix = "pengcheng.ai")
public class AiProperties {

    /** 是否启用 AI 服务 */
    private boolean enabled = true;

    /** 智能判客失败时是否回退到规则引擎 */
    private boolean customerAnalysisFallback = true;

    /** 智能佣金计算失败时是否回退到手动录入 */
    private boolean commissionCalcFallback = true;

    /** 成交概率评分更新失败时是否保留上次评分 */
    private boolean dealProbabilityRetainLast = true;

    /** 会话记忆保留消息条数 */
    private int conversationMessageLimit = 30;

    /** 会话上下文窗口消息条数（拼接到提示词） */
    private int conversationContextWindow = 8;

    /** 是否启用会话压缩 */
    private boolean conversationCompactionEnabled = true;

    /** 会话压缩触发阈值（消息条数） */
    private int conversationCompactionThreshold = 40;

    /** 会话记忆 TTL（小时） */
    private long conversationTtlHours = 24 * 7L;

    /** AI 对话接口超时时间（秒） */
    private int chatTimeoutSeconds = 30;

    /** 路由模式：rule(规则), agent(AI), hybrid(AI优先失败回退规则) */
    private String routeMode = "rule";

    /** AI 路由失败时是否回退规则路由 */
    private boolean routeAgentFallbackToRule = true;

    /** 是否启用路由 A/B 实验 */
    private boolean routeAbExperimentEnabled = false;

    /** 路由 A/B 实验流量比例（命中实验组的百分比） */
    private int routeAbRolloutPercent = 50;

    /** 路由 A/B 控制组模式 */
    private String routeAbControlMode = "rule";

    /** 路由 A/B 实验组模式 */
    private String routeAbExperimentMode = "hybrid";

    /** 是否启用提示词 A/B 实验 */
    private boolean promptAbExperimentEnabled = false;

    /** 提示词 A/B 实验流量比例（命中实验组的百分比） */
    private int promptAbRolloutPercent = 50;

    /** 提示词 A/B 控制组版本 */
    private String promptAbControlVersion = "v1";

    /** 提示词 A/B 实验组版本 */
    private String promptAbExperimentVersion = "v2";

    /** 是否启用实验自动回滚保护 */
    private boolean experimentAutoRollbackEnabled = true;

    /** 实验自动回滚失败阈值（达到后进入冷却期） */
    private int experimentFailureThreshold = 5;

    /** 实验自动回滚冷却时间（秒） */
    private int experimentCooldownSeconds = 300;

    /** 是否启用实验异常告警 */
    private boolean experimentAlertEnabled = true;

    /** 实验异常告警抑制窗口（秒，同一去重键在窗口内仅首条生效） */
    private int experimentAlertSuppressSeconds = 300;

    /** 是否启用实验异常邮件告警 */
    private boolean experimentAlertEmailEnabled = false;

    /** 实验异常邮件告警收件人（逗号分隔） */
    private String experimentAlertEmailRecipients = "";

    /** 是否启用实验异常 Webhook 告警 */
    private boolean experimentAlertWebhookEnabled = false;

    /** 实验异常 Webhook 地址列表（逗号分隔） */
    private String experimentAlertWebhookUrls = "";

    /** 实验异常 Webhook 请求超时（秒） */
    private int experimentAlertWebhookTimeoutSeconds = 5;

    /** critical 级别告警渠道（notice/email/webhook，逗号分隔） */
    private String experimentAlertCriticalChannels = "notice,email,webhook";

    /** warning 级别告警渠道（notice/email/webhook，逗号分隔） */
    private String experimentAlertWarningChannels = "notice,email";

    /** 是否启用告警投递自动重试 */
    private boolean experimentAlertDeliveryRetryEnabled = true;

    /** 告警投递最大尝试次数（含首次投递） */
    private int experimentAlertDeliveryMaxAttempts = 3;

    /** 告警投递重试间隔（秒） */
    private int experimentAlertDeliveryRetryDelaySeconds = 60;

    /** 是否启用投递健康度巡检 */
    private boolean experimentAlertDeliveryHealthCheckEnabled = true;

    /** 投递健康度巡检统计窗口（天） */
    private int experimentAlertDeliveryHealthCheckDays = 7;

    /** 投递健康度死信率阈值（0~1） */
    private double experimentAlertDeliveryHealthDeadRateThreshold = 0.05d;

    /** 投递健康度待重试率阈值（0~1） */
    private double experimentAlertDeliveryHealthPendingRateThreshold = 0.10d;

    /** 投递健康度升级告警冷却时间（秒） */
    private int experimentAlertDeliveryHealthEscalationCooldownSeconds = 600;

    /** 审批智能体是否启用 HITL（高风险需人工复核） */
    private boolean approvalHitlEnabled = true;

    /** 审批智能体高风险金额阈值（含） */
    private BigDecimal approvalHitlAmountThreshold = new BigDecimal("50000");
}
