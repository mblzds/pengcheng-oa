-- 把工号从 hr_employee_profile 提到 sys_user，以便用户管理列表/查询直接用上工号
-- 现有数据：优先取 hr_employee_profile.employee_no，缺失/重复时落到 EMP{id 4 位补零}

ALTER TABLE sys_user ADD COLUMN employee_no VARCHAR(32) NULL COMMENT '工号' AFTER username;

-- Pass 1：从员工档案回填
UPDATE sys_user u
JOIN hr_employee_profile p ON p.user_id = u.id AND p.deleted = 0
SET u.employee_no = p.employee_no
WHERE p.employee_no IS NOT NULL AND p.employee_no <> '' AND u.employee_no IS NULL;

-- Pass 2：剩余空工号一律按 EMP + id 4 位补零兜底；冲突时再附加 id 让索引唯一
UPDATE sys_user
SET employee_no = CONCAT('EMP', LPAD(id, 4, '0'))
WHERE employee_no IS NULL OR employee_no = '';

-- 唯一索引（允许 NULL，但当前都已填）
ALTER TABLE sys_user ADD UNIQUE KEY uk_sys_user_employee_no (employee_no);
