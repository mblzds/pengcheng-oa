-- ============================================================
-- V70: 把内置 user 角色 data_scope 从 3（本部门）改成 5（仅本人）
--
-- 背景：
--   sys_role.code='user' 是项目模板自带的"通用用户"角色，data_scope 一直是
--   3（本部门），与"普通员工只看自己"的直觉语义不一致。考勤模块新加的
--   AttendanceScopeHelper 已经按 data_scope=1 判定"全公司可见"；其它模块
--   （客户 / 销售）走 DataPermissionInterceptor 也按 data_scope 1/3/4/5 分档。
--   保留 data_scope=3 会让所有挂 user 角色的人在客户模块看到"本部门"数据，
--   与"普通员工"应有的最小权限不一致。
--
-- 受影响用户（截至迁移时）：
--   - 9 个测试/演示号（test / mars* / Mars / lisi / test01 / 等）
--   - 5 个业务号：dev_a / dev_b / dev_c / designer_a / designer_b
--   其余挂 user 的用户都额外挂了更宽的角色（admin / flow_gm / flow_hr /
--   flow_dept_mgr 等），data_scope 取并集后不变。
--
-- 对考勤模块：无行为变化（之前 data_scope=3 在考勤里就不生效，本来就走
--   "leader_id 探测 → 仅本人"分支）。
--
-- 对客户/销售模块：上述 5 个真实用户从"本部门所有客户"收紧成"仅本人客户"。
--   如某岗位仍需看本部门，HR 在后台另挂 sales / sales_manager 等专项角色。
--
-- 兼容性：
--   - 字段已存在，仅 UPDATE 一行，无 DDL
--   - 重复执行幂等（仍写成条件 UPDATE 防止外部已经改成 5 时被回退）
-- ============================================================

UPDATE sys_role
SET data_scope = 5
WHERE code = 'user'
  AND data_scope = 3;
