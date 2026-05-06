-- V35: 自动化规则可视化配置
-- 支持可视化配置业务自动化规则，如客户分配、通知触发等

-- 1. 自动化规则主表
CREATE TABLE IF NOT EXISTS `automation_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `name` varchar(100) NOT NULL COMMENT '规则名称',
  `description` varchar(500) DEFAULT NULL COMMENT '规则描述',
  `module` varchar(50) NOT NULL COMMENT '所属模块：customer/meeting/hr/finance',
  `event_type` varchar(50) NOT NULL COMMENT '触发事件类型',
  `condition_type` varchar(20) DEFAULT 'all' COMMENT '条件类型：all-满足所有/any-满足任一',
  `conditions` json DEFAULT NULL COMMENT '条件配置（JSON）',
  `actions` json NOT NULL COMMENT '动作配置（JSON）',
  `enabled` tinyint DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
  `priority` int DEFAULT 100 COMMENT '优先级（数字越小优先级越高）',
  `execute_count` bigint DEFAULT 0 COMMENT '执行次数',
  `last_execute_time` datetime DEFAULT NULL COMMENT '最后执行时间',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自动化规则表';

-- 2. 自动化规则执行日志表
CREATE TABLE IF NOT EXISTS `automation_rule_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `rule_id` bigint NOT NULL COMMENT '规则 ID',
  `event_type` varchar(50) DEFAULT NULL COMMENT '事件类型',
  `event_data` json DEFAULT NULL COMMENT '事件数据（JSON）',
  `execute_result` tinyint DEFAULT 1 COMMENT '执行结果：1-成功 0-失败',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `execute_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `execute_duration` int DEFAULT NULL COMMENT '执行耗时 (ms)',
  PRIMARY KEY (`id`),
  KEY `idx_rule` (`rule_id`),
  KEY `idx_time` (`execute_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自动化规则执行日志表';

-- 3. 初始化示例规则
INSERT INTO `automation_rule` (`name`, `description`, `module`, `event_type`, `conditions`, `actions`, `enabled`, `priority`) VALUES
('新客户自动分配', '新客户报备后自动分配给销售负责人', 'customer', 'customer.created', 
 '{"conditions": [{"field": "project_id", "operator": "exists", "value": null}]}',
 '{"actions": [{"type": "assign_user", "params": {"role": "sales_manager"}}]}', 1, 10),
('会议前提醒', '会议开始前 15 分钟发送提醒通知', 'meeting', 'meeting.reminder',
 '{"conditions": [{"field": "reminder_minutes", "operator": "equals", "value": 15}]}',
 '{"actions": [{"type": "send_notification", "params": {"channel": "站内信"}}]}', 1, 20),
('成交客户祝贺通知', '客户成交后发送祝贺通知给团队', 'customer', 'customer.deal_closed',
 '{"conditions": [{"field": "status", "operator": "equals", "value": 3}]}',
 '{"actions": [{"type": "send_notification", "params": {"channel": "群消息", "template": "deal_congrats"}}]}', 1, 30);
