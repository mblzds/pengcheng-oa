-- 通用审批申请表（Phase 2 of 通用流水线）
-- 让管理后台「新增业务类型」(V62) 创建出来的非内置 business_type，
-- 真正能被员工提交、审批人审批、引擎驱动；表单仅含 标题 + 说明 (MVP 范围).
--
-- 列含义:
--   business_type   - approval_business_type.business_type 的引用，非内置类型
--   applicant_id    - 申请人 sys_user.id
--   title           - 申请标题（必填）
--   description     - 详细说明（可选）
--   status          - ApprovalConstants.STATUS_*: 1=审批中 2=已通过 3=已驳回 4=已撤销

CREATE TABLE IF NOT EXISTS `general_approval_request` (
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `business_type` VARCHAR(64)  NOT NULL COMMENT '业务类型 key',
    `applicant_id`  BIGINT       NOT NULL COMMENT '申请人 sys_user.id',
    `title`         VARCHAR(255) NOT NULL COMMENT '申请标题',
    `description`   TEXT         NULL     COMMENT '详细说明',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '1=审批中 2=已通过 3=已驳回 4=已撤销',
    `create_time`   DATETIME     NULL,
    `update_time`   DATETIME     NULL,
    `create_by`     BIGINT       NULL,
    `update_by`     BIGINT       NULL,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    KEY `idx_applicant`    (`applicant_id`, `status`, `deleted`),
    KEY `idx_business`     (`business_type`, `status`, `deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通用审批申请';
