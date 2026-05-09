-- 密码强度配置：去掉「必须包含特殊字符」的限制，保留长度 6-20 与大写/小写/数字要求
UPDATE `sys_config_group`
SET `config_value` = '{"minLength":6,"maxLength":20,"requireUppercase":true,"requireLowercase":true,"requireNumber":true,"requireSpecial":false,"expireDays":0}',
    `update_time` = NOW()
WHERE `group_code` = 'password';
