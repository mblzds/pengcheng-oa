-- V18: 聊天待办提取 + 经营分析增强

CREATE TABLE IF NOT EXISTS sys_todo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '待办归属人',
    title VARCHAR(200) NOT NULL COMMENT '待办标题',
    description TEXT COMMENT '详细描述',
    source_type VARCHAR(30) DEFAULT 'manual' COMMENT 'manual/chat/ai',
    source_id BIGINT COMMENT '来源消息ID',
    source_chat_type VARCHAR(20) COMMENT 'private/group',
    priority TINYINT DEFAULT 0 COMMENT '0=普通 1=重要 2=紧急',
    status TINYINT DEFAULT 0 COMMENT '0=待办 1=进行中 2=已完成 3=已取消',
    due_date DATETIME COMMENT '截止日期',
    completed_at DATETIME COMMENT '完成时间',
    customer_id BIGINT COMMENT '关联客户',
    assignee_id BIGINT COMMENT '被指派人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_status (user_id, status),
    KEY idx_due (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办事项';

-- 经营分析大屏用：销售日报快照表
CREATE TABLE IF NOT EXISTS sys_sales_daily_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    snapshot_date DATE NOT NULL,
    total_deal_amount DECIMAL(14,2) DEFAULT 0 COMMENT '当日签约总额',
    deal_count INT DEFAULT 0 COMMENT '当日签约单数',
    new_customer_count INT DEFAULT 0 COMMENT '当日新增客户',
    follow_up_count INT DEFAULT 0 COMMENT '当日跟进次数',
    payment_received DECIMAL(14,2) DEFAULT 0 COMMENT '当日回款',
    commission_settled DECIMAL(14,2) DEFAULT 0 COMMENT '当日结佣',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date (snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售日报快照';
