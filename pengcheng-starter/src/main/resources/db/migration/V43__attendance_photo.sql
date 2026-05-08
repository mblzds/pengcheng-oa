SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance_record' AND COLUMN_NAME = 'clock_in_photo') = 0,
  'ALTER TABLE `attendance_record` ADD COLUMN `clock_in_photo` varchar(500) DEFAULT NULL COMMENT ''上班打卡照片URL''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance_record' AND COLUMN_NAME = 'clock_out_photo') = 0,
  'ALTER TABLE `attendance_record` ADD COLUMN `clock_out_photo` varchar(500) DEFAULT NULL COMMENT ''下班打卡照片URL''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
