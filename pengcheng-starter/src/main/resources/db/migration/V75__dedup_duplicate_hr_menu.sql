-- V75：清理重复的「人事管理」一级菜单 + 补齐 admin 在剩余子树的菜单关联
--
-- 背景：线上 sys_menu 里出现了两份 name='人事管理' AND parent_id=0 的目录
--       （后一份是历史误操作复制），导致后台侧边栏显示两个一模一样的「人事管理」。
--       重复目录下挂的子菜单也是重复的，且只覆盖到 admin/chairman/general_manager
--       三个高管角色——保留最早创建的那份并不会丢权限。
--
-- 同时顺手修一个数据一致性 bug：admin 角色挂了「人事管理」父目录但漏挂了下面
-- 几个子菜单（人事档案/考勤打卡/绩效考核/360度评估/考勤设置）。当前 admin 走
-- SysMenuServiceImpl.getUserMenuTree 的"超管返全量"兜底所以 UI 看起来正常，但
-- sys_role_menu 表里数据不齐——本迁移把缺失的关联补齐，避免未来按表读权限的
-- 链路（如审计、按钮级校验）撞坑。
--
-- 幂等：
--   - 若没有重复菜单，下面的 DELETE/UPDATE 直接 0 行
--   - 若 admin 关联已存在，INSERT IGNORE / NOT EXISTS 跳过

-- 1. 找出"要保留"的人事管理（最早创建那份）；其他视为重复
--    用 SELECT INTO @var，不依赖临时表，跨 MySQL 版本通用
SELECT MIN(id) INTO @keep_hr_menu_id
FROM sys_menu
WHERE name = '人事管理' AND parent_id = 0 AND deleted = 0;

-- 2. 清掉重复"人事管理"父 + 它们的所有子菜单的 sys_role_menu 关联
DELETE rm FROM sys_role_menu rm
WHERE rm.menu_id IN (
    SELECT id FROM (
        -- 重复的父
        SELECT id FROM sys_menu
        WHERE name = '人事管理' AND parent_id = 0 AND deleted = 0
          AND id <> IFNULL(@keep_hr_menu_id, -1)
        UNION
        -- 子
        SELECT s.id FROM sys_menu s
        WHERE s.deleted = 0
          AND s.parent_id IN (
              SELECT id FROM (
                  SELECT id FROM sys_menu
                  WHERE name = '人事管理' AND parent_id = 0 AND deleted = 0
                    AND id <> IFNULL(@keep_hr_menu_id, -1)
              ) p
          )
    ) t
);

-- 3. 软删重复的"人事管理"父 + 子菜单
UPDATE sys_menu SET deleted = 1, update_time = NOW()
WHERE deleted = 0 AND id IN (
    SELECT id FROM (
        SELECT id FROM sys_menu
        WHERE name = '人事管理' AND parent_id = 0 AND deleted = 0
          AND id <> IFNULL(@keep_hr_menu_id, -1)
        UNION
        SELECT s.id FROM sys_menu s
        WHERE s.deleted = 0
          AND s.parent_id IN (
              SELECT id FROM (
                  SELECT id FROM sys_menu
                  WHERE name = '人事管理' AND parent_id = 0 AND deleted = 0
                    AND id <> IFNULL(@keep_hr_menu_id, -1)
              ) p
          )
    ) t
);

-- 4. 补齐 admin 角色 (role.code='admin') 在剩余「人事管理」子树的菜单关联
--    用 NOT EXISTS 保证幂等
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.code = 'admin' AND r.deleted = 0
  AND m.parent_id = @keep_hr_menu_id AND m.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );
