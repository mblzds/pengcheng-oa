-- ============================================================
-- V72: 统一角色体系
--
-- 目标：把 12 个角色精简成 6 个标准角色，命名统一，数据可见 / 菜单可见两个维度
-- 完全正交，全部靠 sys_role.data_scope + sys_role_menu 决定，不再需要 flow_*
-- 这类并行角色。
--
-- 最终角色列表：
--   admin              data_scope=1  超管（保留不动）
--   chairman           data_scope=1  董事长（本迁移新建）
--   general_manager    data_scope=1  总经理（V67 已存在）
--   hr                 data_scope=1  HR
--   finance            data_scope=1  财务（原 cw 改名）
--   dept_manager       data_scope=4  部门经理（原 manager 改名，V71 已调 data_scope）
--   employee           data_scope=5  普通员工（原 user 改名，V70 已调 data_scope）
--
-- 改动：
--   1. 改名 user → employee, cw → finance, manager → dept_manager
--   2. 迁移挂在 flow_* 上的用户到对应业务角色（flow_hr→hr, flow_dept_mgr→
--      dept_manager, flow_gm→general_manager, flow_finance→finance）
--   3. 软删 flow_gm / flow_hr / flow_dept_mgr / flow_finance / sales /
--      sales_manager 这 6 个冗余角色
--   4. 新建 chairman 角色
--
-- 审批引擎兼容性：
--   approval_flow_node 模板的 approver_value 直接引用业务角色 id（hr=3, cw=54）
--   而不是 flow_* 角色 id，所以 flow_* 删了不影响任何审批流路由。
--
-- 安全性：
--   - INSERT IGNORE 避免重复挂载冲突
--   - 软删（deleted=1）保留历史可查，不真正物理删除
--   - 重复执行幂等（按 code 判断）
-- ============================================================

-- 1. 改名 user → employee
UPDATE sys_role SET code = 'employee', name = '普通员工'
WHERE code = 'user' AND deleted = 0;

-- 2. 改名 cw → finance
UPDATE sys_role SET code = 'finance', name = '财务'
WHERE code = 'cw' AND deleted = 0;

-- 3. 改名 manager → dept_manager
UPDATE sys_role SET code = 'dept_manager', name = '部门经理'
WHERE code = 'manager' AND deleted = 0;

-- 4. 迁移 flow_hr (审批流-HR) 挂的用户到 hr 业务角色
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user_id, role_id_dest FROM (
  SELECT ur.user_id, r_dst.id AS role_id_dest
  FROM sys_user_role ur
  JOIN sys_role r_src ON r_src.id = ur.role_id
  JOIN sys_role r_dst ON r_dst.code = 'hr' AND r_dst.deleted = 0
  WHERE r_src.code = 'flow_hr' AND r_src.deleted = 0
) AS migration_flow_hr;

-- 5. 迁移 flow_dept_mgr 挂的用户到 dept_manager
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user_id, role_id_dest FROM (
  SELECT ur.user_id, r_dst.id AS role_id_dest
  FROM sys_user_role ur
  JOIN sys_role r_src ON r_src.id = ur.role_id
  JOIN sys_role r_dst ON r_dst.code = 'dept_manager' AND r_dst.deleted = 0
  WHERE r_src.code = 'flow_dept_mgr' AND r_src.deleted = 0
) AS migration_flow_dept_mgr;

-- 6. 迁移 flow_gm 挂的用户到 general_manager
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user_id, role_id_dest FROM (
  SELECT ur.user_id, r_dst.id AS role_id_dest
  FROM sys_user_role ur
  JOIN sys_role r_src ON r_src.id = ur.role_id
  JOIN sys_role r_dst ON r_dst.code = 'general_manager' AND r_dst.deleted = 0
  WHERE r_src.code = 'flow_gm' AND r_src.deleted = 0
) AS migration_flow_gm;

-- 7. 迁移 flow_finance 挂的用户到 finance（即便目前 flow_finance 无用户，保留迁移以防未来）
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user_id, role_id_dest FROM (
  SELECT ur.user_id, r_dst.id AS role_id_dest
  FROM sys_user_role ur
  JOIN sys_role r_src ON r_src.id = ur.role_id
  JOIN sys_role r_dst ON r_dst.code = 'finance' AND r_dst.deleted = 0
  WHERE r_src.code = 'flow_finance' AND r_src.deleted = 0
) AS migration_flow_finance;

-- 8. 清空所有 flow_* 角色的用户挂载
DELETE FROM sys_user_role WHERE role_id IN (
  SELECT id FROM (
    SELECT id FROM sys_role WHERE code IN ('flow_gm', 'flow_hr', 'flow_dept_mgr', 'flow_finance')
  ) AS flow_role_ids
);

-- 9. 软删冗余角色（flow_* + V67 建好但未使用的 sales / sales_manager）
UPDATE sys_role SET deleted = 1
WHERE code IN ('flow_gm', 'flow_hr', 'flow_dept_mgr', 'flow_finance',
               'sales', 'sales_manager')
  AND deleted = 0;

-- 10. 新建 chairman 角色（董事长，data_scope=1）
INSERT INTO sys_role (name, code, sort, status, remark, data_scope, create_time, update_time)
SELECT '董事长', 'chairman', 5, 1, '公司董事长，数据范围全部；菜单按需配置', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'chairman' AND deleted = 0);
