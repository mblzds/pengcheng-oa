/*
 房产销售管理系统 - 定时任务初始化脚本

 说明：
 - 幂等执行（基于 invoke_target 判重）
 - 依赖 sys_job 表已存在
*/

SET NAMES utf8mb4;

-- 说明：该脚本会在容器首次初始化时自动执行，但基础表（含 sys_job）可能由后续 SQL 文件创建。
-- 为避免因执行顺序导致初始化中断，这里改为“表存在才初始化任务”，否则直接跳过。

DROP PROCEDURE IF EXISTS pc_init_realty_jobs;
DELIMITER //
CREATE PROCEDURE pc_init_realty_jobs()
BEGIN
  DECLARE job_table_exists INT DEFAULT 0;

  SELECT COUNT(1)
    INTO job_table_exists
    FROM information_schema.tables
   WHERE table_schema = DATABASE()
     AND table_name = 'sys_job';

  IF job_table_exists = 0 THEN
    -- sys_job 尚未创建，跳过（避免初始化脚本报错导致后续 SQL 无法执行）
    SELECT 'skip V2__realty_job_init: sys_job not exists' AS msg;
  ELSE
    -- 项目到期检查：每天 01:00 执行
    INSERT INTO sys_job (
      job_name,
      job_group,
      invoke_target,
      cron_expression,
      misfire_policy,
      concurrent,
      status,
      remark,
      create_time,
      update_time,
      create_by,
      update_by,
      deleted
    )
    SELECT
      '项目到期检查',
      'REALTY',
      'projectExpiryTask.execute',
      '0 0 1 * * ?',
      3,
      1,
      1,
      '每日检查代理到期项目并发送系统通知',
      NOW(),
      NOW(),
      1,
      1,
      0
    WHERE NOT EXISTS (
      SELECT 1 FROM sys_job
      WHERE invoke_target = 'projectExpiryTask.execute' AND deleted = 0
    );

    -- 客户公海池回收：每天 02:00 执行
    INSERT INTO sys_job (
      job_name,
      job_group,
      invoke_target,
      cron_expression,
      misfire_policy,
      concurrent,
      status,
      remark,
      create_time,
      update_time,
      create_by,
      update_by,
      deleted
    )
    SELECT
      '客户公海池回收',
      'REALTY',
      'customerPoolRecycleTask.execute',
      '0 0 2 * * ?',
      3,
      1,
      1,
      '每日执行客户公海池自动回收',
      NOW(),
      NOW(),
      1,
      1,
      0
    WHERE NOT EXISTS (
      SELECT 1 FROM sys_job
      WHERE invoke_target = 'customerPoolRecycleTask.execute' AND deleted = 0
    );

    -- 成交概率评分更新：每天 03:00 执行
    INSERT INTO sys_job (
      job_name,
      job_group,
      invoke_target,
      cron_expression,
      misfire_policy,
      concurrent,
      status,
      remark,
      create_time,
      update_time,
      create_by,
      update_by,
      deleted
    )
    SELECT
      '成交概率评分更新',
      'REALTY',
      'dealProbabilityUpdateJob.execute',
      '0 0 3 * * ?',
      3,
      1,
      1,
      '每日批量更新活跃客户成交概率评分',
      NOW(),
      NOW(),
      1,
      1,
      0
    WHERE NOT EXISTS (
      SELECT 1 FROM sys_job
      WHERE invoke_target = 'dealProbabilityUpdateJob.execute' AND deleted = 0
    );
  END IF;
END//
DELIMITER ;

CALL pc_init_realty_jobs();
DROP PROCEDURE IF EXISTS pc_init_realty_jobs;
