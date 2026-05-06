-- V30: 修复导航栏菜单重复问题
-- 问题：前端路由和后端菜单都定义了相同的菜单项，导致重复显示
-- 解决方案：删除后端菜单中与前端路由重复的菜单项，保留前端路由定义

-- ========================================
-- 一、删除智能助手目录下与前端路由重复的菜单
-- ========================================

-- 删除"AI 助手"（前端路由已定义 /ai/chat）
UPDATE sys_menu SET deleted = 1 
WHERE name = 'AI 助手' AND parent_id = (SELECT id FROM sys_menu WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"知识库管理"（前端路由已定义 /ai/knowledge，名称为 AI 知识库管理）
UPDATE sys_menu SET deleted = 1 
WHERE name = '知识库管理' AND parent_id = (SELECT id FROM sys_menu WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"AI 实验"（前端路由已定义 /ai/experiment，名称为 AI 实验治理）
UPDATE sys_menu SET deleted = 1 
WHERE name = 'AI 实验' AND parent_id = (SELECT id FROM sys_menu WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"模型配置"（前端路由已定义 /ai/config，名称为模型与技能）
UPDATE sys_menu SET deleted = 1 
WHERE name = '模型配置' AND parent_id = (SELECT id FROM sys_menu WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"AI 记忆"（前端路由已定义 /ai/memory，名称为 AI 记忆管理）
UPDATE sys_menu SET deleted = 1 
WHERE name = 'AI 记忆' AND parent_id = (SELECT id FROM sys_menu WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"Skill 管理"（前端路由已定义 /ai/skills）
UPDATE sys_menu SET deleted = 1 
WHERE name = 'Skill 管理' AND parent_id = (SELECT id FROM sys_menu WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"MCP 工具"（前端路由已定义 /ai/mcp，名称为 MCP 工具管理）
UPDATE sys_menu SET deleted = 1 
WHERE name = 'MCP 工具' AND parent_id = (SELECT id FROM sys_menu WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- ========================================
-- 二、删除其他与前端路由重复的菜单
-- ========================================

-- 删除"考勤打卡"（前端路由已定义 /realty/attendance，名称为考勤管理）
UPDATE sys_menu SET deleted = 1 
WHERE name = '考勤打卡' AND parent_id = (SELECT id FROM sys_menu WHERE name = '考勤管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"人事与绩效"（前端路由已定义 /hr）
UPDATE sys_menu SET deleted = 1 
WHERE name = '人事与绩效' AND parent_id = (SELECT id FROM sys_menu WHERE name = '考勤管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"云文档"（前端路由已定义 /doc）
UPDATE sys_menu SET deleted = 1 
WHERE name = '云文档' AND parent_id = (SELECT id FROM sys_menu WHERE name = '文件管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"会议日程"（前端路由已定义 /meeting）
UPDATE sys_menu SET deleted = 1 
WHERE name = '会议日程' AND parent_id = (SELECT id FROM sys_menu WHERE name = '消息中心' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"渠道推送"（前端路由已定义 /system/channel）
UPDATE sys_menu SET deleted = 1 
WHERE name = '渠道推送' AND parent_id = (SELECT id FROM sys_menu WHERE name = '系统管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"自动化规则"（前端路由已定义 /system/automation）
UPDATE sys_menu SET deleted = 1 
WHERE name = '自动化规则' AND parent_id = (SELECT id FROM sys_menu WHERE name = '系统管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"AI 巡检"（前端路由已定义 /monitor/heartbeat）
UPDATE sys_menu SET deleted = 1 
WHERE name = 'AI 巡检' AND parent_id = (SELECT id FROM sys_menu WHERE name = '系统监控' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"项目管理"（前端路由已定义 /project，在组织管理下）
UPDATE sys_menu SET deleted = 1 
WHERE name = '项目管理' AND parent_id = (SELECT id FROM sys_menu WHERE name = '组织管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"待办事项"（前端路由已定义 /todo）
UPDATE sys_menu SET deleted = 1 
WHERE name = '待办事项' AND parent_id = (SELECT id FROM sys_menu WHERE name = '组织管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"销售日历"（前端路由已定义 /realty/calendar，已在房产业务下）
UPDATE sys_menu SET deleted = 1 
WHERE name = '销售日历' AND parent_id = (SELECT id FROM sys_menu WHERE name = '房产业务' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"拜访记录"（前端路由已定义 /realty/visit，已在房产业务下）
UPDATE sys_menu SET deleted = 1 
WHERE name = '拜访记录' AND parent_id = (SELECT id FROM sys_menu WHERE name = '房产业务' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"AI 日报"（前端路由已定义 /realty/report，已在房产业务下）
UPDATE sys_menu SET deleted = 1 
WHERE name = 'AI 日报' AND parent_id = (SELECT id FROM sys_menu WHERE name = '房产业务' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"销售质检"（前端路由已定义 /realty/quality，已在房产业务下）
UPDATE sys_menu SET deleted = 1 
WHERE name = '销售质检' AND parent_id = (SELECT id FROM sys_menu WHERE name = '房产业务' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"场景模板"（前端路由已定义 /realty/templates，已在房产业务下）
UPDATE sys_menu SET deleted = 1 
WHERE name = '场景模板' AND parent_id = (SELECT id FROM sys_menu WHERE name = '房产业务' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"表格模板管理"（前端路由已定义 /smart-table/template-mgmt）
UPDATE sys_menu SET deleted = 1 
WHERE name = '表格模板管理' AND parent_id = (SELECT id FROM sys_menu WHERE name = '文件管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"通讯录"（前端路由已定义 /contacts）
UPDATE sys_menu SET deleted = 1 
WHERE name = '通讯录' AND parent_id = (SELECT id FROM sys_menu WHERE name = '消息中心' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- 删除"智能表格"（前端路由已定义 /smart-table）
UPDATE sys_menu SET deleted = 1 
WHERE name = '智能表格' AND parent_id = (SELECT id FROM sys_menu WHERE name = '文件管理' AND parent_id = 0 AND deleted = 0 LIMIT 1) AND deleted = 0;

-- ========================================
-- 三、清理关联的角色菜单权限
-- ========================================

-- 清理已删除菜单的角色权限关联
DELETE FROM sys_role_menu 
WHERE menu_id IN (SELECT id FROM sys_menu WHERE deleted = 1);

-- ========================================
-- 四、更新前端路由中的菜单名称以保持一致
-- ========================================

-- 注意：前端路由文件 (router/index.ts) 中的菜单名称需要手动更新以匹配数据库
-- 已在 V29 中定义的菜单名称为准，前端路由应该使用相同的名称
