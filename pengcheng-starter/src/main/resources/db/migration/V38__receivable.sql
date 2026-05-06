-- ========================================================
-- V38__receivable.sql
-- P0-2 回款管理模块
-- 关联 customer_deal，支持分期计划、实际到账流水、逾期告警
-- 参考：DEV-PLAN-V3.1.md Sprint 1
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 回款计划分期表（一条成交 N 期）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `receivable_plan` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '分期ID',
    `deal_id`       BIGINT         NOT NULL COMMENT '成交记录ID',
    `period_no`     INT            NOT NULL COMMENT '期号（从 1 开始）',
    `period_name`   VARCHAR(32)    NULL DEFAULT NULL COMMENT '期名：首付/一期/二期/尾款 等',
    `due_date`      DATE           NOT NULL COMMENT '应付日期',
    `due_amount`    DECIMAL(12, 2) NOT NULL COMMENT '应付金额',
    `paid_amount`   DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '已付金额（累计）',
    `status`        TINYINT        NOT NULL DEFAULT 0 COMMENT '状态: 0-未到期 1-待回款 2-部分回款 3-已回款 4-逾期',
    `remark`        VARCHAR(500)   NULL DEFAULT NULL COMMENT '备注',
    `create_by`     BIGINT         NULL DEFAULT NULL,
    `update_by`     BIGINT         NULL DEFAULT NULL,
    `create_time`   DATETIME       NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME       NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_rp_deal_period` (`deal_id`, `period_no`) USING BTREE,
    INDEX `idx_rp_due_date` (`due_date`) USING BTREE,
    INDEX `idx_rp_status` (`status`) USING BTREE,
    CONSTRAINT `fk_rp_deal` FOREIGN KEY (`deal_id`)
        REFERENCES `customer_deal` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '回款计划分期表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 2. 实际到账流水（一期可多次分批到账）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `receivable_record` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    `plan_id`        BIGINT         NOT NULL COMMENT '计划分期ID',
    `amount`         DECIMAL(12, 2) NOT NULL COMMENT '本次到账金额',
    `paid_date`      DATE           NOT NULL COMMENT '实际到账日期',
    `pay_way`        TINYINT        NOT NULL DEFAULT 1 COMMENT '回款方式: 1-银行转账 2-支票 3-现金 4-承兑 5-其他',
    `payer`          VARCHAR(128)   NULL DEFAULT NULL COMMENT '付款方名称',
    `voucher_no`     VARCHAR(64)    NULL DEFAULT NULL COMMENT '凭证号/流水号',
    `attachment_url` VARCHAR(500)   NULL DEFAULT NULL COMMENT '凭证附件 URL',
    `remark`         VARCHAR(500)   NULL DEFAULT NULL,
    `create_by`      BIGINT         NULL DEFAULT NULL,
    `update_by`      BIGINT         NULL DEFAULT NULL,
    `create_time`    DATETIME       NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME       NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_rr_plan_id` (`plan_id`) USING BTREE,
    INDEX `idx_rr_paid_date` (`paid_date`) USING BTREE,
    CONSTRAINT `fk_rr_plan` FOREIGN KEY (`plan_id`)
        REFERENCES `receivable_plan` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '回款到账流水'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 3. 回款告警表（逾期 / 即将到期）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `receivable_alert` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '告警ID',
    `plan_id`         BIGINT       NOT NULL COMMENT '计划分期ID',
    `alert_type`      TINYINT      NOT NULL COMMENT '告警类型: 1-逾期未回款 2-即将到期',
    `alert_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次告警时间',
    `last_notified`   DATETIME     NULL DEFAULT NULL COMMENT '最后一次通知时间',
    `notify_count`    INT          NOT NULL DEFAULT 1 COMMENT '累计通知次数',
    `handled`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0-未处理 1-已处理',
    `handled_by`      BIGINT       NULL DEFAULT NULL,
    `handled_at`      DATETIME     NULL DEFAULT NULL,
    `handled_remark`  VARCHAR(500) NULL DEFAULT NULL,
    `create_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ra_plan_type` (`plan_id`, `alert_type`) USING BTREE,
    INDEX `idx_ra_handled` (`handled`) USING BTREE,
    CONSTRAINT `fk_ra_plan` FOREIGN KEY (`plan_id`)
        REFERENCES `receivable_plan` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '回款告警记录'
  ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
