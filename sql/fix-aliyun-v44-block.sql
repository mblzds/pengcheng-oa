-- ============================================================
-- 阿里云 V44 部署阻塞 一键修复脚本
-- ============================================================
-- 用途：清掉 V44 失败留下的 flyway 记录 + sys_user 里的重复 phone/open_id，
--       让 Flyway 重启后能继续推进到 V69。
-- 用法：在阿里云 mysql 容器或 RDS 上执行
--   docker compose exec -T mysql sh -c \
--     'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
--     < sql/fix-aliyun-v44-block.sql
-- 安全性：所有 UPDATE 语句保留 deleted=0 + 最小 id 的记录，其它改 NULL；
--        DELETE 仅作用于 flyway_schema_history 中 success=0 的 V44 那行。
-- 幂等：跑多次没副作用。
-- ============================================================

-- ===== Step 1: 看现状（不修改） =====
SELECT '=== 失败的 Flyway 迁移 ===' AS section;
SELECT version, description, success, installed_on
FROM flyway_schema_history
WHERE success = 0;

SELECT '=== 重复 phone（清洗前） ===' AS section;
SELECT phone, COUNT(*) AS cnt
FROM sys_user
WHERE phone IS NOT NULL AND phone <> ''
GROUP BY phone HAVING cnt > 1;

SELECT '=== 重复 open_id（清洗前） ===' AS section;
SELECT open_id, COUNT(*) AS cnt
FROM sys_user
WHERE open_id IS NOT NULL AND open_id <> ''
GROUP BY open_id HAVING cnt > 1;

-- ===== Step 2: 清洗数据 =====
-- 空串归一化
UPDATE sys_user SET phone = NULL WHERE phone = '';
UPDATE sys_user SET open_id = NULL WHERE open_id = '';

-- 清重复 phone（保留 deleted=0 优先，同状态保留最小 id）
UPDATE sys_user u
JOIN (
  SELECT id, ROW_NUMBER() OVER (
    PARTITION BY phone ORDER BY deleted ASC, id ASC
  ) AS rn
  FROM sys_user
  WHERE phone IS NOT NULL AND phone <> ''
) r ON r.id = u.id
SET u.phone = NULL
WHERE r.rn > 1;

-- 清重复 open_id
UPDATE sys_user u
JOIN (
  SELECT id, ROW_NUMBER() OVER (
    PARTITION BY open_id ORDER BY deleted ASC, id ASC
  ) AS rn
  FROM sys_user
  WHERE open_id IS NOT NULL AND open_id <> ''
) r ON r.id = u.id
SET u.open_id = NULL
WHERE r.rn > 1;

-- ===== Step 3: 删 Flyway 失败记录 =====
DELETE FROM flyway_schema_history
WHERE version = '44' AND success = 0;

-- ===== Step 4: 验证 =====
SELECT '=== 清洗后 - 应当全部为空 ===' AS section;
SELECT phone, COUNT(*) AS cnt FROM sys_user
WHERE phone IS NOT NULL AND phone <> ''
GROUP BY phone HAVING cnt > 1;

SELECT open_id, COUNT(*) AS cnt FROM sys_user
WHERE open_id IS NOT NULL AND open_id <> ''
GROUP BY open_id HAVING cnt > 1;

SELECT '=== 残留失败迁移 - 应当为空 ===' AS section;
SELECT version, description, success FROM flyway_schema_history
WHERE success = 0;

SELECT '=== 修复完成，可以重启 app 容器 ===' AS done;
