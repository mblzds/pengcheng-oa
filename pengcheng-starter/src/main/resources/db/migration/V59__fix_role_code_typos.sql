-- 修复 sys_role 中的 code 拼写/大小写问题，使前端 userRole 计算能正确命中
--
-- 现状：
--   role 4  code='manger'  (typo, 缺 a) → 前端 ['manager','director'] 永远不命中
--   role 3  code='HR'      (大写)        → 前端 ['hr','attendance'] 永远不命中
--
-- 影响面：
--   - 部门经理 / HR 在工作台 dashboard 看不到"待我审批"区块（fallback 到 /todo/list 通用待办，0 项）
--   - 工作台快捷入口分组也走错（managerApps/hrApps 从未被命中）
--
-- 全代码库 grep 'manger' 仅出现在 V57 注释；'HR' 仅在 AttendanceScopeHelper 白名单
-- 双写防御 ("HR","hr") 已存在，无业务依赖，重命名安全。

UPDATE `sys_role`
SET code = 'manager', update_time = NOW()
WHERE id = 4 AND code = 'manger' AND deleted = 0;

UPDATE `sys_role`
SET code = 'hr', update_time = NOW()
WHERE id = 3 AND code = 'HR' AND deleted = 0;
