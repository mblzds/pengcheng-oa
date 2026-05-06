CREATE TABLE IF NOT EXISTS `ai_experiment_config_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_code` varchar(50) NOT NULL DEFAULT 'aiExperiment' COMMENT '配置分组',
  `change_type` varchar(32) NOT NULL COMMENT '变更类型(update/rollback)',
  `source` varchar(32) NOT NULL DEFAULT 'api' COMMENT '变更来源(api/startup/system)',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人名称',
  `rollback_from_audit_id` bigint DEFAULT NULL COMMENT '回滚来源审计ID',
  `previous_config_value` longtext COMMENT '变更前配置',
  `config_value` longtext NOT NULL COMMENT '变更后配置',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_time` (`group_code`, `create_time`),
  KEY `idx_operator_time` (`operator_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI实验配置变更审计';
