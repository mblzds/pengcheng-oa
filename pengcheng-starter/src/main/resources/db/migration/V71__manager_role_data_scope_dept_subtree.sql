-- ============================================================
-- V71: manager 角色 data_scope 从 1（全部）改成 4（本部门及以下）
--
-- 背景：
--   sys_role.code='manager' 是项目模板自带的"通用经理"角色。data_scope 一直
--   是 1（全部），但实际挂这个角色的两个用户（张经理-技术部 / 林经理-销售部）
--   都是部门经理，按数据隔离直觉应当只看本部门及下级。
--
--   AttendanceScopeHelper 此版起识别完整 data_scope（1/3/4/5），data_scope=4
--   会展开为"用户 deptId + 所有后代部门员工"，与 V67 sales_manager 同档位。
--
-- 受影响用户：
--   - uid=30 技术部门张经理（dept=302 技术部）
--   - uid=122 销售部林经理（dept=304 销售部）
--   都是销售管理/技术管理类岗位，data_scope=4 比 1 更符合实际职权。
--
-- 兼容性：
--   - 仅 UPDATE 一行，无 DDL
--   - 重复执行幂等
-- ============================================================

UPDATE sys_role
SET data_scope = 4
WHERE code = 'manager'
  AND data_scope = 1;
