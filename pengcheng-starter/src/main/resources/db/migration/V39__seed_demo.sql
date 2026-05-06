-- ========================================================
-- V39__seed_demo.sql
-- P0-3 开箱即用：最小业务种子数据
-- 涵盖 联盟商 / 项目 / 客户 / 到访 / 成交 / 回款计划 / 回款流水 / 佣金
-- 所有插入均用 INSERT IGNORE，可重复执行不报错
-- 参考：DEV-PLAN-V3.1.md Sprint 1 / P0-3
-- ========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 0. 基础账号（pengcheng-system.sql 已装 admin/test/lisi，本文件不再重复）
--    sys_user / sys_role / sys_dept 已由 V0 base schema 加载
-- ----------------------------

-- ----------------------------
-- 1. 联盟商（3 家，covering 钻石 / 金牌 / 普通 三档）
-- ----------------------------
INSERT IGNORE INTO `alliance`
    (id, company_name, office_address, contact_name, contact_phone, staff_size, level, status, user_id, create_by, create_time)
VALUES
    (10001, '鹏诚链家（渠道一部）',   '北京市朝阳区建国门外大街1号', '陈经理', '13800000001', 120, 4, 1, NULL, 1, NOW()),
    (10002, '麦田地产·金牌联盟',     '北京市海淀区中关村大街15号', '李总',   '13800000002',  60, 3, 1, NULL, 1, NOW()),
    (10003, '大千地产（零散渠道）',   '北京市丰台区南三环西路88号', '王伟',   '13800000003',  20, 1, 1, NULL, 1, NOW());

-- ----------------------------
-- 2. 项目 / 楼盘（3 个，覆盖在售/待售/售罄）
-- ----------------------------
INSERT IGNORE INTO `project`
    (id, project_name, developer_name, address, project_type, status, district, agency_start_date, agency_end_date, contact_person, contact_phone, description, create_by, create_time)
VALUES
    (20001, '鹏诚·望京壹号',   '鹏诚地产集团',   '北京市朝阳区望京东路18号',  1, 1, '望京',   '2026-01-01', '2027-12-31', '赵驻场', '13900000011', '豪华高层 · 近地铁 14 号线望京站 · 周边双学区',                     1, NOW()),
    (20002, '鹏诚·中关村公馆', '鹏诚地产集团',   '北京市海淀区中关村南大街11号', 3, 1, '中关村', '2026-02-01', '2027-06-30', '孙驻场', '13900000012', '写字楼+公寓综合体 · 毗邻清北 · 自持 5A 甲级办公',                    1, NOW()),
    (20003, '鹏诚·南城经典',   '鹏诚地产集团',   '北京市丰台区南四环西路25号', 1, 3, '南城',   '2025-06-01', '2026-05-31', '周驻场', '13900000013', '首开即清 · 已全部售罄，可作为近期已结案项目对比',                    1, NOW());

-- ----------------------------
-- 3. 项目佣金规则（3 条，版本 v1 生效）
-- ----------------------------
INSERT IGNORE INTO `project_commission_rule`
    (id, project_id, base_rate, jump_point_rules, cash_reward, first_deal_reward, platform_reward, version, status, create_by, create_time)
VALUES
    (30001, 20001, 0.0180, '[{"threshold":5,"bonus":0.0030},{"threshold":10,"bonus":0.0060}]', 3000.00, 5000.00, 1000.00, 1, 1, 1, NOW()),
    (30002, 20002, 0.0150, '[{"threshold":3,"bonus":0.0020}]',                                  2000.00, 3000.00,  500.00, 1, 1, 1, NOW()),
    (30003, 20003, 0.0120, NULL,                                                                 1000.00, 1000.00,    0.00, 1, 3, 1, NOW());

-- ----------------------------
-- 4. 客户（4 条，覆盖 新报备/已到访/已成交 三种状态）
--    说明：真实环境 phone 为 AES 加密。Demo 数据用占位串，只用 phone_masked 展示
-- ----------------------------
INSERT IGNORE INTO `customer`
    (id, report_no, customer_name, phone, phone_masked, visit_count, visit_time, alliance_id, agent_name, agent_phone, status, pool_type, protection_expire_time, last_follow_time, creator_id, create_by, create_time)
VALUES
    (40001, 'BB20260101001', '张', 'SEED-ENC-40001', '138****0001', 2, DATE_SUB(NOW(), INTERVAL 20 DAY), 10001, '陈经理', '13800000001', 3, 2, DATE_ADD(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),  2, 1, DATE_SUB(NOW(), INTERVAL 25 DAY)),
    (40002, 'BB20260115002', '李', 'SEED-ENC-40002', '138****0002', 1, DATE_SUB(NOW(), INTERVAL  8 DAY), 10001, '陈经理', '13800000001', 2, 2, DATE_ADD(NOW(), INTERVAL 80 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),  2, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (40003, 'BB20260301003', '王', 'SEED-ENC-40003', '139****0003', 0, NULL,                               10002, '李总',   '13800000002', 1, 2, DATE_ADD(NOW(), INTERVAL 85 DAY), NULL,                             3, 1, DATE_SUB(NOW(), INTERVAL  5 DAY)),
    (40004, 'BB20260320004', '赵', 'SEED-ENC-40004', '137****0004', 0, NULL,                               10003, '王伟',   '13800000003', 1, 1, NULL,                              NULL,                             NULL, 1, DATE_SUB(NOW(), INTERVAL 30 DAY));

-- 客户-项目关联
INSERT IGNORE INTO `customer_project` (id, customer_id, project_id, create_time) VALUES
    (41001, 40001, 20001, NOW()),
    (41002, 40001, 20002, NOW()),
    (41003, 40002, 20001, NOW()),
    (41004, 40003, 20002, NOW()),
    (41005, 40004, 20001, NOW());

-- ----------------------------
-- 5. 到访记录
-- ----------------------------
INSERT IGNORE INTO `customer_visit`
    (id, customer_id, actual_visit_time, actual_visit_count, receptionist, remark, create_by, create_time)
VALUES
    (42001, 40001, DATE_SUB(NOW(), INTERVAL 20 DAY), 2, '孙驻场', '第一次带看，反映地段满意，价格需回去商量', 1, DATE_SUB(NOW(), INTERVAL 20 DAY)),
    (42002, 40001, DATE_SUB(NOW(), INTERVAL 10 DAY), 2, '孙驻场', '第二次带看，确认户型 A-3，准备认购',       1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (42003, 40002, DATE_SUB(NOW(), INTERVAL  8 DAY), 1, '孙驻场', '首次带看，意向度中等',                       1, DATE_SUB(NOW(), INTERVAL  8 DAY));

-- ----------------------------
-- 6. 成交记录（1 条：客户 40001 成交望京壹号 A-3-1001，800 万）
-- ----------------------------
INSERT IGNORE INTO `customer_deal`
    (id, customer_id, room_no, deal_amount, deal_time, sign_status, subscribe_type, online_sign_status, filing_status, loan_status, payment_status, create_by, create_time)
VALUES
    (50001, 40001, 'A-3-1001', 8000000.00, DATE_SUB(NOW(), INTERVAL 5 DAY), 1, 2, 1, 0, 1, 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY));

-- ----------------------------
-- 7. 回款计划（P0-2 新功能展示：3 期：首付/二期/尾款）
--    首付 300w 已结清 / 二期 400w 即将到期 / 尾款 100w 已逾期
-- ----------------------------
INSERT IGNORE INTO `receivable_plan`
    (id, deal_id, period_no, period_name, due_date, due_amount, paid_amount, status, remark, create_by, create_time)
VALUES
    -- 首付 300w，已结清
    (60001, 50001, 1, '首付',   DATE_SUB(CURDATE(), INTERVAL 4 DAY),  3000000.00, 3000000.00, 3, '合同签订当日到账',                        1, NOW()),
    -- 二期 400w，T+2 到期（即将到期，调度器会写 UPCOMING 告警）
    (60002, 50001, 2, '二期',   DATE_ADD(CURDATE(), INTERVAL 2 DAY),  4000000.00,       0.00, 0, '银行贷款放款日',                          1, NOW()),
    -- 尾款 100w，已逾期 3 天（调度器会写 OVERDUE 告警）
    (60003, 50001, 3, '尾款',   DATE_SUB(CURDATE(), INTERVAL 3 DAY),  1000000.00,       0.00, 4, '客户办理交房手续时结清',                 1, NOW());

-- ----------------------------
-- 8. 回款流水（首付一笔到账）
-- ----------------------------
INSERT IGNORE INTO `receivable_record`
    (id, plan_id, amount, paid_date, pay_way, payer, voucher_no, remark, create_by, create_time)
VALUES
    (61001, 60001, 3000000.00, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 1, '张先生', 'BANK-20260115-0001', '工行电汇到账',                       1, NOW());

-- ----------------------------
-- 9. 佣金记录（50001 成交对应佣金，按 1.8% 基础 + 跳点）
--    注：commission 与 commission_detail 具体列结构请以 V1 为准；此处只写种子主记录
-- ----------------------------
-- （暂不种佣金，保留给 P0-3 第二批次或以管理端操作生成）

-- ----------------------------
-- 10. 预置自动化规则（P1-2 演示）
--     time_based + notify：客户超 7 天未跟进，通过 ChannelPushService 广播
-- ----------------------------
INSERT IGNORE INTO `sys_automation_rule`
    (id, name, description, trigger_type, trigger_config, action_type, action_config, enabled, priority, trigger_count, created_at, updated_at)
VALUES
    (70001,
     '客户超 7 天未跟进提醒',
     '扫描 private pool 中 last_follow_time 超过 7 天的客户，向所有启用推送渠道广播提醒',
     'time_based',
     JSON_OBJECT('target_table', 'customer', 'check_field', 'last_follow_time', 'interval_days', 7),
     'notify',
     JSON_OBJECT(
         'title', '[跟进提醒] 客户 {customer_name}',
         'template', '客户 {customer_name} 已超过 7 天未跟进，请及时安排',
         'messageType', 'automation'
     ),
     1, 10, 0, NOW(), NOW()),
    (70002,
     '合同到期前 30 天预警',
     '扫描 customer 中 protection_expire_time 30 天内到期的私海记录，广播保护期预警',
     'time_based',
     JSON_OBJECT('target_table', 'customer', 'check_field', 'protection_expire_time', 'advance_days', 30),
     'notify',
     JSON_OBJECT(
         'title', '[保护期到期] 客户 {customer_name}',
         'template', '客户 {customer_name} 的保护期将在 30 天内到期，注意续保或入公海',
         'messageType', 'automation'
     ),
     1, 8, 0, NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================================
-- 种子数据回滚提示：若需清理全部演示数据，执行：
-- DELETE FROM receivable_record WHERE id BETWEEN 61001 AND 61999;
-- DELETE FROM receivable_plan   WHERE id BETWEEN 60001 AND 60999;
-- DELETE FROM customer_deal     WHERE id BETWEEN 50001 AND 50999;
-- DELETE FROM customer_visit    WHERE id BETWEEN 42001 AND 42999;
-- DELETE FROM customer_project  WHERE id BETWEEN 41001 AND 41999;
-- DELETE FROM customer          WHERE id BETWEEN 40001 AND 40999;
-- DELETE FROM project_commission_rule WHERE id BETWEEN 30001 AND 30999;
-- DELETE FROM project           WHERE id BETWEEN 20001 AND 20999;
-- DELETE FROM alliance          WHERE id BETWEEN 10001 AND 10999;
-- ========================================================
