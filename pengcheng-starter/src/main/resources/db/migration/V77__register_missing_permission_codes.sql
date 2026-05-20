-- V77: 补齐 @SaCheckPermission 引用了但 sys_menu 从未注册的按钮权限码
--
-- 背景：Controller 上散落使用了 @SaCheckPermission("realty:alliance:add") 这种码，
--       但项目里只在 V29 等迁移脚本里注册了一部分 `:list` 父菜单，
--       add/edit/delete/toggle/audit 这类「按钮型权限」全部漏注册。
--       结果是：调用对应接口会被 Sa-Token 拦下返 403——包括 admin 自己
--       （admin 的 bypass 是 SELECT DISTINCT permission FROM sys_menu，
--        没注册的码 admin 也拿不到）。
--
-- 本次扫描全部 @SaCheckPermission 调用 + 全部已注册 permission，差集 36 条，
-- 一次性以 type=3（按钮）形式注册：
--   - 能匹配到 :list 父菜单的，parent_id = 父菜单 id
--   - 父不存在的（realty:receivable / system:approval-flow / system:automation
--     这 3 组的 list 也缺失），parent_id = 0（顶级，visible=1 仍可在权限分配 UI 看到）
--
-- 同时给 admin 角色 sys_role_menu 关联（admin 已通过 selectAllPermissions 自动
-- 拿到所有权限，关联只是为了管理界面里这些菜单上的勾被打上，提升可观测性）。
-- 业务侧其他角色（行政文员/总监/驻场/渠道）是否赋权，留给运营在管理界面手动配置。

-- 0. 强制 connection collation 与 sys_menu / sys_role / sys_role_menu 实际一致。
--    现网 sys_menu 等表的 TABLE_COLLATION 是 MySQL 8 默认 utf8mb4_0900_ai_ci
--    （由 baseline 前的 dump 建的，跟 V1 init.sql 里写的 utf8mb4_unicode_ci 不一致），
--    而 mysql JDBC 默认 connection collation 是 latin1_swedish_ci。三方混用 JOIN/=
--    必触发 1267 Illegal mix of collations。一开始就 SET NAMES 锁死 connection。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 1. 临时表存权限码 → 父权限码 → 中文显示名的映射
DROP TEMPORARY TABLE IF EXISTS pengcheng_v77_perms;
-- 列级 + 表级都显式声明 utf8mb4_0900_ai_ci，与 sys_menu 实际 TABLE_COLLATION 一致
CREATE TEMPORARY TABLE pengcheng_v77_perms (
    permission   VARCHAR(100) COLLATE utf8mb4_0900_ai_ci PRIMARY KEY,
    display_name VARCHAR(50)  COLLATE utf8mb4_0900_ai_ci NOT NULL,
    parent_perm  VARCHAR(100) COLLATE utf8mb4_0900_ai_ci NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

INSERT INTO pengcheng_v77_perms (permission, display_name, parent_perm) VALUES
    -- realty: 联盟商
    ('realty:alliance:add',         '新增联盟商',         'realty:alliance:list'),
    ('realty:alliance:edit',        '编辑联盟商',         'realty:alliance:list'),
    ('realty:alliance:toggle',      '启用/停用联盟商',    'realty:alliance:list'),
    -- realty: 佣金
    ('realty:commission:add',       '录入佣金',           'realty:commission:list'),
    ('realty:commission:audit',     '审核佣金',           'realty:commission:list'),
    -- realty: 客户
    ('realty:customer:add',         '新建客户',           'realty:customer:list'),
    ('realty:customer:deal',        '录入成交',           'realty:customer:list'),
    ('realty:customer:poolConfig',  '客户池规则配置',     'realty:customer:list'),
    ('realty:customer:visit',       '录入到访',           'realty:customer:list'),
    -- realty: 付款
    ('realty:payment:add',          '发起付款申请',       'realty:payment:list'),
    ('realty:payment:approve',      '审批付款',           'realty:payment:list'),
    -- realty: 项目
    ('realty:project:add',          '新增项目',           'realty:project:list'),
    ('realty:project:edit',         '编辑项目',           'realty:project:list'),
    ('realty:project:rule',         '配置项目佣金规则',   'realty:project:list'),
    ('realty:project:ruleApprove',  '审批佣金规则',       'realty:project:list'),
    -- realty: 回款（list 父也缺，先建顶级页面权限再挂按钮）
    ('realty:receivable:list',      '回款管理',           NULL),
    ('realty:receivable:add',       '录入回款',           'realty:receivable:list'),
    ('realty:receivable:check',     '核销回款',           'realty:receivable:list'),
    ('realty:receivable:record',    '回款流水',           'realty:receivable:list'),
    -- hr: 员工档案
    ('hr:employee:edit',            '编辑员工档案',       'hr:manage:list'),
    ('hr:employee:change',          '员工人事异动',       'hr:manage:list'),
    -- hr: 绩效
    ('hr:kpi:period',               '考核周期管理',       'hr:performance:list'),
    ('hr:kpi:template',             'KPI 模板管理',       'hr:performance:list'),
    ('hr:kpi:score',                'KPI 打分',           'hr:performance:list'),
    -- hr: 360 评估
    ('hr:review360:config',         '360 评估配置',       'hr:review360:list'),
    ('hr:review360:task',           '360 评估任务管理',   'hr:review360:list'),
    ('hr:review360:submit',         '提交 360 评分',      'hr:review360:list'),
    -- ai: 注意现有父是 ai:skills:list（复数），controller 用 ai:skill:toggle（单数），不修改 controller，挂到现有复数父下
    ('ai:skill:toggle',             'AI 技能启停',        'ai:skills:list'),
    -- monitor: 在线用户
    ('monitor:online:forceLogout',  '强制下线在线用户',   'monitor:online:list'),
    -- system: 审批流配置（list 自身也缺）
    ('system:approval-flow:list',   '审批流配置',         NULL),
    ('system:approval-flow:edit',   '编辑审批流',         'system:approval-flow:list'),
    -- system: 自动化规则（list 自身也缺）
    ('system:automation:list',      '自动化规则',         NULL),
    ('system:automation:add',       '新增自动化规则',     'system:automation:list'),
    ('system:automation:edit',      '编辑自动化规则',     'system:automation:list'),
    ('system:automation:delete',    '删除自动化规则',     'system:automation:list'),
    ('system:automation:execute',   '手动执行自动化规则', 'system:automation:list');

-- 2. 批量 INSERT 缺失的菜单
--    type=3 按钮（避免侵入侧边栏，但仍出现在角色权限分配 UI）
--    parent_id：能找到父 permission 就挂下面，找不到就挂顶级（0），等运营在 UI 里调整
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, create_time, update_time, deleted)
SELECT
    IFNULL(parent.id, 0)                              AS parent_id,
    p.display_name                                    AS name,
    3                                                 AS type,
    ''                                                AS path,
    ''                                                AS component,
    p.permission                                      AS permission,
    ''                                                AS icon,
    99                                                AS sort,
    1                                                 AS visible,
    1                                                 AS status,
    0                                                 AS is_frame,
    NOW()                                             AS create_time,
    NOW()                                             AS update_time,
    0                                                 AS deleted
FROM pengcheng_v77_perms p
LEFT JOIN sys_menu parent
  ON parent.permission = p.parent_perm AND parent.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu m
    WHERE m.permission = p.permission AND m.deleted = 0
);

-- 3. 给 admin 角色补 sys_role_menu 关联
--    admin 通过 selectAllPermissions 已经拿得到，这里关联只是让管理界面的"分配权限"勾选状态正确
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.code = 'admin' AND r.deleted = 0
  AND m.deleted = 0
  AND m.permission IN (SELECT permission FROM pengcheng_v77_perms)
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

-- 4. 同步给 id=1 的超级管理员（多数项目 admin 就是 id=1，这步是兜底；
--    如果上一步已经命中 r.code='admin' 会去重，幂等）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
WHERE m.deleted = 0
  AND m.permission IN (SELECT permission FROM pengcheng_v77_perms)
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = m.id
  );

DROP TEMPORARY TABLE IF EXISTS pengcheng_v77_perms;
