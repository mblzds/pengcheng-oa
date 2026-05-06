-- V28: 为已拥有「文件管理」「消息中心」的角色补充子菜单权限（智能表格、表格模板管理、通讯录）
-- 使非 admin 角色在导航栏能看到新增/归入的菜单，无需手动在角色管理里勾选

-- 已拥有「文件管理」的角色 → 补充「智能表格」「表格模板管理」
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name IN ('智能表格', '表格模板管理') AND m.deleted = 0
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '文件管理' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- 已拥有「消息中心」的角色 → 补充「通讯录」
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name = '通讯录' AND m.deleted = 0
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '消息中心' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);
