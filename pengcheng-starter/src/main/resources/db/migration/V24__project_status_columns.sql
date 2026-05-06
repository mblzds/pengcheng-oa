-- V24: 项目看板状态列配置（可选扩展，支持项目自定义看板列）
CREATE TABLE IF NOT EXISTS pm_project_status_column (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目 id',
    name VARCHAR(50) NOT NULL COMMENT '列名（展示用）',
    status_value VARCHAR(30) NOT NULL COMMENT '对应 pm_task.status 取值',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '看板列顺序',
    is_done TINYINT NOT NULL DEFAULT 0 COMMENT '是否视为已完成列（用于统计）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目看板状态列配置';
