-- 节假日日历管理菜单：挂在「人事管理」一级目录下，紧邻「审批流配置」
-- 权限码：system:holiday:list / edit（管理员维护节假日 + 调休补班，影响考勤月报）

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '节假日日历', 2, '/system/holiday', 'system/holiday/index',
       'system:holiday:list', 'CalendarOutline', 91, 1, 1, 0, 0
FROM sys_menu p
WHERE p.name = '人事管理' AND p.parent_id = 0 AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE name = '节假日日历' AND deleted = 0
  );

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT m.id, '编辑', 3, '', '', 'system:holiday:edit', '', 1, 1, 1, 0, 0
FROM sys_menu m
WHERE m.name = '节假日日历' AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE permission = 'system:holiday:edit' AND deleted = 0
  );

-- 默认授予「超级管理员」（id=1）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m
WHERE m.permission IN ('system:holiday:list', 'system:holiday:edit')
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id
  );
