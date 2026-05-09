-- 配合"审批流单一数据源 + 祖先兜底"设计：尝试用现有 sys_dept.leader 文本反查回填
-- sys_user.nickname 得到 leader_id。仅匹配「nickname 唯一对应一个启用用户」的情况，
-- 多个同名或无匹配的留空——这些部门以后通过 ancestor 回溯也能跑（祖先有负责人即可）。

UPDATE `sys_dept` d
INNER JOIN (
  SELECT u.nickname, MIN(u.id) AS uid
  FROM `sys_user` u
  WHERE u.deleted = 0 AND u.status = 1 AND u.nickname IS NOT NULL AND u.nickname != ''
  GROUP BY u.nickname
  HAVING COUNT(*) = 1
) m ON m.nickname = d.leader
SET d.leader_id = m.uid
WHERE d.deleted = 0
  AND d.leader_id IS NULL
  AND d.leader IS NOT NULL
  AND d.leader != '';
