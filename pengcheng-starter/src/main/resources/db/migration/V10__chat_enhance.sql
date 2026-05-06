-- ============================================================
-- V10: 聊天功能增强 - 消息撤回、引用、ACK、序列号
-- ============================================================

-- 私聊消息表增加字段
ALTER TABLE `sys_chat_message`
    ADD COLUMN `recalled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已撤回：0-否 1-是' AFTER `content`,
    ADD COLUMN `reply_to_id` BIGINT DEFAULT NULL COMMENT '引用的消息ID' AFTER `recalled`,
    ADD COLUMN `seq` BIGINT NOT NULL DEFAULT 0 COMMENT '消息序列号（用于排序和ACK）' AFTER `reply_to_id`,
    ADD COLUMN `delivered` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已送达：0-否 1-是' AFTER `seq`,
    ADD COLUMN `read_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0-否 1-是' AFTER `delivered`;

-- 群聊消息表增加字段
ALTER TABLE `sys_chat_group_message`
    ADD COLUMN `recalled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已撤回：0-否 1-是' AFTER `content`,
    ADD COLUMN `reply_to_id` BIGINT DEFAULT NULL COMMENT '引用的消息ID' AFTER `recalled`,
    ADD COLUMN `seq` BIGINT NOT NULL DEFAULT 0 COMMENT '消息序列号' AFTER `reply_to_id`,
    ADD COLUMN `at_user_ids` VARCHAR(500) DEFAULT NULL COMMENT '@的用户ID列表，逗号分隔' AFTER `seq`;

-- 消息序列号生成表（保证全局递增）
CREATE TABLE IF NOT EXISTS `sys_chat_sequence` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `scope` VARCHAR(50) NOT NULL COMMENT '作用域：global/conversation_{id}',
    `current_seq` BIGINT NOT NULL DEFAULT 0 COMMENT '当前序列号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scope` (`scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息序列号生成器';

INSERT INTO `sys_chat_sequence` (`scope`, `current_seq`) VALUES ('global', 0);

-- 离线消息补偿表
CREATE TABLE IF NOT EXISTS `sys_chat_offline_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `message_id` BIGINT NOT NULL COMMENT '消息ID',
    `message_type` VARCHAR(20) NOT NULL COMMENT '消息类型：chat/groupChat/notice',
    `seq` BIGINT NOT NULL DEFAULT 0 COMMENT '消息序列号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `delivered` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已投递',
    `delivered_at` DATETIME DEFAULT NULL COMMENT '投递时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_delivered` (`user_id`, `delivered`),
    KEY `idx_user_seq` (`user_id`, `seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='离线消息补偿队列';

-- 消息搜索索引
ALTER TABLE `sys_chat_message` ADD FULLTEXT INDEX `ft_content` (`content`) WITH PARSER ngram;
ALTER TABLE `sys_chat_group_message` ADD FULLTEXT INDEX `ft_content` (`content`) WITH PARSER ngram;

-- 为序列号查询添加索引
ALTER TABLE `sys_chat_message` ADD INDEX `idx_seq` (`seq`);
ALTER TABLE `sys_chat_group_message` ADD INDEX `idx_seq` (`seq`);
