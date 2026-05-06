-- ========================================================
-- V41__org_invite.sql
-- 组织邀请 MVP：企业注册后的成员邀请闭环
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `sys_org_invite` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `invite_code`      VARCHAR(32)  NOT NULL COMMENT '邀请码',
    `email`            VARCHAR(128) NULL DEFAULT NULL COMMENT '被邀请邮箱',
    `phone`            VARCHAR(32)  NULL DEFAULT NULL COMMENT '被邀请手机号',
    `role_ids`         VARCHAR(255) NULL DEFAULT NULL COMMENT '角色ID列表，逗号分隔',
    `dept_id`          BIGINT       NULL DEFAULT NULL COMMENT '部门ID',
    `status`           TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待接受 1-已接受 2-已撤销 3-已过期',
    `expires_at`       DATETIME     NOT NULL COMMENT '过期时间',
    `accepted_user_id` BIGINT       NULL DEFAULT NULL COMMENT '接受人用户ID',
    `accepted_at`      DATETIME     NULL DEFAULT NULL COMMENT '接受时间',
    `create_by`        BIGINT       NULL DEFAULT NULL COMMENT '创建人',
    `update_by`        BIGINT       NULL DEFAULT NULL COMMENT '更新人',
    `create_time`      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_sys_org_invite_code` (`invite_code`) USING BTREE,
    INDEX `idx_sys_org_invite_status` (`status`) USING BTREE,
    INDEX `idx_sys_org_invite_expires_at` (`expires_at`) USING BTREE,
    INDEX `idx_sys_org_invite_email` (`email`) USING BTREE,
    INDEX `idx_sys_org_invite_phone` (`phone`) USING BTREE,
    INDEX `idx_sys_org_invite_dept_id` (`dept_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '组织邀请表'
  ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
