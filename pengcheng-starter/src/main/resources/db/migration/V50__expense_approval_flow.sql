-- 报销审批流配置：新增「财务(审批流)」角色 + business_type='expense' 默认节点链
-- 注意：本迁移仅落配置数据。当前 PaymentService 仍走自身硬编码审批，配置不生效；
-- 待后续将 PaymentService 切到 ApprovalFlowService 后，此配置自动接管。

-- 1. 新增审批流角色：财务（与 flow_gm/flow_hr/flow_dept_mgr 同套体系，沿用 id 53）
INSERT INTO `sys_role`
  (`id`, `name`, `code`, `sort`, `status`, `remark`, `create_time`, `update_time`, `deleted`, `data_scope`)
SELECT 53, '财务(审批流)', 'flow_finance', 53, 1, '报销审批：财务节点候选人', NOW(), NOW(), 0, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'flow_finance' AND deleted = 0);

-- 2. 报销审批流节点：直接上级 → 财务
--    seq=1 直接上级（自动解析 sys_user.leader_id，缺失回退 sys_dept.leader_id）
INSERT INTO `approval_flow_node`
  (business_type, seq, node_name, approver_type, approver_value, enabled, create_time, update_time, deleted)
SELECT 'expense', 1, '直接上级', 'direct_supervisor', NULL, 1, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM approval_flow_node WHERE business_type = 'expense' AND seq = 1 AND deleted = 0);

--    seq=2 财务（角色候选；具体成员由管理员在「角色管理」给 flow_finance 角色挂人）
INSERT INTO `approval_flow_node`
  (business_type, seq, node_name, approver_type, approver_value, enabled, create_time, update_time, deleted)
SELECT 'expense', 2, '财务', 'role', '53', 1, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM approval_flow_node WHERE business_type = 'expense' AND seq = 2 AND deleted = 0);
