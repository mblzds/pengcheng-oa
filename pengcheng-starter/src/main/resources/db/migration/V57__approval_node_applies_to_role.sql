-- 审批流节点级角色门控：每个节点可指定"适用于哪些申请人角色"
-- 空值 = 全员适用（保持向后兼容）；非空 = 仅当申请人持有任一角色时该节点才生效
-- 解析时若节点不适用，由引擎在 start() 中跳过（与"剔除自己后为空"是同一类跳过）

ALTER TABLE `approval_flow_node`
  ADD COLUMN `applies_to_role_ids` VARCHAR(200) NULL
  COMMENT '适用申请人角色ID列表（逗号分隔）；空 = 全员适用'
  AFTER `approver_value`;

-- 回填：请假流的"总经理"节点仅对管理岗生效
-- 4=manger, 52=flow_dept_mgr —— 两类都属于管理岗，请假需要总经理签字
-- 普通员工请假时此节点会被跳过，链路自然变成 "直接上级 → HR"
UPDATE `approval_flow_node`
SET applies_to_role_ids = '4,52',
    update_time = NOW()
WHERE business_type = 'leave' AND seq = 4 AND deleted = 0
  AND approver_type = 'role' AND approver_value = '50';
