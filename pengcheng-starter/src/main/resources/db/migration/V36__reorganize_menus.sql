-- V36: 菜单栏全面重组 — 业务优先排序
-- 修复 V30 错误删除的后端菜单，按业务领域重新组织菜单层级
-- V30 错误地认为"前端路由已定义就不需要后端菜单"，但侧边栏完全由 sys_menu 驱动
--
-- 目标结构（9个一级目录）:
--   1.房产业务 → 2.智能助手 → 3.协作办公(新) → 4.人事管理(原考勤管理)
--   → 5.组织管理 → 6.系统管理 → 7.系统监控 → 8.系统日志 → 99.开发工具

-- ==========================================================
-- 第一步：恢复被 V30 错误删除的所有菜单
-- ==========================================================

-- 一级目录（被 V30 误删的）
UPDATE sys_menu SET deleted = 0
WHERE name IN ('智能助手','开发工具')
  AND parent_id = 0 AND deleted = 1;

-- 学生管理（已删除的开发工具子菜单，确保不恢复）
-- 无需操作，id=272 已 deleted=1

-- 智能助手子菜单（7项）
UPDATE sys_menu SET deleted = 0
WHERE name IN ('AI 助手','知识库管理','AI 实验','模型配置','AI 记忆','Skill 管理','MCP 工具')
  AND deleted = 1;

-- 房产业务子菜单（5项）
UPDATE sys_menu SET deleted = 0
WHERE name IN ('拜访记录','AI 日报','销售质检','场景模板','销售日历')
  AND deleted = 1;

-- 文件/消息子菜单（5项）
UPDATE sys_menu SET deleted = 0
WHERE name IN ('云文档','智能表格','表格模板管理','会议日程','通讯录')
  AND deleted = 1;

-- 系统管理/监控子菜单（3项）
UPDATE sys_menu SET deleted = 0
WHERE name IN ('渠道推送','自动化规则','AI 巡检')
  AND deleted = 1;

-- 组织管理子菜单（2项）
UPDATE sys_menu SET deleted = 0
WHERE name IN ('项目管理','待办事项')
  AND deleted = 1;

-- 人事相关（2项）
UPDATE sys_menu SET deleted = 0
WHERE name IN ('考勤打卡','人事与绩效')
  AND deleted = 1;

-- 文件配置及其按钮权限（初始数据即 deleted=1）
UPDATE sys_menu SET deleted = 0 WHERE id IN (130, 131, 132, 133) AND deleted = 1;

-- 修正不存在的图标名称
UPDATE sys_menu SET icon = 'BusinessOutline'  WHERE name = '联盟商管理' AND deleted = 0 AND icon = 'HandshakeOutline';
UPDATE sys_menu SET icon = 'WalkOutline'      WHERE name = '拜访记录'   AND deleted = 0 AND icon = 'FootstepsOutline';

-- ==========================================================
-- 第二步：创建「协作办公」一级目录
-- ==========================================================
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT 0, '协作办公', 1, '', NULL, NULL, 'ChatbubblesOutline', 3, 1, 1, 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '协作办公' AND parent_id = 0 AND deleted = 0);

-- ==========================================================
-- 第三步：重命名目录和菜单
-- ==========================================================

-- 「考勤管理」→「人事管理」
UPDATE sys_menu SET name = '人事管理', icon = 'PeopleOutline'
WHERE name = '考勤管理' AND parent_id = 0 AND deleted = 0;

-- 「人事与绩效」→「人事档案」
UPDATE sys_menu SET name = '人事档案', icon = 'PersonOutline', sort = 1, permission = 'hr:manage:list'
WHERE name = '人事与绩效' AND deleted = 0;

-- ==========================================================
-- 第四步：将子菜单移至新归属目录
-- ==========================================================

-- 4.1 消息中心子菜单 → 协作办公
UPDATE sys_menu child
  JOIN sys_menu new_p ON new_p.name = '协作办公' AND new_p.parent_id = 0 AND new_p.deleted = 0
SET child.parent_id = new_p.id
WHERE child.name IN ('即时聊天','系统通知','通讯录') AND child.deleted = 0
  AND child.parent_id = 134;

-- 4.2 会议日程 → 协作办公（改名为会议日历，指向日历视图）
UPDATE sys_menu child
  JOIN sys_menu new_p ON new_p.name = '协作办公' AND new_p.parent_id = 0 AND new_p.deleted = 0
SET child.parent_id = new_p.id,
    child.name = '会议日历',
    child.path = '/meeting/calendar',
    child.component = 'meeting/MeetingCalendar',
    child.icon = 'CalendarOutline'
WHERE child.name = '会议日程' AND child.deleted = 0;

-- 4.3 文件管理下的协作类菜单 → 协作办公
UPDATE sys_menu child
  JOIN sys_menu new_p ON new_p.name = '协作办公' AND new_p.parent_id = 0 AND new_p.deleted = 0
SET child.parent_id = new_p.id
WHERE child.name IN ('云文档','智能表格','表格模板管理') AND child.deleted = 0
  AND child.parent_id = 126;

-- 4.4 组织管理下的协作类菜单 → 协作办公
UPDATE sys_menu child
  JOIN sys_menu new_p ON new_p.name = '协作办公' AND new_p.parent_id = 0 AND new_p.deleted = 0
SET child.parent_id = new_p.id
WHERE child.name IN ('待办事项','项目管理') AND child.deleted = 0
  AND child.parent_id = 22;

-- 4.5 文件列表、文件配置 → 系统管理(id=1)
UPDATE sys_menu SET parent_id = 1 WHERE name = '文件列表' AND parent_id = 126 AND deleted = 0;
UPDATE sys_menu SET parent_id = 1 WHERE id = 130 AND parent_id = 126 AND deleted = 0;

-- ==========================================================
-- 第五步：添加缺失菜单
-- ==========================================================

-- 客户公海池（房产业务）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '客户公海池', 2, '/realty/customer-pool', 'realty/customer/CustomerPool', 'realty:customer:pool', 'PeopleOutline', 2, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '客户公海池' AND deleted = 0);

-- 360度评估（人事管理）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '360度评估', 2, '/hr/review-360', 'hr/Review360', 'hr:review360:list', 'PeopleCircleOutline', 4, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '人事管理' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '360度评估' AND deleted = 0);

-- ==========================================================
-- 第六步：更新一级目录排序（业务优先）
-- ==========================================================
UPDATE sys_menu SET sort = 1  WHERE name = '房产业务' AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 2  WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 3  WHERE name = '协作办公' AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 4  WHERE name = '人事管理' AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 5  WHERE name = '组织管理' AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 6  WHERE name = '系统管理' AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 7  WHERE name = '系统监控' AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 8  WHERE name = '系统日志' AND parent_id = 0 AND deleted = 0;
UPDATE sys_menu SET sort = 99 WHERE name = '开发工具' AND parent_id = 0 AND deleted = 0;

-- ==========================================================
-- 第七步：更新子菜单排序
-- ==========================================================

-- ---- 房产业务 ----
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 1  WHERE m.name = '客户管理'   AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 2  WHERE m.name = '客户公海池' AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 3  WHERE m.name = '联盟商管理' AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 4  WHERE m.name = '项目楼盘'   AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 5  WHERE m.name = '成交佣金'   AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 6  WHERE m.name = '付款申请'   AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 7  WHERE m.name = '拜访记录'   AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 8  WHERE m.name = '销售日历'   AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 9  WHERE m.name = 'AI 日报'    AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 10 WHERE m.name = '销售质检'   AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 11 WHERE m.name = '场景模板'   AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 12 WHERE m.name = '经营分析'   AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '房产业务' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 13 WHERE m.name = '数据统计'   AND m.parent_id = p.id AND m.deleted = 0;

-- ---- 智能助手 ----
UPDATE sys_menu m JOIN sys_menu p ON p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 1 WHERE m.name = 'AI 助手'    AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 2 WHERE m.name = '知识库管理'  AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 3 WHERE m.name = 'AI 实验'    AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 4, m.name = '模型与技能' WHERE m.name = '模型配置' AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 5 WHERE m.name = 'AI 记忆'    AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 6 WHERE m.name = 'Skill 管理'  AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 7 WHERE m.name = 'MCP 工具'    AND m.parent_id = p.id AND m.deleted = 0;

-- ---- 协作办公 ----
UPDATE sys_menu m JOIN sys_menu p ON p.name = '协作办公' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 1 WHERE m.name = '即时聊天'    AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '协作办公' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 2 WHERE m.name = '通讯录'      AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '协作办公' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 3 WHERE m.name = '系统通知'    AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '协作办公' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 4 WHERE m.name = '会议日历'    AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '协作办公' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 5 WHERE m.name = '云文档'      AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '协作办公' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 6 WHERE m.name = '智能表格'    AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '协作办公' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 7 WHERE m.name = '表格模板管理' AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '协作办公' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 8 WHERE m.name = '待办事项'    AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '协作办公' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 9 WHERE m.name = '项目管理'    AND m.parent_id = p.id AND m.deleted = 0;

-- ---- 人事管理 ----
UPDATE sys_menu SET sort = 1 WHERE name = '人事档案' AND deleted = 0;
UPDATE sys_menu SET sort = 2 WHERE name = '考勤打卡' AND deleted = 0;
UPDATE sys_menu m JOIN sys_menu p ON p.name = '人事管理' AND p.parent_id = 0 AND p.deleted = 0
SET m.sort = 3 WHERE m.name = '绩效考核' AND m.parent_id = p.id AND m.deleted = 0;
UPDATE sys_menu SET sort = 4 WHERE name = '360度评估' AND deleted = 0;

-- ---- 系统管理（新增子项排序） ----
UPDATE sys_menu SET sort = 6 WHERE name = '文件列表'   AND parent_id = 1 AND deleted = 0;
UPDATE sys_menu SET sort = 7 WHERE name = '文件配置'   AND parent_id = 1 AND deleted = 0;
UPDATE sys_menu SET sort = 8 WHERE name = '渠道推送'   AND parent_id = 1 AND deleted = 0;
UPDATE sys_menu SET sort = 9 WHERE name = '自动化规则' AND parent_id = 1 AND deleted = 0;

-- ---- 系统监控（AI巡检排序） ----
UPDATE sys_menu SET sort = 6 WHERE name = 'AI 巡检' AND deleted = 0;

-- ==========================================================
-- 第八步：软删除测试菜单
-- ==========================================================
UPDATE sys_menu SET deleted = 1 WHERE id IN (140, 141);

-- ==========================================================
-- 第九步：软删除已清空的旧目录
-- ==========================================================

-- 消息中心（子菜单已全部迁至协作办公）
UPDATE sys_menu SET deleted = 1 WHERE id = 134 AND deleted = 0;

-- 文件管理（子菜单已迁至系统管理和协作办公）
UPDATE sys_menu SET deleted = 1 WHERE id = 126 AND deleted = 0;

-- ==========================================================
-- 第十步：同步角色菜单权限
-- ==========================================================

-- 为拥有旧「消息中心」权限的角色分配「协作办公」目录
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, nd.id
FROM sys_role_menu rm
CROSS JOIN sys_menu nd
WHERE nd.name = '协作办公' AND nd.parent_id = 0 AND nd.deleted = 0
  AND rm.menu_id = 134
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu r2 WHERE r2.role_id = rm.role_id AND r2.menu_id = nd.id);

-- 为拥有旧「文件管理」权限的角色也分配「协作办公」
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, nd.id
FROM sys_role_menu rm
CROSS JOIN sys_menu nd
WHERE nd.name = '协作办公' AND nd.parent_id = 0 AND nd.deleted = 0
  AND rm.menu_id = 126
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu r2 WHERE r2.role_id = rm.role_id AND r2.menu_id = nd.id);

-- 协作办公子菜单 → 拥有目录权限的角色自动获得
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
JOIN sys_menu dir ON dir.name = '协作办公' AND dir.parent_id = 0 AND dir.deleted = 0
WHERE m.deleted = 0 AND m.type = 2 AND m.parent_id = dir.id
  AND rm.menu_id = dir.id
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu r2 WHERE r2.role_id = rm.role_id AND r2.menu_id = m.id);

-- 人事管理子菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
JOIN sys_menu dir ON dir.name = '人事管理' AND dir.parent_id = 0 AND dir.deleted = 0
WHERE m.deleted = 0 AND m.type = 2 AND m.parent_id = dir.id
  AND rm.menu_id = dir.id
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu r2 WHERE r2.role_id = rm.role_id AND r2.menu_id = m.id);

-- 房产业务子菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
JOIN sys_menu dir ON dir.name = '房产业务' AND dir.parent_id = 0 AND dir.deleted = 0
WHERE m.deleted = 0 AND m.type = 2 AND m.parent_id = dir.id
  AND rm.menu_id = dir.id
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu r2 WHERE r2.role_id = rm.role_id AND r2.menu_id = m.id);

-- 智能助手子菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
JOIN sys_menu dir ON dir.name = '智能助手' AND dir.parent_id = 0 AND dir.deleted = 0
WHERE m.deleted = 0 AND m.type = 2 AND m.parent_id = dir.id
  AND rm.menu_id = dir.id
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu r2 WHERE r2.role_id = rm.role_id AND r2.menu_id = m.id);

-- 系统管理新增子菜单（文件列表、文件配置、渠道推送、自动化规则）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.deleted = 0 AND m.type = 2
  AND m.name IN ('文件列表','文件配置','渠道推送','自动化规则')
  AND m.parent_id = 1
  AND rm.menu_id = 1
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu r2 WHERE r2.role_id = rm.role_id AND r2.menu_id = m.id);

-- 系统监控 AI巡检
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name = 'AI 巡检' AND m.deleted = 0
  AND rm.menu_id = 36
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu r2 WHERE r2.role_id = rm.role_id AND r2.menu_id = m.id);
