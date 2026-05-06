CREATE TABLE IF NOT EXISTS `ai_tool_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tool_name` VARCHAR(64) NOT NULL COMMENT '工具名称',
  `scene` VARCHAR(16) NOT NULL COMMENT '调用场景: APP/ADMIN',
  `intent` VARCHAR(32) NOT NULL COMMENT '意图路由',
  `conversation_id` VARCHAR(64) NULL DEFAULT NULL COMMENT '会话ID',
  `user_id` BIGINT NULL DEFAULT NULL COMMENT '调用用户ID',
  `role_codes` VARCHAR(255) NULL DEFAULT NULL COMMENT '角色编码列表(逗号分隔)',
  `request_summary` VARCHAR(500) NULL DEFAULT NULL COMMENT '请求摘要',
  `response_summary` VARCHAR(500) NULL DEFAULT NULL COMMENT '响应摘要',
  `success` TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功: 1成功 0失败',
  `latency_ms` BIGINT NULL DEFAULT NULL COMMENT '耗时(ms)',
  `call_chain` VARCHAR(255) NULL DEFAULT NULL COMMENT '调用链摘要',
  `error_message` VARCHAR(500) NULL DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_tool_scene_time` (`tool_name`, `scene`, `create_time`),
  INDEX `idx_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI工具调用审计日志';

