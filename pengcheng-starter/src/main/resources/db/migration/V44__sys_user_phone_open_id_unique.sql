-- 为 sys_user.phone / open_id 加唯一索引，避免同一手机号或微信账号被绑定到多个用户
-- 前置：若存在脏数据（同手机号/openId 在多条 deleted=0 记录上），ALTER 会失败，需先清洗：
--   SELECT phone, COUNT(*) FROM sys_user WHERE phone IS NOT NULL AND phone <> '' AND deleted = 0 GROUP BY phone HAVING COUNT(*) > 1;
--   SELECT open_id, COUNT(*) FROM sys_user WHERE open_id IS NOT NULL AND open_id <> '' AND deleted = 0 GROUP BY open_id HAVING COUNT(*) > 1;

-- 1. 空字符串归一化为 NULL，避免 '' 被视为同一非空值触发误判
UPDATE `sys_user` SET `phone` = NULL WHERE `phone` = '';
UPDATE `sys_user` SET `open_id` = NULL WHERE `open_id` = '';

-- 2. 删除既有的非唯一 idx_open_id（如存在），由 uk_open_id 取代
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user'
     AND INDEX_NAME = 'idx_open_id') > 0,
  'ALTER TABLE `sys_user` DROP INDEX `idx_open_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 添加 uk_phone（如不存在）
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user'
     AND INDEX_NAME = 'uk_phone') = 0,
  'ALTER TABLE `sys_user` ADD UNIQUE INDEX `uk_phone` (`phone`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 添加 uk_open_id（如不存在）
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user'
     AND INDEX_NAME = 'uk_open_id') = 0,
  'ALTER TABLE `sys_user` ADD UNIQUE INDEX `uk_open_id` (`open_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
