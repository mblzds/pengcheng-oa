-- ========================================================
-- V40__customer_pool_event_log.sql
-- 客户公海池配置持久化切换到 sys_config_group
-- 并新增客户公海池事件日志表用于真实统计
-- ========================================================

CREATE TABLE IF NOT EXISTS `customer_pool_event_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `customer_id` bigint NOT NULL COMMENT '客户 ID',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型: claim/recycle',
  `event_source` varchar(32) NOT NULL DEFAULT 'manual' COMMENT '事件来源: manual/auto',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人 ID，自动回收为空',
  `event_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_cpel_customer` (`customer_id`),
  KEY `idx_cpel_type_time` (`event_type`, `event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户公海池事件日志';

INSERT INTO `sys_config_group` (`group_code`, `group_name`, `group_icon`, `config_value`, `sort`, `status`, `remark`, `create_time`, `update_time`)
SELECT
  'customerPoolConfig',
  '客户公海池配置',
  NULL,
  CONCAT(
    '{"noFollowDays":', COALESCE((SELECT `config_value` FROM `customer_pool_config` WHERE `config_key` = 'no_follow_days' LIMIT 1), '7'),
    ',"noVisitDays":', COALESCE((SELECT `config_value` FROM `customer_pool_config` WHERE `config_key` = 'no_visit_days' LIMIT 1), '30'),
    ',"protectionDays":', COALESCE((SELECT `config_value` FROM `customer_pool_config` WHERE `config_key` = 'protection_days' LIMIT 1), '3'),
    ',"autoRecycleEnabled":',
      CASE
        WHEN LOWER(COALESCE((SELECT `config_value` FROM `customer_pool_config` WHERE `config_key` = 'auto_recycle_enabled' LIMIT 1), 'true')) = 'false'
          THEN 'false'
        ELSE 'true'
      END,
    '}'
  ),
  19,
  1,
  '客户公海池自动回收规则配置',
  NOW(),
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_config_group` WHERE `group_code` = 'customerPoolConfig'
);
