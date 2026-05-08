-- V49: 注册「我的待审批」菜单（人事管理目录下）
-- 同 V47/V48：项目存在两条 parent_id（281 / 314）的「人事管理」一级目录，分别挂一份。
-- 后端控制器 /admin/approval/** 只校验登录态，菜单权限码用于侧边栏可见性控制。

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, create_time, update_time, deleted)
SELECT 281, '我的待审批', 2, '/hr/approval-pending', NULL, 'realty:approval:pending', 'CheckmarkDoneOutline', 80, 1, 1, 0, NOW(), NOW(), 0
FROM (SELECT 1) AS _one
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE parent_id = 281 AND permission = 'realty:approval:pending' AND deleted = 0
);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, create_time, update_time, deleted)
SELECT 314, '我的待审批', 2, '/hr/approval-pending', NULL, 'realty:approval:pending', 'CheckmarkDoneOutline', 80, 1, 1, 0, NOW(), NOW(), 0
FROM (SELECT 1) AS _one
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE parent_id = 314 AND permission = 'realty:approval:pending' AND deleted = 0
);

-- 任意登录用户都可能成为审批候选人，因此把该菜单授予所有现存角色（避免新员工看不到自己的待办）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission = 'realty:approval:pending' AND m.deleted = 0
WHERE r.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );
