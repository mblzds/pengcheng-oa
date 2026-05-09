-- 员工花名册批量导入菜单（系统管理 → 用户管理 之后）
-- 权限复用 sys:user:add（与"新增用户"同权限即可上岗）

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status)
SELECT 1, '员工导入', 2, '/system/roster-import', '/system/roster-import/index', 'sys:user:add', 'CloudUploadOutline', 2, 1, 1
FROM (SELECT 1) AS _one
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system/roster-import' AND deleted = 0);
