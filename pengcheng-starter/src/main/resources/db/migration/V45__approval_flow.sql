-- 审批流水线：节点模板表 + 实际流转记录表 + 直接上级字段 + 考勤豁免标记
-- 涉及业务：leave_request（请假） / realty_compensate_request（调休）

-- 1. sys_user 加 leader_id（直接上级用户 ID，优先于部门 leader）
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'leader_id') = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `leader_id` BIGINT NULL COMMENT ''直接上级用户ID（审批流 direct_supervisor 解析优先级最高）''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. sys_dept 加 leader_id（部门负责人 ID，sys_user.leader_id 缺失时的 fallback）
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dept' AND COLUMN_NAME = 'leader_id') = 0,
  'ALTER TABLE `sys_dept` ADD COLUMN `leader_id` BIGINT NULL COMMENT ''部门负责人用户ID（sys_user.leader_id 缺失时回退到此）''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. attendance_record 加 exempt_reason（请假/调休审批通过后写入，缺卡判定时跳过）
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance_record' AND COLUMN_NAME = 'exempt_reason') = 0,
  'ALTER TABLE `attendance_record` ADD COLUMN `exempt_reason` VARCHAR(64) NULL COMMENT ''考勤豁免原因（如 leave-病假 / compensate）''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 审批流节点模板（按 business_type 配置一组节点）
CREATE TABLE IF NOT EXISTS `approval_flow_node` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `business_type` VARCHAR(32) NOT NULL COMMENT '业务类型：leave 请假 / compensate 调休',
  `seq` INT NOT NULL COMMENT '节点顺序，从 1 开始',
  `node_name` VARCHAR(64) NOT NULL COMMENT '节点显示名（直接上级 / HR审批 ...）',
  `approver_type` VARCHAR(16) NOT NULL COMMENT 'direct_supervisor / role / user',
  `approver_value` VARCHAR(255) NULL COMMENT 'role/user 时填 ID 列表（逗号分隔）；direct_supervisor 时为 NULL',
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `create_by` BIGINT NULL,
  `update_by` BIGINT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  KEY `idx_business_type` (`business_type`, `enabled`, `deleted`, `seq`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '审批流节点模板';

-- 5. 审批流转实际记录（每条申请的每个节点一行）
CREATE TABLE IF NOT EXISTS `approval_record_node` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `business_type` VARCHAR(32) NOT NULL,
  `business_id` BIGINT NOT NULL COMMENT '关联 leave_request.id 或 realty_compensate_request.id',
  `seq` INT NOT NULL COMMENT '节点顺序',
  `node_name` VARCHAR(64) NOT NULL COMMENT '快照：节点名',
  `candidate_approver_ids` VARCHAR(500) NULL COMMENT '快照：候选审批人 ID 列表（逗号分隔）',
  `approver_id` BIGINT NULL COMMENT '实际执行审批的用户 ID',
  `result` TINYINT NULL COMMENT '1 通过 / 2 驳回 / NULL 待审批',
  `remark` VARCHAR(500) NULL,
  `approval_time` DATETIME NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `create_by` BIGINT NULL,
  `update_by` BIGINT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  KEY `idx_business` (`business_type`, `business_id`, `seq`),
  KEY `idx_pending` (`result`, `deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '审批流转实际记录';

-- 6. 种子默认流程：请假和调休各一个节点（直接上级），首次部署后管理后台可调整
INSERT INTO `approval_flow_node` (business_type, seq, node_name, approver_type, approver_value, enabled, create_time, update_time, deleted)
SELECT 'leave', 1, '直接上级', 'direct_supervisor', NULL, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM approval_flow_node WHERE business_type = 'leave' AND deleted = 0);

INSERT INTO `approval_flow_node` (business_type, seq, node_name, approver_type, approver_value, enabled, create_time, update_time, deleted)
SELECT 'compensate', 1, '直接上级', 'direct_supervisor', NULL, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM approval_flow_node WHERE business_type = 'compensate' AND deleted = 0);

-- 注意：本次迁移不为既有 status=1 的请假/调休回填 approval_record_node。
-- 部署后这些遗留申请将无法在新审批入口出现，需运维手动处理（或重新提交）。
