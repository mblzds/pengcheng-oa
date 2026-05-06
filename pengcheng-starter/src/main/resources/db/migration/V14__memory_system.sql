-- ================================================================
-- V14: AI 记忆系统
-- 三层记忆架构：L0 工作记忆(Redis) / L1 短期记忆 / L2 长期记忆
-- ================================================================

-- AI 记忆主表（L2 长期记忆 + L1 短期记忆）
CREATE TABLE IF NOT EXISTS `ai_memory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
    `customer_id` BIGINT DEFAULT NULL COMMENT '关联客户ID（客户画像记忆时使用）',
    `memory_type` VARCHAR(30) NOT NULL COMMENT '记忆类型：fact/preference/decision/event/profile',
    `memory_level` VARCHAR(10) NOT NULL DEFAULT 'L1' COMMENT '记忆层级：L1-短期 L2-长期',
    `content` TEXT NOT NULL COMMENT '记忆内容',
    `source` VARCHAR(30) NOT NULL DEFAULT 'chat' COMMENT '来源：chat/manual/system/extraction',
    `importance` DECIMAL(3,2) NOT NULL DEFAULT 0.50 COMMENT '重要度评分（0.00-1.00）',
    `access_count` INT NOT NULL DEFAULT 0 COMMENT '访问次数',
    `last_accessed_at` DATETIME DEFAULT NULL COMMENT '最后访问时间',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
    `embedding_id` VARCHAR(100) DEFAULT NULL COMMENT 'PGVector 中的向量 ID',
    `expires_at` DATETIME DEFAULT NULL COMMENT 'L1 短期记忆到期时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_type` (`user_id`, `memory_type`),
    KEY `idx_customer` (`customer_id`),
    KEY `idx_level_expires` (`memory_level`, `expires_at`),
    KEY `idx_importance` (`importance`),
    FULLTEXT INDEX `ft_content` (`content`) WITH PARSER ngram,
    FULLTEXT INDEX `ft_tags` (`tags`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 记忆';

-- 记忆片段/情节表（对话摘要和关键事件）
CREATE TABLE IF NOT EXISTS `ai_memory_episode` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `session_id` VARCHAR(50) NOT NULL COMMENT '关联会话 ID',
    `summary` TEXT NOT NULL COMMENT '对话摘要',
    `key_facts` JSON DEFAULT NULL COMMENT '提取的关键事实数组',
    `participants` VARCHAR(200) DEFAULT NULL COMMENT '参与者（逗号分隔的用户ID）',
    `emotion` VARCHAR(20) DEFAULT NULL COMMENT '情感基调：positive/neutral/negative',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_session` (`user_id`, `session_id`),
    KEY `idx_created` (`created_at`),
    FULLTEXT INDEX `ft_summary` (`summary`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 记忆片段（对话摘要）';

-- 记忆精炼日志（合并/压缩/淘汰记录）
CREATE TABLE IF NOT EXISTS `ai_memory_refinement_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `action` VARCHAR(20) NOT NULL COMMENT '操作：merge/compress/evict/promote/demote',
    `source_ids` VARCHAR(500) NOT NULL COMMENT '源记忆 ID 列表，逗号分隔',
    `target_id` BIGINT DEFAULT NULL COMMENT '目标记忆 ID（合并后的新记忆）',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '操作原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_action` (`action`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记忆精炼日志';
