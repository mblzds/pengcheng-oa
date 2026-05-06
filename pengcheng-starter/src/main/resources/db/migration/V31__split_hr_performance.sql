-- ==============================================
-- V31: 人事与绩效模块拆分 (MySQL 兼容版本)
-- 将「人事与绩效」拆分为「人事管理」和「绩效考核」两个独立模块
-- ==============================================

-- 1. 更新「人事与绩效」菜单名称为「人事管理」
UPDATE sys_menu SET name = '人事管理', permission = 'hr:manage:list', icon = 'PersonOutline'
WHERE name = '人事与绩效' AND deleted = 0;

-- 2. 在「考勤管理」目录下新增「绩效考核」子菜单
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '绩效考核', 2, '/hr/performance', 'hr/performance/index', 'hr:performance:list', 'StatsChartOutline', 3, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '考勤管理' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '绩效考核' AND deleted = 0 AND parent_id = p.id);

-- 3. 为已拥有「人事与绩效」的角色分配「绩效考核」菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name = '绩效考核' AND m.deleted = 0
  AND rm.menu_id = (SELECT id FROM (SELECT id FROM sys_menu WHERE name = '人事管理' AND deleted = 0 LIMIT 1) AS tmp)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- 4. 更新前端路由中的菜单名称（此步骤需手动修改 router/index.ts）
-- 将 /hr 路由的 meta.title 从「人事与绩效」改为「人事管理」
-- 新增 /hr/performance 路由，meta.title 为「绩效考核」
