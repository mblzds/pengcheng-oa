-- V26: 将智能表格、表格模板管理归入「文件管理」菜单下
-- 1) 已存在则只改父级为「文件管理」
UPDATE sys_menu m
INNER JOIN (SELECT id FROM sys_menu WHERE name = '文件管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) file ON 1=1
SET m.parent_id = file.id
WHERE m.name IN ('智能表格', '表格模板管理') AND m.deleted = 0;

-- 2) 若不存在则新增（仅用「文件管理」为父级，便于未执行过菜单脚本的环境）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT file.id, '智能表格', 2, '/smart-table', 'smarttable/index', 'sys:smarttable:list', 'GridOutline', 3, 1, 1, 0, 0
FROM (SELECT id FROM sys_menu WHERE name = '文件管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) file
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '智能表格' AND deleted = 0);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT file.id, '表格模板管理', 2, '/smart-table/template-mgmt', 'smarttable/templates', 'sys:smarttable:template', 'CopyOutline', 4, 1, 1, 0, 0
FROM (SELECT id FROM sys_menu WHERE name = '文件管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) file
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '表格模板管理' AND deleted = 0);
