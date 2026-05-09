-- 审批流统一原则：经过业务/财务节点后，最末必须流转到 HR 备案
-- 角色 id 来源：sys_role.code='flow_hr' 对应 id=51（V45 之前已存在）
-- 注意：模板表 (approval_flow_node) 与执行快照 (approval_record_node) 解耦，
--       本次 UPDATE 不影响在途审批，仅作用于「之后新提交的申请」。

-- 1. 调休 compensate：原来只有「直接上级」一个节点，补一个 HR 备案到末位
INSERT INTO `approval_flow_node`
  (business_type, seq, node_name, approver_type, approver_value, enabled, create_time, update_time, deleted)
SELECT 'compensate', 2, 'HR 备案', 'role', '51', 1, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM approval_flow_node
   WHERE business_type = 'compensate' AND deleted = 0
     AND approver_type = 'role' AND approver_value = '51'
);

-- 2. 报销 expense：直接上级 → 财务，补 HR 备案到末位
INSERT INTO `approval_flow_node`
  (business_type, seq, node_name, approver_type, approver_value, enabled, create_time, update_time, deleted)
SELECT 'expense', 3, 'HR 备案', 'role', '51', 1, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM approval_flow_node
   WHERE business_type = 'expense' AND deleted = 0
     AND approver_type = 'role' AND approver_value = '51'
);

-- 3. 请假 leave：现有 直接上级(1) → 部门经理(2) → HR审(3) → 总经理(4)
--    HR 不是末位，不符合「最终到 HR」原则。把 HR 与 总经理 互换 seq。
--    使用临时 seq=99 避免在执行过程中两行 seq 短暂相同。
UPDATE `approval_flow_node` SET seq = 99
 WHERE business_type = 'leave' AND deleted = 0 AND seq = 3
   AND approver_type = 'role' AND approver_value = '51';

UPDATE `approval_flow_node` SET seq = 3
 WHERE business_type = 'leave' AND deleted = 0 AND seq = 4
   AND approver_type = 'user';

UPDATE `approval_flow_node` SET seq = 4
 WHERE business_type = 'leave' AND deleted = 0 AND seq = 99
   AND approver_type = 'role' AND approver_value = '51';
