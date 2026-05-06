-- V34: 会议日程功能完善
-- 增加会议提醒、会议纪要、会议文件等功能

-- 1. 添加会议通知表
CREATE TABLE IF NOT EXISTS `meeting_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `meeting_id` bigint NOT NULL COMMENT '会议 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `notify_type` tinyint DEFAULT 1 COMMENT '通知类型：1-站内信 2-邮件 3-短信',
  `notify_time` datetime DEFAULT NULL COMMENT '通知时间',
  `status` tinyint DEFAULT 1 COMMENT '状态：1-未通知 2-已通知 3-已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_meeting` (`meeting_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会议通知表';

-- 2. 添加会议纪要表
CREATE TABLE IF NOT EXISTS `meeting_minutes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `meeting_id` bigint NOT NULL COMMENT '会议 ID',
  `content` text COMMENT '纪要内容',
  `conclusions` text COMMENT '会议结论',
  `action_items` text COMMENT '待办事项',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人 ID',
  `status` tinyint DEFAULT 1 COMMENT '状态：1-草稿 2-已发布',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_meeting` (`meeting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会议纪要表';

-- 3. 添加会议文件表
CREATE TABLE IF NOT EXISTS `meeting_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `meeting_id` bigint NOT NULL COMMENT '会议 ID',
  `file_id` bigint NOT NULL COMMENT '文件 ID',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文件名',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传人 ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_meeting` (`meeting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会议文件表';

-- 4. 扩展 CalendarEvent 表添加会议相关字段
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_calendar_event' AND COLUMN_NAME = 'meeting_type') = 0,
  'ALTER TABLE `sys_calendar_event` ADD COLUMN `meeting_type` tinyint DEFAULT 1 COMMENT ''会议类型：1-普通会议 2-视频会 3-电话会''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_calendar_event' AND COLUMN_NAME = 'meeting_url') = 0,
  'ALTER TABLE `sys_calendar_event` ADD COLUMN `meeting_url` varchar(500) DEFAULT NULL COMMENT ''视频会议链接''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_calendar_event' AND COLUMN_NAME = 'organizer_id') = 0,
  'ALTER TABLE `sys_calendar_event` ADD COLUMN `organizer_id` bigint DEFAULT NULL COMMENT ''组织者 ID''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_calendar_event' AND COLUMN_NAME = 'reminder_minutes') = 0,
  'ALTER TABLE `sys_calendar_event` ADD COLUMN `reminder_minutes` int DEFAULT 15 COMMENT ''提前提醒分钟数''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 添加会议重复规则配置
INSERT INTO `sys_config_group` (`group_code`, `group_name`, `config_value`, `sort`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'meetingConfig', '会议配置', '{"defaultReminder":15,"maxParticipants":50,"allowExternal":false}', 18, 1, '会议系统配置', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_config_group WHERE group_code = 'meetingConfig');
