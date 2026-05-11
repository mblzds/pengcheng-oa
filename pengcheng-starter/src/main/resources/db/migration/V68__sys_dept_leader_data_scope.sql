-- ============================================================
-- V68: 启用 sys_dept.leader_id 作为客户数据权限驱动
--
-- 背景：
--   V45 已为审批流加了 sys_dept.leader_id 字段（部门负责人用户ID），
--   但当时只回填了字段，没建索引也没回填历史数据，所以拦截器无法基于
--   它做数据权限判定。
--
-- 本脚本：
--   1. 给 sys_dept.leader_id 加索引（拦截器子查询的过滤列）
--   2. 用 sys_dept.leader（旧 VARCHAR 字段）匹配 sys_user.nickname 回填
--      leader_id —— 仅当 nickname 在 sys_user 表中唯一时才回填，避免同名
--      错配；多名重名的留 NULL 由管理员后台手工补
--
-- 配套代码：
--   - DataPermissionInterceptor.buildRealtyDataScopeFilter 末尾叠加
--     "若当前用户是任意部门的 leader_id，自动获得本部门及以下客户可见权限"
--   - 通过 sys_dept.ancestors 字段做层级下钻（FIND_IN_SET）
--
-- 兼容性：
--   - 字段已存在（V45），仅追加索引和回填数据，重复执行幂等
--   - 不破坏现有 sys_dept.leader 字符串字段，保留作为展示
-- ============================================================

-- 1. 加索引（如不存在）
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dept' AND INDEX_NAME = 'idx_sys_dept_leader_id') = 0,
  'ALTER TABLE `sys_dept` ADD INDEX `idx_sys_dept_leader_id` (`leader_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 历史回填 leader_id：仅匹配 nickname 唯一的用户，避免同名错配
UPDATE sys_dept d
JOIN (
    SELECT u.nickname, MIN(u.id) AS user_id
    FROM sys_user u
    WHERE u.deleted = 0
    GROUP BY u.nickname
    HAVING COUNT(*) = 1
) unique_users ON TRIM(d.leader) = TRIM(unique_users.nickname)
SET d.leader_id = unique_users.user_id
WHERE d.leader_id IS NULL
  AND d.leader IS NOT NULL
  AND TRIM(d.leader) <> ''
  AND d.deleted = 0;
