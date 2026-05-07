-- V47: 注册「考勤设置」菜单
-- 「考勤打卡」目前在两个 parent_id（281 / 314 - 重复的人事管理一级菜单）下都登记了一份，
-- 为保持一致，本次也在两个父菜单下分别加一条「考勤设置」。
-- 权限码命名沿用现有 `realty:attendance:` 前缀（与 sys_menu 中已有的 realty:attendance:list 对齐）。

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, create_time, update_time, deleted)
SELECT 281, '考勤设置', 2, '/realty/attendance/config', NULL, 'realty:attendance:config', 'SettingsOutline', 90, 1, 1, 0, NOW(), NOW(), 0
FROM (SELECT 1) AS _one
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE parent_id = 281 AND permission = 'realty:attendance:config' AND deleted = 0
);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, create_time, update_time, deleted)
SELECT 314, '考勤设置', 2, '/realty/attendance/config', NULL, 'realty:attendance:config', 'SettingsOutline', 90, 1, 1, 0, NOW(), NOW(), 0
FROM (SELECT 1) AS _one
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE parent_id = 314 AND permission = 'realty:attendance:config' AND deleted = 0
);
