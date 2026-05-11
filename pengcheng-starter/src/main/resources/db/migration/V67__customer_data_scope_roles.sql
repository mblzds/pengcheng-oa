-- ============================================================
-- V67: 客户数据权限改造 —— 历史 creator_id 回填 + 销售角色三档
--
-- 背景：
--   customer 表已有 creator_id 字段（"录入驻场人员ID"），但 CustomerService.
--   createCustomer 历史上未填值；同时 DataPermissionInterceptor 房产分支不
--   识别 sys_role.data_scope，导致销售员看不到自己报的客户。
--
-- 配套代码改动：
--   - CustomerService.createCustomer 提交时写入 creator_id（StpUtil 登录用户）
--   - RealtyCustomerMapper @DataScope 增加 userAlias = "creator_id"
--   - DataPermissionInterceptor.buildRealtyDataScopeFilter 叠加 data_scope
--     通用规则（1/2/3/4/5）
--
-- 本脚本：
--   1. 历史 customer.creator_id 用 create_by 兜底回填
--   2. 新建三档销售职能角色：sales / sales_manager / general_manager
--      对应 data_scope = 5（仅本人）/ 4（本部门及以下）/ 1（全部）
--
-- 角色挂载：脚本不自动给员工挂角色，由管理员通过后台「用户管理 →
--   分配角色」按需操作。销售部所有人挂 sales；销售经理叠加 sales_manager；
--   销售总监 / 总经理挂 general_manager。
--
-- 兼容性：
--   - 与现有 resident / channel / alliance_manager 房产角色叠加生效（OR 关系）
--   - 不修改任何现有表结构，仅追加角色数据
--   - 重复执行幂等（WHERE NOT EXISTS 保护）
-- ============================================================

-- 1. 历史数据回填：把 NULL 的 creator_id 用 create_by 兜底
UPDATE customer
SET creator_id = create_by
WHERE creator_id IS NULL
  AND create_by IS NOT NULL
  AND deleted = 0;

-- 2. 新建「销售员」角色 (data_scope = 5 仅本人)
INSERT INTO sys_role (name, code, sort, status, remark, data_scope, create_time, update_time)
SELECT '销售员', 'sales', 10, 1, '仅可查看本人报备的客户', 5, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'sales');

-- 3. 新建「销售经理」角色 (data_scope = 4 本部门及以下)
INSERT INTO sys_role (name, code, sort, status, remark, data_scope, create_time, update_time)
SELECT '销售经理', 'sales_manager', 11, 1, '可查看本部门及下级部门员工报备的客户', 4, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'sales_manager');

-- 4. 新建「总经理」角色 (data_scope = 1 全部)
INSERT INTO sys_role (name, code, sort, status, remark, data_scope, create_time, update_time)
SELECT '总经理', 'general_manager', 12, 1, '可查看全部客户数据', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'general_manager');
