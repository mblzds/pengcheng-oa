-- ========================================================
-- V37__pay_gateway.sql
-- P0-1 支付回调修复：为 payment_request 增加第三方支付字段
-- 并新增支付回调审计日志表 pay_notify_log
-- 参考：DEV-PLAN-V3.1.md Sprint 1
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. payment_request 扩展列
-- ----------------------------
ALTER TABLE `payment_request`
    ADD COLUMN `order_no` VARCHAR(64) NULL DEFAULT NULL COMMENT '业务订单号（对外，传递给支付通道）' AFTER `id`,
    ADD COLUMN `pay_channel` VARCHAR(16) NULL DEFAULT NULL COMMENT '支付渠道: alipay/wechat/offline',
    ADD COLUMN `third_trade_no` VARCHAR(64) NULL DEFAULT NULL COMMENT '第三方交易号',
    ADD COLUMN `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态: 0-未付款 1-付款中 2-已付款 3-已退款 4-失败',
    ADD COLUMN `paid_time` DATETIME NULL DEFAULT NULL COMMENT '实际支付完成时间',
    ADD UNIQUE INDEX `uk_pr_order_no` (`order_no`) USING BTREE,
    ADD INDEX `idx_pr_pay_status` (`pay_status`) USING BTREE;

-- ----------------------------
-- 2. 支付回调审计日志（兼顾幂等兜底）
--    verifyService.isProcessed 做前置幂等，本表做审计+二次幂等
-- ----------------------------
CREATE TABLE IF NOT EXISTS `pay_notify_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `notify_id`       VARCHAR(128) NOT NULL COMMENT '第三方通知ID（幂等键）',
    `channel`         VARCHAR(16)  NOT NULL COMMENT '渠道: alipay/wechat',
    `order_no`        VARCHAR(64)  NOT NULL COMMENT '业务订单号',
    `third_trade_no`  VARCHAR(64)  NULL DEFAULT NULL COMMENT '第三方交易号',
    `amount`          DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '支付金额（元）',
    `raw_payload`     MEDIUMTEXT   NULL COMMENT '原始回调内容（JSON 或签名串）',
    `process_result`  VARCHAR(16)  NOT NULL COMMENT 'success/failure/duplicate',
    `error_msg`       VARCHAR(500) NULL DEFAULT NULL COMMENT '失败原因',
    `create_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_pnl_notify_id` (`notify_id`) USING BTREE,
    INDEX `idx_pnl_order_no` (`order_no`) USING BTREE,
    INDEX `idx_pnl_channel_time` (`channel`, `create_time`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '支付回调审计日志（幂等兜底）'
  ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
