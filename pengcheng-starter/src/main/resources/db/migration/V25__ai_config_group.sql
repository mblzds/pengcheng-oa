-- ==============================================
-- V25: AI 模型与功能配置分组
-- 供「模型与技能」页面读写模型开关、功能开关等
-- ==============================================

INSERT INTO sys_config_group (group_code, group_name, group_icon, config_value, sort, status, remark, create_time, update_time)
SELECT 'aiConfig', 'AI配置', NULL, '{}', 16, 1, 'AI模型启用/参数、功能开关（模型与技能页）', NOW(), NOW()
FROM (SELECT 1) AS _one
WHERE NOT EXISTS (SELECT 1 FROM sys_config_group WHERE group_code = 'aiConfig');
