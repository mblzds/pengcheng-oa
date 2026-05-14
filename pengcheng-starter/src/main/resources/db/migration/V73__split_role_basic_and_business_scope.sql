-- V73：分离基础职级范围 / 业务模块加成
-- 背景：V72 把"职能 + 全员可见"塞进 sys_role.data_scope 一个字段，导致挂了
--       finance 角色的人在所有模块都看全员（包括考勤）。同时这种设计让财务
--       员工"看自己考勤"和"看全公司报销"无法兼得。
--
-- 新模型（参见 doc/PERMISSION-AND-ROLES.md）：
--   sys_role.data_scope = 基础可见范围（按职级：员工 / 部门经理 / 高管）
--   业务加成 = sys_config_group(<module>).fullScopeRoleCodes
--     attendance.fullScopeRoleCodes = "hr"       → hr 角色在考勤升级为全员
--     payment.fullScopeRoleCodes = "finance"     → finance 角色在付款升级为全员
--     commission.fullScopeRoleCodes = "finance"  → 同上
--   admin / chairman / general_manager 通过 data_scope=1 自动覆盖全员，无需列入加成
--
-- 本迁移做两件事：
--   1. finance / hr 的 data_scope: 1 → 5（仅本人）；它们不再决定基础可见
--   2. 现有挂 finance / hr 的用户**追加挂 employee**（不删 finance / hr），
--      避免 data_scope 突然降为 5 后丢失基础访问能力（自己打卡、看自己请假等）
--      若该用户原本同时是部门经理，HR 后台需另行追加挂 dept_manager（本迁移不
--      自动判定，避免误升权）。

-- 1. finance.data_scope: 1 → 5
UPDATE sys_role SET data_scope = 5
WHERE code = 'finance' AND deleted = 0;

-- 2. hr.data_scope: 1 → 5
UPDATE sys_role SET data_scope = 5
WHERE code = 'hr' AND deleted = 0;

-- 3. 给挂 finance 的用户追加挂 employee（已挂的跳过）
INSERT INTO sys_user_role (user_id, role_id)
SELECT ur.user_id, r_emp.id
FROM sys_user_role ur
JOIN sys_role r_src ON r_src.id = ur.role_id AND r_src.code = 'finance' AND r_src.deleted = 0
JOIN sys_role r_emp ON r_emp.code = 'employee' AND r_emp.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_role ur2
    WHERE ur2.user_id = ur.user_id AND ur2.role_id = r_emp.id
);

-- 4. 给挂 hr 的用户追加挂 employee（已挂的跳过）
INSERT INTO sys_user_role (user_id, role_id)
SELECT ur.user_id, r_emp.id
FROM sys_user_role ur
JOIN sys_role r_src ON r_src.id = ur.role_id AND r_src.code = 'hr' AND r_src.deleted = 0
JOIN sys_role r_emp ON r_emp.code = 'employee' AND r_emp.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_role ur2
    WHERE ur2.user_id = ur.user_id AND ur2.role_id = r_emp.id
);
