-- 密码强度配置：长度 8-20 位，字母（大小写均可）+ 数字即可，不再强制大小写
UPDATE `sys_config_group`
SET `config_value` = '{"minLength":8,"maxLength":20,"requireLetter":true,"requireUppercase":false,"requireLowercase":false,"requireNumber":true,"requireSpecial":false,"expireDays":0}',
    `update_time` = NOW()
WHERE `group_code` = 'password';
