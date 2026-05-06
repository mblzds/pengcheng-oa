INSERT INTO sys_config_group (
    group_code,
    group_name,
    group_icon,
    config_value,
    sort,
    status,
    remark,
    create_time,
    update_time
)
SELECT
    'aiExperiment',
    'AI实验配置',
    NULL,
    '{}',
    16,
    1,
    'AI路由与提示词实验配置',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config_group WHERE group_code = 'aiExperiment'
);
