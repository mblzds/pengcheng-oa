-- 审批流业务类型注册表
-- 之前业务类型 tab 是把 ApprovalFlowServiceImpl 里 LinkedHashMap 写死的 5 种回显给前端，
-- 加新 tab 必须改代码。这张表把"业务类型 = 一行配置"，运营在管理后台就能新增。
--
-- 注意：本表只是 UI 配置，跟 approval_flow_node 解耦——
--   ① 没在这张表的 business_type 不会出现在管理 tab 列表
--   ② 真正能否提交申请仍取决于后端是否有对应业务表 + Service 调 approvalFlowService.start
-- 5 个内置类型（leave/compensate/expense/advance/prepay）seed 时打 builtin=1，前端禁删除

CREATE TABLE IF NOT EXISTS `approval_business_type` (
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `business_type` VARCHAR(64)  NOT NULL COMMENT '业务类型 key（小写字母+下划线，对应 approval_flow_node.business_type）',
    `label`         VARCHAR(64)  NOT NULL COMMENT '中文显示名（用作管理 tab 文案）',
    `description`   VARCHAR(255) NULL     COMMENT '备注说明',
    `builtin`       TINYINT      NOT NULL DEFAULT 0 COMMENT '1=内置类型，前端禁删除/禁改 key（label 仍可改）',
    `enabled`       TINYINT      NOT NULL DEFAULT 1,
    `sort`          INT          NOT NULL DEFAULT 100 COMMENT 'tab 显示顺序，升序',
    `create_time`   DATETIME     NULL,
    `update_time`   DATETIME     NULL,
    `create_by`     BIGINT       NULL,
    `update_by`     BIGINT       NULL,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_business_type` (`business_type`, `deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '审批流业务类型注册表';

-- 内置 5 种 seed（已硬编码绑定后端 Service，不可删）
INSERT INTO `approval_business_type`
  (business_type, label, description, builtin, enabled, sort, create_time, update_time, deleted)
SELECT 'leave',      '请假',   '员工请假申请',         1, 1, 1, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM approval_business_type WHERE business_type='leave' AND deleted=0);

INSERT INTO `approval_business_type`
  (business_type, label, description, builtin, enabled, sort, create_time, update_time, deleted)
SELECT 'compensate', '调休',   '员工调休申请',         1, 1, 2, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM approval_business_type WHERE business_type='compensate' AND deleted=0);

INSERT INTO `approval_business_type`
  (business_type, label, description, builtin, enabled, sort, create_time, update_time, deleted)
SELECT 'expense',    '报销',   '费用报销申请',         1, 1, 3, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM approval_business_type WHERE business_type='expense' AND deleted=0);

INSERT INTO `approval_business_type`
  (business_type, label, description, builtin, enabled, sort, create_time, update_time, deleted)
SELECT 'advance',    '垫佣',   '渠道垫佣申请',         1, 1, 4, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM approval_business_type WHERE business_type='advance' AND deleted=0);

INSERT INTO `approval_business_type`
  (business_type, label, description, builtin, enabled, sort, create_time, update_time, deleted)
SELECT 'prepay',     '预付佣', '预付佣申请',           1, 1, 5, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM approval_business_type WHERE business_type='prepay' AND deleted=0);
