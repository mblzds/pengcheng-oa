CREATE TABLE IF NOT EXISTS `ai_experiment_alert_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `alert_type` varchar(32) NOT NULL COMMENT '告警类型(auto_rollback/manual_block)',
  `experiment_type` varchar(16) NOT NULL COMMENT '实验类型(route/prompt)',
  `trigger_source` varchar(32) NOT NULL DEFAULT 'system' COMMENT '触发来源(system/manual)',
  `title` varchar(255) NOT NULL COMMENT '告警标题',
  `content` varchar(1024) NOT NULL COMMENT '告警内容',
  `dedupe_key` varchar(128) NOT NULL COMMENT '抑制去重键',
  `suppressed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否被抑制(0否1是)',
  `suppressed_until_epoch_ms` bigint DEFAULT NULL COMMENT '抑制截止时间戳(毫秒)',
  `metadata_json` text COMMENT '扩展元数据JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_time` (`experiment_type`, `create_time`),
  KEY `idx_alert_time` (`alert_type`, `create_time`),
  KEY `idx_dedupe_time` (`dedupe_key`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI实验异常告警日志';
