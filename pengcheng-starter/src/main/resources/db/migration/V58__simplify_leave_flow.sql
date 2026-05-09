-- 化简请假审批流到 2 节点：直接上级 → HR 备案
--
-- 设计原则：
--   1) direct_supervisor 已经覆盖"沿链向上找"的语义——
--      普通员工解析到部门经理，部门经理解析到总经理，总经理解析为空被自然跳过
--   2) HR 备案是流程结束后的归档动作，应该收尾，不夹中间
--   3) 出厂模板只保留通用骨架，董事长/副总等特殊节点由后台 UI 自行追加
--
-- 旧节点（V46/V56/V57 累积）：
--   seq=1 直接上级           （保留）
--   seq=2 本部门负责人       （删，与直接上级重复）
--   seq=3 HR 备案            （重排到 seq=2）
--   seq=4 总经理签字 仅管理岗（删，与直接上级链对部门经理重复触发）
--
-- 新节点：
--   seq=1 直接上级（全员）
--   seq=2 HR 备案（全员）
--
-- 后端 applicant_dept_manager 解析逻辑保留（防御历史数据），UI 选项已下线。

-- 删 seq=2「本部门负责人」
UPDATE `approval_flow_node`
SET deleted = 1,
    update_time = NOW()
WHERE business_type = 'leave' AND seq = 2 AND deleted = 0
  AND approver_type = 'applicant_dept_manager';

-- 删 seq=4「总经理签字」
UPDATE `approval_flow_node`
SET deleted = 1,
    update_time = NOW()
WHERE business_type = 'leave' AND seq = 4 AND deleted = 0
  AND approver_type = 'role';

-- 把 HR 备案（原 seq=3）重排到 seq=2
UPDATE `approval_flow_node`
SET seq = 2,
    update_time = NOW()
WHERE business_type = 'leave' AND seq = 3 AND deleted = 0;
