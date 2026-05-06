-- V32: 添加客户公海池自动回收定时任务配置
-- 每日凌晨 2 点执行，自动回收无跟进和未到访客户

-- 兼容当前项目的 sys_job 表结构（invoke_target 模式）
INSERT INTO sys_job (
  job_name,
  job_group,
  invoke_target,
  cron_expression,
  misfire_policy,
  concurrent,
  status,
  remark,
  create_time,
  update_time,
  deleted
)
SELECT
  '客户公海池回收',
  'REALTY',
  'customerPoolRecycleTask.execute',
  '0 0 2 * * ?',
  3,
  1,
  1,
  '每日执行客户公海池自动回收',
  NOW(),
  NOW(),
  0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
    FROM sys_job
   WHERE invoke_target = 'customerPoolRecycleTask.execute'
     AND deleted = 0
);

-- 添加公海池配置表（用于存储回收规则配置）
CREATE TABLE IF NOT EXISTS `customer_pool_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `config_key` varchar(50) NOT NULL COMMENT '配置键',
  `config_value` varchar(500) NOT NULL COMMENT '配置值',
  `config_type` varchar(20) DEFAULT 'string' COMMENT '配置类型：string/number/boolean',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户公海池配置表';

-- 初始化默认配置
INSERT INTO `customer_pool_config` (`config_key`, `config_value`, `config_type`, `remark`) VALUES
('no_follow_days', '7', 'number', '无跟进记录回收天数阈值'),
('no_visit_days', '30', 'number', '未到访回收天数阈值'),
('protection_days', '3', 'number', '领取后保护期天数'),
('auto_recycle_enabled', 'true', 'boolean', '是否启用自动回收')
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);
