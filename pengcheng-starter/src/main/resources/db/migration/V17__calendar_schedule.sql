-- V17: 销售日历 + 自动化规则引擎

-- 日历事件表
CREATE TABLE IF NOT EXISTS sys_calendar_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '事件标题',
    description TEXT COMMENT '事件描述',
    event_type VARCHAR(30) NOT NULL DEFAULT 'custom' COMMENT '类型: visit/sign/payment/meeting/reminder/custom',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    all_day TINYINT DEFAULT 0 COMMENT '是否全天事件',
    color VARCHAR(20) DEFAULT '#18a058' COMMENT '颜色标识',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    customer_id BIGINT COMMENT '关联客户',
    project_id BIGINT COMMENT '关联项目',
    location VARCHAR(200) COMMENT '地点',
    reminder_minutes INT DEFAULT 30 COMMENT '提前提醒分钟数',
    reminder_sent TINYINT DEFAULT 0 COMMENT '提醒是否已发送',
    recurrence VARCHAR(30) COMMENT '重复规则: daily/weekly/monthly/none',
    status TINYINT DEFAULT 1 COMMENT '1=有效 0=取消',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, start_time),
    KEY idx_type (event_type),
    KEY idx_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日历事件';

-- 自动化规则表
CREATE TABLE IF NOT EXISTS sys_automation_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    description VARCHAR(500) COMMENT '规则描述',
    trigger_type VARCHAR(30) NOT NULL COMMENT '触发类型: time_based/event_based/condition_based',
    trigger_config JSON NOT NULL COMMENT '触发条件配置',
    action_type VARCHAR(30) NOT NULL COMMENT '动作类型: notify/assign/update_status/create_task',
    action_config JSON NOT NULL COMMENT '动作配置',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    priority INT DEFAULT 0 COMMENT '优先级（越大越先）',
    created_by BIGINT COMMENT '创建人',
    last_triggered_at DATETIME COMMENT '上次触发时间',
    trigger_count INT DEFAULT 0 COMMENT '累计触发次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_enabled (enabled),
    KEY idx_trigger_type (trigger_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动化规则';

-- 自动化规则执行日志
CREATE TABLE IF NOT EXISTS sys_automation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    trigger_data JSON COMMENT '触发数据',
    action_result VARCHAR(500) COMMENT '执行结果',
    status TINYINT DEFAULT 1 COMMENT '1=成功 0=失败',
    executed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rule (rule_id),
    KEY idx_time (executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动化规则执行日志';

-- 预置自动化规则
INSERT INTO sys_automation_rule (name, description, trigger_type, trigger_config, action_type, action_config, enabled) VALUES
('超期未跟进提醒', '客户超过7天未跟进自动提醒销售', 'time_based', '{"interval_days": 7, "check_field": "last_follow_time", "target_table": "customer"}', 'notify', '{"template": "您的客户「{customer_name}」已超过{days}天未跟进，请及时联系", "channel": "system"}', 1),
('合同到期预警', '合同到期前30天自动通知', 'time_based', '{"advance_days": 30, "check_field": "contract_expire_date", "target_table": "customer"}', 'notify', '{"template": "客户「{customer_name}」的合同将于{days}天后到期", "channel": "system"}', 1),
('新客户自动分配', '新报备客户按区域自动分配销售', 'event_based', '{"event": "customer_created", "target_table": "customer"}', 'assign', '{"strategy": "round_robin", "scope": "dept"}', 0),
('成交自动通知', '客户成交后自动通知相关人员', 'event_based', '{"event": "customer_status_changed", "to_status": 3}', 'notify', '{"template": "恭喜！客户「{customer_name}」已成交", "channel": "system", "notify_roles": ["manager"]}', 1);
