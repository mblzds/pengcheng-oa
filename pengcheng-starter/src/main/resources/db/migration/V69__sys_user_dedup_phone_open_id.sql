-- V69: 清洗 sys_user 中重复的 phone / open_id，并幂等地确保 uk_phone / uk_open_id 存在
--
-- 背景 / 为什么需要这条迁移：
--   V44 给 sys_user.phone / open_id 加了 UNIQUE INDEX，但 V44 仅在注释里写了"前置需手工
--   清洗重复数据"，SQL 体本身没做清洗。任何环境只要导入的 dump（如 sql/pengcheng-system.sql）
--   或残留测试数据里存在重复 phone / open_id，V44 必然 Duplicate entry 失败，
--   并在 flyway_schema_history 里留 success=0，后续每次启动 Flyway 都拒绝继续。
--
--   本迁移作为防御层：
--     1. 主动清掉重复，避免下次 dev / test 库重建踩同样的雷
--     2. 幂等补齐 uk_phone / uk_open_id（V44 已加过则跳过），让 V44 即便被人工 repair
--        跳过也仍然能达到目标终态
--
-- 注意：
--   本迁移**不能挽救已经在 flyway_schema_history 留下 success=0 的环境**——
--   Flyway 会在到达 V69 之前就被 V44 那行失败记录卡住。受影响环境仍需先：
--     DELETE FROM flyway_schema_history WHERE version = '44' AND success = 0;
--   清掉失败记录，Flyway 才能继续推进到 V69。
--
-- 清洗规则：每个重复值保留 1 条，其它置 NULL
--   优先保留 deleted=0（有效用户）；同 deleted 状态下保留最小 id（最早创建的）。

-- 1. 先把空字符串归一化为 NULL（与 V44 保持一致，幂等无副作用）
UPDATE `sys_user` SET `phone` = NULL WHERE `phone` = '';
UPDATE `sys_user` SET `open_id` = NULL WHERE `open_id` = '';

-- 2. 清重复 phone
--    窗口函数按 (deleted ASC, id ASC) 排序，rn=1 的留下，其余改 NULL
UPDATE `sys_user` u
JOIN (
  SELECT id, ROW_NUMBER() OVER (
    PARTITION BY phone
    ORDER BY deleted ASC, id ASC
  ) AS rn
  FROM `sys_user`
  WHERE phone IS NOT NULL AND phone <> ''
) r ON r.id = u.id
SET u.`phone` = NULL
WHERE r.rn > 1;

-- 3. 清重复 open_id
UPDATE `sys_user` u
JOIN (
  SELECT id, ROW_NUMBER() OVER (
    PARTITION BY open_id
    ORDER BY deleted ASC, id ASC
  ) AS rn
  FROM `sys_user`
  WHERE open_id IS NOT NULL AND open_id <> ''
) r ON r.id = u.id
SET u.`open_id` = NULL
WHERE r.rn > 1;

-- 4. 幂等补 uk_phone（V44 已加则跳过）
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user'
     AND INDEX_NAME = 'uk_phone') = 0,
  'ALTER TABLE `sys_user` ADD UNIQUE INDEX `uk_phone` (`phone`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. 幂等补 uk_open_id
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user'
     AND INDEX_NAME = 'uk_open_id') = 0,
  'ALTER TABLE `sys_user` ADD UNIQUE INDEX `uk_open_id` (`open_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
