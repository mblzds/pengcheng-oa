-- V33: 360 度绩效评估功能
-- 支持自评、上级评价、同事评价、下级评价多维度考核

-- 1. 创建 360 度评估表
CREATE TABLE IF NOT EXISTS `kpi_review_360` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `period_id` bigint NOT NULL COMMENT '考核周期 ID',
  `user_id` bigint NOT NULL COMMENT '被评估人 ID',
  `reviewer_id` bigint NOT NULL COMMENT '评估人 ID',
  `review_type` tinyint NOT NULL DEFAULT 1 COMMENT '评估类型：1-自评 2-上级 3-同事 4-下级',
  `total_score` decimal(5,2) DEFAULT NULL COMMENT '总分',
  `comment` varchar(1000) DEFAULT NULL COMMENT '评价意见',
  `strengths` varchar(500) DEFAULT NULL COMMENT '优点/长处',
  `improvements` varchar(500) DEFAULT NULL COMMENT '待改进项',
  `status` tinyint DEFAULT 1 COMMENT '状态：1-待评估 2-已完成',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_period_user` (`period_id`, `user_id`),
  KEY `idx_reviewer` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='360 度评估表';

-- 2. 创建评估关系配置表
CREATE TABLE IF NOT EXISTS `kpi_review_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `manager_id` bigint DEFAULT NULL COMMENT '上级 ID',
  `period_id` bigint NOT NULL COMMENT '考核周期 ID',
  `status` tinyint DEFAULT 1 COMMENT '状态：1-有效 0-无效',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_period` (`user_id`, `period_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评估关系配置表';

-- 3. 创建同事评估关系表
CREATE TABLE IF NOT EXISTS `kpi_peer_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `peer_id` bigint NOT NULL COMMENT '同事 ID',
  `period_id` bigint NOT NULL COMMENT '考核周期 ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门 ID（用于同部门筛选）',
  `status` tinyint DEFAULT 1 COMMENT '状态：1-有效 0-无效',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_period` (`user_id`, `period_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='同事评估关系表';

-- 4. 初始化 360 度评估权重配置（自评 10%、上级 40%、同事 30%、下级 20%）
INSERT INTO `sys_config_group` (
  `group_code`, `group_name`, `group_icon`, `config_value`, `sort`, `status`, `remark`, `create_time`, `update_time`
) VALUES (
  'kpi360Config',
  '360 度评估配置',
  NULL,
  '{"weights":{"self":0.1,"manager":0.4,"peer":0.3,"subordinate":0.2},"minReviewers":3,"anonymous":true}',
  17,
  1,
  '360 度绩效评估权重和规则配置',
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE
  `group_name` = VALUES(`group_name`),
  `group_icon` = VALUES(`group_icon`),
  `config_value` = VALUES(`config_value`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`),
  `update_time` = VALUES(`update_time`);
