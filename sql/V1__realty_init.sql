/*
 房产销售管理系统 - 数据库初始化脚本

 Target Server Type    : MySQL
 Target Server Version : 80032 (8.0.32)

 Date: 2026-02-10
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 项目表 project
-- ----------------------------
CREATE TABLE IF NOT EXISTS `project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_name` VARCHAR(200) NOT NULL COMMENT '项目名称',
  `developer_name` VARCHAR(200) NULL DEFAULT NULL COMMENT '开发商名称',
  `address` VARCHAR(500) NULL DEFAULT NULL COMMENT '项目地址',
  `project_type` TINYINT NULL DEFAULT NULL COMMENT '项目类型：1-住宅 2-商业 3-办公 4-综合体',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '项目状态：1-在售 2-待售 3-售罄 4-已到期',
  `district` VARCHAR(100) NULL DEFAULT NULL COMMENT '所属片区',
  `agency_start_date` DATE NULL DEFAULT NULL COMMENT '代理开始时间',
  `agency_end_date` DATE NULL DEFAULT NULL COMMENT '代理结束时间',
  `contact_person` VARCHAR(50) NULL DEFAULT NULL COMMENT '联系驻场',
  `contact_phone` VARCHAR(20) NULL DEFAULT NULL COMMENT '联系电话',
  `description` TEXT NULL COMMENT '项目介绍',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_status` (`status`) USING BTREE,
  INDEX `idx_project_district` (`district`) USING BTREE,
  INDEX `idx_project_type` (`project_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代理项目表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 2. 项目佣金规则表 project_commission_rule
-- ----------------------------
CREATE TABLE IF NOT EXISTS `project_commission_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `base_rate` DECIMAL(8,4) NULL DEFAULT NULL COMMENT '基础佣金比例',
  `jump_point_rules` JSON NULL COMMENT '跳点规则JSON',
  `cash_reward` DECIMAL(12,2) NULL DEFAULT NULL COMMENT '现金奖',
  `first_deal_reward` DECIMAL(12,2) NULL DEFAULT NULL COMMENT '开单奖',
  `platform_reward` DECIMAL(12,2) NULL DEFAULT NULL COMMENT '平台奖励',
  `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-生效 2-待审批 3-已失效',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pcr_project_id` (`project_id`) USING BTREE,
  INDEX `idx_pcr_status` (`status`) USING BTREE,
  CONSTRAINT `fk_pcr_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目佣金规则表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- 3. 联盟商表 alliance
-- ----------------------------
CREATE TABLE IF NOT EXISTS `alliance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '联盟商ID',
  `company_name` VARCHAR(200) NOT NULL COMMENT '联盟公司名称',
  `office_address` VARCHAR(500) NULL DEFAULT NULL COMMENT '办公地址',
  `contact_name` VARCHAR(50) NOT NULL COMMENT '负责人姓名',
  `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系方式',
  `staff_size` INT NULL DEFAULT NULL COMMENT '人员规模',
  `level` TINYINT NULL DEFAULT NULL COMMENT '联盟商等级：1-普通 2-银牌 3-金牌 4-钻石',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-停用',
  `user_id` BIGINT NULL DEFAULT NULL COMMENT '关联系统账号ID',
  `channel_user_id` BIGINT NULL DEFAULT NULL COMMENT '对接渠道人员ID',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_alliance_status` (`status`) USING BTREE,
  INDEX `idx_alliance_user_id` (`user_id`) USING BTREE,
  INDEX `idx_alliance_channel_user_id` (`channel_user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '联盟商表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 4. 客户表 customer
-- ----------------------------
CREATE TABLE IF NOT EXISTS `customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `report_no` VARCHAR(50) NOT NULL COMMENT '报备编号',
  `customer_name` VARCHAR(50) NOT NULL COMMENT '客户姓氏',
  `phone` VARCHAR(255) NOT NULL COMMENT '联系方式（AES加密存储）',
  `phone_masked` VARCHAR(20) NULL DEFAULT NULL COMMENT '脱敏手机号（前3后4）',
  `visit_count` INT NULL DEFAULT NULL COMMENT '带看人数',
  `visit_time` DATETIME NULL DEFAULT NULL COMMENT '带看时间',
  `alliance_id` BIGINT NOT NULL COMMENT '带看公司（联盟商ID）',
  `agent_name` VARCHAR(50) NULL DEFAULT NULL COMMENT '经纪人姓名',
  `agent_phone` VARCHAR(20) NULL DEFAULT NULL COMMENT '经纪人联系方式',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-已报备 2-已到访 3-已成交',
  `pool_type` TINYINT NOT NULL DEFAULT 2 COMMENT '池类型：1-公海 2-私海',
  `protection_expire_time` DATETIME NULL DEFAULT NULL COMMENT '保护期到期时间',
  `last_follow_time` DATETIME NULL DEFAULT NULL COMMENT '最后跟进时间',
  `deal_probability` DECIMAL(5,4) NULL DEFAULT NULL COMMENT '成交概率评分（0-1）',
  `creator_id` BIGINT NULL DEFAULT NULL COMMENT '录入驻场人员ID',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_report_no` (`report_no`) USING BTREE,
  INDEX `idx_customer_phone` (`phone`(32)) USING BTREE,
  INDEX `idx_customer_alliance_id` (`alliance_id`) USING BTREE,
  INDEX `idx_customer_status` (`status`) USING BTREE,
  INDEX `idx_customer_pool_type` (`pool_type`) USING BTREE,
  INDEX `idx_customer_creator_id` (`creator_id`) USING BTREE,
  CONSTRAINT `fk_customer_alliance` FOREIGN KEY (`alliance_id`) REFERENCES `alliance` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- 5. 客户-项目关联表 customer_project（多对多）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `customer_project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_customer_project` (`customer_id`, `project_id`) USING BTREE,
  INDEX `idx_cp_project_id` (`project_id`) USING BTREE,
  CONSTRAINT `fk_cp_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_cp_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户-项目关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 6. 客户到访记录表 customer_visit
-- ----------------------------
CREATE TABLE IF NOT EXISTS `customer_visit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '到访记录ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `actual_visit_time` DATETIME NOT NULL COMMENT '实际到访时间',
  `actual_visit_count` INT NULL DEFAULT NULL COMMENT '实际到访人数',
  `receptionist` VARCHAR(50) NULL DEFAULT NULL COMMENT '接待人员',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_cv_customer_id` (`customer_id`) USING BTREE,
  CONSTRAINT `fk_cv_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户到访记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 7. 客户成交记录表 customer_deal
-- ----------------------------
CREATE TABLE IF NOT EXISTS `customer_deal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成交记录ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `room_no` VARCHAR(50) NULL DEFAULT NULL COMMENT '成交房号',
  `deal_amount` DECIMAL(12,2) NULL DEFAULT NULL COMMENT '成交金额',
  `deal_time` DATETIME NULL DEFAULT NULL COMMENT '成交时间',
  `sign_status` TINYINT NULL DEFAULT NULL COMMENT '签约状态：1-已签约 2-未签约',
  `subscribe_type` TINYINT NULL DEFAULT NULL COMMENT '认购类型：1-小订 2-大定',
  `online_sign_status` TINYINT NULL DEFAULT 0 COMMENT '网签状态：0-未网签 1-已网签',
  `filing_status` TINYINT NULL DEFAULT 0 COMMENT '备案状态：0-未备案 1-已备案',
  `loan_status` TINYINT NULL DEFAULT 0 COMMENT '贷款状态：0-未申请 1-审批中 2-已放款 3-已拒绝',
  `payment_status` TINYINT NULL DEFAULT 0 COMMENT '回款状态：0-未回款 1-部分回款 2-全部回款',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_cd_customer_id` (`customer_id`) USING BTREE,
  CONSTRAINT `fk_cd_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户成交记录表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- 8. 佣金主表 commission
-- ----------------------------
CREATE TABLE IF NOT EXISTS `commission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '佣金ID',
  `deal_id` BIGINT NOT NULL COMMENT '关联成交记录ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `alliance_id` BIGINT NOT NULL COMMENT '联盟商ID',
  `receivable_amount` DECIMAL(12,2) NOT NULL COMMENT '应收佣金',
  `payable_amount` DECIMAL(12,2) NOT NULL COMMENT '应结佣金',
  `platform_fee` DECIMAL(12,2) NOT NULL COMMENT '公司平台费',
  `audit_status` TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态：1-待审核 2-审核通过 3-审核驳回',
  `audit_remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '审核备注',
  `auditor_id` BIGINT NULL DEFAULT NULL COMMENT '审核人ID',
  `audit_time` DATETIME NULL DEFAULT NULL COMMENT '审核时间',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_comm_deal_id` (`deal_id`) USING BTREE,
  INDEX `idx_comm_project_id` (`project_id`) USING BTREE,
  INDEX `idx_comm_alliance_id` (`alliance_id`) USING BTREE,
  INDEX `idx_comm_audit_status` (`audit_status`) USING BTREE,
  CONSTRAINT `fk_comm_deal` FOREIGN KEY (`deal_id`) REFERENCES `customer_deal` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_comm_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_comm_alliance` FOREIGN KEY (`alliance_id`) REFERENCES `alliance` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '佣金主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 9. 佣金明细表 commission_detail
-- ----------------------------
CREATE TABLE IF NOT EXISTS `commission_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `commission_id` BIGINT NOT NULL COMMENT '佣金ID',
  `base_commission` DECIMAL(12,2) NULL DEFAULT NULL COMMENT '基础佣金',
  `jump_point_commission` DECIMAL(12,2) NULL DEFAULT NULL COMMENT '跳点佣金',
  `cash_reward` DECIMAL(12,2) NULL DEFAULT NULL COMMENT '现金奖',
  `first_deal_reward` DECIMAL(12,2) NULL DEFAULT NULL COMMENT '开单奖',
  `platform_reward` DECIMAL(12,2) NULL DEFAULT NULL COMMENT '平台奖励',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_cd_commission_id` (`commission_id`) USING BTREE,
  CONSTRAINT `fk_cd_commission` FOREIGN KEY (`commission_id`) REFERENCES `commission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '佣金明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 10. 佣金变更日志表 commission_change_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `commission_change_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `commission_id` BIGINT NOT NULL COMMENT '佣金ID',
  `field_name` VARCHAR(100) NOT NULL COMMENT '变更字段',
  `old_value` VARCHAR(500) NULL DEFAULT NULL COMMENT '变更前值',
  `new_value` VARCHAR(500) NULL DEFAULT NULL COMMENT '变更后值',
  `operator_id` BIGINT NOT NULL COMMENT '变更人ID',
  `change_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ccl_commission_id` (`commission_id`) USING BTREE,
  CONSTRAINT `fk_ccl_commission` FOREIGN KEY (`commission_id`) REFERENCES `commission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '佣金变更日志表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- 11. 考勤打卡记录表 attendance_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `attendance_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '员工ID',
  `attendance_date` DATE NOT NULL COMMENT '考勤日期',
  `clock_in_time` DATETIME NULL DEFAULT NULL COMMENT '上班打卡时间',
  `clock_in_location` VARCHAR(200) NULL DEFAULT NULL COMMENT '上班打卡位置',
  `clock_in_status` TINYINT NULL DEFAULT NULL COMMENT '上班状态：1-正常 2-迟到',
  `clock_out_time` DATETIME NULL DEFAULT NULL COMMENT '下班打卡时间',
  `clock_out_location` VARCHAR(200) NULL DEFAULT NULL COMMENT '下班打卡位置',
  `clock_out_status` TINYINT NULL DEFAULT NULL COMMENT '下班状态：1-正常 2-早退',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date` (`user_id`, `attendance_date`) USING BTREE,
  INDEX `idx_ar_user_id` (`user_id`) USING BTREE,
  INDEX `idx_ar_attendance_date` (`attendance_date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考勤打卡记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 12. 请假申请表 leave_request
-- ----------------------------
CREATE TABLE IF NOT EXISTS `leave_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '请假ID',
  `user_id` BIGINT NOT NULL COMMENT '申请人ID',
  `leave_type` TINYINT NOT NULL COMMENT '请假类型：1-事假 2-病假 3-年假 4-婚假 5-产假 6-调休',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `reason` VARCHAR(500) NULL DEFAULT NULL COMMENT '请假原因',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '审批状态：1-待审批 2-已通过 3-已驳回',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_lr_user_id` (`user_id`) USING BTREE,
  INDEX `idx_lr_status` (`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '请假申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 13. 签到记录表 sign_in_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sign_in_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '签到ID',
  `user_id` BIGINT NOT NULL COMMENT '员工ID',
  `sign_in_time` DATETIME NOT NULL COMMENT '签到时间',
  `location` VARCHAR(200) NULL DEFAULT NULL COMMENT '签到位置',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '签到备注',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sir_user_id` (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '签到记录表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- 14. 付款申请表 payment_request
-- ----------------------------
CREATE TABLE IF NOT EXISTS `payment_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
  `request_type` TINYINT NOT NULL COMMENT '申请类型：1-费用报销 2-垫佣 3-预付佣',
  `expense_type` TINYINT NULL DEFAULT NULL COMMENT '报销类型：1-交通费 2-餐饮费 3-住宿费 4-办公用品 5-其他',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
  `description` VARCHAR(500) NULL DEFAULT NULL COMMENT '说明',
  `related_deal_id` BIGINT NULL DEFAULT NULL COMMENT '关联成交记录ID',
  `related_alliance_id` BIGINT NULL DEFAULT NULL COMMENT '关联联盟商ID',
  `attachments` JSON NULL COMMENT '附件路径JSON',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '审批状态：1-待审批 2-审批中 3-已通过 4-已驳回',
  `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pr_applicant_id` (`applicant_id`) USING BTREE,
  INDEX `idx_pr_status` (`status`) USING BTREE,
  INDEX `idx_pr_request_type` (`request_type`) USING BTREE,
  INDEX `idx_pr_related_deal_id` (`related_deal_id`) USING BTREE,
  INDEX `idx_pr_related_alliance_id` (`related_alliance_id`) USING BTREE,
  CONSTRAINT `fk_pr_deal` FOREIGN KEY (`related_deal_id`) REFERENCES `customer_deal` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_pr_alliance` FOREIGN KEY (`related_alliance_id`) REFERENCES `alliance` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '付款申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 15. 付款审批记录表 payment_approval
-- ----------------------------
CREATE TABLE IF NOT EXISTS `payment_approval` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审批记录ID',
  `request_id` BIGINT NOT NULL COMMENT '付款申请ID',
  `approver_id` BIGINT NOT NULL COMMENT '审批人ID',
  `result` TINYINT NOT NULL COMMENT '审批结果：1-通过 2-驳回',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '审批备注',
  `approval_order` INT NOT NULL COMMENT '审批顺序',
  `approval_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pa_request_id` (`request_id`) USING BTREE,
  INDEX `idx_pa_approver_id` (`approver_id`) USING BTREE,
  CONSTRAINT `fk_pa_request` FOREIGN KEY (`request_id`) REFERENCES `payment_request` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '付款审批记录表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
