-- 回滚 V53：审批流的节点链由管理员通过后台 /system/approval-flow 自配，
-- 不应通过迁移强制以 HR 收尾。本迁移精确撤回 V53 添加/重排的节点，
-- 不动 V45/V50 默认配置，也不动管理员或测试种子手工配置的其他节点。

-- 1. 撤回 V53 给 compensate 加的「HR 备案」节点
DELETE FROM `approval_flow_node`
 WHERE `business_type` = 'compensate'
   AND `seq` = 2
   AND `node_name` = 'HR 备案'
   AND `approver_type` = 'role'
   AND `approver_value` = '51'
   AND `deleted` = 0;

-- 2. 撤回 V53 给 expense 加的「HR 备案」节点
DELETE FROM `approval_flow_node`
 WHERE `business_type` = 'expense'
   AND `seq` = 3
   AND `node_name` = 'HR 备案'
   AND `approver_type` = 'role'
   AND `approver_value` = '51'
   AND `deleted` = 0;

-- 3. 撤回 V53 的 leave seq 互换：把 HR审 还原到 seq=3、总经理还原到 seq=4
--    使用临时 seq=99 避免两行 seq 短暂相同
UPDATE `approval_flow_node` SET seq = 99
 WHERE business_type = 'leave' AND deleted = 0 AND seq = 4
   AND approver_type = 'role' AND approver_value = '51';

UPDATE `approval_flow_node` SET seq = 4
 WHERE business_type = 'leave' AND deleted = 0 AND seq = 3
   AND approver_type = 'user';

UPDATE `approval_flow_node` SET seq = 3
 WHERE business_type = 'leave' AND deleted = 0 AND seq = 99
   AND approver_type = 'role' AND approver_value = '51';
