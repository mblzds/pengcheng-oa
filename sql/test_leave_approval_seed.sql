-- =============================================================================
-- 请假审批流程测试数据
-- 用途：为 leave 审批流水线（V45__approval_flow.sql）提供完整组织架构 + 4 节点审批配置
--
-- 组织结构：
--   朋诚科技 (id=1，已存在)
--   ├── 技术中心 (100)              负责人: 张总监 (104)
--   │   └── 后端组 (110)            负责人: 王经理 (108)
--   ├── 产品中心 (200)              负责人: 李总监 (105)
--   │   └── 设计组 (210)            负责人: 赵经理 (109)
--   └── 人力资源部 (300)            负责人: 钱总监 (106)
--
-- 审批流（leave）：
--   1. 直接上级           direct_supervisor
--   2. 部门负责人         role=52 (部门负责人)
--   3. HR 审批            role=51 (HR)
--   4. 总经理             user=101 (周总)
--
-- 测试账号密码统一为 123456（沿用 admin 的 BCrypt 哈希）
--
-- 幂等：本脚本可重复执行；会先按 ID 范围清理同范围测试数据再插入。
-- 范围：
--   sys_dept.id  IN (100,110,200,210,300)
--   sys_user.id  BETWEEN 101 AND 199
--   sys_role.id  IN (50,51,52)
--   approval_flow_node.business_type = 'leave'
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 0. 清理同范围旧数据（保证可重复执行）
-- -----------------------------------------------------------------------------
DELETE FROM `sys_user_role`  WHERE `user_id`  BETWEEN 101 AND 199;
DELETE FROM `sys_user_role`  WHERE `role_id`  IN (50, 51, 52);
DELETE FROM `sys_user`       WHERE `id`       BETWEEN 101 AND 199;
DELETE FROM `sys_role`       WHERE `id`       IN (50, 51, 52);
DELETE FROM `sys_dept`       WHERE `id`       IN (100, 110, 200, 210, 300);

DELETE FROM `approval_flow_node` WHERE `business_type` = 'leave';

-- -----------------------------------------------------------------------------
-- 1. 部门 sys_dept（leader_id 由 V45 引入，作为 direct_supervisor 的 fallback）
-- -----------------------------------------------------------------------------
INSERT INTO `sys_dept`
  (`id`, `parent_id`, `ancestors`, `dept_name`, `sort`, `leader`, `leader_id`, `status`, `create_time`, `update_time`, `deleted`)
VALUES
  (100, 1,   '0,1',         '技术中心',     10, '张总监', 104, 1, NOW(), NOW(), 0),
  (110, 100, '0,1,100',     '后端组',       11, '王经理', 108, 1, NOW(), NOW(), 0),
  (200, 1,   '0,1',         '产品中心',     20, '李总监', 105, 1, NOW(), NOW(), 0),
  (210, 200, '0,1,200',     '设计组',       21, '赵经理', 109, 1, NOW(), NOW(), 0),
  (300, 1,   '0,1',         '人力资源部',   30, '钱总监', 106, 1, NOW(), NOW(), 0);

-- 顶层"朋诚科技"的负责人补成总经理周总（id=101），保证总经理自身或挂在公司根部门下的孤立用户
-- 仍能解析出审批人（fallback 到 dept.leader_id）。
UPDATE `sys_dept` SET `leader_id` = 101 WHERE `id` = 1;

-- -----------------------------------------------------------------------------
-- 2. 角色 sys_role（新增 3 个流程相关角色）
-- -----------------------------------------------------------------------------
INSERT INTO `sys_role`
  (`id`, `name`, `code`, `sort`, `status`, `remark`, `create_time`, `update_time`, `deleted`, `data_scope`)
VALUES
  -- code 加 flow_ 前缀避免与既有 role.code 碰撞（utf8mb4_0900_ai_ci 大小写不敏感，'hr' 会撞既有 'HR'）
  (50, '总经理(审批流)',     'flow_gm',       50, 1, '请假审批：终审节点',              NOW(), NOW(), 0, 1),
  (51, 'HR(审批流)',         'flow_hr',       51, 1, '请假审批：HR 审批节点候选人',     NOW(), NOW(), 0, 1),
  (52, '部门负责人(审批流)', 'flow_dept_mgr', 52, 1, '请假审批：部门负责人节点候选人', NOW(), NOW(), 0, 4);

-- -----------------------------------------------------------------------------
-- 3. 用户 sys_user（密码=123456；leader_id 由 V45 引入，是 direct_supervisor 第一优先级）
--    user_type=admin 让他们能直接登录 PC 后台测试
-- -----------------------------------------------------------------------------
-- 公共 BCrypt 哈希，明文 123456（$2a$10 cost；Hutool BCrypt 兼容）
SET @PWD = '$2a$10$XjJusUMuIVxyae6.TjvSSe71x/5QP0ME9PILjAbx2XRofH21eD4TK';

-- 手机号说明：均为 11 位，符合 1[3-9]\d{9} 国内运营商号段；按角色分配不同运营商前缀，
-- 后 4 位与用户 ID 对齐方便排查（例：138 0013 0101 → 用户 101）
--   138/137/136/135 中国移动；188/187 中国移动；158/159 中国移动
--   155/156/186/185 中国联通；199/189 中国电信
INSERT INTO `sys_user`
  (`id`, `dept_id`, `username`,    `password`, `nickname`,   `email`,                `phone`,        `gender`, `status`, `is_quit`, `leader_id`, `user_type`, `create_time`, `update_time`, `deleted`)
VALUES
  -- 高管层（公司根部门）
  (101, 1,   'gm_zhou',    @PWD, '周总(总经理)',    'gm@example.com',         '13800130101', 1, 1, 0, NULL, 'admin', NOW(), NOW(), 0),

  -- 技术中心 (100) -- 张总监直接汇报给周总
  (104, 100, 'cto_zhang',  @PWD, '张总监(技术总监)', 'cto@example.com',        '13700130104', 1, 1, 0, 101,  'admin', NOW(), NOW(), 0),

  -- 后端组 (110) -- 王经理→张总监
  (108, 110, 'mgr_wang',   @PWD, '王经理(后端经理)', 'be.mgr@example.com',     '18800130108', 1, 1, 0, 104,  'admin', NOW(), NOW(), 0),
  (111, 110, 'dev_a',      @PWD, '陈一(开发A)',     'dev.a@example.com',      '15800130111', 1, 1, 0, 108,  'admin', NOW(), NOW(), 0),
  (112, 110, 'dev_b',      @PWD, '林二(开发B)',     'dev.b@example.com',      '15600130112', 2, 1, 0, 108,  'admin', NOW(), NOW(), 0),
  -- 故意不设 leader_id：测试 fallback 到 dept.leader_id (110 部门负责人 = 108)
  (113, 110, 'dev_c',      @PWD, '黄三(开发C)',     'dev.c@example.com',      '15500130113', 1, 1, 0, NULL, 'admin', NOW(), NOW(), 0),

  -- 产品中心 (200) -- 李总监→周总
  (105, 200, 'pdir_li',    @PWD, '李总监(产品总监)', 'pd@example.com',         '13600130105', 2, 1, 0, 101,  'admin', NOW(), NOW(), 0),

  -- 设计组 (210) -- 赵经理→李总监
  (109, 210, 'mgr_zhao',   @PWD, '赵经理(设计经理)', 'design.mgr@example.com', '18700130109', 2, 1, 0, 105,  'admin', NOW(), NOW(), 0),
  (114, 210, 'designer_a', @PWD, '吴四(设计A)',     'design.a@example.com',   '18600130114', 2, 1, 0, 109,  'admin', NOW(), NOW(), 0),
  (115, 210, 'designer_b', @PWD, '郑五(设计B)',     'design.b@example.com',   '18500130115', 1, 1, 0, 109,  'admin', NOW(), NOW(), 0),

  -- HR 部 (300) -- 钱总监→周总
  (106, 300, 'hr_dir_qian',@PWD, '钱总监(HR总监)',   'hr.dir@example.com',     '13500130106', 2, 1, 0, 101,  'admin', NOW(), NOW(), 0),
  (116, 300, 'hr_a',       @PWD, '孙六(HR专员A)',   'hr.a@example.com',       '19900130116', 2, 1, 0, 106,  'admin', NOW(), NOW(), 0),
  (117, 300, 'hr_b',       @PWD, '周七(HR专员B)',   'hr.b@example.com',       '18900130117', 2, 1, 0, 106,  'admin', NOW(), NOW(), 0);

-- -----------------------------------------------------------------------------
-- 4. 用户-角色绑定 sys_user_role
--    每人都赋予普通用户 role=2，再叠加流程相关角色
-- -----------------------------------------------------------------------------
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, 2 FROM `sys_user` u WHERE u.id BETWEEN 101 AND 199;

-- 总经理 role=50：周总
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
  (101, 50),
  (101, 1);   -- 顺带给周总 admin 角色，方便管理后台调试

-- HR role=51：HR 总监 + 2 个 HR 专员（候选人池有 3 人，可测多候选选最早领取）
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
  (106, 51),
  (116, 51),
  (117, 51);

-- 部门负责人 role=52：技术总监、产品总监、HR总监、后端经理、设计经理
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
  (104, 52),
  (105, 52),
  (106, 52),
  (108, 52),
  (109, 52);

-- -----------------------------------------------------------------------------
-- 5. 审批流模板 approval_flow_node（leave 业务，4 节点）
--    seq=1 直接上级       direct_supervisor
--    seq=2 部门负责人     role=52
--    seq=3 HR 审批        role=51
--    seq=4 总经理         user=101
-- -----------------------------------------------------------------------------
INSERT INTO `approval_flow_node`
  (`business_type`, `seq`, `node_name`, `approver_type`,    `approver_value`, `enabled`, `create_time`, `update_time`, `deleted`)
VALUES
  ('leave', 1, '直接上级',     'direct_supervisor', NULL,  1, NOW(), NOW(), 0),
  ('leave', 2, '部门负责人',   'role',              '52',  1, NOW(), NOW(), 0),
  ('leave', 3, 'HR审批',       'role',              '51',  1, NOW(), NOW(), 0),
  ('leave', 4, '总经理',       'user',              '101', 1, NOW(), NOW(), 0);

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 测试场景速查
-- =============================================================================
-- 推荐用例 A：基层员工链路最完整
--   申请人 dev_a (111) → 直接上级 mgr_wang (108) → 部门负责人候选(104,105,106,108,109)
--   → HR 候选(106,116,117) → 总经理 gm_zhou (101)
--
-- 推荐用例 B：测试 leader_id 缺失时回退到 dept.leader_id
--   申请人 dev_c (113) leader_id=NULL → 部门 110 的 leader_id=108 → mgr_wang 接单
--
-- 推荐用例 C：跨部门链路（设计组）
--   申请人 designer_a (114) → mgr_zhao (109) → 部门负责人(同 A) → HR → 总经理
--
-- 推荐用例 D：中层管理者请假
--   申请人 mgr_wang (108) → 直接上级 cto_zhang (104) → 部门负责人 → HR → 总经理
--   注意：seq=2 的候选里包含 108 自己（dept_manager 角色），系统当前不阻止"候选包含自身"，
--   但 seq=1 已被 104 处理，到 seq=2 时 108 出现在候选里属于正常配置；如不希望中层自己审自己，
--   测试时让 104 或其他 dept_manager 接单即可。
--
-- 推荐用例 E：触发"无可用审批人"异常
--   把 sys_user.id=111 的 leader_id 临时改 NULL，并把 dept.id=110 的 leader_id 改 NULL，
--   提交请假应得到："节点【直接上级】无可用审批人"。
--
-- 验证 SQL：
--   SELECT * FROM approval_flow_node WHERE business_type='leave' ORDER BY seq;
--   SELECT id, username, nickname, dept_id, leader_id FROM sys_user WHERE id BETWEEN 101 AND 199;
--   SELECT id, parent_id, dept_name, leader_id FROM sys_dept WHERE id IN (1,100,110,200,210,300);
--   SELECT ur.user_id, u.nickname, ur.role_id, r.name
--     FROM sys_user_role ur JOIN sys_user u ON u.id=ur.user_id JOIN sys_role r ON r.id=ur.role_id
--     WHERE ur.role_id IN (50,51,52) ORDER BY ur.role_id, ur.user_id;
--
-- 提交一条测试请假（dev_a 请病假 1 天）：
--   INSERT INTO leave_request (user_id, leave_type, start_time, end_time, reason, status, create_by, create_time, update_time, deleted)
--   VALUES (111, 2, '2026-05-09 09:00:00', '2026-05-09 18:00:00', '感冒发烧', 1, 111, NOW(), NOW(), 0);
--   注意：直接 INSERT 不会触发 ApprovalFlowService.start()，approval_record_node 不会自动生成。
--   正常测试请走 App / 后台的"提交请假"接口，由代码调用 start() 创建流转记录。
-- =============================================================================
