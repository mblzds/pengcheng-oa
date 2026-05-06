-- 【说明】本脚本为历史可选菜单脚本，表结构（如 menu_name/order_num/perms）可能与当前 sys_menu 不一致。
-- 正式环境菜单以 Flyway 迁移（db/migration/Vxx__*.sql）与全量 SQL（sql/pengcheng-system.sql）为准；
-- 避免与 Flyway 重复执行导致重复菜单或归属冲突。详见 doc/MENU-CHANGE-PROCEDURE.md。
--
-- 1. 协作办公 (Workspace)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES 
('协作办公', 0, 2, 'workspace', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'AppsOutline', 'admin', NOW(), '', NULL, '协作办公一级菜单'),
('消息中心', (SELECT menu_id FROM sys_menu WHERE menu_name = '协作办公' LIMIT 1), 1, 'message/chat', 'message/chat/index', NULL, 1, 0, 'C', '0', '0', 'workspace:message:list', 'ChatbubbleOutline', 'admin', NOW(), '', NULL, '消息中心'),
('通讯录', (SELECT menu_id FROM sys_menu WHERE menu_name = '协作办公' LIMIT 1), 2, 'contacts', 'contacts/index', NULL, 1, 0, 'C', '0', '0', 'workspace:contacts:list', 'BookOutline', 'admin', NOW(), '', NULL, '通讯录'),
('会议日程', (SELECT menu_id FROM sys_menu WHERE menu_name = '协作办公' LIMIT 1), 3, 'meeting', 'meeting/index', NULL, 1, 0, 'C', '0', '0', 'workspace:meeting:list', 'CalendarOutline', 'admin', NOW(), '', NULL, '会议日程'),
('智能表格', (SELECT menu_id FROM sys_menu WHERE menu_name = '协作办公' LIMIT 1), 4, 'smart-table', 'smart-table/index', NULL, 1, 0, 'C', '0', '0', 'workspace:smarttable:list', 'GridOutline', 'admin', NOW(), '', NULL, '智能表格'),
('云文档', (SELECT menu_id FROM sys_menu WHERE menu_name = '协作办公' LIMIT 1), 5, 'system/file', 'system/file/index', NULL, 1, 0, 'C', '0', '0', 'system:file:list', 'FolderOpenOutline', 'admin', NOW(), '', NULL, '云文档');

-- 2. 房产业务 (Realty)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
('房产业务', 0, 3, 'realty', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'BusinessOutline', 'admin', NOW(), '', NULL, '房产业务一级菜单'),
('项目管理', (SELECT menu_id FROM sys_menu WHERE menu_name = '房产业务' LIMIT 1), 1, 'realty/project', 'realty/project/index', NULL, 1, 0, 'C', '0', '0', 'realty:project:list', 'BusinessOutline', 'admin', NOW(), '', NULL, '项目管理'),
('客户管理', (SELECT menu_id FROM sys_menu WHERE menu_name = '房产业务' LIMIT 1), 2, 'realty/customer', 'realty/customer/index', NULL, 1, 0, 'C', '0', '0', 'realty:customer:list', 'PeopleOutline', 'admin', NOW(), '', NULL, '客户管理'),
('联盟商管理', (SELECT menu_id FROM sys_menu WHERE menu_name = '房产业务' LIMIT 1), 3, 'realty/alliance', 'realty/alliance/index', NULL, 1, 0, 'C', '0', '0', 'realty:alliance:list', 'BusinessOutline', 'admin', NOW(), '', NULL, '联盟商管理'),
('成交佣金', (SELECT menu_id FROM sys_menu WHERE menu_name = '房产业务' LIMIT 1), 4, 'realty/commission', 'realty/commission/index', NULL, 1, 0, 'C', '0', '0', 'realty:commission:list', 'CashOutline', 'admin', NOW(), '', NULL, '成交佣金'),
('考勤管理', (SELECT menu_id FROM sys_menu WHERE menu_name = '房产业务' LIMIT 1), 5, 'realty/attendance', 'realty/attendance/index', NULL, 1, 0, 'C', '0', '0', 'realty:attendance:list', 'TimeOutline', 'admin', NOW(), '', NULL, '考勤管理'),
('付款申请', (SELECT menu_id FROM sys_menu WHERE menu_name = '房产业务' LIMIT 1), 6, 'realty/payment', 'realty/payment/index', NULL, 1, 0, 'C', '0', '0', 'realty:payment:list', 'WalletOutline', 'admin', NOW(), '', NULL, '付款申请'),
('数据统计', (SELECT menu_id FROM sys_menu WHERE menu_name = '房产业务' LIMIT 1), 7, 'realty/stats', 'realty/stats/index', NULL, 1, 0, 'C', '0', '0', 'realty:stats:list', 'StatsChartOutline', 'admin', NOW(), '', NULL, '数据统计');

-- 3. 智能助手 (AI)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
('智能助手', 0, 4, 'ai', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'SparklesOutline', 'admin', NOW(), '', NULL, '智能助手一级菜单'),
('AI 助手', (SELECT menu_id FROM sys_menu WHERE menu_name = '智能助手' LIMIT 1), 1, 'ai/chat', 'ai/chat/index', NULL, 1, 0, 'C', '0', '0', 'ai:chat:list', 'ChatbubbleOutline', 'admin', NOW(), '', NULL, 'AI 助手'),
('知识库管理', (SELECT menu_id FROM sys_menu WHERE menu_name = '智能助手' LIMIT 1), 2, 'ai/knowledge', 'ai/knowledge/index', NULL, 1, 0, 'C', '0', '0', 'ai:knowledge:list', 'LibraryOutline', 'admin', NOW(), '', NULL, '知识库管理'),
('模型与技能', (SELECT menu_id FROM sys_menu WHERE menu_name = '智能助手' LIMIT 1), 3, 'ai/config', 'ai/config/index', NULL, 1, 0, 'C', '0', '0', 'ai:config:list', 'SettingsOutline', 'admin', NOW(), '', NULL, '模型与技能');

-- 4. 清理旧菜单 (如有需要，可取消注释)
-- DELETE FROM sys_menu WHERE menu_name IN ('文件管理', '通知公告');
