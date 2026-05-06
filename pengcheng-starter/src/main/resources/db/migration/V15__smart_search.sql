-- ================================================================
-- V15: 全局智能搜索增强
-- 为多表添加 FULLTEXT 索引，支持跨模块联合搜索
-- ================================================================

-- 客户表全文索引
ALTER TABLE `customer`
    ADD FULLTEXT INDEX `ft_customer_search` (`customer_name`, `agent_name`) WITH PARSER ngram;

-- 项目表全文索引
ALTER TABLE `project`
    ADD FULLTEXT INDEX `ft_project_search` (`project_name`, `address`, `description`) WITH PARSER ngram;

-- 联盟商表全文索引
ALTER TABLE `alliance`
    ADD FULLTEXT INDEX `ft_alliance_search` (`company_name`, `contact_name`) WITH PARSER ngram;

-- 系统通知全文索引
ALTER TABLE `sys_notice`
    ADD FULLTEXT INDEX `ft_notice_search` (`title`, `content`) WITH PARSER ngram;

-- 搜索历史表
CREATE TABLE IF NOT EXISTS `sys_search_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `keyword` VARCHAR(200) NOT NULL COMMENT '搜索关键词',
    `scope` VARCHAR(30) NOT NULL DEFAULT 'all' COMMENT '搜索范围：all/customer/project/doc/chat',
    `result_count` INT NOT NULL DEFAULT 0 COMMENT '结果数量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史';

-- 热门搜索统计表
CREATE TABLE IF NOT EXISTS `sys_search_hot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `keyword` VARCHAR(200) NOT NULL COMMENT '搜索关键词',
    `search_count` INT NOT NULL DEFAULT 1 COMMENT '搜索次数',
    `last_searched_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_keyword` (`keyword`),
    KEY `idx_count` (`search_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热门搜索统计';
