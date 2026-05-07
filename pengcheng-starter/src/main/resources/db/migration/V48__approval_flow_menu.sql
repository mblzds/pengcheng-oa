-- 审批流配置菜单：挂在「人事管理」一级目录下
-- 注意：菜单驱动的侧边栏 + RBAC 权限码（system:approval-flow:list / edit）

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '审批流配置', 2, '/system/approval-flow', 'system/approval-flow/index',
       'system:approval-flow:list', 'GitBranchOutline', 90, 1, 1, 0, 0
FROM sys_menu p
WHERE p.name = '人事管理' AND p.parent_id = 0 AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE name = '审批流配置' AND deleted = 0
  );

-- 编辑按钮权限（type=3 表示按钮）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT m.id, '编辑', 3, '', '', 'system:approval-flow:edit', '', 1, 1, 1, 0, 0
FROM sys_menu m
WHERE m.name = '审批流配置' AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE permission = 'system:approval-flow:edit' AND deleted = 0
  );

-- 默认把这两个权限授予「超级管理员」角色（id=1，按现有种子约定）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m
WHERE m.permission IN ('system:approval-flow:list', 'system:approval-flow:edit')
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id
  );
