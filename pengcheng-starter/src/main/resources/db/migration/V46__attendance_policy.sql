-- V46: 考勤策略配置
-- 1. sys_config_group 插入 attendance 分组（上下班时间 + 合规位置 + 百度地图 AK）
-- 2. attendance_record 加 location_status（1=合规 2=超出范围 NULL=未启用位置校验）

-- 1. 配置分组（默认开启时间校验、关闭位置校验，等管理员配置好位置后再开）
INSERT INTO sys_config_group (group_code, group_name, group_icon, config_value, sort, status, remark, create_time, update_time)
SELECT 'attendance', '考勤设置', NULL,
       '{"workStartTime":"09:00","workEndTime":"18:00","enforceTime":true,"enforceLocation":false,"allowedLocations":[],"baiduMapAk":""}',
       17, 1, '上下班时间、合规打卡位置、百度地图 AK', NOW(), NOW()
FROM (SELECT 1) AS _one
WHERE NOT EXISTS (SELECT 1 FROM sys_config_group WHERE group_code = 'attendance');

-- 2. attendance_record 加 location_status
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance_record' AND COLUMN_NAME = 'location_status') = 0,
  'ALTER TABLE `attendance_record` ADD COLUMN `location_status` TINYINT NULL COMMENT ''位置合规状态：1=合规 2=超出允许范围 NULL=未启用位置校验''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
