-- V74：清理重复请假/调休单
-- 背景：V73 之前 submitLeaveRequest / submitCompensateRequest 不拦截重叠时段，
--       可能产生"同一员工同一时段多份单"的脏数据，导致月度汇总累加翻倍。
--       本次提交（f132a8f）已经在提交端加重叠拦截、月报端做时段并集，
--       但**历史已落库的重复单**仍然摆在那；本迁移把多余的撤销。
--
-- 策略：仅清理"完全相同的重复"（user_id + start_time + end_time 三元组完全一致，
--       或调休 user_id + compensate_date 二元组完全一致）；部分重叠不动，
--       由 HR 后台人工核对。这样最稳，不会误撤"上午半天事假 + 下午半天病假"
--       那种合法二段请假。
--
-- 保留谁：每个重复组按 APPROVED(2) > PENDING(1) 优先 + 同 status 取最小 id。
-- 撤销谁：其余的 status 改 STATUS_CANCELLED=4；对应 approval_record_node 中
--         result IS NULL 的节点同步改 RESULT_CANCELLED=3，避免审批人列表残留孤儿。

-- 1. 撤销 leave_request 重复单
-- MySQL 8.0 支持 CTE 配合 UPDATE-JOIN
UPDATE leave_request lr
JOIN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY user_id, start_time, end_time
                   ORDER BY CASE status WHEN 2 THEN 1 WHEN 1 THEN 2 ELSE 3 END,
                            id
               ) AS rn
        FROM leave_request
        WHERE deleted = 0 AND status IN (1, 2)
    ) ranked
    WHERE rn > 1
) dup ON lr.id = dup.id
SET lr.status = 4, lr.update_time = NOW();

-- 2. 撤销 realty_compensate_request 重复单（按 user_id + compensate_date）
-- 注意：CompensateRequest 实体没有 deleted 字段，直接用 status 过滤
UPDATE realty_compensate_request cr
JOIN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY user_id, compensate_date
                   ORDER BY CASE status WHEN 2 THEN 1 WHEN 1 THEN 2 ELSE 3 END,
                            id
               ) AS rn
        FROM realty_compensate_request
        WHERE status IN (1, 2)
    ) ranked
    WHERE rn > 1
) dup ON cr.id = dup.id
SET cr.status = 4, cr.update_time = NOW();

-- 3. 同步撤销 approval_record_node 中"业务单已撤销但节点仍待审批"的孤儿
-- 顺便处理本次迁移之外的历史孤儿（如往期手工撤销但节点没收尾的）
UPDATE approval_record_node arn
SET arn.result = 3, arn.update_time = NOW()
WHERE arn.deleted = 0
  AND arn.result IS NULL
  AND arn.business_type = 'leave'
  AND EXISTS (
      SELECT 1 FROM leave_request lr
      WHERE lr.id = arn.business_id AND lr.status = 4 AND lr.deleted = 0
  );

UPDATE approval_record_node arn
SET arn.result = 3, arn.update_time = NOW()
WHERE arn.deleted = 0
  AND arn.result IS NULL
  AND arn.business_type = 'compensate'
  AND EXISTS (
      SELECT 1 FROM realty_compensate_request cr
      WHERE cr.id = arn.business_id AND cr.status = 4
  );
