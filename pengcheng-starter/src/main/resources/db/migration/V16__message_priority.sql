-- V16: 消息智能优先级

ALTER TABLE sys_chat_message ADD COLUMN priority TINYINT DEFAULT 0 COMMENT '优先级 0=普通 1=重要 2=紧急';
ALTER TABLE sys_chat_message ADD COLUMN pinned TINYINT DEFAULT 0 COMMENT '是否置顶 0=否 1=是';

ALTER TABLE sys_chat_group_message ADD COLUMN priority TINYINT DEFAULT 0 COMMENT '优先级 0=普通 1=重要 2=紧急';

ALTER TABLE sys_chat_group ADD COLUMN priority_level TINYINT DEFAULT 0 COMMENT '会话优先级 0=普通 1=关注 2=特别关注';

-- 消息分类表（用于智能分组显示）
CREATE TABLE IF NOT EXISTS sys_message_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    chat_type VARCHAR(20) NOT NULL COMMENT 'private/group',
    target_id BIGINT NOT NULL COMMENT '对方用户ID或群ID',
    category VARCHAR(30) NOT NULL DEFAULT 'normal' COMMENT 'focus/starred/muted/normal',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_chat (user_id, chat_type, target_id),
    KEY idx_user_category (user_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息分类管理';
