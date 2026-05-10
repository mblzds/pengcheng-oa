-- 垫佣 / 预付佣审批流配置：默认套用与报销相同的「直接上级 → 财务」骨架
-- 背景：V50 只为 expense 落了模板，advance/prepay 没有；改造后 PaymentService
-- 已切到 ApprovalFlowService 引擎，缺模板会导致 createPaymentRequest 抛
-- "未配置 X 的审批流"。这里补默认模板，管理后台可后续再调。
--
-- 复用 V50 已新建的 sys_role.code='flow_finance'（id=53）

-- 1. 垫佣（advance）
INSERT INTO `approval_flow_node`
  (business_type, seq, node_name, approver_type, approver_value, enabled, create_time, update_time, deleted)
SELECT 'advance', 1, '直接上级', 'direct_supervisor', NULL, 1, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM approval_flow_node WHERE business_type = 'advance' AND seq = 1 AND deleted = 0);

INSERT INTO `approval_flow_node`
  (business_type, seq, node_name, approver_type, approver_value, enabled, create_time, update_time, deleted)
SELECT 'advance', 2, '财务', 'role', '53', 1, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM approval_flow_node WHERE business_type = 'advance' AND seq = 2 AND deleted = 0);

-- 2. 预付佣（prepay）
INSERT INTO `approval_flow_node`
  (business_type, seq, node_name, approver_type, approver_value, enabled, create_time, update_time, deleted)
SELECT 'prepay', 1, '直接上级', 'direct_supervisor', NULL, 1, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM approval_flow_node WHERE business_type = 'prepay' AND seq = 1 AND deleted = 0);

INSERT INTO `approval_flow_node`
  (business_type, seq, node_name, approver_type, approver_value, enabled, create_time, update_time, deleted)
SELECT 'prepay', 2, '财务', 'role', '53', 1, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM approval_flow_node WHERE business_type = 'prepay' AND seq = 2 AND deleted = 0);
