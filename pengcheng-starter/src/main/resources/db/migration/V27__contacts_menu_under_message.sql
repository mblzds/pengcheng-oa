-- V27: 在「消息中心」下增加「通讯录」菜单（若不存在则插入，解决导航栏不显示问题）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT msg.id, '通讯录', 2, '/contacts', 'contacts/index', 'sys:chat:list', 'BookOutline', 3, 1, 1, 0, 0
FROM (SELECT id FROM sys_menu WHERE name = '消息中心' AND parent_id = 0 AND deleted = 0 LIMIT 1) msg
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '通讯录' AND deleted = 0);
