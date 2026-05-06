/*
 内部员工多端工作平台 - 通知消息与调休申请表

 Target Server Type    : MySQL
 Target Server Version : 80032 (8.0.32)

 新增表：
 - sys_notification：系统通知消息
 - realty_compensate_request：调休申请
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 通知消息表 sys_notification
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_notification` (
    `id` BIGINT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '接收人ID',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `type` TINYINT NOT NULL COMMENT '通知类型：1=客户状态变更 2=审批状态变更 3=新审批到达',
    `biz_type` VARCHAR(50) COMMENT '业务类型',
    `biz_id` BIGINT COMMENT '业务ID',
    `read_status` TINYINT DEFAULT 0 COMMENT '0=未读 1=已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_read` (`user_id`, `read_status`),
    INDEX `idx_user_create` (`user_id`, `create_time` DESC)
) COMMENT '系统通知消息';

-- ----------------------------
-- 2. 调休申请表 realty_compensate_request
-- ----------------------------
CREATE TABLE IF NOT EXISTS `realty_compensate_request` (
    `id` BIGINT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `compensate_date` DATE NOT NULL COMMENT '调休日期',
    `reason` VARCHAR(500) COMMENT '调休原因',
    `status` TINYINT DEFAULT 1 COMMENT '1=待审批 2=已通过 3=已驳回',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_status` (`user_id`, `status`)
) COMMENT '调休申请';

SET FOREIGN_KEY_CHECKS = 1;
