-- 人事菜单（公司级）：一级目录「人事」+ 子菜单「人事与绩效」「考勤管理」
-- 执行前请确认 sys_menu 表结构同 update_menu_2.0.sql（menu_name, parent_id, order_num, path, component, menu_type, perms 等）
-- 若表字段为 name/type/sort 等，请按实际表结构调整列名

-- 1. 人事（一级目录）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('人事', 0, 8, 'hr', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'PeopleCircleOutline', 'admin', NOW(), '', NULL, '公司级人事与假勤');

-- 2. 人事与绩效（人事目录下）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('人事与绩效', (SELECT menu_id FROM sys_menu WHERE menu_name = '人事' AND parent_id = 0 LIMIT 1), 1, 'hr', 'hr/index', NULL, 1, 0, 'C', '0', '0', 'hr:index', 'RibbonOutline', 'admin', NOW(), '', NULL, '人事档案、异动、考核周期、KPI、绩效考核');

-- 3. 考勤管理（人事目录下，与房产业务下考勤同路由）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('考勤管理', (SELECT menu_id FROM sys_menu WHERE menu_name = '人事' AND parent_id = 0 LIMIT 1), 2, 'realty/attendance', 'realty/attendance/index', NULL, 1, 0, 'C', '0', '0', 'realty:attendance:list', 'TimeOutline', 'admin', NOW(), '', NULL, '考勤记录、月度汇总、请假调休');
