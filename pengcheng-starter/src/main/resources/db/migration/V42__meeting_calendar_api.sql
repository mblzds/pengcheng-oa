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

INSERT INTO `sys_config_group` (`group_code`, `group_name`, `config_value`, `sort`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'meetingConfig',
       '会议配置',
       '{"defaultReminder":15,"internalNotification":true,"email":false}',
       18,
       1,
       '会议日历提醒配置',
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_config_group WHERE group_code = 'meetingConfig');
