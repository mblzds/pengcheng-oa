-- V29: 补全所有缺失菜单，确保侧边栏完整展示全部功能模块
-- 使用 NOT EXISTS 防止重复插入

-- ========================================
-- 一、新增一级目录（type=1）
-- ========================================

-- 1. 房产业务
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT 0, '房产业务', 1, '', NULL, NULL, 'BusinessOutline', 3, 1, 1, 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '房产业务' AND parent_id = 0 AND deleted = 0);

-- 2. 智能助手
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT 0, '智能助手', 1, '', NULL, NULL, 'SparklesOutline', 4, 1, 1, 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0);

-- 3. 考勤管理（一级）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT 0, '考勤管理', 1, '', NULL, NULL, 'TimeOutline', 6, 1, 1, 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '考勤管理' AND parent_id = 0 AND deleted = 0);

-- 4. 开发工具（若不存在）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT 0, '开发工具', 1, '', NULL, NULL, 'HammerOutline', 99, 1, 1, 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '开发工具' AND parent_id = 0 AND deleted = 0);

-- ========================================
-- 二、房产业务 子菜单（type=2）
-- ========================================

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '客户管理', 2, '/realty/customer', 'realty/customer/CustomerManage', 'realty:customer:list', 'PeopleOutline', 1, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '客户管理' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '联盟商管理', 2, '/realty/alliance', 'realty/alliance/AllianceManage', 'realty:alliance:list', 'HandshakeOutline', 2, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '联盟商管理' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '成交佣金', 2, '/realty/commission', 'realty/commission/CommissionManage', 'realty:commission:list', 'CashOutline', 3, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '成交佣金' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '项目楼盘', 2, '/realty/project', 'realty/project/ProjectManage', 'realty:project:list', 'BusinessOutline', 4, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '项目楼盘' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '付款申请', 2, '/realty/payment', 'realty/payment/PaymentManage', 'realty:payment:list', 'WalletOutline', 5, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '付款申请' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '经营分析', 2, '/realty/analysis', 'realty/analysis/index', 'realty:analysis:list', 'StatsChartOutline', 6, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '经营分析' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '拜访记录', 2, '/realty/visit', 'realty/visit/index', 'realty:visit:list', 'FootstepsOutline', 7, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '拜访记录' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, 'AI 日报', 2, '/realty/report', 'realty/report/index', 'realty:report:list', 'NewspaperOutline', 8, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = 'AI 日报' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '销售质检', 2, '/realty/quality', 'realty/quality/index', 'realty:quality:list', 'CheckmarkCircleOutline', 9, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '销售质检' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '场景模板', 2, '/realty/templates', 'realty/templates/index', 'realty:templates:list', 'DocumentTextOutline', 10, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '场景模板' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '数据统计', 2, '/realty/stats', 'realty/stats/DashboardPage', 'realty:stats:list', 'BarChartOutline', 11, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '数据统计' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '销售日历', 2, '/realty/calendar', 'realty/calendar/index', 'realty:calendar:list', 'CalendarOutline', 12, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '销售日历' AND deleted = 0 AND parent_id = p.id);

-- ========================================
-- 三、智能助手 子菜单（type=2）
-- ========================================

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, 'AI 助手', 2, '/ai/chat', 'ai/chat/index', 'ai:chat:list', 'ChatbubbleOutline', 1, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = 'AI 助手' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '知识库管理', 2, '/ai/knowledge', 'ai/knowledge/AiKnowledge', 'ai:knowledge:list', 'LibraryOutline', 2, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '知识库管理' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, 'AI 实验', 2, '/ai/experiment', 'ai/experiment/AiExperiment', 'ai:experiment:list', 'FlaskOutline', 3, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = 'AI 实验' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '模型配置', 2, '/ai/config', 'ai/config/index', 'ai:config:list', 'SettingsOutline', 4, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '模型配置' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, 'AI 记忆', 2, '/ai/memory', 'ai/memory/index', 'ai:memory:list', 'BrainOutline', 5, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = 'AI 记忆' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, 'Skill 管理', 2, '/ai/skills', 'ai/skills/index', 'ai:skills:list', 'ExtensionPuzzleOutline', 6, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = 'Skill 管理' AND deleted = 0 AND parent_id = p.id);

INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, 'MCP 工具', 2, '/ai/mcp', 'ai/mcp/index', 'ai:mcp:list', 'ConstructOutline', 7, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = 'MCP 工具' AND deleted = 0 AND parent_id = p.id);

-- ========================================
-- 四、文件管理下新增子菜单
-- ========================================

-- 云文档
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '云文档', 2, '/doc', 'doc/index', 'sys:doc:list', 'DocumentOutline', 1, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '文件管理' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '云文档' AND deleted = 0 AND parent_id = p.id);

-- ========================================
-- 五、消息中心下新增子菜单
-- ========================================

-- 会议日程
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '会议日程', 2, '/meeting', 'meeting/index', 'sys:meeting:list', 'CalendarOutline', 4, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '消息中心' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '会议日程' AND deleted = 0 AND parent_id = p.id);

-- ========================================
-- 六、系统管理下新增子菜单
-- ========================================

-- 渠道推送
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '渠道推送', 2, '/system/channel', 'system/channel/index', 'sys:channel:list', 'NotificationsOutline', 6, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '系统管理' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '渠道推送' AND deleted = 0 AND parent_id = p.id);

-- 自动化规则
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '自动化规则', 2, '/system/automation', 'system/automation/index', 'sys:automation:list', 'GitCompareOutline', 7, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '系统管理' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '自动化规则' AND deleted = 0 AND parent_id = p.id);

-- ========================================
-- 七、系统监控下新增子菜单
-- ========================================

-- AI 巡检
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, 'AI 巡检', 2, '/monitor/heartbeat', 'monitor/heartbeat/index', 'monitor:heartbeat:list', 'PulseOutline', 6, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '系统监控' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = 'AI 巡检' AND deleted = 0 AND parent_id = p.id);

-- ========================================
-- 八、考勤管理下新增子菜单
-- ========================================

-- 考勤打卡
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '考勤打卡', 2, '/realty/attendance', 'realty/attendance/AttendanceManage', 'realty:attendance:list', 'FingerPrintOutline', 1, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '考勤管理' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '考勤打卡' AND deleted = 0 AND parent_id = p.id);

-- 人事与绩效
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '人事与绩效', 2, '/hr', 'hr/index', 'hr:index', 'RibbonOutline', 2, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '考勤管理' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '人事与绩效' AND deleted = 0 AND parent_id = p.id);

-- ========================================
-- 九、组织管理下新增子菜单
-- ========================================

-- 项目管理
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '项目管理', 2, '/project', 'project/index', 'project:list', 'FolderOutline', 3, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '组织管理' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '项目管理' AND deleted = 0 AND parent_id = p.id);

-- 待办事项
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '待办事项', 2, '/todo', 'todo/index', 'sys:todo:list', 'CheckboxOutline', 4, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '组织管理' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '待办事项' AND deleted = 0 AND parent_id = p.id);

-- ========================================
-- 十、开发工具下新增子菜单
-- ========================================

-- 代码生成（若不存在）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '代码生成', 2, '/tool/gen', 'tool/gen/index', 'tool:gen:list', 'CodeSlashOutline', 1, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '开发工具' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '代码生成' AND deleted = 0 AND parent_id = p.id);

-- ========================================
-- 十一、为 admin 角色自动分配所有新菜单
-- ========================================
-- admin 角色（超级管理员）通过 getUserMenuTree 中的 roleCodes.contains("admin") 已获取全量菜单，
-- 无需额外分配 sys_role_menu。

-- 为非 admin 角色：将新菜单分配给已拥有父目录权限的角色
-- 房产业务子菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.deleted = 0 AND m.type = 2
  AND m.parent_id = (SELECT id FROM sys_menu WHERE name = '房产业务' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '房产业务' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- 智能助手子菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.deleted = 0 AND m.type = 2
  AND m.parent_id = (SELECT id FROM sys_menu WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- 文件管理子菜单（云文档）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name = '云文档' AND m.deleted = 0
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '文件管理' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- 消息中心子菜单（会议日程）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name = '会议日程' AND m.deleted = 0
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '消息中心' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- 系统管理子菜单（渠道推送、自动化规则）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name IN ('渠道推送', '自动化规则') AND m.deleted = 0
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '系统管理' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- 系统监控子菜单（AI巡检）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name = 'AI 巡检' AND m.deleted = 0
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '系统监控' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- 考勤管理子菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.deleted = 0 AND m.type = 2
  AND m.parent_id = (SELECT id FROM sys_menu WHERE name = '考勤管理' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '考勤管理' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- 组织管理子菜单（项目管理、待办事项）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name IN ('项目管理', '待办事项') AND m.deleted = 0
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '组织管理' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);
