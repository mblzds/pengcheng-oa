-- 请假审批流优化：
--   1. seq=2 "部门负责人"由 role=52 (flow_dept_mgr 全公司) 改为 applicant_dept_manager
--      → 只解析"申请人所在部门"的负责人，不再拉全公司部门管理者
--   2. seq=4 "总经理"由 user=101 (gm_zhou 硬编码) 改为 role=50 (flow_gm)
--      → 总经理离职/换人只需要重挂 flow_gm 角色，不用动审批流模板

UPDATE `approval_flow_node`
SET approver_type = 'applicant_dept_manager',
    approver_value = NULL,
    update_time = NOW()
WHERE business_type = 'leave' AND seq = 2 AND deleted = 0
  AND approver_type = 'role' AND approver_value = '52';

UPDATE `approval_flow_node`
SET approver_type = 'role',
    approver_value = '50',
    update_time = NOW()
WHERE business_type = 'leave' AND seq = 4 AND deleted = 0
  AND approver_type = 'user' AND approver_value = '101';
