-- V12: 云文档系统

CREATE TABLE IF NOT EXISTS sys_doc_space (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '空间名称',
    description VARCHAR(500) COMMENT '空间描述',
    owner_id BIGINT NOT NULL COMMENT '所有者',
    visibility VARCHAR(20) DEFAULT 'private' COMMENT 'private/dept/all',
    dept_id BIGINT COMMENT '所属部门',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    KEY idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档空间';

CREATE TABLE IF NOT EXISTS sys_doc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL COMMENT '所属空间',
    parent_id BIGINT DEFAULT 0 COMMENT '父文档ID(目录树)',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content MEDIUMTEXT COMMENT 'Markdown内容',
    doc_type VARCHAR(20) DEFAULT 'markdown' COMMENT 'markdown/folder',
    sort_order INT DEFAULT 0,
    creator_id BIGINT NOT NULL,
    last_editor_id BIGINT COMMENT '最后编辑者',
    word_count INT DEFAULT 0,
    version INT DEFAULT 1,
    pinned TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    KEY idx_space (space_id),
    KEY idx_parent (parent_id),
    FULLTEXT INDEX ft_doc (title, content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档';

CREATE TABLE IF NOT EXISTS sys_doc_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id BIGINT NOT NULL,
    version INT NOT NULL,
    title VARCHAR(200),
    content MEDIUMTEXT,
    editor_id BIGINT,
    change_summary VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_doc (doc_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档版本历史';

-- V13: 多渠道推送
CREATE TABLE IF NOT EXISTS sys_channel_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_type VARCHAR(30) NOT NULL COMMENT 'dingtalk/feishu/wecom/email',
    channel_name VARCHAR(100) NOT NULL,
    webhook_url VARCHAR(500) COMMENT 'Webhook地址',
    app_key VARCHAR(200) COMMENT '应用Key',
    app_secret VARCHAR(200) COMMENT '应用Secret',
    extra_config JSON COMMENT '额外配置',
    enabled TINYINT DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_type (channel_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推送渠道配置';

CREATE TABLE IF NOT EXISTS sys_channel_push_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_id BIGINT NOT NULL,
    message_type VARCHAR(30),
    content TEXT,
    target VARCHAR(200),
    status TINYINT DEFAULT 0 COMMENT '0=待发 1=成功 2=失败',
    error_msg VARCHAR(500),
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_channel (channel_id),
    KEY idx_time (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推送日志';

INSERT INTO sys_channel_config (channel_type, channel_name, webhook_url, enabled) VALUES
('dingtalk', '钉钉通知群', '', 0),
('feishu', '飞书通知群', '', 0),
('wecom', '企业微信群', '', 0);
