/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80032 (8.0.32)
 Source Host           : localhost:3306
 Source Schema         : pengcheng-system

 Target Server Type    : MySQL
 Target Server Version : 80032 (8.0.32)
 File Encoding         : 65001

 Date: 09/02/2026 21:30:03
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '表描述',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '实体类名称',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '生成功能名',
  `author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '生成功能作者',
  `gen_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'crud' COMMENT '生成类型（crud单表 tree树表）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '/' COMMENT '生成路径',
  `front_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'naive-ui' COMMENT '前端模板类型',
  `parent_menu_id` bigint NULL DEFAULT NULL COMMENT '上级菜单ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成业务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of gen_table
-- ----------------------------
INSERT INTO `gen_table` VALUES (4, 'student', '学生表', 'Student', 'com.pengcheng.system', 'system', 'student', '学生表', 'Pengcheng', 'crud', '/', 'naive-ui', NULL, NULL, '2026-02-09 12:44:26', '2026-02-09 12:44:26');

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint NULL DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '列类型',
  `java_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'Java类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'Java字段名',
  `is_pk` tinyint NULL DEFAULT 0 COMMENT '是否主键（1是）',
  `is_increment` tinyint NULL DEFAULT 0 COMMENT '是否自增（1是）',
  `is_required` tinyint NULL DEFAULT 0 COMMENT '是否必填（1是）',
  `is_insert` tinyint NULL DEFAULT 0 COMMENT '是否为插入字段（1是）',
  `is_edit` tinyint NULL DEFAULT 0 COMMENT '是否编辑字段（1是）',
  `is_list` tinyint NULL DEFAULT 0 COMMENT '是否列表字段（1是）',
  `is_query` tinyint NULL DEFAULT 0 COMMENT '是否查询字段（1是）',
  `query_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'EQ' COMMENT '查询方式',
  `html_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '显示类型',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典类型',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_table_id`(`table_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成字段表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of gen_table_column
-- ----------------------------
INSERT INTO `gen_table_column` VALUES (34, 4, 'id', 'id', 'bigint', 'Long', 'id', 1, 1, 1, 0, 0, 1, 1, 'EQ', 'input', '', 1);
INSERT INTO `gen_table_column` VALUES (35, 4, 'student_no', '学号', 'varchar(50)', 'String', 'studentNo', 0, 0, 1, 1, 1, 1, 0, 'EQ', 'input', '', 2);
INSERT INTO `gen_table_column` VALUES (36, 4, 'name', '姓名', 'varchar(50)', 'String', 'name', 0, 0, 1, 1, 1, 1, 1, 'EQ', 'input', '', 3);
INSERT INTO `gen_table_column` VALUES (37, 4, 'gender', '性别 1男 2女', 'tinyint', 'Integer', 'gender', 0, 0, 0, 1, 1, 1, 0, 'EQ', 'select', '', 4);
INSERT INTO `gen_table_column` VALUES (38, 4, 'birthday', '出生日期', 'date', 'LocalDate', 'birthday', 0, 0, 0, 1, 1, 1, 0, 'EQ', 'datetime', '', 5);
INSERT INTO `gen_table_column` VALUES (39, 4, 'phone', '手机号', 'varchar(20)', 'String', 'phone', 0, 0, 0, 1, 1, 1, 0, 'EQ', 'input', '', 6);
INSERT INTO `gen_table_column` VALUES (40, 4, 'email', '邮箱', 'varchar(100)', 'String', 'email', 0, 0, 0, 1, 1, 1, 0, 'EQ', 'input', '', 7);
INSERT INTO `gen_table_column` VALUES (41, 4, 'address', '地址', 'varchar(200)', 'String', 'address', 0, 0, 0, 1, 1, 1, 0, 'EQ', 'input', '', 8);
INSERT INTO `gen_table_column` VALUES (42, 4, 'class_id', '班级ID', 'bigint', 'Long', 'classId', 0, 0, 0, 1, 1, 1, 0, 'EQ', 'input', '', 9);
INSERT INTO `gen_table_column` VALUES (43, 4, 'status', '状态', 'tinyint', 'Integer', 'status', 0, 0, 0, 1, 1, 1, 1, 'EQ', 'select', '', 10);
INSERT INTO `gen_table_column` VALUES (44, 4, 'deleted', 'deleted', 'tinyint', 'Integer', 'deleted', 0, 0, 0, 0, 0, 0, 0, 'EQ', 'input', '', 11);
INSERT INTO `gen_table_column` VALUES (45, 4, 'create_time', 'create_time', 'datetime', 'LocalDateTime', 'createTime', 0, 0, 0, 0, 0, 1, 0, 'EQ', 'datetime', '', 12);
INSERT INTO `gen_table_column` VALUES (46, 4, 'update_time', 'update_time', 'datetime', 'LocalDateTime', 'updateTime', 0, 0, 0, 0, 0, 1, 0, 'EQ', 'datetime', '', 13);

-- ----------------------------
-- Table structure for qrtz_blob_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `blob_data` blob NULL COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Blob类型的触发器表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_blob_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_calendars
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`, `calendar_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '日历信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_calendars
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_cron_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `cron_expression` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Cron类型的触发器表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_cron_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_fired_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint NOT NULL COMMENT '触发的时间',
  `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
  `priority` int NOT NULL COMMENT '优先级',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`, `entry_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '已触发的触发器表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_fired_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_job_details
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_job_details`;
CREATE TABLE `qrtz_job_details`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务组名',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否接受恢复执行',
  `job_data` blob NULL COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`, `job_name`, `job_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务详细信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_job_details
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_locks
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`, `lock_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '存储的悲观锁信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_locks
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_paused_trigger_grps
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`, `trigger_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '暂停的触发器表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_paused_trigger_grps
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_scheduler_state
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`, `instance_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '调度器状态表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_scheduler_state
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_simple_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '简单触发器的信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_simple_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_simprop_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `str_prop_1` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `str_prop_2` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `str_prop_3` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `int_prop_1` int NULL DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `int_prop_2` int NULL DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `long_prop_1` bigint NULL DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `long_prop_2` bigint NULL DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `dec_prop_1` decimal(13, 4) NULL DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `dec_prop_2` decimal(13, 4) NULL DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `bool_prop_1` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `bool_prop_2` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '同步机制的行锁表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_simprop_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint NULL DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint NULL DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int NULL DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的类型',
  `start_time` bigint NOT NULL COMMENT '开始时间',
  `end_time` bigint NULL DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint NULL DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` blob NULL COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  INDEX `sched_name`(`sched_name` ASC, `job_name` ASC, `job_group` ASC) USING BTREE,
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '触发器详细信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of qrtz_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学号',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `gender` tinyint NULL DEFAULT NULL COMMENT '性别 1男 2女',
  `birthday` date NULL DEFAULT NULL COMMENT '出生日期',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `class_id` bigint NULL DEFAULT NULL COMMENT '班级ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态',
  `deleted` tinyint NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES (4, '12', '12', NULL, NULL, '1', '1', '1', 1, 1, 1, '2026-02-09 12:55:30', '2026-02-09 12:55:33');

-- ----------------------------
-- Table structure for sys_chat_group
-- ----------------------------
DROP TABLE IF EXISTS `sys_chat_group`;
CREATE TABLE `sys_chat_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '群ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '群名称',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '群头像',
  `owner_id` bigint NOT NULL COMMENT '群主ID',
  `announcement` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '群公告',
  `max_members` int NULL DEFAULT 200 COMMENT '最大成员数',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0-解散 1-正常',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_owner_id`(`owner_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群聊表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_chat_group
-- ----------------------------
INSERT INTO `sys_chat_group` VALUES (1, '测试', NULL, 2, '1111111111', 200, 0, '2026-01-31 11:24:29', '2026-01-31 12:39:26');
INSERT INTO `sys_chat_group` VALUES (2, '内部沟通群', NULL, 1, NULL, 200, 1, '2026-01-31 12:42:47', '2026-02-07 09:54:17');
INSERT INTO `sys_chat_group` VALUES (3, '测试', NULL, 1, NULL, 200, 1, '2026-01-31 23:20:05', '2026-01-31 23:20:37');

-- ----------------------------
-- Table structure for sys_chat_group_member
-- ----------------------------
DROP TABLE IF EXISTS `sys_chat_group_member`;
CREATE TABLE `sys_chat_group_member`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `group_id` bigint NOT NULL COMMENT '群ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '群内昵称',
  `role` tinyint NULL DEFAULT 0 COMMENT '角色：0-普通成员 1-管理员 2-群主',
  `muted` tinyint NULL DEFAULT 0 COMMENT '是否禁言：0-否 1-是',
  `join_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_group_user`(`group_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群成员表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_chat_group_member
-- ----------------------------
INSERT INTO `sys_chat_group_member` VALUES (4, 2, 1, NULL, 2, 0, '2026-01-31 12:42:47');
INSERT INTO `sys_chat_group_member` VALUES (5, 2, 2, NULL, 0, 0, '2026-01-31 12:42:47');
INSERT INTO `sys_chat_group_member` VALUES (6, 2, 3, NULL, 0, 0, '2026-01-31 12:42:47');
INSERT INTO `sys_chat_group_member` VALUES (7, 3, 1, NULL, 2, 0, '2026-01-31 23:20:05');
INSERT INTO `sys_chat_group_member` VALUES (8, 3, 2, NULL, 0, 0, '2026-01-31 23:20:05');
INSERT INTO `sys_chat_group_member` VALUES (9, 3, 3, NULL, 0, 0, '2026-01-31 23:20:05');
INSERT INTO `sys_chat_group_member` VALUES (10, 3, 4, NULL, 0, 0, '2026-01-31 23:20:05');
INSERT INTO `sys_chat_group_member` VALUES (11, 3, 5, NULL, 0, 0, '2026-01-31 23:20:05');

-- ----------------------------
-- Table structure for sys_chat_group_message
-- ----------------------------
DROP TABLE IF EXISTS `sys_chat_group_message`;
CREATE TABLE `sys_chat_group_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `group_id` bigint NOT NULL COMMENT '群ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `sender_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者名称',
  `sender_avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者头像',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `msg_type` tinyint NULL DEFAULT 1 COMMENT '消息类型：1-文本 2-图片 3-文件 4-系统消息',
  `send_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_group_id`(`group_id` ASC) USING BTREE,
  INDEX `idx_send_time`(`send_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 111 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_chat_group_message
-- ----------------------------
INSERT INTO `sys_chat_group_message` VALUES (1, 1, 0, '系统消息', NULL, 'test 创建了群聊', 4, '2026-01-31 11:24:29');
INSERT INTO `sys_chat_group_message` VALUES (2, 1, 2, 'test', NULL, '你好', 1, '2026-01-31 11:24:35');
INSERT INTO `sys_chat_group_message` VALUES (3, 1, 1, '超级管理员', NULL, '1111', 1, '2026-01-31 11:24:49');
INSERT INTO `sys_chat_group_message` VALUES (4, 1, 2, 'test', NULL, '1212', 1, '2026-01-31 11:24:51');
INSERT INTO `sys_chat_group_message` VALUES (5, 1, 2, 'test', NULL, '哈哈哈', 1, '2026-01-31 11:24:57');
INSERT INTO `sys_chat_group_message` VALUES (6, 1, 1, '超级管理员', NULL, '牛逼的额啊', 1, '2026-01-31 11:25:03');
INSERT INTO `sys_chat_group_message` VALUES (7, 1, 2, 'test', NULL, '111', 1, '2026-01-31 11:25:13');
INSERT INTO `sys_chat_group_message` VALUES (8, 1, 1, '超级管理员', NULL, '112', 1, '2026-01-31 11:25:17');
INSERT INTO `sys_chat_group_message` VALUES (9, 1, 2, 'test', NULL, '哈哈哈', 1, '2026-01-31 11:25:21');
INSERT INTO `sys_chat_group_message` VALUES (10, 1, 2, 'test', NULL, '牛逼', 1, '2026-01-31 11:25:24');
INSERT INTO `sys_chat_group_message` VALUES (11, 1, 1, '超级管理员', NULL, '老牛逼', 1, '2026-01-31 11:25:27');
INSERT INTO `sys_chat_group_message` VALUES (12, 1, 2, 'test', NULL, '你是', 1, '2026-01-31 11:25:30');
INSERT INTO `sys_chat_group_message` VALUES (13, 1, 1, '超级管理员', NULL, '？？？', 1, '2026-01-31 11:25:34');
INSERT INTO `sys_chat_group_message` VALUES (14, 1, 1, '超级管理员', NULL, '傻逼的吧', 1, '2026-01-31 11:25:37');
INSERT INTO `sys_chat_group_message` VALUES (15, 1, 1, '超级管理员', NULL, '111', 1, '2026-01-31 12:26:07');
INSERT INTO `sys_chat_group_message` VALUES (16, 1, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:26:08');
INSERT INTO `sys_chat_group_message` VALUES (17, 1, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:26:08');
INSERT INTO `sys_chat_group_message` VALUES (18, 1, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:26:08');
INSERT INTO `sys_chat_group_message` VALUES (19, 1, 2, 'test', NULL, '121212', 1, '2026-01-31 12:26:13');
INSERT INTO `sys_chat_group_message` VALUES (20, 1, 1, '超级管理员', NULL, '222', 1, '2026-01-31 12:26:15');
INSERT INTO `sys_chat_group_message` VALUES (21, 1, 2, 'test', NULL, '牛逼啊', 1, '2026-01-31 12:26:19');
INSERT INTO `sys_chat_group_message` VALUES (22, 1, 1, '超级管理员', NULL, '111', 1, '2026-01-31 12:26:21');
INSERT INTO `sys_chat_group_message` VALUES (23, 1, 1, '超级管理员', NULL, 'http://localhost:8080/file/images/2026/01/31/1b4f6f9062ab400f9259bdac0eb8db10.jpg', 2, '2026-01-31 12:26:52');
INSERT INTO `sys_chat_group_message` VALUES (24, 1, 2, 'test', NULL, '111', 1, '2026-01-31 12:26:58');
INSERT INTO `sys_chat_group_message` VALUES (25, 1, 1, '超级管理员', NULL, 'http://localhost:8080/file/images/2026/01/31/788f706ff0a548c0a4b744de208e73a5.jpg', 2, '2026-01-31 12:27:03');
INSERT INTO `sys_chat_group_message` VALUES (26, 1, 1, '超级管理员', NULL, '😊', 1, '2026-01-31 12:29:07');
INSERT INTO `sys_chat_group_message` VALUES (27, 1, 2, 'test', NULL, '22', 1, '2026-01-31 12:29:17');
INSERT INTO `sys_chat_group_message` VALUES (28, 1, 2, 'test', NULL, '22', 1, '2026-01-31 12:29:24');
INSERT INTO `sys_chat_group_message` VALUES (29, 1, 1, '超级管理员', NULL, '22222', 1, '2026-01-31 12:38:58');
INSERT INTO `sys_chat_group_message` VALUES (30, 1, 2, 'test', NULL, '哈哈哈', 1, '2026-01-31 12:39:05');
INSERT INTO `sys_chat_group_message` VALUES (31, 1, 1, '超级管理员', NULL, '牛逼啊', 1, '2026-01-31 12:39:10');
INSERT INTO `sys_chat_group_message` VALUES (32, 1, 2, 'test', NULL, '111', 1, '2026-01-31 12:39:26');
INSERT INTO `sys_chat_group_message` VALUES (33, 2, 0, '系统消息', NULL, '超级管理员 创建了群聊', 4, '2026-01-31 12:42:48');
INSERT INTO `sys_chat_group_message` VALUES (34, 2, 1, '超级管理员', NULL, '1111', 1, '2026-01-31 12:42:51');
INSERT INTO `sys_chat_group_message` VALUES (35, 2, 2, 'test', NULL, '牛逼啊', 1, '2026-01-31 12:43:00');
INSERT INTO `sys_chat_group_message` VALUES (36, 2, 1, '超级管理员', NULL, '111', 1, '2026-01-31 12:43:03');
INSERT INTO `sys_chat_group_message` VALUES (37, 2, 2, 'test', NULL, '12', 1, '2026-01-31 12:43:04');
INSERT INTO `sys_chat_group_message` VALUES (38, 2, 2, 'test', NULL, '2', 1, '2026-01-31 12:43:05');
INSERT INTO `sys_chat_group_message` VALUES (39, 2, 2, 'test', NULL, '1', 1, '2026-01-31 12:43:05');
INSERT INTO `sys_chat_group_message` VALUES (40, 2, 2, 'test', NULL, '21', 1, '2026-01-31 12:43:05');
INSERT INTO `sys_chat_group_message` VALUES (41, 2, 2, 'test', NULL, '2', 1, '2026-01-31 12:43:05');
INSERT INTO `sys_chat_group_message` VALUES (42, 2, 2, 'test', NULL, '12', 1, '2026-01-31 12:43:05');
INSERT INTO `sys_chat_group_message` VALUES (43, 2, 1, '超级管理员', NULL, '12', 1, '2026-01-31 12:43:07');
INSERT INTO `sys_chat_group_message` VALUES (44, 2, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:43:07');
INSERT INTO `sys_chat_group_message` VALUES (45, 2, 1, '超级管理员', NULL, '3', 1, '2026-01-31 12:43:07');
INSERT INTO `sys_chat_group_message` VALUES (46, 2, 1, '超级管理员', NULL, '2', 1, '2026-01-31 12:43:07');
INSERT INTO `sys_chat_group_message` VALUES (47, 2, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:43:07');
INSERT INTO `sys_chat_group_message` VALUES (48, 2, 1, '超级管理员', NULL, '2', 1, '2026-01-31 12:43:08');
INSERT INTO `sys_chat_group_message` VALUES (49, 2, 1, '超级管理员', NULL, '12', 1, '2026-01-31 12:43:08');
INSERT INTO `sys_chat_group_message` VALUES (50, 2, 2, 'test', NULL, '1', 1, '2026-01-31 12:43:09');
INSERT INTO `sys_chat_group_message` VALUES (51, 2, 2, 'test', NULL, '21', 1, '2026-01-31 12:43:09');
INSERT INTO `sys_chat_group_message` VALUES (52, 2, 2, 'test', NULL, '2', 1, '2026-01-31 12:43:09');
INSERT INTO `sys_chat_group_message` VALUES (53, 2, 2, 'test', NULL, '1', 1, '2026-01-31 12:43:09');
INSERT INTO `sys_chat_group_message` VALUES (54, 2, 2, 'test', NULL, '3', 1, '2026-01-31 12:43:10');
INSERT INTO `sys_chat_group_message` VALUES (55, 2, 2, 'test', NULL, '12', 1, '2026-01-31 12:43:10');
INSERT INTO `sys_chat_group_message` VALUES (56, 2, 1, '超级管理员', NULL, '可以的', 1, '2026-01-31 12:43:29');
INSERT INTO `sys_chat_group_message` VALUES (57, 2, 2, 'test', NULL, '好用的额', 1, '2026-01-31 12:43:32');
INSERT INTO `sys_chat_group_message` VALUES (58, 2, 1, '超级管理员', NULL, '111', 1, '2026-01-31 12:44:53');
INSERT INTO `sys_chat_group_message` VALUES (59, 2, 1, '超级管理员', NULL, '111', 1, '2026-01-31 12:45:00');
INSERT INTO `sys_chat_group_message` VALUES (60, 2, 1, '超级管理员', NULL, '222', 1, '2026-01-31 12:45:13');
INSERT INTO `sys_chat_group_message` VALUES (61, 2, 2, 'test', NULL, '111', 1, '2026-01-31 12:51:12');
INSERT INTO `sys_chat_group_message` VALUES (62, 2, 1, '超级管理员', NULL, '1111111', 1, '2026-01-31 12:51:16');
INSERT INTO `sys_chat_group_message` VALUES (63, 2, 2, 'test', NULL, '66666666', 1, '2026-01-31 12:51:20');
INSERT INTO `sys_chat_group_message` VALUES (64, 2, 1, '超级管理员', NULL, '111', 1, '2026-01-31 12:52:08');
INSERT INTO `sys_chat_group_message` VALUES (65, 2, 1, '超级管理员', NULL, '212', 1, '2026-01-31 12:52:09');
INSERT INTO `sys_chat_group_message` VALUES (66, 2, 1, '超级管理员', NULL, '2', 1, '2026-01-31 12:52:09');
INSERT INTO `sys_chat_group_message` VALUES (67, 2, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:52:09');
INSERT INTO `sys_chat_group_message` VALUES (68, 2, 1, '超级管理员', NULL, '21', 1, '2026-01-31 12:52:10');
INSERT INTO `sys_chat_group_message` VALUES (69, 2, 1, '超级管理员', NULL, '2', 1, '2026-01-31 12:52:10');
INSERT INTO `sys_chat_group_message` VALUES (70, 2, 1, '超级管理员', NULL, '1111111111', 1, '2026-01-31 12:52:26');
INSERT INTO `sys_chat_group_message` VALUES (71, 2, 1, '超级管理员', NULL, '222', 1, '2026-01-31 12:52:30');
INSERT INTO `sys_chat_group_message` VALUES (72, 2, 1, '超级管理员', NULL, '111111111', 1, '2026-01-31 12:53:21');
INSERT INTO `sys_chat_group_message` VALUES (73, 2, 1, '超级管理员', NULL, '1212', 1, '2026-01-31 12:53:23');
INSERT INTO `sys_chat_group_message` VALUES (74, 2, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:53:23');
INSERT INTO `sys_chat_group_message` VALUES (75, 2, 1, '超级管理员', NULL, '2', 1, '2026-01-31 12:53:24');
INSERT INTO `sys_chat_group_message` VALUES (76, 2, 1, '超级管理员', NULL, '12', 1, '2026-01-31 12:53:24');
INSERT INTO `sys_chat_group_message` VALUES (77, 2, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:53:24');
INSERT INTO `sys_chat_group_message` VALUES (78, 2, 1, '超级管理员', NULL, '2', 1, '2026-01-31 12:53:25');
INSERT INTO `sys_chat_group_message` VALUES (79, 2, 1, '超级管理员', NULL, '12', 1, '2026-01-31 12:53:25');
INSERT INTO `sys_chat_group_message` VALUES (80, 2, 1, '超级管理员', NULL, '12', 1, '2026-01-31 12:53:26');
INSERT INTO `sys_chat_group_message` VALUES (81, 2, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:53:26');
INSERT INTO `sys_chat_group_message` VALUES (82, 2, 1, '超级管理员', NULL, '2', 1, '2026-01-31 12:53:26');
INSERT INTO `sys_chat_group_message` VALUES (83, 2, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:53:26');
INSERT INTO `sys_chat_group_message` VALUES (84, 2, 1, '超级管理员', NULL, '21', 1, '2026-01-31 12:53:26');
INSERT INTO `sys_chat_group_message` VALUES (85, 2, 1, '超级管理员', NULL, '2', 1, '2026-01-31 12:53:26');
INSERT INTO `sys_chat_group_message` VALUES (86, 2, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:53:27');
INSERT INTO `sys_chat_group_message` VALUES (87, 2, 1, '超级管理员', NULL, '2', 1, '2026-01-31 12:53:27');
INSERT INTO `sys_chat_group_message` VALUES (88, 2, 1, '超级管理员', NULL, '1', 1, '2026-01-31 12:53:27');
INSERT INTO `sys_chat_group_message` VALUES (89, 2, 1, '超级管理员', NULL, '2', 1, '2026-01-31 12:53:27');
INSERT INTO `sys_chat_group_message` VALUES (90, 2, 1, '超级管理员', NULL, '12', 1, '2026-01-31 12:53:27');
INSERT INTO `sys_chat_group_message` VALUES (91, 2, 2, 'test', NULL, '1212', 1, '2026-01-31 12:53:30');
INSERT INTO `sys_chat_group_message` VALUES (92, 2, 2, 'test', NULL, '12', 1, '2026-01-31 12:53:31');
INSERT INTO `sys_chat_group_message` VALUES (93, 2, 2, 'test', NULL, '12', 1, '2026-01-31 12:53:31');
INSERT INTO `sys_chat_group_message` VALUES (94, 2, 2, 'test', NULL, '12', 1, '2026-01-31 12:53:31');
INSERT INTO `sys_chat_group_message` VALUES (95, 2, 3, 'mars', NULL, '111', 1, '2026-01-31 13:54:46');
INSERT INTO `sys_chat_group_message` VALUES (96, 2, 3, 'mars', NULL, '11', 1, '2026-01-31 13:54:48');
INSERT INTO `sys_chat_group_message` VALUES (97, 2, 3, 'mars', NULL, '12', 1, '2026-01-31 13:54:49');
INSERT INTO `sys_chat_group_message` VALUES (98, 2, 3, 'mars', NULL, '12', 1, '2026-01-31 13:54:49');
INSERT INTO `sys_chat_group_message` VALUES (99, 2, 3, 'mars', NULL, '1', 1, '2026-01-31 13:54:49');
INSERT INTO `sys_chat_group_message` VALUES (100, 3, 0, '系统消息', NULL, '超级管理员 创建了群聊', 4, '2026-01-31 23:20:06');
INSERT INTO `sys_chat_group_message` VALUES (101, 3, 1, '超级管理员', NULL, '1111', 1, '2026-01-31 23:20:08');
INSERT INTO `sys_chat_group_message` VALUES (102, 3, 4, 'lisi', NULL, 'hahah', 1, '2026-01-31 23:20:20');
INSERT INTO `sys_chat_group_message` VALUES (103, 3, 4, 'lisi', NULL, '111', 1, '2026-01-31 23:20:28');
INSERT INTO `sys_chat_group_message` VALUES (104, 3, 1, '超级管理员', NULL, '牛逼的', 1, '2026-01-31 23:20:34');
INSERT INTO `sys_chat_group_message` VALUES (105, 3, 4, 'lisi', NULL, '11212', 1, '2026-01-31 23:20:37');
INSERT INTO `sys_chat_group_message` VALUES (106, 3, 4, 'lisi', NULL, '1', 1, '2026-01-31 23:20:37');
INSERT INTO `sys_chat_group_message` VALUES (107, 3, 4, 'lisi', NULL, '12', 1, '2026-01-31 23:20:37');
INSERT INTO `sys_chat_group_message` VALUES (108, 3, 4, 'lisi', NULL, '1', 1, '2026-01-31 23:20:37');
INSERT INTO `sys_chat_group_message` VALUES (109, 2, 1, '超级管理员', NULL, '哈哈哈', 1, '2026-02-07 09:54:05');
INSERT INTO `sys_chat_group_message` VALUES (110, 2, 3, 'mars', NULL, '有点东西', 1, '2026-02-07 09:54:17');

-- ----------------------------
-- Table structure for sys_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `sys_chat_message`;
CREATE TABLE `sys_chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `sender_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者名称',
  `sender_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者头像',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID(0表示群发)',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容',
  `msg_type` tinyint NULL DEFAULT 1 COMMENT '消息类型(1文本 2图片 3文件)',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '是否已读(0未读 1已读)',
  `send_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sender_id`(`sender_id` ASC) USING BTREE,
  INDEX `idx_receiver_id`(`receiver_id` ASC) USING BTREE,
  INDEX `idx_send_time`(`send_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 487 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_chat_message
-- ----------------------------
INSERT INTO `sys_chat_message` VALUES (137, 1, '超级管理员', NULL, 3, '111', 1, 1, '2026-01-31 11:09:21');
INSERT INTO `sys_chat_message` VALUES (216, 1, '超级管理员', NULL, 2, '111', 1, 1, '2026-01-31 12:50:28');
INSERT INTO `sys_chat_message` VALUES (217, 2, 'test', NULL, 1, '1111', 1, 1, '2026-01-31 12:50:35');
INSERT INTO `sys_chat_message` VALUES (218, 2, 'test', NULL, 1, '222', 1, 1, '2026-01-31 12:50:39');
INSERT INTO `sys_chat_message` VALUES (219, 1, '超级管理员', NULL, 2, '1111', 1, 1, '2026-01-31 12:50:41');
INSERT INTO `sys_chat_message` VALUES (220, 1, '超级管理员', NULL, 2, '222', 1, 1, '2026-01-31 12:50:46');
INSERT INTO `sys_chat_message` VALUES (221, 1, '超级管理员', NULL, 2, '222', 1, 1, '2026-01-31 12:51:07');
INSERT INTO `sys_chat_message` VALUES (222, 2, 'test', NULL, 1, '2222222222', 1, 1, '2026-01-31 12:53:37');
INSERT INTO `sys_chat_message` VALUES (223, 1, '超级管理员', NULL, 2, '哈哈哈', 1, 1, '2026-01-31 12:53:44');
INSERT INTO `sys_chat_message` VALUES (224, 1, '超级管理员', NULL, 2, '牛逼的', 1, 1, '2026-01-31 12:53:45');
INSERT INTO `sys_chat_message` VALUES (225, 2, 'test', NULL, 1, '哈哈哈', 1, 1, '2026-01-31 12:54:24');
INSERT INTO `sys_chat_message` VALUES (226, 2, 'test', NULL, 1, '牛逼的额啊', 1, 1, '2026-01-31 12:54:26');
INSERT INTO `sys_chat_message` VALUES (227, 1, '超级管理员', NULL, 2, '11', 1, 1, '2026-01-31 12:55:04');
INSERT INTO `sys_chat_message` VALUES (228, 2, 'test', NULL, 1, '111', 1, 1, '2026-01-31 13:48:21');
INSERT INTO `sys_chat_message` VALUES (229, 1, '超级管理员', NULL, 2, '1212', 1, 1, '2026-01-31 13:48:25');
INSERT INTO `sys_chat_message` VALUES (230, 1, '超级管理员', NULL, 2, '1212', 1, 1, '2026-01-31 13:48:37');
INSERT INTO `sys_chat_message` VALUES (231, 2, 'test', NULL, 1, '222', 1, 1, '2026-01-31 13:49:04');
INSERT INTO `sys_chat_message` VALUES (232, 1, '超级管理员', NULL, 2, '1111111', 1, 1, '2026-01-31 13:49:07');
INSERT INTO `sys_chat_message` VALUES (233, 2, 'test', NULL, 1, '121', 1, 1, '2026-01-31 13:49:08');
INSERT INTO `sys_chat_message` VALUES (234, 1, '超级管理员', NULL, 2, '1111111', 1, 1, '2026-01-31 13:49:11');
INSERT INTO `sys_chat_message` VALUES (235, 2, 'test', NULL, 1, '222', 1, 1, '2026-01-31 13:49:12');
INSERT INTO `sys_chat_message` VALUES (236, 1, '超级管理员', NULL, 2, '222222222222', 1, 1, '2026-01-31 13:49:14');
INSERT INTO `sys_chat_message` VALUES (237, 2, 'test', NULL, 1, '11111', 1, 1, '2026-01-31 13:52:30');
INSERT INTO `sys_chat_message` VALUES (238, 1, '超级管理员', NULL, 2, '111', 1, 1, '2026-01-31 13:52:37');
INSERT INTO `sys_chat_message` VALUES (239, 1, '超级管理员', NULL, 2, '22222222222', 1, 1, '2026-01-31 13:52:52');
INSERT INTO `sys_chat_message` VALUES (240, 2, 'test', NULL, 1, '牛逼的', 1, 1, '2026-01-31 13:52:55');
INSERT INTO `sys_chat_message` VALUES (241, 1, '超级管理员', NULL, 2, '哈哈哈', 1, 1, '2026-01-31 13:52:58');
INSERT INTO `sys_chat_message` VALUES (242, 3, 'mars', NULL, 1, '222', 1, 1, '2026-01-31 13:54:24');
INSERT INTO `sys_chat_message` VALUES (243, 3, 'mars', NULL, 2, '1111', 1, 1, '2026-01-31 13:54:27');
INSERT INTO `sys_chat_message` VALUES (244, 1, '超级管理员', NULL, 3, 'nihao', 1, 1, '2026-01-31 13:54:35');
INSERT INTO `sys_chat_message` VALUES (245, 1, '超级管理员', NULL, 3, '11', 1, 1, '2026-01-31 13:55:37');
INSERT INTO `sys_chat_message` VALUES (246, 3, 'mars', NULL, 1, '1212', 1, 1, '2026-01-31 13:55:45');
INSERT INTO `sys_chat_message` VALUES (247, 1, '超级管理员', NULL, 2, '111', 1, 1, '2026-01-31 13:57:32');
INSERT INTO `sys_chat_message` VALUES (248, 1, '超级管理员', NULL, 2, '111', 1, 1, '2026-01-31 13:57:41');
INSERT INTO `sys_chat_message` VALUES (249, 2, 'test', NULL, 1, '1212', 1, 1, '2026-01-31 13:57:52');
INSERT INTO `sys_chat_message` VALUES (250, 1, '超级管理员', NULL, 2, '2222', 1, 1, '2026-01-31 13:57:55');
INSERT INTO `sys_chat_message` VALUES (251, 2, 'test', NULL, 1, '222', 1, 1, '2026-01-31 14:00:09');
INSERT INTO `sys_chat_message` VALUES (252, 2, 'test', NULL, 1, '1212', 1, 1, '2026-01-31 14:00:18');
INSERT INTO `sys_chat_message` VALUES (253, 2, 'test', NULL, 1, '2222222', 1, 1, '2026-01-31 14:00:21');
INSERT INTO `sys_chat_message` VALUES (254, 2, 'test', NULL, 1, '111111111111111111111', 1, 1, '2026-01-31 14:00:25');
INSERT INTO `sys_chat_message` VALUES (255, 1, '超级管理员', NULL, 2, '22222222222', 1, 1, '2026-01-31 14:00:30');
INSERT INTO `sys_chat_message` VALUES (256, 1, '超级管理员', NULL, 2, '1111111111111', 1, 0, '2026-01-31 14:00:38');
INSERT INTO `sys_chat_message` VALUES (257, 1, '超级管理员', NULL, 2, '1212', 1, 0, '2026-01-31 14:00:41');
INSERT INTO `sys_chat_message` VALUES (258, 1, '超级管理员', NULL, 2, '121312', 1, 0, '2026-01-31 14:00:43');
INSERT INTO `sys_chat_message` VALUES (259, 1, '超级管理员', NULL, 2, '121212', 1, 0, '2026-01-31 14:00:47');
INSERT INTO `sys_chat_message` VALUES (260, 1, '超级管理员', NULL, 2, '121', 1, 0, '2026-01-31 14:00:47');
INSERT INTO `sys_chat_message` VALUES (261, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:47');
INSERT INTO `sys_chat_message` VALUES (262, 1, '超级管理员', NULL, 2, '12', 1, 0, '2026-01-31 14:00:48');
INSERT INTO `sys_chat_message` VALUES (263, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:48');
INSERT INTO `sys_chat_message` VALUES (264, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:48');
INSERT INTO `sys_chat_message` VALUES (265, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:48');
INSERT INTO `sys_chat_message` VALUES (266, 1, '超级管理员', NULL, 2, '21', 1, 0, '2026-01-31 14:00:48');
INSERT INTO `sys_chat_message` VALUES (267, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:49');
INSERT INTO `sys_chat_message` VALUES (268, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:49');
INSERT INTO `sys_chat_message` VALUES (269, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:49');
INSERT INTO `sys_chat_message` VALUES (270, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:49');
INSERT INTO `sys_chat_message` VALUES (271, 1, '超级管理员', NULL, 2, '21', 1, 0, '2026-01-31 14:00:49');
INSERT INTO `sys_chat_message` VALUES (272, 1, '超级管理员', NULL, 2, '3', 1, 0, '2026-01-31 14:00:49');
INSERT INTO `sys_chat_message` VALUES (273, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:50');
INSERT INTO `sys_chat_message` VALUES (274, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:50');
INSERT INTO `sys_chat_message` VALUES (275, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:50');
INSERT INTO `sys_chat_message` VALUES (276, 1, '超级管理员', NULL, 2, '21', 1, 0, '2026-01-31 14:00:50');
INSERT INTO `sys_chat_message` VALUES (277, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:50');
INSERT INTO `sys_chat_message` VALUES (278, 1, '超级管理员', NULL, 2, '12', 1, 0, '2026-01-31 14:00:51');
INSERT INTO `sys_chat_message` VALUES (279, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:51');
INSERT INTO `sys_chat_message` VALUES (280, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:51');
INSERT INTO `sys_chat_message` VALUES (281, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:51');
INSERT INTO `sys_chat_message` VALUES (282, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:51');
INSERT INTO `sys_chat_message` VALUES (283, 1, '超级管理员', NULL, 2, '3', 1, 0, '2026-01-31 14:00:52');
INSERT INTO `sys_chat_message` VALUES (284, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:52');
INSERT INTO `sys_chat_message` VALUES (285, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:52');
INSERT INTO `sys_chat_message` VALUES (286, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:52');
INSERT INTO `sys_chat_message` VALUES (287, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:52');
INSERT INTO `sys_chat_message` VALUES (288, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:52');
INSERT INTO `sys_chat_message` VALUES (289, 1, '超级管理员', NULL, 2, '121', 1, 0, '2026-01-31 14:00:53');
INSERT INTO `sys_chat_message` VALUES (290, 1, '超级管理员', NULL, 2, '3', 1, 0, '2026-01-31 14:00:53');
INSERT INTO `sys_chat_message` VALUES (291, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:53');
INSERT INTO `sys_chat_message` VALUES (292, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:53');
INSERT INTO `sys_chat_message` VALUES (293, 1, '超级管理员', NULL, 2, '12', 1, 0, '2026-01-31 14:00:54');
INSERT INTO `sys_chat_message` VALUES (294, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:54');
INSERT INTO `sys_chat_message` VALUES (295, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:54');
INSERT INTO `sys_chat_message` VALUES (296, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:54');
INSERT INTO `sys_chat_message` VALUES (297, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:54');
INSERT INTO `sys_chat_message` VALUES (298, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:55');
INSERT INTO `sys_chat_message` VALUES (299, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:55');
INSERT INTO `sys_chat_message` VALUES (300, 1, '超级管理员', NULL, 2, '21', 1, 0, '2026-01-31 14:00:55');
INSERT INTO `sys_chat_message` VALUES (301, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:55');
INSERT INTO `sys_chat_message` VALUES (302, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:55');
INSERT INTO `sys_chat_message` VALUES (303, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:55');
INSERT INTO `sys_chat_message` VALUES (304, 1, '超级管理员', NULL, 2, '13', 1, 0, '2026-01-31 14:00:56');
INSERT INTO `sys_chat_message` VALUES (305, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:56');
INSERT INTO `sys_chat_message` VALUES (306, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:56');
INSERT INTO `sys_chat_message` VALUES (307, 1, '超级管理员', NULL, 2, '12', 1, 0, '2026-01-31 14:00:56');
INSERT INTO `sys_chat_message` VALUES (308, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:56');
INSERT INTO `sys_chat_message` VALUES (309, 1, '超级管理员', NULL, 2, '12', 1, 0, '2026-01-31 14:00:57');
INSERT INTO `sys_chat_message` VALUES (310, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:57');
INSERT INTO `sys_chat_message` VALUES (311, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:57');
INSERT INTO `sys_chat_message` VALUES (312, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:57');
INSERT INTO `sys_chat_message` VALUES (313, 1, '超级管理员', NULL, 2, '12', 1, 0, '2026-01-31 14:00:58');
INSERT INTO `sys_chat_message` VALUES (314, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:58');
INSERT INTO `sys_chat_message` VALUES (315, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:58');
INSERT INTO `sys_chat_message` VALUES (316, 1, '超级管理员', NULL, 2, '12', 1, 0, '2026-01-31 14:00:58');
INSERT INTO `sys_chat_message` VALUES (317, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:59');
INSERT INTO `sys_chat_message` VALUES (318, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:59');
INSERT INTO `sys_chat_message` VALUES (319, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:00:59');
INSERT INTO `sys_chat_message` VALUES (320, 1, '超级管理员', NULL, 2, '31', 1, 0, '2026-01-31 14:00:59');
INSERT INTO `sys_chat_message` VALUES (321, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:00:59');
INSERT INTO `sys_chat_message` VALUES (322, 1, '超级管理员', NULL, 2, '12', 1, 0, '2026-01-31 14:01:00');
INSERT INTO `sys_chat_message` VALUES (323, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:01:00');
INSERT INTO `sys_chat_message` VALUES (324, 1, '超级管理员', NULL, 2, '3', 1, 0, '2026-01-31 14:01:00');
INSERT INTO `sys_chat_message` VALUES (325, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:01:00');
INSERT INTO `sys_chat_message` VALUES (326, 1, '超级管理员', NULL, 2, '21', 1, 0, '2026-01-31 14:01:00');
INSERT INTO `sys_chat_message` VALUES (327, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:01:01');
INSERT INTO `sys_chat_message` VALUES (328, 1, '超级管理员', NULL, 2, '12', 1, 0, '2026-01-31 14:01:03');
INSERT INTO `sys_chat_message` VALUES (329, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:01:03');
INSERT INTO `sys_chat_message` VALUES (330, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:01:04');
INSERT INTO `sys_chat_message` VALUES (331, 1, '超级管理员', NULL, 2, '2', 1, 0, '2026-01-31 14:01:04');
INSERT INTO `sys_chat_message` VALUES (332, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:01:04');
INSERT INTO `sys_chat_message` VALUES (333, 1, '超级管理员', NULL, 2, '1', 1, 0, '2026-01-31 14:01:04');
INSERT INTO `sys_chat_message` VALUES (334, 1, '超级管理员', NULL, 2, '3', 1, 0, '2026-01-31 14:01:04');
INSERT INTO `sys_chat_message` VALUES (335, 1, '超级管理员', NULL, 2, '12', 1, 0, '2026-01-31 14:01:05');
INSERT INTO `sys_chat_message` VALUES (336, 1, '超级管理员', NULL, 2, '121', 1, 0, '2026-01-31 14:01:05');
INSERT INTO `sys_chat_message` VALUES (337, 1, '超级管理员', NULL, 2, '111', 1, 0, '2026-01-31 14:08:18');
INSERT INTO `sys_chat_message` VALUES (338, 1, '超级管理员', NULL, 2, '1212', 1, 0, '2026-01-31 14:24:15');
INSERT INTO `sys_chat_message` VALUES (339, 1, '超级管理员', NULL, 2, '1212', 1, 0, '2026-01-31 14:25:30');
INSERT INTO `sys_chat_message` VALUES (340, 1, '超级管理员', NULL, 3, '111', 1, 1, '2026-01-31 14:31:43');
INSERT INTO `sys_chat_message` VALUES (341, 3, 'mars', NULL, 1, '222', 1, 1, '2026-01-31 14:31:48');
INSERT INTO `sys_chat_message` VALUES (342, 1, '超级管理员', NULL, 2, '你好', 1, 0, '2026-01-31 20:48:31');
INSERT INTO `sys_chat_message` VALUES (343, 1, '超级管理员', NULL, 4, '11', 1, 1, '2026-01-31 21:37:40');
INSERT INTO `sys_chat_message` VALUES (344, 4, 'lisi', NULL, 1, 'nihao', 1, 1, '2026-01-31 21:37:46');
INSERT INTO `sys_chat_message` VALUES (345, 5, 'mars666', NULL, 2, '1212', 1, 0, '2026-01-31 22:31:32');
INSERT INTO `sys_chat_message` VALUES (346, 5, 'mars666', NULL, 1, '11', 1, 1, '2026-01-31 22:32:42');
INSERT INTO `sys_chat_message` VALUES (347, 1, '超级管理员', NULL, 4, '111', 1, 1, '2026-01-31 22:54:12');
INSERT INTO `sys_chat_message` VALUES (348, 1, '超级管理员', NULL, 4, '111', 1, 1, '2026-01-31 23:19:38');
INSERT INTO `sys_chat_message` VALUES (349, 1, '超级管理员', NULL, 4, '稍等，我确认一下', 1, 1, '2026-01-31 23:19:48');
INSERT INTO `sys_chat_message` VALUES (350, 1, '超级管理员', NULL, 4, '感谢你的反馈', 1, 1, '2026-01-31 23:19:52');
INSERT INTO `sys_chat_message` VALUES (351, 1, '超级管理员', NULL, 2, 'hahah', 1, 0, '2026-02-03 09:03:27');
INSERT INTO `sys_chat_message` VALUES (352, 1, '超级管理员', NULL, 2, '你好', 1, 0, '2026-02-03 09:03:29');
INSERT INTO `sys_chat_message` VALUES (353, 1, '超级管理员', NULL, 2, '你好', 1, 0, '2026-02-07 09:53:11');
INSERT INTO `sys_chat_message` VALUES (354, 3, 'mars', NULL, 1, '1', 1, 1, '2026-02-07 09:53:42');
INSERT INTO `sys_chat_message` VALUES (355, 3, 'mars', NULL, 1, '11', 1, 1, '2026-02-07 09:53:43');
INSERT INTO `sys_chat_message` VALUES (356, 3, 'mars', NULL, 1, '2', 1, 1, '2026-02-07 09:53:44');
INSERT INTO `sys_chat_message` VALUES (357, 3, 'mars', NULL, 1, '12', 1, 1, '2026-02-07 09:53:50');
INSERT INTO `sys_chat_message` VALUES (358, 3, 'mars', NULL, 1, '13', 1, 1, '2026-02-07 09:53:50');
INSERT INTO `sys_chat_message` VALUES (359, 3, 'mars', NULL, 1, '12', 1, 1, '2026-02-07 09:53:51');
INSERT INTO `sys_chat_message` VALUES (360, 3, 'mars', NULL, 1, '3', 1, 1, '2026-02-07 09:53:51');
INSERT INTO `sys_chat_message` VALUES (361, 3, 'mars', NULL, 1, '12', 1, 1, '2026-02-07 09:53:51');
INSERT INTO `sys_chat_message` VALUES (362, 3, 'mars', NULL, 1, '3', 1, 1, '2026-02-07 09:53:52');
INSERT INTO `sys_chat_message` VALUES (363, 3, 'mars', NULL, 1, '1', 1, 1, '2026-02-07 09:53:52');
INSERT INTO `sys_chat_message` VALUES (364, 3, 'mars', NULL, 1, '2', 1, 1, '2026-02-07 09:53:52');
INSERT INTO `sys_chat_message` VALUES (365, 3, 'mars', NULL, 1, '1', 1, 1, '2026-02-07 09:53:52');
INSERT INTO `sys_chat_message` VALUES (366, 1, '超级管理员', NULL, 3, '222', 1, 1, '2026-02-07 09:53:56');
INSERT INTO `sys_chat_message` VALUES (367, 1, '超级管理员', NULL, 3, '22', 1, 1, '2026-02-07 22:16:53');
INSERT INTO `sys_chat_message` VALUES (368, 1, '超级管理员', NULL, 3, '12', 1, 1, '2026-02-07 22:16:53');
INSERT INTO `sys_chat_message` VALUES (369, 3, 'mars', NULL, 1, 'hahha', 1, 1, '2026-02-07 22:17:46');
INSERT INTO `sys_chat_message` VALUES (370, 1, '超级管理员', NULL, 3, '1', 1, 1, '2026-02-07 22:17:54');
INSERT INTO `sys_chat_message` VALUES (371, 1, '超级管理员', NULL, 3, '11', 1, 1, '2026-02-07 22:17:55');
INSERT INTO `sys_chat_message` VALUES (372, 3, 'mars', NULL, 1, '有点东西啊', 1, 1, '2026-02-07 22:18:07');
INSERT INTO `sys_chat_message` VALUES (373, 1, '超级管理员', NULL, 7, '111', 1, 0, '2026-02-08 14:46:57');
INSERT INTO `sys_chat_message` VALUES (374, 1, '超级管理员', NULL, 7, '222', 1, 0, '2026-02-08 14:47:09');
INSERT INTO `sys_chat_message` VALUES (375, 1, '超级管理员', NULL, 7, '222', 1, 0, '2026-02-08 14:49:30');
INSERT INTO `sys_chat_message` VALUES (376, 1, '超级管理员', NULL, 7, '222', 1, 0, '2026-02-08 14:55:04');
INSERT INTO `sys_chat_message` VALUES (377, 1, '超级管理员', NULL, 10, '111', 1, 1, '2026-02-08 17:24:04');
INSERT INTO `sys_chat_message` VALUES (378, 1, '超级管理员', NULL, 10, '222', 1, 1, '2026-02-08 17:24:16');
INSERT INTO `sys_chat_message` VALUES (379, 1, '超级管理员', NULL, 10, 'hahah', 1, 1, '2026-02-08 17:24:20');
INSERT INTO `sys_chat_message` VALUES (380, 1, '超级管理员', NULL, 10, '牛逼啊', 1, 1, '2026-02-08 17:24:25');
INSERT INTO `sys_chat_message` VALUES (381, 10, 'Mars', 'Zev+JAmwlem/Y8N5.X6N4DTKdz36zGco2W8Manbj77m9ul7TSvZyidbZVTm0VaX3cRc1bYlVtd4aF6xAgp7NLzMa5gUZtSbq3WbfQF64DbryX2gCjdQ5jctIsee8QluGv', 1, '你好', 1, 1, '2026-02-08 17:24:31');
INSERT INTO `sys_chat_message` VALUES (382, 10, 'Mars', 'Zev+JAmwlem/Y8N5.X6N4DTKdz36zGco2W8Manbj77m9ul7TSvZyidbZVTm0VaX3cRc1bYlVtd4aF6xAgp7NLzMa5gUZtSbq3WbfQF64DbryX2gCjdQ5jctIsee8QluGv', 1, '有点东西啊', 1, 1, '2026-02-08 17:24:39');
INSERT INTO `sys_chat_message` VALUES (383, 1, '超级管理员', NULL, 10, '11', 1, 1, '2026-02-08 17:24:44');
INSERT INTO `sys_chat_message` VALUES (384, 1, '超级管理员', NULL, 10, '哈哈哈', 1, 1, '2026-02-08 17:24:46');
INSERT INTO `sys_chat_message` VALUES (385, 1, '超级管理员', NULL, 10, '没我内天的', 1, 1, '2026-02-08 17:24:47');
INSERT INTO `sys_chat_message` VALUES (386, 1, '超级管理员', NULL, 10, '没问题啊', 1, 1, '2026-02-08 17:24:50');
INSERT INTO `sys_chat_message` VALUES (387, 1, '超级管理员', NULL, 10, '111', 1, 1, '2026-02-08 17:25:19');
INSERT INTO `sys_chat_message` VALUES (388, 1, '超级管理员', NULL, 10, '哈哈哈', 1, 1, '2026-02-08 17:25:22');
INSERT INTO `sys_chat_message` VALUES (389, 1, '超级管理员', NULL, 10, 'u屁懂啊吗动漫行', 1, 1, '2026-02-08 17:25:25');
INSERT INTO `sys_chat_message` VALUES (390, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:25:27');
INSERT INTO `sys_chat_message` VALUES (391, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:25:27');
INSERT INTO `sys_chat_message` VALUES (392, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:25:27');
INSERT INTO `sys_chat_message` VALUES (393, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:25:27');
INSERT INTO `sys_chat_message` VALUES (394, 1, '超级管理员', NULL, 10, '212', 1, 1, '2026-02-08 17:25:29');
INSERT INTO `sys_chat_message` VALUES (395, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:25:30');
INSERT INTO `sys_chat_message` VALUES (396, 1, '超级管理员', NULL, 10, '131', 1, 1, '2026-02-08 17:25:30');
INSERT INTO `sys_chat_message` VALUES (397, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 17:25:31');
INSERT INTO `sys_chat_message` VALUES (398, 1, '超级管理员', NULL, 10, '123123', 1, 1, '2026-02-08 17:25:32');
INSERT INTO `sys_chat_message` VALUES (399, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:25:32');
INSERT INTO `sys_chat_message` VALUES (400, 10, 'Mars', 'Zev+JAmwlem/Y8N5.X6N4DTKdz36zGco2W8Manbj77m9ul7TSvZyidbZVTm0VaX3cRc1bYlVtd4aF6xAgp7NLzMa5gUZtSbq3WbfQF64DbryX2gCjdQ5jctIsee8QluGv', 1, '哈哈哈', 1, 1, '2026-02-08 17:25:38');
INSERT INTO `sys_chat_message` VALUES (401, 10, 'Mars', 'Zev+JAmwlem/Y8N5.X6N4DTKdz36zGco2W8Manbj77m9ul7TSvZyidbZVTm0VaX3cRc1bYlVtd4aF6xAgp7NLzMa5gUZtSbq3WbfQF64DbryX2gCjdQ5jctIsee8QluGv', 1, '12', 1, 1, '2026-02-08 17:25:41');
INSERT INTO `sys_chat_message` VALUES (402, 10, 'Mars', 'Zev+JAmwlem/Y8N5.X6N4DTKdz36zGco2W8Manbj77m9ul7TSvZyidbZVTm0VaX3cRc1bYlVtd4aF6xAgp7NLzMa5gUZtSbq3WbfQF64DbryX2gCjdQ5jctIsee8QluGv', 1, '11', 1, 1, '2026-02-08 17:25:44');
INSERT INTO `sys_chat_message` VALUES (403, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:27:22');
INSERT INTO `sys_chat_message` VALUES (404, 1, '超级管理员', NULL, 10, '111', 1, 1, '2026-02-08 17:27:25');
INSERT INTO `sys_chat_message` VALUES (405, 1, '超级管理员', NULL, 10, '212', 1, 1, '2026-02-08 17:27:26');
INSERT INTO `sys_chat_message` VALUES (406, 1, '超级管理员', NULL, 10, '112', 1, 1, '2026-02-08 17:27:35');
INSERT INTO `sys_chat_message` VALUES (407, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 17:30:23');
INSERT INTO `sys_chat_message` VALUES (408, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:30:25');
INSERT INTO `sys_chat_message` VALUES (409, 1, '超级管理员', NULL, 10, '3', 1, 1, '2026-02-08 17:30:25');
INSERT INTO `sys_chat_message` VALUES (410, 1, '超级管理员', NULL, 10, '21', 1, 1, '2026-02-08 17:30:26');
INSERT INTO `sys_chat_message` VALUES (411, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:30:27');
INSERT INTO `sys_chat_message` VALUES (412, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:30:27');
INSERT INTO `sys_chat_message` VALUES (413, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:30:27');
INSERT INTO `sys_chat_message` VALUES (414, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:30:28');
INSERT INTO `sys_chat_message` VALUES (415, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 17:30:28');
INSERT INTO `sys_chat_message` VALUES (416, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:30:28');
INSERT INTO `sys_chat_message` VALUES (417, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 17:30:28');
INSERT INTO `sys_chat_message` VALUES (418, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:30:28');
INSERT INTO `sys_chat_message` VALUES (419, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 17:30:29');
INSERT INTO `sys_chat_message` VALUES (420, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:30:29');
INSERT INTO `sys_chat_message` VALUES (421, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 17:30:29');
INSERT INTO `sys_chat_message` VALUES (422, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:30:29');
INSERT INTO `sys_chat_message` VALUES (423, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 17:30:29');
INSERT INTO `sys_chat_message` VALUES (424, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:30:30');
INSERT INTO `sys_chat_message` VALUES (425, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 17:30:31');
INSERT INTO `sys_chat_message` VALUES (426, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:31:22');
INSERT INTO `sys_chat_message` VALUES (427, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:31:24');
INSERT INTO `sys_chat_message` VALUES (428, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:31:25');
INSERT INTO `sys_chat_message` VALUES (429, 10, 'Mars', 'http://localhost:8080/api/files/images/2026/02/08/fdc1626199204477ae2093aeedc7aec8.jpeg', 1, '哈哈哈', 1, 1, '2026-02-08 17:34:12');
INSERT INTO `sys_chat_message` VALUES (430, 10, 'Mars', 'http://localhost:8080/api/files/images/2026/02/08/fdc1626199204477ae2093aeedc7aec8.jpeg', 1, '有点东西', 1, 1, '2026-02-08 17:34:14');
INSERT INTO `sys_chat_message` VALUES (431, 1, '超级管理员', NULL, 10, '22', 1, 1, '2026-02-08 17:38:17');
INSERT INTO `sys_chat_message` VALUES (432, 10, 'Mars', 'http://localhost:8080/api/files/images/2026/02/08/fdc1626199204477ae2093aeedc7aec8.jpeg', 1, '111', 1, 1, '2026-02-08 17:46:43');
INSERT INTO `sys_chat_message` VALUES (433, 10, 'Mars', 'http://localhost:8080/api/files/images/2026/02/08/fdc1626199204477ae2093aeedc7aec8.jpeg', 1, '22', 1, 1, '2026-02-08 17:46:52');
INSERT INTO `sys_chat_message` VALUES (434, 1, '超级管理员', NULL, 10, '22', 1, 1, '2026-02-08 17:49:30');
INSERT INTO `sys_chat_message` VALUES (435, 1, '超级管理员', NULL, 10, '222', 1, 1, '2026-02-08 17:52:00');
INSERT INTO `sys_chat_message` VALUES (436, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:52:01');
INSERT INTO `sys_chat_message` VALUES (437, 1, '超级管理员', NULL, 10, '13', 1, 1, '2026-02-08 17:52:02');
INSERT INTO `sys_chat_message` VALUES (438, 1, '超级管理员', NULL, 10, '22', 1, 1, '2026-02-08 17:54:26');
INSERT INTO `sys_chat_message` VALUES (439, 1, '超级管理员', NULL, 10, '哈哈哈', 1, 1, '2026-02-08 17:54:29');
INSERT INTO `sys_chat_message` VALUES (440, 1, '超级管理员', NULL, 10, '有点东西', 1, 1, '2026-02-08 17:54:31');
INSERT INTO `sys_chat_message` VALUES (441, 10, 'Mars', 'http://localhost:8080/api/files/images/2026/02/08/fdc1626199204477ae2093aeedc7aec8.jpeg', 1, '牛逼的', 1, 1, '2026-02-08 17:54:39');
INSERT INTO `sys_chat_message` VALUES (442, 1, '超级管理员', NULL, 10, '哈哈哈', 1, 1, '2026-02-08 17:54:43');
INSERT INTO `sys_chat_message` VALUES (443, 1, '超级管理员', NULL, 10, '你好', 1, 1, '2026-02-08 17:54:47');
INSERT INTO `sys_chat_message` VALUES (444, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 17:55:01');
INSERT INTO `sys_chat_message` VALUES (445, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 17:55:03');
INSERT INTO `sys_chat_message` VALUES (446, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:55:04');
INSERT INTO `sys_chat_message` VALUES (447, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:55:04');
INSERT INTO `sys_chat_message` VALUES (448, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:55:04');
INSERT INTO `sys_chat_message` VALUES (449, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:55:04');
INSERT INTO `sys_chat_message` VALUES (450, 1, '超级管理员', NULL, 10, '111', 1, 1, '2026-02-08 17:55:08');
INSERT INTO `sys_chat_message` VALUES (451, 1, '超级管理员', NULL, 10, '11', 1, 1, '2026-02-08 17:55:15');
INSERT INTO `sys_chat_message` VALUES (452, 10, 'Mars', 'http://localhost:8080/api/files/images/2026/02/08/fdc1626199204477ae2093aeedc7aec8.jpeg', 1, '111', 1, 1, '2026-02-08 17:55:21');
INSERT INTO `sys_chat_message` VALUES (453, 1, '超级管理员', NULL, 10, '222', 1, 1, '2026-02-08 17:55:25');
INSERT INTO `sys_chat_message` VALUES (454, 1, '超级管理员', NULL, 10, '33', 1, 1, '2026-02-08 17:55:26');
INSERT INTO `sys_chat_message` VALUES (455, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 17:55:27');
INSERT INTO `sys_chat_message` VALUES (456, 10, 'Mars', 'http://localhost:8080/api/files/images/2026/02/08/fdc1626199204477ae2093aeedc7aec8.jpeg', 1, '11', 1, 1, '2026-02-08 17:55:31');
INSERT INTO `sys_chat_message` VALUES (457, 1, '超级管理员', NULL, 10, '222', 1, 1, '2026-02-08 17:55:35');
INSERT INTO `sys_chat_message` VALUES (458, 1, '超级管理员', NULL, 10, '222', 1, 1, '2026-02-08 18:03:07');
INSERT INTO `sys_chat_message` VALUES (459, 1, '超级管理员', NULL, 10, '111', 1, 1, '2026-02-08 18:03:15');
INSERT INTO `sys_chat_message` VALUES (460, 1, '超级管理员', NULL, 10, '222', 1, 1, '2026-02-08 18:05:16');
INSERT INTO `sys_chat_message` VALUES (461, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 18:05:19');
INSERT INTO `sys_chat_message` VALUES (462, 1, '超级管理员', NULL, 10, '121', 1, 1, '2026-02-08 18:05:20');
INSERT INTO `sys_chat_message` VALUES (463, 10, 'Mars', 'http://m8e8f9e2.natappfree.ccUwFJO7qRoTLVK2Dd.d4QaPhYpq2qtueEW3UevAMTiKfwtL/i8tzAcwK04/RUiPB1q5AS3pUmjxEcMIGGO6yLhVb8hZESA0pDM1ipQ0YIQS3+HAy8D333uNbXXb3rYz0Y=', 1, '哈哈哈', 1, 1, '2026-02-08 18:05:26');
INSERT INTO `sys_chat_message` VALUES (464, 10, 'Mars', 'http://m8e8f9e2.natappfree.ccUwFJO7qRoTLVK2Dd.d4QaPhYpq2qtueEW3UevAMTiKfwtL/i8tzAcwK04/RUiPB1q5AS3pUmjxEcMIGGO6yLhVb8hZESA0pDM1ipQ0YIQS3+HAy8D333uNbXXb3rYz0Y=', 1, '牛逼', 1, 1, '2026-02-08 18:05:36');
INSERT INTO `sys_chat_message` VALUES (465, 10, 'Mars', 'http://m8e8f9e2.natappfree.ccUwFJO7qRoTLVK2Dd.d4QaPhYpq2qtueEW3UevAMTiKfwtL/i8tzAcwK04/RUiPB1q5AS3pUmjxEcMIGGO6yLhVb8hZESA0pDM1ipQ0YIQS3+HAy8D333uNbXXb3rYz0Y=', 1, '🤗🤗🤔🤔🤔', 1, 1, '2026-02-08 18:05:39');
INSERT INTO `sys_chat_message` VALUES (466, 10, 'Mars', 'http://m8e8f9e2.natappfree.ccUwFJO7qRoTLVK2Dd.d4QaPhYpq2qtueEW3UevAMTiKfwtL/i8tzAcwK04/RUiPB1q5AS3pUmjxEcMIGGO6yLhVb8hZESA0pDM1ipQ0YIQS3+HAy8D333uNbXXb3rYz0Y=', 1, '李敏', 1, 1, '2026-02-08 18:05:44');
INSERT INTO `sys_chat_message` VALUES (467, 10, 'Mars', 'http://m8e8f9e2.natappfree.ccUwFJO7qRoTLVK2Dd.d4QaPhYpq2qtueEW3UevAMTiKfwtL/i8tzAcwK04/RUiPB1q5AS3pUmjxEcMIGGO6yLhVb8hZESA0pDM1ipQ0YIQS3+HAy8D333uNbXXb3rYz0Y=', 1, '啊哈哈哈', 1, 1, '2026-02-08 18:05:50');
INSERT INTO `sys_chat_message` VALUES (468, 10, 'Mars', 'http://m8e8f9e2.natappfree.ccUwFJO7qRoTLVK2Dd.d4QaPhYpq2qtueEW3UevAMTiKfwtL/i8tzAcwK04/RUiPB1q5AS3pUmjxEcMIGGO6yLhVb8hZESA0pDM1ipQ0YIQS3+HAy8D333uNbXXb3rYz0Y=', 1, '有点东西', 1, 1, '2026-02-08 18:05:58');
INSERT INTO `sys_chat_message` VALUES (469, 10, 'Mars', 'http://m8e8f9e2.natappfree.ccUwFJO7qRoTLVK2Dd.d4QaPhYpq2qtueEW3UevAMTiKfwtL/i8tzAcwK04/RUiPB1q5AS3pUmjxEcMIGGO6yLhVb8hZESA0pDM1ipQ0YIQS3+HAy8D333uNbXXb3rYz0Y=', 1, '牛逼', 1, 1, '2026-02-08 18:06:12');
INSERT INTO `sys_chat_message` VALUES (470, 10, 'Mars', 'http://m8e8f9e2.natappfree.ccUwFJO7qRoTLVK2Dd.d4QaPhYpq2qtueEW3UevAMTiKfwtL/i8tzAcwK04/RUiPB1q5AS3pUmjxEcMIGGO6yLhVb8hZESA0pDM1ipQ0YIQS3+HAy8D333uNbXXb3rYz0Y=', 1, '啊哈哈', 1, 1, '2026-02-08 18:06:25');
INSERT INTO `sys_chat_message` VALUES (471, 1, '超级管理员', NULL, 10, '212', 1, 1, '2026-02-08 18:06:29');
INSERT INTO `sys_chat_message` VALUES (472, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 18:06:31');
INSERT INTO `sys_chat_message` VALUES (473, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 18:06:31');
INSERT INTO `sys_chat_message` VALUES (474, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 18:06:31');
INSERT INTO `sys_chat_message` VALUES (475, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 18:06:31');
INSERT INTO `sys_chat_message` VALUES (476, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 18:06:32');
INSERT INTO `sys_chat_message` VALUES (477, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 18:06:32');
INSERT INTO `sys_chat_message` VALUES (478, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 18:06:32');
INSERT INTO `sys_chat_message` VALUES (479, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 18:06:32');
INSERT INTO `sys_chat_message` VALUES (480, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 18:06:32');
INSERT INTO `sys_chat_message` VALUES (481, 1, '超级管理员', NULL, 10, '1', 1, 1, '2026-02-08 18:06:32');
INSERT INTO `sys_chat_message` VALUES (482, 1, '超级管理员', NULL, 10, '2', 1, 1, '2026-02-08 18:06:33');
INSERT INTO `sys_chat_message` VALUES (483, 1, '超级管理员', NULL, 10, '12', 1, 1, '2026-02-08 18:06:33');
INSERT INTO `sys_chat_message` VALUES (484, 10, 'Mars', 'http://m8e8f9e2.natappfree.ccUwFJO7qRoTLVK2Dd.d4QaPhYpq2qtueEW3UevAMTiKfwtL/i8tzAcwK04/RUiPB1q5AS3pUmjxEcMIGGO6yLhVb8hZESA0pDM1ipQ0YIQS3+HAy8D333uNbXXb3rYz0Y=', 1, '哈哈哈', 1, 1, '2026-02-08 18:06:37');
INSERT INTO `sys_chat_message` VALUES (485, 4, 'lisi', NULL, 1, '111', 1, 1, '2026-02-09 12:57:56');
INSERT INTO `sys_chat_message` VALUES (486, 4, 'lisi', NULL, 1, '111', 1, 1, '2026-02-09 12:58:07');

-- ----------------------------
-- Table structure for sys_config_group
-- ----------------------------
DROP TABLE IF EXISTS `sys_config_group`;
CREATE TABLE `sys_config_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组编码',
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组名称',
  `group_icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分组图标',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '配置值(JSON格式)',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_group_code`(`group_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置分组表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_config_group
-- ----------------------------
INSERT INTO `sys_config_group` VALUES (1, 'system', '系统配置', NULL, '{\"siteName\":\"MasterLife\",\"siteDescription\":\"现代化企业级管理系统\",\"siteLogo\":\"\",\"copyright\":\"版权所有@朋诚科技\",\"icp\":\"\",\"watermarkEnabled\":true,\"watermarkType\":\"sitename\",\"watermarkOpacity\":0.06}', 1, 1, NULL, '2026-01-31 14:38:29', '2026-01-31 14:38:29');
INSERT INTO `sys_config_group` VALUES (2, 'register', '注册配置', NULL, '{\"enabled\":true,\"verifyEmail\":false,\"verifyPhone\":false,\"defaultRole\":\"user\",\"needAudit\":true}', 2, 1, NULL, '2026-01-31 14:38:29', '2026-01-31 14:38:29');
INSERT INTO `sys_config_group` VALUES (3, 'login', '登录配置', NULL, '{\"captchaEnabled\":true,\"captchaType\":\"image\",\"maxRetryCount\":5,\"lockTime\":30,\"rememberMe\":true,\"singleLogin\":false}', 3, 1, NULL, '2026-01-31 14:38:29', '2026-01-31 15:38:39');
INSERT INTO `sys_config_group` VALUES (4, 'password', '密码配置', NULL, '{\"minLength\":6,\"maxLength\":20,\"requireUppercase\":true,\"requireLowercase\":true,\"requireNumber\":true,\"requireSpecial\":true,\"expireDays\":0}', 4, 1, NULL, '2026-01-31 14:38:29', '2026-01-31 14:38:29');
INSERT INTO `sys_config_group` VALUES (5, 'email', '邮件配置', NULL, '{\"host\":\"smtp.qq.com\",\"port\":465,\"username\":\"850994281@qq.com\",\"password\":\"pbfbulghhkqmbedj\",\"fromName\":\"MasterLife\",\"ssl\":true,\"enabled\":true}', 5, 1, NULL, '2026-01-31 14:38:29', '2026-01-31 22:13:14');
INSERT INTO `sys_config_group` VALUES (6, 'emailTemplate', '邮件模板', NULL, '{\"verifyCode\":\"您的验证码是：{code}，有效期{expire}分钟。\",\"resetPassword\":\"您正在重置密码，验证码：{code}，有效期{expire}分钟。\",\"welcome\":\"欢迎注册{siteName}，您的账号已创建成功。\"}', 6, 1, NULL, '2026-01-31 14:38:29', '2026-01-31 14:38:29');
INSERT INTO `sys_config_group` VALUES (7, 'sms', '短信配置', NULL, '{\"provider\":\"aliyun\",\"accessKeyId\":\"\",\"accessKeySecret\":\"\",\"signName\":\"\",\"enabled\":false}', 7, 1, NULL, '2026-01-31 14:38:29', '2026-01-31 14:38:29');
INSERT INTO `sys_config_group` VALUES (9, 'storage', '文件配置', NULL, '{\"provider\":\"local\",\"domain\":\"http://localhost:8080\",\"localPath\":\"./uploads\",\"maxSize\":10,\"allowTypes\":\"jpg,jpeg,png,gif,webp,bmp,ico,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,csv,xml,json,yaml,yml,html,htm,css,js,ts,vue,java,py,go,sql,sh,bat,mp4,avi,mov,wmv,flv,mkv,webm,mp3,wav,ogg,flac,aac,zip,rar,7z,tar,gz,apk,exe,dmg\",\"minioEndpoint\":\"\",\"minioAccessKey\":\"\",\"minioSecretKey\":\"\",\"minioBucket\":\"\",\"aliyunEndpoint\":\"https://oss-cn-beijing.aliyuncs.com\",\"aliyunAccessKey\":\"LTAI5tEPLV6eFkXgFiRcNAj1\",\"aliyunSecretKey\":\"rOOOUyxCxFsxfWru1NojIycWMOvCWJ\",\"aliyunBucket\":\"pengcheng-admin\",\"tencentSecretId\":\"\",\"tencentSecretKey\":\"\",\"tencentBucket\":\"\",\"tencentRegion\":\"\"}', 9, 1, NULL, '2026-01-31 14:38:29', '2026-02-02 17:02:01');
INSERT INTO `sys_config_group` VALUES (10, 'push', '推送配置', NULL, '{\r\n  \"enabled\": false,\r\n  \"provider\": \"console\",\r\n  \"appKey\": \"\",\r\n  \"masterSecret\": \"\"\r\n}', 10, 1, NULL, '2026-01-31 14:38:29', '2026-01-31 16:26:34');
INSERT INTO `sys_config_group` VALUES (11, 'thirdParty', '第三方配置', NULL, '{\"wechat\":{\"enabled\":false,\"appId\":\"\",\"appSecret\":\"\"},\"alipay\":{\"enabled\":false,\"appId\":\"\",\"privateKey\":\"\",\"publicKey\":\"\"},\"github\":{\"enabled\":false,\"clientId\":\"\",\"clientSecret\":\"\"}}', 11, 1, NULL, '2026-01-31 14:38:29', '2026-01-31 14:38:29');
INSERT INTO `sys_config_group` VALUES (12, 'payment', '支付配置', NULL, '{\r\n    \"wechatPay\": {\r\n        \"enabled\": true,\r\n        \"mchId\": \"1627500294\",\r\n        \"appId\": \"wxe97894ad8c7ef7e0\",\r\n        \"apiKey\": \"\",\r\n        \"apiV3Key\": \"lxpvkwojpnxafnoutgqowbecdwdsmpwq\",\r\n        \"privateKey\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDlGsA4SciJOYYq\\nTL+/hYlaRLkkJ060c+2MrOl7egozzwddhNLHRC0wasgGdQdbDI39mAm34I7mLdMV\\nlv10dgtKXgpQBHc9QPKy3bPFcgFrz7rxS0YcFrqmzzB69a0LVVfAsZE2SD/4yKc3\\nVFW8cLKQZKRYYm3gZGwN0rsJFVU3dfWgOaoNlBkc5bNIbY7j4aHeW7tJXOQCiig6\\nKj+Dh7r1/POzTciCfqVB1Vjf+VkFMuF6oyKLxMBzFzxvXCGw3PySL6HuY1g5xI7j\\nbNi+xfqtzxZEQAv1QjbfBjzygQXeLCpsuYGVFRRVdyNYxkV90FDVI8swLXpMh65b\\nYNgBGtn1AgMBAAECggEBAKlIx+mPk07aI2mUBkcU+7WofAjbxosN8eP1TBxBw9Ie\\nUnnmj/xPQvi4ng4vYP0E3NIaCmxE0DICgCs+ww7Pvm336LTRZ+3p1KsXqCLnp2cr\\nOh3bGfXdUZO6Gj9w0qlCKTInwn2SizpfwTbf6O3xc++/fbQVHs0kRrc8E5mVmr77\\n01aGIJvXxtQPfdn/R2TMBwqiN8pO5igILlDzNAEusXnfSDOp3rYsXwcnCxJqgnVm\\nydlo7JMU2iqRKSD09qeKFgb+Hbr9aJIQdcvjGBSNmF3MsCFgs/XIb47B4xvy2HBN\\nvIBRwBy08fFeih0GE+0IKr0LyAQ8naMjRTD8A6SbPE0CgYEA9HJ+qigfUPsh/Q+u\\nyyoZeIrsR1xoNVcwANwpWnChsic+B3V/D/pWMJxPv9wKRsVt/dc4kVht//j69tS8\\ny3BFoUxSfUuoK5hdhI8osk3wdVFOnrPPs57s2bMcPPF3Rd5iMvcRNqM1IENCpDAR\\n4zlrEqcMpGSNfaSVhFEyo0fvsV8CgYEA7+6gxxkZJD7DwoVUk8w0BJoq7pNUZc43\\nC0uI8EIRCWxSkd5ahruJjreJuFM1IQUmmqFgewdhEIdUjyORwgVQlo469uwqYPQ5\\n8RWMJcQVK8+QEWV/TdywO3P7oEFgFmVlII/h7Janz/ZlOFZ1X8ANVvpenqgeB2j+\\nl1JMVfjnUSsCgYAHmvRT6PGofFe/XtiKW6H1PSVCxx464p6MuEzVEoIFX/EvHDm6\\nzogV9RcKGhd7wjK83hBVfVHWz/FG8rF5BuIztYMvgMYXrSLjt+yFN6WOkNwIVgHV\\nTdGCqG7tennCg7u8aDFx6LwDZ/RP1WsJDcVGDEp5ZuN8ED3SoxAXQmqzswKBgCk3\\nOtM40oLRbVtq//5ro7vup9VX5bWfWQFNtnZfQwH1Y7G/GpnueVDU4omRcZz8f4cs\\nlaBMwjXOqY31NEK6Gv/h6usj4pvJGHL7mpmaN3DRNRRn9RhxAq0T3XPIBzORs2+G\\nh+7WanllADpPT9Zk7WW1mK90fcQUGzfvYUGbglEFAoGARffpCUANEp1oedOyUVRN\\nSKIvSRggxxqMuzSdnm7eGKDmm+kbA8Iig2C0jgcn4vQZpngbhlNsGrb26Bvdh8wE\\nTBtkcxSBjzsFBdE4kSdVqxnZeVezouWixvkxL4ax1xwczS9hyJlunDljsUb2PkwZ\\nBE39glMdpIqGYrpSTM3p6mI=\\n-----END PRIVATE KEY-----\",\r\n        \"certSerialNo\": \"2FD947564972A8536BDD750944C4796CDF3265EB\",\r\n        \"notifyUrl\": \"http://q668fd96.natappfree.cc/api/pay/notify/wechat\"\r\n    },\r\n    \"alipay\": {\r\n        \"enabled\": true,\r\n        \"appId\": \"2021005192689177\",\r\n        \"privateKey\": \"MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCc1KF+8tqemN/+k99xwIxtfrZ8Py7ZtSv6zoDa0Z9pW1IWpYmz+Zm1wtF3CTX5xApyyBTWQgK8Pb/oEz4u4zKzuwcZFX/IdfP2mWaMHRvWnDLunWqH8rQO+JOaWDvzA68lL02AfubuafQldXY+hdeOpt+t5Kj/OnC+o4z0qTAcM9uTpeX9Z8lttlcW9JtCATRP+klr52bRaOcACgh3MIJrQ3OteeiikvbVtZtw4u3X2h5tdRlCl2/youKO6/iZXGmAmtTGRU8Iy8iBAMI6Ow8K6XH5xHccTzTOx8xv1PZ2IszVvMVhLJDXaUg4DyVbhN8hrmKFmu1i9eBbdSZixkNpAgMBAAECggEAXDjFBqu0VxK6lS9Lc86wRSsAECvvVuIsjH2mVAZ0YTXsHZkWUpjyBGodVow6Czd2lWyGpD+I8Dy3frbiGBxOElZmpB96VtzVqyslnDr5xcdwQ9SZcnwL2cnesiI0joCaG5mnT2pQTd5MTUK3V6jIyv/iBJWzsvIgnln6Z1yeB9ai/3c5Lvu0/ZnhC7trqD73BB1x49E0AV90y0/C/IA+FLEKio9/xjgYweSvTiaYTCBKzQv74Oco54HDtd93rlavZUu7F1qdpOWAj903N1xf8A/fepcL8/qPdSZNoRbPr2NgPMZa70hLvnWDfIXRWoaOZ+lFnPtewI8FAaVX4mI3AQKBgQDO8GxdEunrmRuOXbv/JqTj0dG2lXT7kUvDdJ6QVr3HIsmyxkZXQsp/7QdXh/FdRBNFwkOirmClUqrvYq3CbytgjNdxmYdZQ2A/YXqDdTs8J2Li36hbkOPIFNyMZsjtYF39eosf2oF0/ydRSlMqW5B6jpUh5qCYVWkVtUjLuXaM2QKBgQDCAwMqd6Um9X50dKNIqY1X2ImLiRdLVaqn4/pTwylxxIrRO9f5jF7PnenDci809+Sc+yCcZarvdh1QbUE+YGhYOjj2WGaB9sS2TGDFzOguGs7m7hCIQPa6VEyP2I07kaZcpb+r5GqnT9U47mPRcLJe3zop+w3B7cW5JcdtOSCREQKBgQCbpbALzWcOIoncado2Dk3lYPJ4fy+O6/jtWTDOZb+2IQ9OHN3ZUk5XK+PizUgYm1RXmscefEQK9QPGrBT/cnhQ1X5SXmS0Gf4xjdMFP06/buxsskbCIFeDLVW5cLHeASaQufQckE/gvO1IsjudV2NzGv1Gk13lVhCFGGZZfPSS+QKBgG4y2dSAWx1y6d3p9mkqbW9NPms0djfDNAji9GgpfVvyoErSbA2BzsSs1H/AVtIGUCNefRp4oQwdEe+B70In7nzWrU43zhnZ+cf2QC16AxNVBNqktF1AUSRrB4XZIfeI9m6/csyHFJFuRhVtSuNG2PoMX3RC9oCFtv5AWDNQ9I+RAoGAZF1dQs826kCeptQHXnlgTGNNIX9jLGyfO2qysBOCcqwFIrcJpsb11Q1xLrQmju6EHzr4kAINp32Qd5fo/oCM25JuSiw+fK6CgkAEYjSr/9dD4KpGicHmsib3GyfPj850K2RwFz2RckwX+If/NgI3dIecMTgTJ0tfytaaeqFH4PY=\",\r\n        \"publicKey\": \"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiwci3m6eLWeR1kgfeWm/F0V1e68VWUyRB4N5mhnxryTHqiLeN8ilxN9Kn/Ute1C9cL3b4hfx3NYk7zt60QWP9ly8QJQOlqd1H7XsG16AlEpsIaN1SrMYWq16nAD6uwvMmK0nTdzhuNIKOfdC2YWyv3AJTWh0nCTddYV2D+eSH/Ui6xkfgK8pFn/X1Q0xjXvuZrsXxF+WTk5mymEy2u4Kp7/rD/lClfNAv68kOHe92iKj1VzhtROrSp5//xuvL2PA7FLMqo5olZpBmda3eMWgnvHNwvaJvHJENN2ubANwMPNkwMkQ7MKLCBI33fzEERxJBACrJCc6lo8t+wq3zDo/uwIDAQAB\",\r\n        \"signType\": \"RSA2\",\r\n        \"charset\": \"UTF-8\",\r\n        \"gatewayUrl\": \"https://openapi.alipay.com/gateway.do\",\r\n        \"notifyUrl\": \"http://q668fd96.natappfree.cc/api/pay/notify/alipay\",\r\n        \"returnUrl\": \"\"\r\n    }\r\n}', 12, 1, NULL, '2026-01-31 14:38:29', '2026-01-31 16:13:21');
INSERT INTO `sys_config_group` VALUES (13, 'security', '安全配置', NULL, '{\"encryptEnabled\":false,\"encryptScope\":\"global\",\"encryptPublicKey\":\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxwcKZj5Wdozt6+8i9H2WW2eNaNUvI9iyU7Ot2P5XW9MSfTqRTCbv/aVEUOm60SHm7OXFAbuwUeuo6Pu2P7qPffXiqCXBdC1joo7VywNlapnmkwXP6jhuP+oHM31BvG2uInv40LHocUIRbMhREavnw+By7kT3Cq2SmgLBGsRkoIrpAuMBe47n8DjRGq2cvFde/EoChO0uO0AxlTUpfNXatUDGH0NtCEJeECoMBkg4nI0JAPnZETkimurbryPFoAVk5ld/GJg5WruQ1piicy9YgbOhjWnmb6gJ1RUU9xypNeHI/jLQCdjBn4NGQFtD73v36/WFnv4MgFAZV6iKr5kSdQIDAQAB\",\"encryptPrivateKey\":\"MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDHBwpmPlZ2jO3r7yL0fZZbZ41o1S8j2LJTs63Y/ldb0xJ9OpFMJu/9pURQ6brRIebs5cUBu7BR66jo+7Y/uo999eKoJcF0LWOijtXLA2VqmeaTBc/qOG4/6gczfUG8ba4ie/jQsehxQhFsyFERq+fD4HLuRPcKrZKaAsEaxGSgiukC4wF7jufwONEarZy8V178SgKE7S47QDGVNSl81dq1QMYfQ20IQl4QKgwGSDicjQkA+dkROSKa6tuvI8WgBWTmV38YmDlau5DWmKJzL1iBs6GNaeZvqAnVFRT3HKk14cj+MtAJ2MGfg0ZAW0Pve/fr9YWe/gyAUBlXqIqvmRJ1AgMBAAECggEAIOYACRCK2EPJXDOGMqXDwc4nKMn8Zc9/AqjztqesJwiHyN1ygQT6rJGx7jIEaGdTNZtxaiztI01x+TkKUhRzfZ20XpkHFj4edxNnMYyZKfrQi0LtsEitqLD1icRNpmj23MpjQZP22SnTmYivJd2ljNJADTSnJUO1tPF5nAQUohipaHm9ikipKzT+Qa605nj1TvG1NF1a0y/IElBGb5FFyQGISgUoiPh8/aZXeO5pS6YMJTTQul/9Q7f9fwJFrzPl3qqc3kDxYjagJcPtV5VmX/nSrMpeLnaTvRIg78ocwNF+XYJ5L1Sr9wxYEADykw4P8E0ijGYynSeZlo0u+Q7U7QKBgQDZaG5ITWYmt+4KQrR0r1HHGFWJPtFVKcwjC+EIm9I1S+gTOjZ/6SG45upDqlHtmCOMf1drRFhSehdD6UHUFL4xN/fAxkP3F+iKU/KfJy6yclCuhW+k0Efi6W4mKR9ZkhINJvVibsNdA0vXQa603bbr7hfHVeJl1xI761htsnEFuwKBgQDqW1s5f67gXowzjmK6a40Z+/DIoHBTd267zOIEknhUg6oaMtW1v/yPjwWrf6wJmpUFO3Mq3xTDd/k1iXBOke2vHmZG2AplNVScreRx20lRBmzuGe+9sSDozTfFJO25oPhH86wmIAmqMB5nu1L1TJjbKRAU+hcdC+v22NWMQ48tjwKBgALF9kIt2pO73Ol8mFi0s9JaWRz7FCiF8/iuehxmAHR1l2xHXdKb4rY9G9fpIEprmmh8Z10S7h1/OTTAkPpnmVV/ZUWsQcmxIGJDV+D32vyjwKu5QAdWMNSQLbuG4sN9vYU1bgPnbc6N8DW6vMPJ4D96Ngtw6QZri+v/wI0FrbNpAoGAcpvuxvXMXemfAu+VFLnYLWbqYBMmG4uC2dDej4HZ2urw2xMVNGcJamN1UGOFjMTOL9rc/ZBPJTCc7TOjeqke5c8mEWtB2jD0ihL4bz3gYwGTb/W7Krde8rq5lW3z3B3+jaF7BMISN+qEVBJmBZRKBJPWS4vqlcfow7VS6d94O70CgYBTLo2LdYZV9rn7FGmgC9/fuJOgWEfeqmunNx8SsYUjaXSyy+Vb+dlgH/YRfypb37rxxsNwWQKggZww6gSO1/TkFoV73W035XBKbMB3XLEFHp2v75qYBYEHvVpW1YEl2QGlUzOUWXrP5G/3v8O0/+5yJwjKcmkWDjPGIIKj8GPZsQ==\",\"xssFilter\":true,\"sqlInject\":true,\"tokenName\":\"Authorization\",\"tokenTimeout\":3600,\"tokenActiveTimeout\":86400,\"tokenIsConcurrent\":true,\"tokenIsShare\":true,\"tokenStyle\":\"uuid\",\"tokenIsLog\":false,\"tokenIsReadBody\":false,\"tokenIsReadCookie\":false,\"tokenIsReadHeader\":true,\"tokenIsPrint\":true,\"tokenIsWriteHeader\":false}', 13, 1, NULL, '2026-01-31 14:38:29', '2026-02-07 16:04:06');
INSERT INTO `sys_config_group` VALUES (15, 'wechatMiniProgram', '小程序配置', NULL, '{\"enabled\":true,\"appId\":\"wxe97894ad8c7ef7e0\",\"appSecret\":\"ef498f4264b2271eac752b36433aca63\"}', 14, 1, '微信小程序登录配置', '2026-02-03 10:48:41', '2026-02-03 10:48:41');
INSERT INTO `sys_config_group` VALUES (16, 'wechatMp', '公众号配置', NULL, '{\"enabled\":true,\"appId\":\"wx12721c4ea1370b36\",\"appSecret\":\"f860891f96df4fffe78f0424b913aedd\",\"token\":\"pengcheng_wechat_token1\",\"aesKey\":\"zBtP7b8qZKCSW2eU7Ozm6Jyapv5PCQu2Vxpj1v72qBP\",\"callbackUrl\":\"https://api.pengcheng-admin.cn/api/wechat/callback\",\"oauthRedirectUrl\":\"http://localhost:3001/login\",\"menuConfig\":\"\"}', 15, 1, '微信公众号配置', '2026-02-03 10:48:41', '2026-02-03 10:48:41');

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父部门ID',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `sort` int NULL DEFAULT 0 COMMENT '显示顺序',
  `leader` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-停用 1-正常)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (1, 0, '0', '朋诚科技', 0, '管理员', NULL, NULL, 1, '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, 1, 0);
INSERT INTO `sys_dept` VALUES (2, 1, '0,1', '技术部', 1, '张三', NULL, NULL, 1, '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dept` VALUES (3, 1, '0,1', '产品部', 2, '李四', NULL, NULL, 1, '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dept` VALUES (4, 1, '0,1', '运营部', 3, '王五', NULL, NULL, 1, '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sort` int NULL DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '样式属性',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表格回显样式',
  `is_default` tinyint NULL DEFAULT 0 COMMENT '是否默认(0-否 1-是)',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-停用 1-正常)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (1, 1, '男', '1', 'sys_user_sex', NULL, 'default', 0, 1, NULL, '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dict_data` VALUES (2, 2, '女', '2', 'sys_user_sex', NULL, 'default', 0, 1, NULL, '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dict_data` VALUES (3, 3, '未知', '0', 'sys_user_sex', NULL, 'default', 0, 1, NULL, '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dict_data` VALUES (4, 1, '正常', '1', 'sys_status', NULL, 'success', 0, 1, NULL, '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dict_data` VALUES (5, 2, '停用', '0', 'sys_status', NULL, 'error', 0, 1, NULL, '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dict_data` VALUES (6, 1, '是', '1', 'sys_yes_no', NULL, 'success', 0, 1, NULL, '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dict_data` VALUES (7, 2, '否', '0', 'sys_yes_no', NULL, 'error', 0, 1, NULL, '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dict_data` VALUES (8, 0, 'sex', '1', 'sex', NULL, 'default', 0, 1, '', '2026-01-29 23:21:38', '2026-01-29 23:21:38', 1, 1, 0);
INSERT INTO `sys_dict_data` VALUES (9, 0, '女', '0', 'sex', NULL, 'default', 0, 1, '', '2026-01-29 23:21:58', '2026-01-29 23:21:58', 1, 1, 0);

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-停用 1-正常)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dict_type`(`dict_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典类型表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO `sys_dict_type` VALUES (1, '用户性别', 'sys_user_sex', 1, '用户性别列表', '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dict_type` VALUES (2, '系统状态', 'sys_status', 1, '系统通用状态', '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dict_type` VALUES (3, '是否', 'sys_yes_no', 1, '是否选项', '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_dict_type` VALUES (4, '性别', 'sex', 1, '', '2026-01-29 23:21:29', '2026-02-07 15:33:54', 1, 1, 1);
INSERT INTO `sys_dict_type` VALUES (5, '111', '222', 1, '', '2026-02-07 19:42:53', '2026-02-07 23:26:19', 1, 1, 1);
INSERT INTO `sys_dict_type` VALUES (6, '222', '333', 1, '', '2026-02-07 19:43:46', '2026-02-07 23:26:17', 1, 1, 1);

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `original_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '原始文件名',
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存储文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件路径',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '文件访问URL',
  `file_size` bigint NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `file_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '文件类型（MIME类型）',
  `file_suffix` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '文件后缀',
  `storage_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '存储类型',
  `bucket_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '存储桶名称',
  `group_id` bigint NULL DEFAULT NULL COMMENT '分组ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_path`(`file_path`(191) ASC) USING BTREE,
  INDEX `idx_storage_type`(`storage_type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_group_id`(`group_id` ASC) USING BTREE,
  INDEX `idx_file_type`(`file_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 56 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_file
-- ----------------------------
INSERT INTO `sys_file` VALUES (10, 'FPj0uHDhtvlIgwhfEk0-Lu-bp4Duopj5_GorB8Sxbqe4pKUR4-7HxwbA7VFa8fTK.jpg', '97c4a311e92b409db8eeaa919c2014e6.jpg', '2026/02/02/97c4a311e92b409db8eeaa919c2014e6.jpg', '/api/files/2026/02/02/97c4a311e92b409db8eeaa919c2014e6.jpg', 823196, 'image/jpeg', '.jpg', 'local', '', 1, '', '1', '2026-02-02 16:54:05');
INSERT INTO `sys_file` VALUES (13, '222.yaml', 'fb28a3b6569746688514518c32bb0237.yaml', '2026/02/02/fb28a3b6569746688514518c32bb0237.yaml', '/api/files/2026/02/02/fb28a3b6569746688514518c32bb0237.yaml', 6582, 'application/octet-stream', '.yaml', 'local', '', NULL, '', '1', '2026-02-02 17:02:45');
INSERT INTO `sys_file` VALUES (38, '8sWHmSAlwmey2d398ffc33c404811e76aaf9697d2ed3.jpeg', '00e31155cc7f4a4fafca1943446541b3.jpeg', 'images/2026/02/08/00e31155cc7f4a4fafca1943446541b3.jpeg', '/api/files/images/2026/02/08/00e31155cc7f4a4fafca1943446541b3.jpeg', 7086, 'image/jpeg', '.jpeg', 'local', '', NULL, '', '8', '2026-02-08 16:43:09');
INSERT INTO `sys_file` VALUES (39, '7WKDXjCMF6V0788491cd9d8fc92f435747cb5e46f478.jpeg', '8f812f8bbf634a0cae6b4189d4fd5465.jpeg', 'images/2026/02/08/8f812f8bbf634a0cae6b4189d4fd5465.jpeg', '/api/files/images/2026/02/08/8f812f8bbf634a0cae6b4189d4fd5465.jpeg', 7086, 'image/jpeg', '.jpeg', 'local', '', NULL, '', '9', '2026-02-08 16:44:12');
INSERT INTO `sys_file` VALUES (40, 'gWUFBuyJ0yTl100deb149c357e85d0ffccb56fecb67b.jpeg', '2b3c6ece52694e66a256d72c0ae0be6a.jpeg', 'images/2026/02/08/2b3c6ece52694e66a256d72c0ae0be6a.jpeg', '/api/files/images/2026/02/08/2b3c6ece52694e66a256d72c0ae0be6a.jpeg', 7086, 'image/jpeg', '.jpeg', 'local', '', NULL, '', '10', '2026-02-08 16:46:44');
INSERT INTO `sys_file` VALUES (41, 'XSGeetx0TvKka3c75490d3d905a46f0da72568fa9f78.jpeg', 'fdc1626199204477ae2093aeedc7aec8.jpeg', 'images/2026/02/08/fdc1626199204477ae2093aeedc7aec8.jpeg', '/api/files/images/2026/02/08/fdc1626199204477ae2093aeedc7aec8.jpeg', 7086, 'image/jpeg', '.jpeg', 'local', '', NULL, '', '10', '2026-02-08 17:26:38');
INSERT INTO `sys_file` VALUES (44, 'img_v3_02ui_f0990cef-9f0d-4c6e-bba2-005ed10e088g.jpg', 'e3065b470f9342f2bfeef9aa35182d39.jpg', '2026/02/09/e3065b470f9342f2bfeef9aa35182d39.jpg', '/api/files/2026/02/09/e3065b470f9342f2bfeef9aa35182d39.jpg', 101083, 'image/jpeg', '.jpg', 'local', '', NULL, '', '1', '2026-02-09 11:49:05');
INSERT INTO `sys_file` VALUES (45, 'img_v3_02ui_f0990cef-9f0d-4c6e-bba2-005ed10e088g.jpg', 'e321bfaf05754cb98700459c53fed98a.jpg', '2026/02/09/e321bfaf05754cb98700459c53fed98a.jpg', '/api/files/2026/02/09/e321bfaf05754cb98700459c53fed98a.jpg', 101083, 'image/jpeg', '.jpg', 'local', '', 1, '', '1', '2026-02-09 16:52:03');
INSERT INTO `sys_file` VALUES (46, '项目汇总交付表.xlsx', 'c2a527651b6845f7ac36af3edf32e6c3.xlsx', '2026/02/09/c2a527651b6845f7ac36af3edf32e6c3.xlsx', '/api/files/2026/02/09/c2a527651b6845f7ac36af3edf32e6c3.xlsx', 5925, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '.xlsx', 'local', '', 2, '', '1', '2026-02-09 16:53:29');
INSERT INTO `sys_file` VALUES (47, 'img_v3_02t8_80c939c9-3e79-4c59-b10c-1716a88818ag.jpg', 'b4d816e8567f4a219c33b9d0ee1a5615.jpg', '2026/02/09/b4d816e8567f4a219c33b9d0ee1a5615.jpg', '/api/files/2026/02/09/b4d816e8567f4a219c33b9d0ee1a5615.jpg', 108824, 'image/jpeg', '.jpg', 'local', '', 2, '', '1', '2026-02-09 16:53:52');
INSERT INTO `sys_file` VALUES (48, 'img_v3_02t8_80c939c9-3e79-4c59-b10c-1716a88818ag.jpg', 'be55af87595046cdb2834cc611a81f30.jpg', '2026/02/09/be55af87595046cdb2834cc611a81f30.jpg', '/api/files/2026/02/09/be55af87595046cdb2834cc611a81f30.jpg', 108824, 'image/jpeg', '.jpg', 'local', '', 1, '', '1', '2026-02-09 16:54:07');
INSERT INTO `sys_file` VALUES (49, 'img_v3_02ui_f0990cef-9f0d-4c6e-bba2-005ed10e088g.jpg', '2aa015bf21dd4bf78e25b904507e88a9.jpg', '2026/02/09/2aa015bf21dd4bf78e25b904507e88a9.jpg', '/api/files/2026/02/09/2aa015bf21dd4bf78e25b904507e88a9.jpg', 101083, 'image/jpeg', '.jpg', 'local', '', NULL, '', '1', '2026-02-09 21:05:03');
INSERT INTO `sys_file` VALUES (50, 'img_v3_02ui_f0990cef-9f0d-4c6e-bba2-005ed10e088g.jpg', '78ab2e5845d245d4976f18069258a1e3.jpg', '2026/02/09/78ab2e5845d245d4976f18069258a1e3.jpg', '/api/files/2026/02/09/78ab2e5845d245d4976f18069258a1e3.jpg', 101083, 'image/jpeg', '.jpg', 'local', '', NULL, '', '1', '2026-02-09 21:06:38');
INSERT INTO `sys_file` VALUES (51, 'img_v3_02un_25630100-71c0-4026-8965-5133f5d735dg.jpg', 'd983f41b0997442bacb33a057c3adf56.jpg', '2026/02/09/d983f41b0997442bacb33a057c3adf56.jpg', '/api/files/2026/02/09/d983f41b0997442bacb33a057c3adf56.jpg', 92978, 'image/jpeg', '.jpg', 'local', '', NULL, '', '1', '2026-02-09 21:06:42');
INSERT INTO `sys_file` VALUES (52, 'img_v3_02ui_f0990cef-9f0d-4c6e-bba2-005ed10e088g.jpg', 'f4b77e83a7974286a5e8e3bd2c0bf6a9.jpg', '2026/02/09/f4b77e83a7974286a5e8e3bd2c0bf6a9.jpg', '/api/files/2026/02/09/f4b77e83a7974286a5e8e3bd2c0bf6a9.jpg', 101083, 'image/jpeg', '.jpg', 'local', '', NULL, '', '1', '2026-02-09 21:06:44');
INSERT INTO `sys_file` VALUES (53, 'img_v3_02un_25630100-71c0-4026-8965-5133f5d735dg.jpg', 'c5b39025dcc5452fb652bdcea9a1618f.jpg', '2026/02/09/c5b39025dcc5452fb652bdcea9a1618f.jpg', '/api/files/2026/02/09/c5b39025dcc5452fb652bdcea9a1618f.jpg', 92978, 'image/jpeg', '.jpg', 'local', '', NULL, '', '1', '2026-02-09 21:06:46');
INSERT INTO `sys_file` VALUES (54, 'FPj0uHDhtvlIgwhfEk0-Lu-bp4Duopj5_GorB8Sxbqe4pKUR4-7HxwbA7VFa8fTK.jpg', '23d5546b227743c6b9b24eb19bb50476.jpg', '2026/02/09/23d5546b227743c6b9b24eb19bb50476.jpg', '/api/files/2026/02/09/23d5546b227743c6b9b24eb19bb50476.jpg', 823196, 'image/jpeg', '.jpg', 'local', '', NULL, '', '1', '2026-02-09 21:07:03');
INSERT INTO `sys_file` VALUES (55, 'caa41ac5c974e4fa96b1df176aeba849.png', '7d8d2dbcb3924c128ac708d3bef8a63d.png', '2026/02/09/7d8d2dbcb3924c128ac708d3bef8a63d.png', '/api/files/2026/02/09/7d8d2dbcb3924c128ac708d3bef8a63d.png', 16286, 'image/png', '.png', 'local', '', NULL, '', '1', '2026-02-09 21:07:06');

-- ----------------------------
-- Table structure for sys_file_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_file_config`;
CREATE TABLE `sys_file_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
  `storage_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存储类型(local/minio/aliyun)',
  `master` tinyint NULL DEFAULT 0 COMMENT '是否为主配置(0否 1是)',
  `domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访问域名',
  `base_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '基础路径(本地存储)',
  `bucket_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '存储桶名称',
  `access_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访问密钥',
  `secret_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '秘密密钥',
  `endpoint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '端点地址',
  `region` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地域',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0禁用 1启用)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_storage_type`(`storage_type` ASC) USING BTREE,
  INDEX `idx_master`(`master` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件存储配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_file_config
-- ----------------------------
INSERT INTO `sys_file_config` VALUES (1, '本地存储', 'local', 1, 'http://localhost:8080', 'D:/uploads', NULL, NULL, NULL, NULL, NULL, 1, '默认本地存储配置', NULL, '2026-01-30 23:35:08', NULL, '2026-01-30 23:35:08');

-- ----------------------------
-- Table structure for sys_file_group
-- ----------------------------
DROP TABLE IF EXISTS `sys_file_group`;
CREATE TABLE `sys_file_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分组ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组名称',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件分组表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_file_group
-- ----------------------------
INSERT INTO `sys_file_group` VALUES (1, '测试分组', 0, NULL, '2026-02-02 16:50:50', NULL);
INSERT INTO `sys_file_group` VALUES (2, '111', 0, NULL, '2026-02-09 16:53:04', NULL);

-- ----------------------------
-- Table structure for sys_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'cron执行表达式',
  `misfire_policy` tinyint NULL DEFAULT 3 COMMENT '计划执行错误策略(1-立即执行 2-执行一次 3-放弃执行)',
  `concurrent` tinyint NULL DEFAULT 1 COMMENT '是否并发执行(0-允许 1-禁止)',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态(0-暂停 1-正常)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '定时任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_job
-- ----------------------------
INSERT INTO `sys_job` VALUES (1, '系统默认（无参）', 'DEFAULT', 'sampleTask.noParams', '0/10 * * * * ?', 3, 1, 0, '无参数的示例任务', '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_job` VALUES (2, '系统默认（有参）', 'DEFAULT', 'sampleTask.withParams(\'hello\')', '0/15 * * * * ?', 3, 1, 0, '有参数的示例任务', '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_job` VALUES (3, '1', 'DEFAULT', 'sampleTask.noParams', '0/10 * * * * ?', 3, 1, 0, '111', '2026-01-29 22:59:47', '2026-01-29 22:59:47', 1, 1, 0);

-- ----------------------------
-- Table structure for sys_job_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日志信息',
  `status` tinyint NULL DEFAULT 0 COMMENT '执行状态(0-正常 1-失败)',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '异常信息',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `stop_time` datetime NULL DEFAULT NULL COMMENT '停止时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '定时任务日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_job_log
-- ----------------------------
INSERT INTO `sys_job_log` VALUES (1, '1', 'DEFAULT', '1', '执行失败', 1, 'java.lang.StringIndexOutOfBoundsException: begin 0, end -1, length 1\r\n	at java.base/java.lang.String.checkBoundsBeginEnd(String.java:4602)\r\n	at java.base/java.lang.String.substring(String.java:2705)\r\n	at com.pengcheng.job.util.JobInvokeUtil.getBeanName(JobInvokeUtil.java:45)\r\n	at com.pengcheng.job.util.JobInvokeUtil.invokeMethod(JobInvokeUtil.java:20)\r\n	at com.pengcheng.job.util.QuartzDisallowConcurrentExecution.doExecute(QuartzDisallowConcurrentExecution.java:15)\r\n	at com.pengcheng.job.util.AbstractQuartzJob.execute(AbstractQuartzJob.java:40)\r\n	at org.quartz.core.JobRunShell.run(JobRunShell.java:202)\r\n	at org.quartz.simpl.SimpleThreadPool$WorkerThread.run(SimpleThreadPool.java:573)\r\n', '2026-01-29 22:59:50', '2026-01-29 22:59:50');
INSERT INTO `sys_job_log` VALUES (2, '1', 'DEFAULT', '1', '执行失败', 1, 'java.lang.StringIndexOutOfBoundsException: begin 0, end -1, length 1\r\n	at java.base/java.lang.String.checkBoundsBeginEnd(String.java:4602)\r\n	at java.base/java.lang.String.substring(String.java:2705)\r\n	at com.pengcheng.job.util.JobInvokeUtil.getBeanName(JobInvokeUtil.java:45)\r\n	at com.pengcheng.job.util.JobInvokeUtil.invokeMethod(JobInvokeUtil.java:20)\r\n	at com.pengcheng.job.util.QuartzDisallowConcurrentExecution.doExecute(QuartzDisallowConcurrentExecution.java:15)\r\n	at com.pengcheng.job.util.AbstractQuartzJob.execute(AbstractQuartzJob.java:40)\r\n	at org.quartz.core.JobRunShell.run(JobRunShell.java:202)\r\n	at org.quartz.simpl.SimpleThreadPool$WorkerThread.run(SimpleThreadPool.java:573)\r\n', '2026-01-29 22:59:55', '2026-01-29 22:59:55');
INSERT INTO `sys_job_log` VALUES (3, '1', 'DEFAULT', '1', '执行失败', 1, 'java.lang.StringIndexOutOfBoundsException: begin 0, end -1, length 1\r\n	at java.base/java.lang.String.checkBoundsBeginEnd(String.java:4602)\r\n	at java.base/java.lang.String.substring(String.java:2705)\r\n	at com.pengcheng.job.util.JobInvokeUtil.getBeanName(JobInvokeUtil.java:45)\r\n	at com.pengcheng.job.util.JobInvokeUtil.invokeMethod(JobInvokeUtil.java:20)\r\n	at com.pengcheng.job.util.QuartzDisallowConcurrentExecution.doExecute(QuartzDisallowConcurrentExecution.java:15)\r\n	at com.pengcheng.job.util.AbstractQuartzJob.execute(AbstractQuartzJob.java:40)\r\n	at org.quartz.core.JobRunShell.run(JobRunShell.java:202)\r\n	at org.quartz.simpl.SimpleThreadPool$WorkerThread.run(SimpleThreadPool.java:573)\r\n', '2026-01-29 23:01:29', '2026-01-29 23:01:29');
INSERT INTO `sys_job_log` VALUES (4, '1', 'DEFAULT', '1', '执行失败', 1, 'java.lang.StringIndexOutOfBoundsException: begin 0, end -1, length 1\r\n	at java.base/java.lang.String.checkBoundsBeginEnd(String.java:4602)\r\n	at java.base/java.lang.String.substring(String.java:2705)\r\n	at com.pengcheng.job.util.JobInvokeUtil.getBeanName(JobInvokeUtil.java:45)\r\n	at com.pengcheng.job.util.JobInvokeUtil.invokeMethod(JobInvokeUtil.java:20)\r\n	at com.pengcheng.job.util.QuartzDisallowConcurrentExecution.doExecute(QuartzDisallowConcurrentExecution.java:15)\r\n	at com.pengcheng.job.util.AbstractQuartzJob.execute(AbstractQuartzJob.java:40)\r\n	at org.quartz.core.JobRunShell.run(JobRunShell.java:202)\r\n	at org.quartz.simpl.SimpleThreadPool$WorkerThread.run(SimpleThreadPool.java:573)\r\n', '2026-01-29 23:01:54', '2026-01-29 23:01:54');
INSERT INTO `sys_job_log` VALUES (5, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-01-29 23:02:57', '2026-01-29 23:02:57');
INSERT INTO `sys_job_log` VALUES (6, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-01-29 23:03:02', '2026-01-29 23:03:02');
INSERT INTO `sys_job_log` VALUES (7, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-01-31 12:33:00', '2026-01-31 12:33:00');
INSERT INTO `sys_job_log` VALUES (8, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-01-31 22:36:20', '2026-01-31 22:36:20');
INSERT INTO `sys_job_log` VALUES (9, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-01-31 22:36:30', '2026-01-31 22:36:30');
INSERT INTO `sys_job_log` VALUES (10, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-07 09:51:50', '2026-02-07 09:51:50');
INSERT INTO `sys_job_log` VALUES (11, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-07 23:54:56', '2026-02-07 23:54:56');
INSERT INTO `sys_job_log` VALUES (12, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-07 23:55:04', '2026-02-07 23:55:04');
INSERT INTO `sys_job_log` VALUES (13, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-09 17:04:25', '2026-02-09 17:04:25');
INSERT INTO `sys_job_log` VALUES (14, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-09 17:04:27', '2026-02-09 17:04:27');
INSERT INTO `sys_job_log` VALUES (15, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-09 17:04:27', '2026-02-09 17:04:27');
INSERT INTO `sys_job_log` VALUES (16, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-09 17:04:27', '2026-02-09 17:04:27');
INSERT INTO `sys_job_log` VALUES (17, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-09 17:04:27', '2026-02-09 17:04:27');
INSERT INTO `sys_job_log` VALUES (18, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-09 17:04:34', '2026-02-09 17:04:34');
INSERT INTO `sys_job_log` VALUES (19, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-09 17:04:35', '2026-02-09 17:04:35');
INSERT INTO `sys_job_log` VALUES (20, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-09 17:04:35', '2026-02-09 17:04:35');
INSERT INTO `sys_job_log` VALUES (21, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-09 17:04:35', '2026-02-09 17:04:35');
INSERT INTO `sys_job_log` VALUES (22, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-09 17:04:35', '2026-02-09 17:04:35');
INSERT INTO `sys_job_log` VALUES (23, '1', 'DEFAULT', 'sampleTask.noParams', '执行成功', 0, NULL, '2026-02-09 17:04:35', '2026-02-09 17:04:35');

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作系统',
  `status` tinyint NULL DEFAULT 0 COMMENT '登录状态(0-成功 1-失败)',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提示消息',
  `login_time` datetime NULL DEFAULT NULL COMMENT '登录时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 214 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_login_log
-- ----------------------------
INSERT INTO `sys_login_log` VALUES (160, 'wx_opzUF43Xlv', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 14:20:50');
INSERT INTO `sys_login_log` VALUES (161, 'wx_opzUF43Xlv', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 14:41:42');
INSERT INTO `sys_login_log` VALUES (162, 'wx_opzUF43Xlv', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 14:45:12');
INSERT INTO `sys_login_log` VALUES (163, 'wx_opzUF43Xlv', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 14:45:19');
INSERT INTO `sys_login_log` VALUES (164, 'wx_opzUF43Xlv', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 15:06:00');
INSERT INTO `sys_login_log` VALUES (165, 'wx_opzUF43Xlv', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 16:32:55');
INSERT INTO `sys_login_log` VALUES (166, 'wx_opzUF43Xlv', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 16:38:22');
INSERT INTO `sys_login_log` VALUES (167, 'wx_opzUF43Xlv', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 16:43:03');
INSERT INTO `sys_login_log` VALUES (168, 'wx_opzUF43Xlv', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 16:44:06');
INSERT INTO `sys_login_log` VALUES (169, 'wx_opzUF43Xlv', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 16:46:40');
INSERT INTO `sys_login_log` VALUES (170, 'Mars', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 17:26:31');
INSERT INTO `sys_login_log` VALUES (171, 'Mars', '127.0.0.1', '内网IP', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 17:33:57');
INSERT INTO `sys_login_log` VALUES (172, 'Mars', '117.174.18.21', '中国 四川省 成都市', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 18:02:47');
INSERT INTO `sys_login_log` VALUES (173, 'Mars', '111.55.145.39', '中国 山西省', 'MicroMessenger', 'iPhone', 0, '登录成功', '2026-02-08 18:07:38');
INSERT INTO `sys_login_log` VALUES (174, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-08 22:34:47');
INSERT INTO `sys_login_log` VALUES (175, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-08 22:44:15');
INSERT INTO `sys_login_log` VALUES (176, 'Mars', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 1, '非管理端用户，无权登录后台', '2026-02-08 23:56:53');
INSERT INTO `sys_login_log` VALUES (177, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-08 23:57:05');
INSERT INTO `sys_login_log` VALUES (178, 'test', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 1, '用户已被禁用', '2026-02-09 00:21:39');
INSERT INTO `sys_login_log` VALUES (179, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 00:21:51');
INSERT INTO `sys_login_log` VALUES (180, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 11:48:00');
INSERT INTO `sys_login_log` VALUES (181, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 12:20:22');
INSERT INTO `sys_login_log` VALUES (182, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 12:22:03');
INSERT INTO `sys_login_log` VALUES (183, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 12:25:31');
INSERT INTO `sys_login_log` VALUES (184, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 12:26:49');
INSERT INTO `sys_login_log` VALUES (185, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 12:28:00');
INSERT INTO `sys_login_log` VALUES (186, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 12:28:36');
INSERT INTO `sys_login_log` VALUES (187, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 12:48:22');
INSERT INTO `sys_login_log` VALUES (188, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 12:56:34');
INSERT INTO `sys_login_log` VALUES (189, 'Mars', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 1, '非管理端用户，无权登录后台', '2026-02-09 12:57:00');
INSERT INTO `sys_login_log` VALUES (190, 'test', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 1, '用户已被禁用', '2026-02-09 12:57:12');
INSERT INTO `sys_login_log` VALUES (191, 'mars01', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2026-02-09 12:57:21');
INSERT INTO `sys_login_log` VALUES (192, 'lisi', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 12:57:44');
INSERT INTO `sys_login_log` VALUES (193, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 12:58:17');
INSERT INTO `sys_login_log` VALUES (194, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 14:18:48');
INSERT INTO `sys_login_log` VALUES (195, 'mars666', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 14:25:32');
INSERT INTO `sys_login_log` VALUES (196, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 14:31:37');
INSERT INTO `sys_login_log` VALUES (197, 'mars02', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 14:32:22');
INSERT INTO `sys_login_log` VALUES (198, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 14:32:42');
INSERT INTO `sys_login_log` VALUES (199, 'mars02', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 14:33:33');
INSERT INTO `sys_login_log` VALUES (200, 'mars02', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', 1, '密码错误', '2026-02-09 14:41:52');
INSERT INTO `sys_login_log` VALUES (201, 'mars02', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 14:42:05');
INSERT INTO `sys_login_log` VALUES (202, 'test', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 14:58:46');
INSERT INTO `sys_login_log` VALUES (203, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 1, '密码错误', '2026-02-09 14:59:01');
INSERT INTO `sys_login_log` VALUES (204, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 1, '密码错误', '2026-02-09 14:59:04');
INSERT INTO `sys_login_log` VALUES (205, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 1, '用户已离职', '2026-02-09 14:59:12');
INSERT INTO `sys_login_log` VALUES (206, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 1, '用户已离职', '2026-02-09 14:59:28');
INSERT INTO `sys_login_log` VALUES (207, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 1, '用户已离职', '2026-02-09 14:59:42');
INSERT INTO `sys_login_log` VALUES (208, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 15:00:20');
INSERT INTO `sys_login_log` VALUES (209, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 16:01:44');
INSERT INTO `sys_login_log` VALUES (210, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 16:54:58');
INSERT INTO `sys_login_log` VALUES (211, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 17:02:33');
INSERT INTO `sys_login_log` VALUES (212, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 19:12:42');
INSERT INTO `sys_login_log` VALUES (213, 'admin', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2026-02-09 20:27:54');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父级ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `type` tinyint NOT NULL COMMENT '菜单类型(1-目录 2-菜单 3-按钮)',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由地址',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图标',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `visible` tinyint NULL DEFAULT 1 COMMENT '是否可见(0-隐藏 1-显示)',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
  `is_frame` tinyint NULL DEFAULT 0 COMMENT '是否外链(0-否 1-是)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 314 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- 菜单结构：房产业务→智能助手→协作办公→人事管理→组织管理→系统管理→系统监控→系统日志→开发工具
-- ----------------------------

-- ===== 1. 房产业务 (sort=1) =====
INSERT INTO `sys_menu` VALUES (279, 0, '房产业务', 1, '', NULL, NULL, 'BusinessOutline', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (282, 279, '客户管理', 2, '/realty/customer', 'realty/customer/CustomerManage', 'realty:customer:list', 'PeopleOutline', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (312, 279, '客户公海池', 2, '/realty/customer-pool', 'realty/customer/CustomerPool', 'realty:customer:pool', 'PeopleOutline', 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (283, 279, '联盟商管理', 2, '/realty/alliance', 'realty/alliance/AllianceManage', 'realty:alliance:list', 'BusinessOutline', 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (285, 279, '项目楼盘', 2, '/realty/project', 'realty/project/ProjectManage', 'realty:project:list', 'BusinessOutline', 4, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (284, 279, '成交佣金', 2, '/realty/commission', 'realty/commission/CommissionManage', 'realty:commission:list', 'CashOutline', 5, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (286, 279, '付款申请', 2, '/realty/payment', 'realty/payment/PaymentManage', 'realty:payment:list', 'WalletOutline', 6, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (288, 279, '拜访记录', 2, '/realty/visit', 'realty/visit/index', 'realty:visit:list', 'WalkOutline', 7, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (293, 279, '销售日历', 2, '/realty/calendar', 'realty/calendar/index', 'realty:calendar:list', 'CalendarOutline', 8, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (289, 279, 'AI 日报', 2, '/realty/report', 'realty/report/index', 'realty:report:list', 'NewspaperOutline', 9, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (290, 279, '销售质检', 2, '/realty/quality', 'realty/quality/index', 'realty:quality:list', 'CheckmarkCircleOutline', 10, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (291, 279, '场景模板', 2, '/realty/templates', 'realty/templates/index', 'realty:templates:list', 'DocumentTextOutline', 11, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (287, 279, '经营分析', 2, '/realty/analysis', 'realty/analysis/index', 'realty:analysis:list', 'StatsChartOutline', 12, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (292, 279, '数据统计', 2, '/realty/stats', 'realty/stats/DashboardPage', 'realty:stats:list', 'BarChartOutline', 13, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);

-- ===== 2. 智能助手 (sort=2) =====
INSERT INTO `sys_menu` VALUES (280, 0, '智能助手', 1, '', NULL, NULL, 'SparklesOutline', 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (294, 280, 'AI 助手', 2, '/ai/chat', 'ai/chat/index', 'ai:chat:list', 'ChatbubbleOutline', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (295, 280, '知识库管理', 2, '/ai/knowledge', 'ai/knowledge/AiKnowledge', 'ai:knowledge:list', 'LibraryOutline', 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (296, 280, 'AI 实验', 2, '/ai/experiment', 'ai/experiment/AiExperiment', 'ai:experiment:list', 'FlaskOutline', 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (297, 280, '模型与技能', 2, '/ai/config', 'ai/config/index', 'ai:config:list', 'SettingsOutline', 4, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (298, 280, 'AI 记忆', 2, '/ai/memory', 'ai/memory/index', 'ai:memory:list', 'BrainOutline', 5, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (299, 280, 'Skill 管理', 2, '/ai/skills', 'ai/skills/index', 'ai:skills:list', 'ExtensionPuzzleOutline', 6, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (300, 280, 'MCP 工具', 2, '/ai/mcp', 'ai/mcp/index', 'ai:mcp:list', 'ConstructOutline', 7, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);

-- ===== 3. 协作办公 (sort=3) =====
INSERT INTO `sys_menu` VALUES (311, 0, '协作办公', 1, '', NULL, NULL, 'ChatbubblesOutline', 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (139, 311, '即时聊天', 2, '/message/chat', '/message/chat/index', 'sys:chat:list', 'ChatbubbleOutline', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (278, 311, '通讯录', 2, '/contacts', 'contacts/index', 'sys:chat:list', 'BookOutline', 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (135, 311, '系统通知', 2, '/message/notice', '/message/notice/index', 'sys:notice:list', 'NotificationsOutline', 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (136, 135, '新增通知', 3, NULL, NULL, 'sys:notice:add', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (137, 135, '编辑通知', 3, NULL, NULL, 'sys:notice:edit', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (138, 135, '删除通知', 3, NULL, NULL, 'sys:notice:delete', NULL, 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (153, 135, '系统通知', 3, '', '', 'sys:notice:list', '', 0, 1, 1, 0, NOW(), NOW(), 1, 1, 0);
INSERT INTO `sys_menu` VALUES (302, 311, '会议日历', 2, '/meeting/calendar', 'meeting/MeetingCalendar', 'sys:meeting:list', 'CalendarOutline', 4, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (301, 311, '云文档', 2, '/doc', 'doc/index', 'sys:doc:list', 'DocumentOutline', 5, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (276, 311, '智能表格', 2, '/smart-table', 'smarttable/index', 'sys:smarttable:list', 'GridOutline', 6, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (277, 311, '表格模板管理', 2, '/smart-table/template-mgmt', 'smarttable/templates', 'sys:smarttable:template', 'CopyOutline', 7, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (309, 311, '待办事项', 2, '/todo', 'todo/index', 'sys:todo:list', 'CheckboxOutline', 8, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (308, 311, '项目管理', 2, '/project', 'project/index', 'project:list', 'FolderOutline', 9, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);

-- ===== 4. 人事管理 (sort=4) =====
INSERT INTO `sys_menu` VALUES (281, 0, '人事管理', 1, '', NULL, NULL, 'PeopleOutline', 4, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (307, 281, '人事档案', 2, '/hr', 'hr/index', 'hr:manage:list', 'PersonOutline', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (306, 281, '考勤打卡', 2, '/realty/attendance', 'realty/attendance/AttendanceManage', 'realty:attendance:list', 'FingerPrintOutline', 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (310, 281, '绩效考核', 2, '/hr/performance', 'hr/performance', 'hr:performance:list', 'StatsChartOutline', 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (313, 281, '360度评估', 2, '/hr/review-360', 'hr/Review360', 'hr:review360:list', 'PeopleCircleOutline', 4, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);

-- ===== 5. 组织管理 (sort=5) =====
INSERT INTO `sys_menu` VALUES (22, 0, '组织管理', 1, '/org', NULL, NULL, 'BusinessOutline', 5, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (23, 22, '部门管理', 2, '/org/dept', '/org/dept/index', 'sys:dept:list', 'GitNetworkOutline', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (24, 23, '新增部门', 3, NULL, NULL, 'sys:dept:add', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (25, 23, '编辑部门', 3, NULL, NULL, 'sys:dept:edit', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (26, 23, '删除部门', 3, NULL, NULL, 'sys:dept:delete', NULL, 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (146, 23, '部门列表', 3, '', '', 'sys:dept:list', '', 0, 1, 1, 0, NOW(), NOW(), 4, 4, 0);
INSERT INTO `sys_menu` VALUES (27, 22, '岗位管理', 2, '/org/post', '/org/post/index', 'sys:post:list', 'IdCardOutline', 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (28, 27, '新增岗位', 3, NULL, NULL, 'sys:post:add', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (29, 27, '编辑岗位', 3, NULL, NULL, 'sys:post:edit', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (30, 27, '删除岗位', 3, NULL, NULL, 'sys:post:delete', NULL, 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (147, 27, '岗位列表', 3, '', '', 'sys:post:list', '', 0, 1, 1, 0, NOW(), NOW(), 4, 4, 0);

-- ===== 6. 系统管理 (sort=6) =====
INSERT INTO `sys_menu` VALUES (1, 0, '系统管理', 1, '/system', NULL, NULL, 'SettingsOutline', 6, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (2, 1, '用户管理', 2, '/system/user', '/system/user/index', 'sys:user:list', 'PersonOutline', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (142, 2, '用户列表', 3, '', '', 'sys:user:list', '', 0, 1, 1, 0, NOW(), NOW(), 4, 4, 0);
INSERT INTO `sys_menu` VALUES (3, 2, '新增用户', 3, NULL, NULL, 'sys:user:add', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (4, 2, '编辑用户', 3, NULL, NULL, 'sys:user:edit', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (5, 2, '删除用户', 3, NULL, NULL, 'sys:user:delete', NULL, 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (6, 1, '角色管理', 2, '/system/role', '/system/role/index', 'sys:role:list', 'PeopleOutline', 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (143, 6, '角色列表', 3, '', '', 'sys:role:list', '', 0, 1, 1, 0, NOW(), NOW(), 4, 4, 0);
INSERT INTO `sys_menu` VALUES (7, 6, '新增角色', 3, NULL, NULL, 'sys:role:add', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (8, 6, '编辑角色', 3, NULL, NULL, 'sys:role:edit', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (9, 6, '删除角色', 3, NULL, NULL, 'sys:role:delete', NULL, 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (10, 1, '菜单管理', 2, '/system/menu', '/system/menu/index', 'sys:menu:list', 'MenuOutline', 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (144, 10, '菜单列表', 3, '', '', 'sys:menu:list', '', 0, 1, 1, 0, NOW(), NOW(), 4, 4, 0);
INSERT INTO `sys_menu` VALUES (11, 10, '新增菜单', 3, NULL, NULL, 'sys:menu:add', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (12, 10, '编辑菜单', 3, NULL, NULL, 'sys:menu:edit', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (13, 10, '删除菜单', 3, NULL, NULL, 'sys:menu:delete', NULL, 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (14, 1, '字典管理', 2, '/system/dict', '/system/dict/index', 'sys:dict:list', 'BookOutline', 4, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (145, 14, '字典列表', 3, '', '', 'sys:dict:list', '', 0, 1, 1, 0, NOW(), NOW(), 4, 4, 0);
INSERT INTO `sys_menu` VALUES (15, 14, '新增字典', 3, NULL, NULL, 'sys:dict:add', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (16, 14, '编辑字典', 3, NULL, NULL, 'sys:dict:edit', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (17, 14, '删除字典', 3, NULL, NULL, 'sys:dict:delete', NULL, 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (18, 1, '系统配置', 2, '/system/config', '/system/config/index', 'sys:config:list', 'SettingsSharp', 5, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (19, 18, '新增配置', 3, NULL, NULL, 'sys:config:add', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (20, 18, '编辑配置', 3, NULL, NULL, 'sys:config:edit', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (21, 18, '删除配置', 3, NULL, NULL, 'sys:config:delete', NULL, 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (127, 1, '文件列表', 2, '/system/file', '/system/file/index', 'sys:file:list', 'DocumentOutline', 6, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (152, 127, '文件列表', 3, '', '', 'sys:file:list', '', 0, 1, 1, 0, NOW(), NOW(), 1, 1, 0);
INSERT INTO `sys_menu` VALUES (128, 127, '上传文件', 3, NULL, NULL, 'sys:file:upload', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (129, 127, '删除文件', 3, NULL, NULL, 'sys:file:delete', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (130, 1, '文件配置', 2, '/system/file-config', '/system/file-config/index', 'sys:fileConfig:list', 'CloudOutline', 7, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (131, 130, '新增配置', 3, NULL, NULL, 'sys:fileConfig:add', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (132, 130, '编辑配置', 3, NULL, NULL, 'sys:fileConfig:edit', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (133, 130, '删除配置', 3, NULL, NULL, 'sys:fileConfig:delete', NULL, 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (303, 1, '渠道推送', 2, '/system/channel', 'system/channel/index', 'sys:channel:list', 'NotificationsOutline', 8, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (304, 1, '自动化规则', 2, '/system/automation', 'system/automation/index', 'sys:automation:list', 'GitCompareOutline', 9, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (170, 1, '客户表', 2, '/system/customer', '/system/customer/index', '', 'ListOutline', 99, 0, 0, 0, NOW(), NOW(), NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (171, 170, '客户表查询', 3, '', '', 'system:customer:list', '', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (172, 170, '客户表详情', 3, '', '', 'system:customer:query', '', 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (173, 170, '客户表新增', 3, '', '', 'system:customer:add', '', 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (174, 170, '客户表修改', 3, '', '', 'system:customer:edit', '', 4, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (175, 170, '客户表删除', 3, '', '', 'system:customer:remove', '', 5, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);

-- ===== 7. 系统监控 (sort=7) =====
INSERT INTO `sys_menu` VALUES (36, 0, '系统监控', 1, '/monitor', NULL, NULL, 'PulseOutline', 7, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (37, 36, '在线用户', 2, '/monitor/online', '/monitor/online/index', 'monitor:online:list', 'PeopleCircleOutline', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (150, 37, '在线用户列表', 3, '', '', 'monitor:online:list', '', 0, 1, 1, 0, NOW(), NOW(), 4, 4, 0);
INSERT INTO `sys_menu` VALUES (38, 37, '强退用户', 3, NULL, NULL, 'monitor:online:forceLogout', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (39, 36, '定时任务', 2, '/monitor/job', '/monitor/job/index', 'monitor:job:list', 'TimerOutline', 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (151, 39, '任务列表', 3, '', '', 'monitor:job:list', '', 0, 1, 1, 0, NOW(), NOW(), 4, 4, 0);
INSERT INTO `sys_menu` VALUES (40, 39, '新增任务', 3, NULL, NULL, 'monitor:job:add', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (41, 39, '编辑任务', 3, NULL, NULL, 'monitor:job:edit', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (42, 39, '删除任务', 3, NULL, NULL, 'monitor:job:delete', NULL, 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (43, 36, '缓存监控', 2, '/monitor/cache', '/monitor/cache/index', 'monitor:cache:list', 'ServerOutline', 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (44, 43, '删除缓存', 3, NULL, NULL, 'monitor:cache:delete', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (45, 36, '服务监控', 2, '/monitor/server', '/monitor/server/index', 'monitor:server:list', 'DesktopOutline', 4, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (154, 36, '服务器管理', 2, '/monitor/server-manager', '/monitor/server-manager/index', NULL, 'ServerOutline', 5, 1, 1, 0, NOW(), NOW(), NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (155, 154, '服务器列表', 3, NULL, NULL, 'monitor:server:list', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (156, 154, '服务器详情', 3, NULL, NULL, 'monitor:server:query', NULL, 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (157, 154, '新增服务器', 3, NULL, NULL, 'monitor:server:add', NULL, 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (158, 154, '编辑服务器', 3, NULL, NULL, 'monitor:server:edit', NULL, 4, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (159, 154, '删除服务器', 3, NULL, NULL, 'monitor:server:remove', NULL, 5, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (160, 154, '测试连接', 3, NULL, NULL, 'monitor:server:test', NULL, 6, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (305, 36, 'AI 巡检', 2, '/monitor/heartbeat', 'monitor/heartbeat/index', 'monitor:heartbeat:list', 'PulseOutline', 6, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);

-- ===== 8. 系统日志 (sort=8) =====
INSERT INTO `sys_menu` VALUES (31, 0, '系统日志', 1, '/log', NULL, NULL, 'DocumentTextOutline', 8, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (32, 31, '操作日志', 2, '/log/operlog', '/log/operlog/index', 'monitor:operlog:list', 'ListOutline', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (148, 32, '查询日志', 3, '', '', 'monitor:operlog:list', '', 0, 1, 1, 0, NOW(), NOW(), 4, 4, 0);
INSERT INTO `sys_menu` VALUES (33, 32, '删除日志', 3, NULL, NULL, 'monitor:operlog:delete', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (34, 31, '登录日志', 2, '/log/loginlog', '/log/loginlog/index', 'monitor:loginlog:list', 'LogInOutline', 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (149, 34, '日志列表', 3, '', '', 'monitor:loginlog:list', '', 0, 1, 1, 0, NOW(), NOW(), 4, 4, 0);
INSERT INTO `sys_menu` VALUES (35, 34, '删除日志', 3, NULL, NULL, 'monitor:loginlog:delete', NULL, 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);

-- ===== 9. 开发工具 (sort=99) =====
INSERT INTO `sys_menu` VALUES (161, 0, '开发工具', 1, '', '', '', 'HammerOutline', 99, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (162, 161, '代码生成', 2, '/tool/gen', '/tool/gen/index', '', 'CodeSlashOutline', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (163, 162, '查询', 3, '', '', 'tool:gen:list', '', 1, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (164, 162, '详情', 3, '', '', 'tool:gen:query', '', 2, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (165, 162, '导入', 3, '', '', 'tool:gen:import', '', 3, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (166, 162, '编辑', 3, '', '', 'tool:gen:edit', '', 4, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (167, 162, '删除', 3, '', '', 'tool:gen:remove', '', 5, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (168, 162, '预览', 3, '', '', 'tool:gen:preview', '', 6, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);
INSERT INTO `sys_menu` VALUES (169, 162, '生成代码', 3, '', '', 'tool:gen:code', '', 7, 1, 1, 0, NOW(), NOW(), NULL, NULL, 0);

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '通知内容',
  `notice_type` tinyint NULL DEFAULT 1 COMMENT '通知类型(1通知 2公告)',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态(0草稿 1发布)',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者ID',
  `create_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者名称',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notice_type`(`notice_type` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统通知表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES (1, '欢迎使用鹏程管理系统', '欢迎使用鹏程后台管理系统！这是一条测试通知。', 1, 1, 1, '超级管理员', '2026-01-30 23:53:55', '2026-01-30 23:53:55', 0);

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模块标题',
  `business_type` int NULL DEFAULT 0 COMMENT '业务类型(0-其它 1-新增 2-修改 3-删除)',
  `method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方式',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人员',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '主机地址',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '返回参数',
  `status` int NULL DEFAULT 0 COMMENT '操作状态(0-正常 1-异常)',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误消息',
  `oper_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint NULL DEFAULT 0 COMMENT '消耗时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 371 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------
INSERT INTO `sys_oper_log` VALUES (310, '批量删除文件', 3, 'com.pengcheng.admin.controller.file.SysFileController.deleteBatch()', 'DELETE', 'admin', '/api/sys/file/batch', '127.0.0.1', '[43,42]', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 11:49:00', 0);
INSERT INTO `sys_oper_log` VALUES (311, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":44,\"originalName\":\"img_v3_02ui_f0990cef-9f0d-4c6e-bba2-005ed10e088g.jpg\",\"fileName\":\"e3065b470f9342f2bfeef9aa35182d39.jpg\",\"filePath\":\"2026/02/09/e3065b470f9342f2bfeef9aa35182d39.jpg\",\"url\":\"/api/files/2026/02/09/e3065b470f9342f2bfeef9aa35182d39.jpg\",\"fileSize\":101083,\"fileType\":\"image/jpeg\",\"fileSuffix\":\".jpg\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":null,\"createBy\":\"1\",\"createTime\":\"2026-02-09T11:49:04.5790909\",\"remark\":null}}', 0, NULL, '2026-02-09 11:49:05', 0);
INSERT INTO `sys_oper_log` VALUES (312, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":5,\"createTime\":\"2026-01-31T22:30:46\",\"updateTime\":\"2026-01-31T22:30:46\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"deptId\":2,\"deptName\":null,\"username\":\"mars666\",\"password\":null,\"nickname\":\"mars666\",\"avatar\":null,\"email\":null,\"phone\":null,\"gender\":1,\"status\":1,\"remark\":null,\"userType\":\"admin\",\"openId\":null},\"roleIds\":[2]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 12:04:11', 0);
INSERT INTO `sys_oper_log` VALUES (313, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":5,\"createTime\":\"2026-01-31T22:30:46\",\"updateTime\":\"2026-01-31T22:30:46\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"deptId\":2,\"deptName\":null,\"username\":\"mars666\",\"password\":null,\"nickname\":\"mars666\",\"avatar\":null,\"email\":\"wqexpore@163.com\",\"phone\":\"18888888888\",\"gender\":1,\"status\":1,\"remark\":null,\"userType\":\"admin\",\"openId\":null},\"roleIds\":[2]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 12:04:23', 0);
INSERT INTO `sys_oper_log` VALUES (314, '删除代码生成表', 3, 'com.pengcheng.admin.controller.gen.GenController.remove()', 'DELETE', 'admin', '/api/tool/gen/3,2,1', '127.0.0.1', '[3,2,1]', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 12:44:22', 0);
INSERT INTO `sys_oper_log` VALUES (315, '导入表结构', 1, 'com.pengcheng.admin.controller.gen.GenController.importTable()', 'POST', 'admin', '/api/tool/gen/import', '127.0.0.1', '[\"student\"]', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 12:44:26', 0);
INSERT INTO `sys_oper_log` VALUES (316, '生成代码到项目', 1, 'com.pengcheng.admin.controller.gen.GenController.generateToProject()', 'POST', 'admin', '/api/tool/gen/generate/4', '127.0.0.1', '4', '{\"code\":200,\"message\":\"操作成功\",\"data\":[\"/pengcheng-core/pengcheng-system/src/main/java/com/pengcheng/system/entity/Student.java\",\"/pengcheng-core/pengcheng-system/src/main/java/com/pengcheng/system/mapper/StudentMapper.java\",\"/pengcheng-core/pengcheng-system/src/main/java/com/pengcheng/system/service/StudentService.java\",\"/pengcheng-core/pengcheng-system/src/main/java/com/pengcheng/system/service/impl/StudentServiceImpl.java\",\"/pengcheng-api/pengcheng-admin-api/src/main/java/com/pengcheng/admin/controller/system/StudentController.java\",\"/pengcheng-ui/src/api/student.ts\",\"/pengcheng-ui/src/views/system/student/index.vue\",\"[数据库] 菜单数据已自动创建\"]}', 0, NULL, '2026-02-09 12:46:12', 0);
INSERT INTO `sys_oper_log` VALUES (317, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":1,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"超级管理员\",\"code\":\"admin\",\"sort\":1,\"status\":1,\"remark\":\"拥有所有权限\"},\"menuIds\":[7,8,9,143,142,3,4,5,144,11,12,13,145,15,16,17,19,20,21,146,24,25,26,147,28,29,30,148,33,149,35,150,38,151,40,41,42,44,45,153,136,137,138,139,141,155,156,157,158,159,160,163,164,165,166,167,168,169,171,172,173,174,175,273,274,275,276,277,152,128,129]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 12:47:51', 0);
INSERT INTO `sys_oper_log` VALUES (318, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":1,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"超级管理员\",\"code\":\"admin\",\"sort\":1,\"status\":1,\"remark\":\"拥有所有权限\"},\"menuIds\":[7,8,9,143,142,3,4,5,144,11,12,13,145,15,16,17,19,20,21,146,24,25,26,147,28,29,30,148,33,149,35,150,38,151,40,41,42,44,45,153,136,137,138,139,141,155,156,157,158,159,160,163,164,165,166,167,168,169,171,172,173,174,175,273,274,275,276,277,152,128,129]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 12:48:12', 0);
INSERT INTO `sys_oper_log` VALUES (319, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":1,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"超级管理员\",\"code\":\"admin\",\"sort\":1,\"status\":1,\"remark\":\"拥有所有权限\"},\"menuIds\":[7,8,9,143,142,3,4,5,144,11,12,13,145,15,16,17,19,20,21,146,24,25,26,147,28,29,30,148,33,149,35,150,38,151,40,41,42,44,45,153,136,137,138,139,141,155,156,157,158,159,160,163,164,165,166,167,168,169,171,172,173,174,175,273,274,275,276,277,152,128,129]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 12:48:47', 0);
INSERT INTO `sys_oper_log` VALUES (320, '菜单管理', 2, 'com.pengcheng.admin.controller.system.SysMenuController.update()', 'PUT', 'admin', '/api/sys/menu', '127.0.0.1', '{\"id\":272,\"createTime\":\"2026-02-03T08:58:09\",\"updateTime\":\"2026-02-03T08:58:09\",\"createBy\":1,\"updateBy\":1,\"deleted\":0,\"parentId\":161,\"name\":\"学生管理\",\"type\":2,\"path\":\"system/student\",\"component\":\"system/student/index\",\"permission\":\"system:student:list\",\"icon\":\"ListOutline\",\"sort\":1,\"visible\":1,\"status\":1,\"isFrame\":0,\"children\":[{\"id\":273,\"createTime\":\"2026-02-03T08:58:09\",\"updateTime\":\"2026-02-03T08:58:09\",\"createBy\":1,\"updateBy\":1,\"deleted\":0,\"parentId\":272,\"name\":\"学生管理查询\",\"type\":3,\"path\":null,\"component\":null,\"permission\":\"system:student:list\",\"icon\":null,\"sort\":1,\"visible\":1,\"status\":1,\"isFrame\":0,\"children\":null},{\"id\":274,\"createTime\":\"2026-02-03T08:58:09\",\"updateTime\":\"2026-02-03T08:58:09\",\"createBy\":1,\"updateBy\":1,\"deleted\":0,\"parentId\":272,\"name\":\"学生管理详情\",\"type\":3,\"path\":null,\"component\":null,\"permission\":\"system:student:query\",\"icon\":null,\"sort\":2,\"visible\":1,\"status\":1,\"isFrame\":0,\"children\":null},{\"id\":275,\"createTime\":\"2026-02-03T08:58:09\",\"updateTime\":\"2026-02-03T08:58:09\",\"createBy\":1,\"updateBy\":1,\"deleted\":0,\"parentId\":272,\"name\":\"学生管理新增\",\"type\":3,\"path\":null,\"component\":null,\"permission\":\"system:student:add\",\"icon\":null,\"sort\":3,\"visible\":1,\"status\":1,\"isFrame\":0,\"children\":null},{\"id\":276,\"createTime\":\"2026-02-03T08:58:09\",\"updateTime\":\"2026-02-03T08:58:09\",\"createBy\":1,\"updateBy\":1,\"deleted\":0,\"parentId\":272,\"name\":\"学生管理修改\",\"type\":3,\"path\":null,\"component\":null,\"permission\":\"system:student:edit\",\"icon\":null,\"sort\":4,\"visible\":1,\"status\":1,\"isFrame\":0,\"children\":null},{\"id\":277,\"createTime\":\"2026-02-03T08:58:09\",\"updateTime\":\"2026-02-03T08:58:09\",\"createBy\":1,\"updateBy\":1,\"deleted\":0,\"parentId\":272,\"name\":\"学生管理删除\",\"type\":3,\"path\":null,\"component\":null,\"permission\":\"system:student:remove\",\"icon\":null,\"sort\":5,\"visible\":1,\"status\":1,\"isFrame\":0,\"children\":null}]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 12:55:15', 0);
INSERT INTO `sys_oper_log` VALUES (321, '学生表', 1, 'com.pengcheng.web.controller.system.StudentController.add()', 'POST', 'admin', '/api/system/student', '127.0.0.1', '{\"id\":4,\"studentNo\":\"12\",\"name\":\"12\",\"gender\":null,\"birthday\":null,\"phone\":\"1\",\"email\":\"1\",\"address\":\"1\",\"classId\":1,\"status\":null,\"deleted\":null,\"createTime\":\"2026-02-09T12:55:30.0402385\",\"updateTime\":\"2026-02-09T12:55:30.0402385\"}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 12:55:30', 0);
INSERT INTO `sys_oper_log` VALUES (322, '学生表', 3, 'com.pengcheng.web.controller.system.StudentController.remove()', 'DELETE', 'admin', '/api/system/student/4', '127.0.0.1', '[4]', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 12:55:34', 0);
INSERT INTO `sys_oper_log` VALUES (323, '移除已生成代码', 3, 'com.pengcheng.admin.controller.gen.GenController.removeGeneratedCode()', 'DELETE', 'admin', '/api/tool/gen/remove-code/4', '127.0.0.1', '4', '{\"code\":200,\"message\":\"操作成功\",\"data\":[\"/pengcheng-core/pengcheng-system/src/main/java/com/pengcheng/system/entity/Student.java\",\"/pengcheng-core/pengcheng-system/src/main/java/com/pengcheng/system/mapper/StudentMapper.java\",\"/pengcheng-core/pengcheng-system/src/main/java/com/pengcheng/system/service/StudentService.java\",\"/pengcheng-core/pengcheng-system/src/main/java/com/pengcheng/system/service/impl/StudentServiceImpl.java\",\"/pengcheng-api/pengcheng-admin-api/src/main/java/com/pengcheng/admin/controller/system/StudentController.java\",\"/pengcheng-ui/src/api/student.ts\",\"/pengcheng-ui/src/views/system/student/index.vue\",\"[数据库] 菜单数据已删除\"]}', 0, NULL, '2026-02-09 12:55:57', 0);
INSERT INTO `sys_oper_log` VALUES (324, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":2,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"普通用户\",\"code\":\"user\",\"sort\":2,\"status\":1,\"remark\":\"普通用户角色\"},\"menuIds\":[139,153,136,137,138,163,164,165,166,167,168,169,135,162,134,161,36,37,150,38,39,151,40,41,42,43,44,45,154,155,156,157,158,159,160,31,32,148,33,34,149,35]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 12:56:49', 0);
INSERT INTO `sys_oper_log` VALUES (325, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":2,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"普通用户\",\"code\":\"user\",\"sort\":2,\"status\":1,\"dataScope\":5,\"remark\":\"普通用户角色\"},\"menuIds\":[139,153,136,137,138,163,164,165,166,167,168,169,150,38,151,40,41,42,44,45,155,156,157,158,159,160,148,33,149,35,32,34,37,39,43,154,135,162,31,36,134,161,1,2,142,3,4,5,170,171,172,173,174,175,6,143,7,8,9,10,144,11,12,13,14,145,15,16,17,18,19,20,21,22,23,146,24,25,26,27,147,28,29,30,126,127,152,128,129,140,141],\"deptIds\":[]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:19:31', 0);
INSERT INTO `sys_oper_log` VALUES (326, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.resetPassword()', 'POST', 'admin', '/api/sys/user/5/reset-password', '127.0.0.1', '5', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:25:16', 0);
INSERT INTO `sys_oper_log` VALUES (327, '用户管理', 1, 'com.pengcheng.admin.controller.system.SysUserController.create()', 'POST', 'mars666', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":11,\"createTime\":\"2026-02-09T14:31:09.2982817\",\"updateTime\":\"2026-02-09T14:31:09.2992778\",\"createBy\":5,\"updateBy\":5,\"deleted\":null,\"deptId\":2,\"deptName\":null,\"username\":\"mars02\",\"password\":\"$2a$10$4LVE/zBTH6smGMjfOu/bku1PBaFun3JpFCYG1bTZgPHJQuE3hyjVC\",\"nickname\":\"mars02\",\"avatar\":null,\"email\":\"111@qq.com\",\"phone\":\"18888888888\",\"gender\":1,\"status\":1,\"remark\":\"\",\"userType\":\"admin\",\"openId\":null},\"roleIds\":[2]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:31:09', 0);
INSERT INTO `sys_oper_log` VALUES (328, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":2,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"普通用户\",\"code\":\"user\",\"sort\":2,\"status\":1,\"dataScope\":3,\"remark\":\"普通用户角色\"},\"menuIds\":[139,153,136,137,138,163,164,165,166,167,168,169,150,38,151,40,41,42,44,45,155,156,157,158,159,160,148,33,149,35,142,3,4,5,171,172,173,174,175,143,7,8,9,144,11,12,13,145,15,16,17,19,20,21,146,24,25,26,147,28,29,30,152,128,129,141],\"deptIds\":[]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:31:56', 0);
INSERT INTO `sys_oper_log` VALUES (329, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":2,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"普通用户\",\"code\":\"user\",\"sort\":2,\"status\":1,\"dataScope\":3,\"remark\":\"普通用户角色\"},\"menuIds\":[139,153,136,137,138,163,164,165,166,167,168,169,150,38,151,40,41,42,44,45,155,156,157,158,159,160,148,33,149,35,142,3,4,5,171,172,173,174,175,143,7,8,9,144,11,12,13,145,15,16,17,19,20,21,146,24,25,26,147,28,29,30,152,128,129,141],\"deptIds\":[]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:32:55', 0);
INSERT INTO `sys_oper_log` VALUES (330, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":11,\"createTime\":\"2026-02-09T14:31:09\",\"updateTime\":\"2026-02-09T14:31:09\",\"createBy\":5,\"updateBy\":5,\"deleted\":0,\"deptId\":2,\"deptName\":null,\"username\":\"mars02\",\"password\":null,\"nickname\":\"mars02\",\"avatar\":null,\"email\":\"111@qq.com\",\"phone\":\"18888888888\",\"gender\":1,\"status\":1,\"remark\":\"\",\"userType\":\"admin\",\"openId\":null},\"roleIds\":[2]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:33:01', 0);
INSERT INTO `sys_oper_log` VALUES (331, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":2,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"普通用户\",\"code\":\"user\",\"sort\":2,\"status\":1,\"dataScope\":1,\"remark\":\"普通用户角色\"},\"menuIds\":[139,153,136,137,138,163,164,165,166,167,168,169,150,38,151,40,41,42,44,45,155,156,157,158,159,160,148,33,149,35,142,3,4,5,171,172,173,174,175,143,7,8,9,144,11,12,13,145,15,16,17,19,20,21,146,24,25,26,147,28,29,30,152,128,129,141],\"deptIds\":[]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:33:57', 0);
INSERT INTO `sys_oper_log` VALUES (332, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":2,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"普通用户\",\"code\":\"user\",\"sort\":2,\"status\":1,\"dataScope\":1,\"remark\":\"普通用户角色\"},\"menuIds\":[139,153,136,137,138,150,38,151,40,41,42,44,45,155,156,157,158,159,160,148,33,149,35,142,3,4,5,171,172,173,174,175,143,7,8,9,144,11,12,13,145,15,16,17,19,20,21,146,24,25,26,147,28,29,30,152,128,129,141,2,170,6,10,14,18,23,27,32,34,37,39,43,154,127,135,1,22,31,36,126,134,140],\"deptIds\":[]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:42:29', 0);
INSERT INTO `sys_oper_log` VALUES (333, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":2,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"普通用户\",\"code\":\"user\",\"sort\":2,\"status\":1,\"dataScope\":1,\"remark\":\"普通用户角色\"},\"menuIds\":[139,153,136,137,138,150,38,151,40,41,42,44,45,155,156,157,158,159,160,148,33,149,35,142,3,4,5,171,172,173,174,175,143,7,8,9,144,11,12,13,145,15,16,17,19,20,21,146,24,25,26,147,28,29,30,152,128,129,141,2,170,6,10,14,18,23,27,32,34,37,39,43,154,127,135,1,22,31,36,126,134,140,161,162,163,164,165,166,167,168,169],\"deptIds\":[]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:42:53', 0);
INSERT INTO `sys_oper_log` VALUES (334, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":2,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"普通用户\",\"code\":\"user\",\"sort\":2,\"status\":1,\"dataScope\":3,\"remark\":\"普通用户角色\"},\"menuIds\":[139,153,136,137,138,150,38,151,40,41,42,44,45,155,156,157,158,159,160,148,33,149,35,142,3,4,5,171,172,173,174,175,143,7,8,9,144,11,12,13,145,15,16,17,19,20,21,146,24,25,26,147,28,29,30,152,128,129,141,163,164,165,166,167,168,169],\"deptIds\":[]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:43:15', 0);
INSERT INTO `sys_oper_log` VALUES (335, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":1,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"超级管理员\",\"code\":\"admin\",\"sort\":1,\"status\":1,\"dataScope\":1,\"remark\":\"拥有所有权限\"},\"menuIds\":[7,8,9,143,142,3,4,5,144,11,12,13,145,15,16,17,19,20,21,146,24,25,26,147,28,29,30,148,33,149,35,150,38,151,40,41,42,44,45,153,136,137,138,139,141,155,156,157,158,159,160,171,172,173,174,175,152,128,129,2,170,6,10,14,18,23,27,32,34,37,39,43,154,127,135,1,22,31,36,126,134,140],\"deptIds\":[]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:45:24', 0);
INSERT INTO `sys_oper_log` VALUES (336, '角色管理', 2, 'com.pengcheng.admin.controller.system.SysRoleController.update()', 'PUT', 'admin', '/api/sys/role', '127.0.0.1', '{\"role\":{\"id\":1,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"name\":\"超级管理员\",\"code\":\"admin\",\"sort\":1,\"status\":1,\"dataScope\":1,\"remark\":\"拥有所有权限\"},\"menuIds\":[7,8,9,143,142,3,4,5,144,11,12,13,145,15,16,17,19,20,21,146,24,25,26,147,28,29,30,148,33,149,35,150,38,151,40,41,42,44,45,153,136,137,138,139,141,155,156,157,158,159,160,171,172,173,174,175,152,128,129,2,170,6,10,14,18,23,27,32,34,37,39,43,154,127,135,1,22,31,36,126,134,140,161,162,163,164,165,166,167,168,169],\"deptIds\":[]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:45:28', 0);
INSERT INTO `sys_oper_log` VALUES (337, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":6,\"createTime\":\"2026-02-07T16:01:02\",\"updateTime\":\"2026-02-07T16:01:02\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"deptId\":2,\"deptName\":null,\"username\":\"test01\",\"password\":null,\"nickname\":\"test01\",\"avatar\":null,\"email\":null,\"phone\":null,\"gender\":0,\"status\":1,\"remark\":null,\"userType\":\"admin\",\"openId\":null},\"roleIds\":[2]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:47:28', 0);
INSERT INTO `sys_oper_log` VALUES (338, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":2,\"createTime\":\"2026-01-29T23:21:12\",\"updateTime\":\"2026-01-29T23:21:12\",\"createBy\":1,\"updateBy\":1,\"deleted\":0,\"deptId\":4,\"deptName\":null,\"username\":\"test\",\"password\":null,\"nickname\":\"test\",\"avatar\":null,\"email\":\"111@qq.com\",\"phone\":\"1888888881\",\"gender\":1,\"status\":0,\"remark\":\"\",\"userType\":\"admin\",\"openId\":null},\"roleIds\":[2]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:48:30', 0);
INSERT INTO `sys_oper_log` VALUES (339, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":5,\"createTime\":\"2026-01-31T22:30:46\",\"updateTime\":\"2026-02-09T14:30:30\",\"createBy\":5,\"updateBy\":1,\"deleted\":0,\"deptId\":2,\"deptName\":null,\"username\":\"mars666\",\"password\":null,\"nickname\":\"mars666\",\"avatar\":null,\"email\":\"wqexpore@163.com\",\"phone\":\"18888888882\",\"gender\":1,\"status\":1,\"remark\":null,\"userType\":\"admin\",\"openId\":null},\"roleIds\":[2]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:48:34', 0);
INSERT INTO `sys_oper_log` VALUES (340, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.toggleQuit()', 'POST', 'admin', '/api/sys/user/2/quit', '127.0.0.1', '2', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:58:26', 0);
INSERT INTO `sys_oper_log` VALUES (341, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":2,\"createTime\":\"2026-01-29T23:21:12\",\"updateTime\":\"2026-01-29T23:21:12\",\"createBy\":1,\"updateBy\":1,\"deleted\":0,\"deptId\":4,\"deptName\":null,\"username\":\"test\",\"password\":null,\"nickname\":\"test\",\"avatar\":null,\"email\":\"111@qq.com\",\"phone\":\"1888888881\",\"gender\":1,\"status\":1,\"remark\":\"\",\"userType\":\"admin\",\"openId\":null,\"isQuit\":1},\"roleIds\":[2]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 14:58:32', 0);
INSERT INTO `sys_oper_log` VALUES (342, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.toggleQuit()', 'POST', 'admin', '/api/sys/user/2/quit', '127.0.0.1', '2', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:01:03', 0);
INSERT INTO `sys_oper_log` VALUES (343, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":11,\"createTime\":\"2026-02-09T14:31:09\",\"updateTime\":\"2026-02-09T14:31:09\",\"createBy\":5,\"updateBy\":5,\"deleted\":0,\"deptId\":2,\"deptName\":null,\"username\":\"mars02\",\"password\":null,\"nickname\":\"mars02\",\"avatar\":null,\"email\":\"111@qq.com\",\"phone\":\"18888888888\",\"gender\":1,\"status\":1,\"remark\":\"\",\"userType\":\"admin\",\"openId\":null,\"isQuit\":0},\"roleIds\":[2],\"postIds\":[2]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:39:33', 0);
INSERT INTO `sys_oper_log` VALUES (344, '岗位管理', 2, 'com.pengcheng.admin.controller.system.SysPostController.update()', 'PUT', 'admin', '/api/sys/post', '127.0.0.1', '{\"id\":4,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"parentId\":1,\"children\":null,\"postCode\":\"dev\",\"postName\":\"开发工程师\",\"sort\":4,\"status\":1,\"remark\":\"开发工程师\"}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:42:38', 0);
INSERT INTO `sys_oper_log` VALUES (345, '岗位管理', 1, 'com.pengcheng.admin.controller.system.SysPostController.create()', 'POST', 'admin', '/api/sys/post', '127.0.0.1', '{\"id\":6,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":1,\"updateBy\":1,\"deleted\":0,\"parentId\":0,\"children\":null,\"postCode\":\"manager\",\"postName\":\"总经理\",\"sort\":2,\"status\":1,\"remark\":\"\"}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:43:11', 0);
INSERT INTO `sys_oper_log` VALUES (346, '岗位管理', 2, 'com.pengcheng.admin.controller.system.SysPostController.update()', 'PUT', 'admin', '/api/sys/post', '127.0.0.1', '{\"id\":6,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":1,\"updateBy\":1,\"deleted\":0,\"parentId\":1,\"children\":null,\"postCode\":\"manager\",\"postName\":\"总经理\",\"sort\":2,\"status\":1,\"remark\":\"\"}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:44:19', 0);
INSERT INTO `sys_oper_log` VALUES (347, '岗位管理', 2, 'com.pengcheng.admin.controller.system.SysPostController.update()', 'PUT', 'admin', '/api/sys/post', '127.0.0.1', '{\"id\":2,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"parentId\":6,\"children\":null,\"postCode\":\"cto\",\"postName\":\"技术总监\",\"sort\":2,\"status\":1,\"remark\":\"技术总监\"}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:44:43', 0);
INSERT INTO `sys_oper_log` VALUES (348, '岗位管理', 2, 'com.pengcheng.admin.controller.system.SysPostController.update()', 'PUT', 'admin', '/api/sys/post', '127.0.0.1', '{\"id\":4,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"parentId\":2,\"children\":null,\"postCode\":\"dev\",\"postName\":\"开发工程师\",\"sort\":4,\"status\":1,\"remark\":\"开发工程师\"}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:45:03', 0);
INSERT INTO `sys_oper_log` VALUES (349, '岗位管理', 2, 'com.pengcheng.admin.controller.system.SysPostController.update()', 'PUT', 'admin', '/api/sys/post', '127.0.0.1', '{\"id\":3,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:42:08\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"parentId\":2,\"children\":null,\"postCode\":\"pm\",\"postName\":\"产品经理\",\"sort\":3,\"status\":1,\"remark\":\"产品经理\"}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:45:29', 0);
INSERT INTO `sys_oper_log` VALUES (350, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":10,\"createTime\":\"2026-02-08T16:46:40\",\"updateTime\":\"2026-02-08T16:46:40\",\"createBy\":null,\"updateBy\":10,\"deleted\":0,\"deptId\":null,\"deptName\":null,\"username\":\"Mars\",\"password\":null,\"nickname\":\"Mars\",\"avatar\":\"http://m8e8f9e2.natappfree.cc/api/files/images/2026/02/08/3008954e93634f0ebbf0d78f2fe26fc0.jpg\",\"email\":null,\"phone\":null,\"gender\":0,\"status\":1,\"remark\":null,\"userType\":\"app\",\"openId\":\"opzUF43XlvnVUw5S9qS2cI6L7p9M\",\"isQuit\":0},\"roleIds\":[2],\"postIds\":[4]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:46:01', 0);
INSERT INTO `sys_oper_log` VALUES (351, '岗位管理', 1, 'com.pengcheng.admin.controller.system.SysPostController.create()', 'POST', 'admin', '/api/sys/post', '127.0.0.1', '{\"id\":7,\"createTime\":\"2026-02-09T15:47:34.7205627\",\"updateTime\":\"2026-02-09T15:47:34.7205627\",\"createBy\":1,\"updateBy\":1,\"deleted\":null,\"parentId\":0,\"children\":null,\"postCode\":\"test_coder\",\"postName\":\"测试工程师\",\"sort\":0,\"status\":1,\"remark\":\"\"}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:47:35', 0);
INSERT INTO `sys_oper_log` VALUES (352, '岗位管理', 2, 'com.pengcheng.admin.controller.system.SysPostController.move()', 'POST', 'admin', '/api/sys/post/7/move', '127.0.0.1', '7 2', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:50:58', 0);
INSERT INTO `sys_oper_log` VALUES (353, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":5,\"createTime\":\"2026-01-31T22:30:46\",\"updateTime\":\"2026-02-09T14:30:30\",\"createBy\":5,\"updateBy\":1,\"deleted\":0,\"deptId\":2,\"deptName\":null,\"username\":\"mars666\",\"password\":null,\"nickname\":\"mars666\",\"avatar\":null,\"email\":\"wqexpore@163.com\",\"phone\":\"18888888882\",\"gender\":1,\"status\":1,\"remark\":null,\"userType\":\"admin\",\"openId\":null,\"isQuit\":0},\"roleIds\":[2],\"postIds\":[3,4]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 15:55:56', 0);
INSERT INTO `sys_oper_log` VALUES (354, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":10,\"createTime\":\"2026-02-08T16:46:40\",\"updateTime\":\"2026-02-08T16:46:40\",\"createBy\":null,\"updateBy\":10,\"deleted\":0,\"deptId\":2,\"deptName\":null,\"postNames\":null,\"username\":\"Mars\",\"password\":null,\"nickname\":\"Mars\",\"avatar\":\"http://m8e8f9e2.natappfree.cc/api/files/images/2026/02/08/3008954e93634f0ebbf0d78f2fe26fc0.jpg\",\"email\":null,\"phone\":null,\"gender\":0,\"status\":1,\"remark\":null,\"userType\":\"app\",\"openId\":\"opzUF43XlvnVUw5S9qS2cI6L7p9M\",\"isQuit\":0},\"roleIds\":[2],\"postIds\":[4]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 16:21:54', 0);
INSERT INTO `sys_oper_log` VALUES (355, '用户管理', 2, 'com.pengcheng.admin.controller.system.SysUserController.update()', 'PUT', 'admin', '/api/sys/user', '127.0.0.1', '{\"user\":{\"id\":1,\"createTime\":\"2026-01-29T22:42:08\",\"updateTime\":\"2026-01-29T22:58:21\",\"createBy\":null,\"updateBy\":1,\"deleted\":0,\"deptId\":1,\"deptName\":null,\"postNames\":null,\"username\":\"admin\",\"password\":null,\"nickname\":\"超级管理员\",\"avatar\":null,\"email\":null,\"phone\":null,\"gender\":0,\"status\":1,\"remark\":null,\"userType\":\"admin\",\"openId\":null,\"isQuit\":0},\"roleIds\":[1],\"postIds\":[1]}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 16:23:22', 0);
INSERT INTO `sys_oper_log` VALUES (356, '岗位管理', 2, 'com.pengcheng.admin.controller.system.SysPostController.move()', 'POST', 'admin', '/api/sys/post/4/move', '127.0.0.1', '4 1', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 16:27:55', 0);
INSERT INTO `sys_oper_log` VALUES (357, '岗位管理', 2, 'com.pengcheng.admin.controller.system.SysPostController.move()', 'POST', 'admin', '/api/sys/post/4/move', '127.0.0.1', '4 2', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 16:27:58', 0);
INSERT INTO `sys_oper_log` VALUES (358, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '1', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":45,\"originalName\":\"img_v3_02ui_f0990cef-9f0d-4c6e-bba2-005ed10e088g.jpg\",\"fileName\":\"e321bfaf05754cb98700459c53fed98a.jpg\",\"filePath\":\"2026/02/09/e321bfaf05754cb98700459c53fed98a.jpg\",\"url\":\"/api/files/2026/02/09/e321bfaf05754cb98700459c53fed98a.jpg\",\"fileSize\":101083,\"fileType\":\"image/jpeg\",\"fileSuffix\":\".jpg\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":1,\"createBy\":\"1\",\"createTime\":\"2026-02-09T16:52:02.7691962\",\"remark\":null}}', 0, NULL, '2026-02-09 16:52:03', 0);
INSERT INTO `sys_oper_log` VALUES (359, '创建文件分组', 1, 'com.pengcheng.admin.controller.file.SysFileGroupController.create()', 'POST', 'admin', '/api/sys/file-group', '127.0.0.1', '{\"id\":2,\"name\":\"111\",\"sort\":0,\"fileCount\":null,\"createBy\":null,\"createTime\":\"2026-02-09T16:53:03.5460584\",\"updateTime\":null}', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 16:53:04', 0);
INSERT INTO `sys_oper_log` VALUES (360, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '2', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":46,\"originalName\":\"项目汇总交付表.xlsx\",\"fileName\":\"c2a527651b6845f7ac36af3edf32e6c3.xlsx\",\"filePath\":\"2026/02/09/c2a527651b6845f7ac36af3edf32e6c3.xlsx\",\"url\":\"/api/files/2026/02/09/c2a527651b6845f7ac36af3edf32e6c3.xlsx\",\"fileSize\":5925,\"fileType\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"fileSuffix\":\".xlsx\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":2,\"createBy\":\"1\",\"createTime\":\"2026-02-09T16:53:28.72321\",\"remark\":null}}', 0, NULL, '2026-02-09 16:53:29', 0);
INSERT INTO `sys_oper_log` VALUES (361, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '2', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":47,\"originalName\":\"img_v3_02t8_80c939c9-3e79-4c59-b10c-1716a88818ag.jpg\",\"fileName\":\"b4d816e8567f4a219c33b9d0ee1a5615.jpg\",\"filePath\":\"2026/02/09/b4d816e8567f4a219c33b9d0ee1a5615.jpg\",\"url\":\"/api/files/2026/02/09/b4d816e8567f4a219c33b9d0ee1a5615.jpg\",\"fileSize\":108824,\"fileType\":\"image/jpeg\",\"fileSuffix\":\".jpg\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":2,\"createBy\":\"1\",\"createTime\":\"2026-02-09T16:53:52.4690099\",\"remark\":null}}', 0, NULL, '2026-02-09 16:53:52', 0);
INSERT INTO `sys_oper_log` VALUES (362, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '1', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":48,\"originalName\":\"img_v3_02t8_80c939c9-3e79-4c59-b10c-1716a88818ag.jpg\",\"fileName\":\"be55af87595046cdb2834cc611a81f30.jpg\",\"filePath\":\"2026/02/09/be55af87595046cdb2834cc611a81f30.jpg\",\"url\":\"/api/files/2026/02/09/be55af87595046cdb2834cc611a81f30.jpg\",\"fileSize\":108824,\"fileType\":\"image/jpeg\",\"fileSuffix\":\".jpg\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":1,\"createBy\":\"1\",\"createTime\":\"2026-02-09T16:54:07.1678495\",\"remark\":null}}', 0, NULL, '2026-02-09 16:54:07', 0);
INSERT INTO `sys_oper_log` VALUES (363, '岗位管理', 3, 'com.pengcheng.admin.controller.system.SysPostController.delete()', 'DELETE', 'admin', '/api/sys/post/7', '127.0.0.1', '7', '{\"code\":200,\"message\":\"操作成功\",\"data\":null}', 0, NULL, '2026-02-09 17:03:40', 0);
INSERT INTO `sys_oper_log` VALUES (364, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":49,\"originalName\":\"img_v3_02ui_f0990cef-9f0d-4c6e-bba2-005ed10e088g.jpg\",\"fileName\":\"2aa015bf21dd4bf78e25b904507e88a9.jpg\",\"filePath\":\"2026/02/09/2aa015bf21dd4bf78e25b904507e88a9.jpg\",\"url\":\"/api/files/2026/02/09/2aa015bf21dd4bf78e25b904507e88a9.jpg\",\"fileSize\":101083,\"fileType\":\"image/jpeg\",\"fileSuffix\":\".jpg\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":null,\"createBy\":\"1\",\"createTime\":\"2026-02-09T21:05:02.8086823\",\"remark\":null}}', 0, NULL, '2026-02-09 21:05:03', 0);
INSERT INTO `sys_oper_log` VALUES (365, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":50,\"originalName\":\"img_v3_02ui_f0990cef-9f0d-4c6e-bba2-005ed10e088g.jpg\",\"fileName\":\"78ab2e5845d245d4976f18069258a1e3.jpg\",\"filePath\":\"2026/02/09/78ab2e5845d245d4976f18069258a1e3.jpg\",\"url\":\"/api/files/2026/02/09/78ab2e5845d245d4976f18069258a1e3.jpg\",\"fileSize\":101083,\"fileType\":\"image/jpeg\",\"fileSuffix\":\".jpg\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":null,\"createBy\":\"1\",\"createTime\":\"2026-02-09T21:06:37.8053148\",\"remark\":null}}', 0, NULL, '2026-02-09 21:06:38', 0);
INSERT INTO `sys_oper_log` VALUES (366, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":51,\"originalName\":\"img_v3_02un_25630100-71c0-4026-8965-5133f5d735dg.jpg\",\"fileName\":\"d983f41b0997442bacb33a057c3adf56.jpg\",\"filePath\":\"2026/02/09/d983f41b0997442bacb33a057c3adf56.jpg\",\"url\":\"/api/files/2026/02/09/d983f41b0997442bacb33a057c3adf56.jpg\",\"fileSize\":92978,\"fileType\":\"image/jpeg\",\"fileSuffix\":\".jpg\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":null,\"createBy\":\"1\",\"createTime\":\"2026-02-09T21:06:41.6506017\",\"remark\":null}}', 0, NULL, '2026-02-09 21:06:42', 0);
INSERT INTO `sys_oper_log` VALUES (367, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":52,\"originalName\":\"img_v3_02ui_f0990cef-9f0d-4c6e-bba2-005ed10e088g.jpg\",\"fileName\":\"f4b77e83a7974286a5e8e3bd2c0bf6a9.jpg\",\"filePath\":\"2026/02/09/f4b77e83a7974286a5e8e3bd2c0bf6a9.jpg\",\"url\":\"/api/files/2026/02/09/f4b77e83a7974286a5e8e3bd2c0bf6a9.jpg\",\"fileSize\":101083,\"fileType\":\"image/jpeg\",\"fileSuffix\":\".jpg\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":null,\"createBy\":\"1\",\"createTime\":\"2026-02-09T21:06:43.780783\",\"remark\":null}}', 0, NULL, '2026-02-09 21:06:44', 0);
INSERT INTO `sys_oper_log` VALUES (368, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":53,\"originalName\":\"img_v3_02un_25630100-71c0-4026-8965-5133f5d735dg.jpg\",\"fileName\":\"c5b39025dcc5452fb652bdcea9a1618f.jpg\",\"filePath\":\"2026/02/09/c5b39025dcc5452fb652bdcea9a1618f.jpg\",\"url\":\"/api/files/2026/02/09/c5b39025dcc5452fb652bdcea9a1618f.jpg\",\"fileSize\":92978,\"fileType\":\"image/jpeg\",\"fileSuffix\":\".jpg\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":null,\"createBy\":\"1\",\"createTime\":\"2026-02-09T21:06:45.5588546\",\"remark\":null}}', 0, NULL, '2026-02-09 21:06:46', 0);
INSERT INTO `sys_oper_log` VALUES (369, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":54,\"originalName\":\"FPj0uHDhtvlIgwhfEk0-Lu-bp4Duopj5_GorB8Sxbqe4pKUR4-7HxwbA7VFa8fTK.jpg\",\"fileName\":\"23d5546b227743c6b9b24eb19bb50476.jpg\",\"filePath\":\"2026/02/09/23d5546b227743c6b9b24eb19bb50476.jpg\",\"url\":\"/api/files/2026/02/09/23d5546b227743c6b9b24eb19bb50476.jpg\",\"fileSize\":823196,\"fileType\":\"image/jpeg\",\"fileSuffix\":\".jpg\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":null,\"createBy\":\"1\",\"createTime\":\"2026-02-09T21:07:02.8586868\",\"remark\":null}}', 0, NULL, '2026-02-09 21:07:03', 0);
INSERT INTO `sys_oper_log` VALUES (370, '上传文件', 1, 'com.pengcheng.admin.controller.file.SysFileController.upload()', 'POST', 'admin', '/api/sys/file/upload', '127.0.0.1', '', '{\"code\":200,\"message\":\"操作成功\",\"data\":{\"id\":55,\"originalName\":\"caa41ac5c974e4fa96b1df176aeba849.png\",\"fileName\":\"7d8d2dbcb3924c128ac708d3bef8a63d.png\",\"filePath\":\"2026/02/09/7d8d2dbcb3924c128ac708d3bef8a63d.png\",\"url\":\"/api/files/2026/02/09/7d8d2dbcb3924c128ac708d3bef8a63d.png\",\"fileSize\":16286,\"fileType\":\"image/png\",\"fileSuffix\":\".png\",\"storageType\":\"local\",\"bucketName\":null,\"groupId\":null,\"createBy\":\"1\",\"createTime\":\"2026-02-09T21:07:06.2040492\",\"remark\":null}}', 0, NULL, '2026-02-09 21:07:06', 0);

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父岗位ID',
  `post_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `sort` int NULL DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-停用 1-正常)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_code`(`post_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES (1, 0, 'ceo', '董事长', 1, 1, '公司董事长', '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, NULL, 0);
INSERT INTO `sys_post` VALUES (2, 6, 'cto', '技术总监', 2, 1, '技术总监', '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, 1, 0);
INSERT INTO `sys_post` VALUES (3, 2, 'pm', '产品经理', 3, 1, '产品经理', '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, 1, 0);
INSERT INTO `sys_post` VALUES (4, 2, 'dev', '开发工程师', 4, 1, '开发工程师', '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, 1, 0);
INSERT INTO `sys_post` VALUES (6, 1, 'manager', '总经理', 2, 1, '', '2026-01-29 22:42:08', '2026-01-29 22:42:08', 1, 1, 0);
INSERT INTO `sys_post` VALUES (7, 2, 'test_coder', '测试工程师', 0, 1, '', '2026-02-09 15:47:35', '2026-02-09 17:03:40', 1, 1, 1);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  `data_scope` tinyint NOT NULL DEFAULT 1 COMMENT '数据范围(1全部 2自定义 3本部门 4本部门及以下 5仅本人)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'admin', 1, 1, '拥有所有权限', '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, 1, 0, 1);
INSERT INTO `sys_role` VALUES (2, '普通用户', 'user', 2, 1, '普通用户角色', '2026-01-29 22:42:08', '2026-01-29 22:42:08', NULL, 1, 0, 3);

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `dept_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_dept`(`role_id` ASC, `dept_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色-部门 数据权限关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7696 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (7457, 2, 139);
INSERT INTO `sys_role_menu` VALUES (7458, 2, 153);
INSERT INTO `sys_role_menu` VALUES (7459, 2, 136);
INSERT INTO `sys_role_menu` VALUES (7460, 2, 137);
INSERT INTO `sys_role_menu` VALUES (7461, 2, 138);
INSERT INTO `sys_role_menu` VALUES (7462, 2, 150);
INSERT INTO `sys_role_menu` VALUES (7463, 2, 38);
INSERT INTO `sys_role_menu` VALUES (7464, 2, 151);
INSERT INTO `sys_role_menu` VALUES (7465, 2, 40);
INSERT INTO `sys_role_menu` VALUES (7466, 2, 41);
INSERT INTO `sys_role_menu` VALUES (7467, 2, 42);
INSERT INTO `sys_role_menu` VALUES (7468, 2, 44);
INSERT INTO `sys_role_menu` VALUES (7469, 2, 45);
INSERT INTO `sys_role_menu` VALUES (7470, 2, 155);
INSERT INTO `sys_role_menu` VALUES (7471, 2, 156);
INSERT INTO `sys_role_menu` VALUES (7472, 2, 157);
INSERT INTO `sys_role_menu` VALUES (7473, 2, 158);
INSERT INTO `sys_role_menu` VALUES (7474, 2, 159);
INSERT INTO `sys_role_menu` VALUES (7475, 2, 160);
INSERT INTO `sys_role_menu` VALUES (7476, 2, 148);
INSERT INTO `sys_role_menu` VALUES (7477, 2, 33);
INSERT INTO `sys_role_menu` VALUES (7478, 2, 149);
INSERT INTO `sys_role_menu` VALUES (7479, 2, 35);
INSERT INTO `sys_role_menu` VALUES (7480, 2, 142);
INSERT INTO `sys_role_menu` VALUES (7481, 2, 3);
INSERT INTO `sys_role_menu` VALUES (7482, 2, 4);
INSERT INTO `sys_role_menu` VALUES (7483, 2, 5);
INSERT INTO `sys_role_menu` VALUES (7484, 2, 171);
INSERT INTO `sys_role_menu` VALUES (7485, 2, 172);
INSERT INTO `sys_role_menu` VALUES (7486, 2, 173);
INSERT INTO `sys_role_menu` VALUES (7487, 2, 174);
INSERT INTO `sys_role_menu` VALUES (7488, 2, 175);
INSERT INTO `sys_role_menu` VALUES (7489, 2, 143);
INSERT INTO `sys_role_menu` VALUES (7490, 2, 7);
INSERT INTO `sys_role_menu` VALUES (7491, 2, 8);
INSERT INTO `sys_role_menu` VALUES (7492, 2, 9);
INSERT INTO `sys_role_menu` VALUES (7493, 2, 144);
INSERT INTO `sys_role_menu` VALUES (7494, 2, 11);
INSERT INTO `sys_role_menu` VALUES (7495, 2, 12);
INSERT INTO `sys_role_menu` VALUES (7496, 2, 13);
INSERT INTO `sys_role_menu` VALUES (7497, 2, 145);
INSERT INTO `sys_role_menu` VALUES (7498, 2, 15);
INSERT INTO `sys_role_menu` VALUES (7499, 2, 16);
INSERT INTO `sys_role_menu` VALUES (7500, 2, 17);
INSERT INTO `sys_role_menu` VALUES (7501, 2, 19);
INSERT INTO `sys_role_menu` VALUES (7502, 2, 20);
INSERT INTO `sys_role_menu` VALUES (7503, 2, 21);
INSERT INTO `sys_role_menu` VALUES (7504, 2, 146);
INSERT INTO `sys_role_menu` VALUES (7505, 2, 24);
INSERT INTO `sys_role_menu` VALUES (7506, 2, 25);
INSERT INTO `sys_role_menu` VALUES (7507, 2, 26);
INSERT INTO `sys_role_menu` VALUES (7508, 2, 147);
INSERT INTO `sys_role_menu` VALUES (7509, 2, 28);
INSERT INTO `sys_role_menu` VALUES (7510, 2, 29);
INSERT INTO `sys_role_menu` VALUES (7511, 2, 30);
INSERT INTO `sys_role_menu` VALUES (7512, 2, 152);
INSERT INTO `sys_role_menu` VALUES (7513, 2, 128);
INSERT INTO `sys_role_menu` VALUES (7514, 2, 129);
INSERT INTO `sys_role_menu` VALUES (7515, 2, 141);
INSERT INTO `sys_role_menu` VALUES (7516, 2, 163);
INSERT INTO `sys_role_menu` VALUES (7517, 2, 164);
INSERT INTO `sys_role_menu` VALUES (7518, 2, 165);
INSERT INTO `sys_role_menu` VALUES (7519, 2, 166);
INSERT INTO `sys_role_menu` VALUES (7520, 2, 167);
INSERT INTO `sys_role_menu` VALUES (7521, 2, 168);
INSERT INTO `sys_role_menu` VALUES (7522, 2, 169);
INSERT INTO `sys_role_menu` VALUES (7605, 1, 7);
INSERT INTO `sys_role_menu` VALUES (7606, 1, 8);
INSERT INTO `sys_role_menu` VALUES (7607, 1, 9);
INSERT INTO `sys_role_menu` VALUES (7608, 1, 143);
INSERT INTO `sys_role_menu` VALUES (7609, 1, 142);
INSERT INTO `sys_role_menu` VALUES (7610, 1, 3);
INSERT INTO `sys_role_menu` VALUES (7611, 1, 4);
INSERT INTO `sys_role_menu` VALUES (7612, 1, 5);
INSERT INTO `sys_role_menu` VALUES (7613, 1, 144);
INSERT INTO `sys_role_menu` VALUES (7614, 1, 11);
INSERT INTO `sys_role_menu` VALUES (7615, 1, 12);
INSERT INTO `sys_role_menu` VALUES (7616, 1, 13);
INSERT INTO `sys_role_menu` VALUES (7617, 1, 145);
INSERT INTO `sys_role_menu` VALUES (7618, 1, 15);
INSERT INTO `sys_role_menu` VALUES (7619, 1, 16);
INSERT INTO `sys_role_menu` VALUES (7620, 1, 17);
INSERT INTO `sys_role_menu` VALUES (7621, 1, 19);
INSERT INTO `sys_role_menu` VALUES (7622, 1, 20);
INSERT INTO `sys_role_menu` VALUES (7623, 1, 21);
INSERT INTO `sys_role_menu` VALUES (7624, 1, 146);
INSERT INTO `sys_role_menu` VALUES (7625, 1, 24);
INSERT INTO `sys_role_menu` VALUES (7626, 1, 25);
INSERT INTO `sys_role_menu` VALUES (7627, 1, 26);
INSERT INTO `sys_role_menu` VALUES (7628, 1, 147);
INSERT INTO `sys_role_menu` VALUES (7629, 1, 28);
INSERT INTO `sys_role_menu` VALUES (7630, 1, 29);
INSERT INTO `sys_role_menu` VALUES (7631, 1, 30);
INSERT INTO `sys_role_menu` VALUES (7632, 1, 148);
INSERT INTO `sys_role_menu` VALUES (7633, 1, 33);
INSERT INTO `sys_role_menu` VALUES (7634, 1, 149);
INSERT INTO `sys_role_menu` VALUES (7635, 1, 35);
INSERT INTO `sys_role_menu` VALUES (7636, 1, 150);
INSERT INTO `sys_role_menu` VALUES (7637, 1, 38);
INSERT INTO `sys_role_menu` VALUES (7638, 1, 151);
INSERT INTO `sys_role_menu` VALUES (7639, 1, 40);
INSERT INTO `sys_role_menu` VALUES (7640, 1, 41);
INSERT INTO `sys_role_menu` VALUES (7641, 1, 42);
INSERT INTO `sys_role_menu` VALUES (7642, 1, 44);
INSERT INTO `sys_role_menu` VALUES (7643, 1, 45);
INSERT INTO `sys_role_menu` VALUES (7644, 1, 153);
INSERT INTO `sys_role_menu` VALUES (7645, 1, 136);
INSERT INTO `sys_role_menu` VALUES (7646, 1, 137);
INSERT INTO `sys_role_menu` VALUES (7647, 1, 138);
INSERT INTO `sys_role_menu` VALUES (7648, 1, 139);
INSERT INTO `sys_role_menu` VALUES (7649, 1, 141);
INSERT INTO `sys_role_menu` VALUES (7650, 1, 155);
INSERT INTO `sys_role_menu` VALUES (7651, 1, 156);
INSERT INTO `sys_role_menu` VALUES (7652, 1, 157);
INSERT INTO `sys_role_menu` VALUES (7653, 1, 158);
INSERT INTO `sys_role_menu` VALUES (7654, 1, 159);
INSERT INTO `sys_role_menu` VALUES (7655, 1, 160);
INSERT INTO `sys_role_menu` VALUES (7656, 1, 171);
INSERT INTO `sys_role_menu` VALUES (7657, 1, 172);
INSERT INTO `sys_role_menu` VALUES (7658, 1, 173);
INSERT INTO `sys_role_menu` VALUES (7659, 1, 174);
INSERT INTO `sys_role_menu` VALUES (7660, 1, 175);
INSERT INTO `sys_role_menu` VALUES (7661, 1, 152);
INSERT INTO `sys_role_menu` VALUES (7662, 1, 128);
INSERT INTO `sys_role_menu` VALUES (7663, 1, 129);
INSERT INTO `sys_role_menu` VALUES (7664, 1, 2);
INSERT INTO `sys_role_menu` VALUES (7665, 1, 170);
INSERT INTO `sys_role_menu` VALUES (7666, 1, 6);
INSERT INTO `sys_role_menu` VALUES (7667, 1, 10);
INSERT INTO `sys_role_menu` VALUES (7668, 1, 14);
INSERT INTO `sys_role_menu` VALUES (7669, 1, 18);
INSERT INTO `sys_role_menu` VALUES (7670, 1, 23);
INSERT INTO `sys_role_menu` VALUES (7671, 1, 27);
INSERT INTO `sys_role_menu` VALUES (7672, 1, 32);
INSERT INTO `sys_role_menu` VALUES (7673, 1, 34);
INSERT INTO `sys_role_menu` VALUES (7674, 1, 37);
INSERT INTO `sys_role_menu` VALUES (7675, 1, 39);
INSERT INTO `sys_role_menu` VALUES (7676, 1, 43);
INSERT INTO `sys_role_menu` VALUES (7677, 1, 154);
INSERT INTO `sys_role_menu` VALUES (7678, 1, 127);
INSERT INTO `sys_role_menu` VALUES (7679, 1, 135);
INSERT INTO `sys_role_menu` VALUES (7680, 1, 1);
INSERT INTO `sys_role_menu` VALUES (7681, 1, 22);
INSERT INTO `sys_role_menu` VALUES (7682, 1, 31);
INSERT INTO `sys_role_menu` VALUES (7683, 1, 36);
INSERT INTO `sys_role_menu` VALUES (7684, 1, 126);
INSERT INTO `sys_role_menu` VALUES (7685, 1, 134);
INSERT INTO `sys_role_menu` VALUES (7686, 1, 140);
INSERT INTO `sys_role_menu` VALUES (7687, 1, 161);
INSERT INTO `sys_role_menu` VALUES (7688, 1, 162);
INSERT INTO `sys_role_menu` VALUES (7689, 1, 163);
INSERT INTO `sys_role_menu` VALUES (7690, 1, 164);
INSERT INTO `sys_role_menu` VALUES (7691, 1, 165);
INSERT INTO `sys_role_menu` VALUES (7692, 1, 166);
INSERT INTO `sys_role_menu` VALUES (7693, 1, 167);
INSERT INTO `sys_role_menu` VALUES (7694, 1, 168);
INSERT INTO `sys_role_menu` VALUES (7695, 1, 169);

-- ----------------------------
-- Table structure for sys_server
-- ----------------------------
DROP TABLE IF EXISTS `sys_server`;
CREATE TABLE `sys_server`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '服务器名称',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '服务器地址',
  `port` int NOT NULL DEFAULT 22 COMMENT 'SSH端口',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `auth_type` tinyint NOT NULL DEFAULT 1 COMMENT '认证方式：1-密码 2-密钥',
  `password` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码（加密存储）',
  `private_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '私钥内容',
  `passphrase` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '私钥密码（加密存储）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `last_connect_time` datetime NULL DEFAULT NULL COMMENT '最后连接时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '服务器管理表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_server
-- ----------------------------
INSERT INTO `sys_server` VALUES (1, '测试服务器', '47.108.187.25', 22, 'root', 1, '111', '', '', '', 1, 0, '2026-02-07 09:52:27', 1, '2026-01-31 23:46:31', 1, '2026-02-07 09:52:27', 0);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门id',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别(0-未知 1-男 2-女)',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
  `is_quit` tinyint NULL DEFAULT 0 COMMENT '是否离职(0-否 1-是)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `user_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'admin' COMMENT '用户类型(admin-后台管理员 pc-PC前台用户 app-App/小程序用户)',
  `open_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信openId(微信扫码登录时使用)',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  INDEX `idx_open_id`(`open_id` ASC) USING BTREE,
  INDEX `idx_user_type`(`user_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 1, 'admin', '$2a$10$NHwBrsRfesK2pSMgG3NjZ.3JiUXd0msi5ib34QAROfTNq5t0UTL6y', '超级管理员', NULL, NULL, NULL, 0, 1, 0, NULL, '2026-01-29 22:42:08', '2026-01-29 22:58:21', NULL, 1, 'admin', NULL, 0);
INSERT INTO `sys_user` VALUES (2, 4, 'test', '$2a$10$kTn0Z9BPDnOAU1qB.sJrF.unLh4bbj9FQ7tVsG4AtSBQXFs1V/ewq', 'test', NULL, '111@qq.com', '1888888881', 1, 1, 1, '', '2026-01-29 23:21:12', '2026-02-09 14:59:31', 1, 1, 'admin', NULL, 0);
INSERT INTO `sys_user` VALUES (3, 2, 'mars11', '$2a$10$goR4f6wAzry8a6jTrWHDGeI7Fiq2SovcXYrVprcoRgC6mCnK1fM4G', 'mars11', NULL, '1121@qq.com', '18888888881', 1, 1, 0, '', '2026-01-29 23:21:12', '2026-02-08 16:44:59', 1, 1, 'admin', NULL, 0);
INSERT INTO `sys_user` VALUES (4, 3, 'lisi', '$2a$10$4pFtybVAOwePb9T9LsnYU.OJzo7PIOf3ZxU4MOylb03D6MUK/bSb6', 'lisi', NULL, NULL, NULL, 0, 1, 0, NULL, '2026-01-31 20:49:34', '2026-01-31 20:49:34', NULL, 1, 'admin', NULL, 0);
INSERT INTO `sys_user` VALUES (5, 2, 'mars666', '$2a$10$i3ztgmo5kmPow2ro8Kuxouq0yLnv/JM4huDoZdVIeDp3ErKtBSxiS', 'mars666', NULL, 'wqexpore@163.com', '18888888882', 1, 1, 0, NULL, '2026-01-31 22:30:46', '2026-02-09 14:30:30', 5, 1, 'admin', NULL, 0);
INSERT INTO `sys_user` VALUES (6, 2, 'test01', '$2a$10$ML3nX/GYeLWlCMroXCmSv.i61Rnu9/UEKpWE8uXRi6ly86stXYZqu', 'test01', NULL, NULL, NULL, 0, 1, 0, NULL, '2026-02-07 16:01:02', '2026-02-07 16:01:02', NULL, 1, 'admin', NULL, 0);
INSERT INTO `sys_user` VALUES (10, 2, 'Mars', '$2a$10$zr4zrzbZasuckIfyahZiL.LUgzgoCARw9ocLTXV.HsV7fU9PxgMa.', 'Mars', 'http://m8e8f9e2.natappfree.cc/api/files/images/2026/02/08/3008954e93634f0ebbf0d78f2fe26fc0.jpg', NULL, NULL, 0, 1, 0, NULL, '2026-02-08 16:46:40', '2026-02-08 16:46:40', NULL, 10, 'app', 'opzUF43XlvnVUw5S9qS2cI6L7p9M', 0);
INSERT INTO `sys_user` VALUES (11, 2, 'mars02', '$2a$10$4LVE/zBTH6smGMjfOu/bku1PBaFun3JpFCYG1bTZgPHJQuE3hyjVC', 'mars02', NULL, '111@qq.com', '18888888888', 1, 1, 0, '', '2026-02-09 14:31:09', '2026-02-09 14:31:09', 5, 5, 'admin', NULL, 0);
INSERT INTO `sys_user` VALUES (13, 2, 'Mars111', '$2a$10$zr4zrzbZasuckIfyahZiL.LUgzgoCARw9ocLTXV.HsV7fU9PxgMa.', 'Mars', 'http://m8e8f9e2.natappfree.cc/api/files/images/2026/02/08/3008954e93634f0ebbf0d78f2fe26fc0.jpg', NULL, NULL, 0, 1, 0, NULL, '2026-02-08 16:46:40', '2026-02-08 16:46:40', NULL, 10, 'app', 'opzUF43XlvnVUw5S9qS2cI6L7p9M', 0);
INSERT INTO `sys_user` VALUES (14, 2, 'Mars222', '$2a$10$zr4zrzbZasuckIfyahZiL.LUgzgoCARw9ocLTXV.HsV7fU9PxgMa.', 'Mars', 'http://m8e8f9e2.natappfree.cc/api/files/images/2026/02/08/3008954e93634f0ebbf0d78f2fe26fc0.jpg', NULL, NULL, 0, 1, 0, NULL, '2026-02-08 16:46:40', '2026-02-08 16:46:40', NULL, 10, 'app', 'opzUF43XlvnVUw5S9qS2cI6L7p9M', 0);
INSERT INTO `sys_user` VALUES (15, 2, 'Mars333', '$2a$10$zr4zrzbZasuckIfyahZiL.LUgzgoCARw9ocLTXV.HsV7fU9PxgMa.', 'Mars', 'http://m8e8f9e2.natappfree.cc/api/files/images/2026/02/08/3008954e93634f0ebbf0d78f2fe26fc0.jpg', NULL, NULL, 0, 1, 0, NULL, '2026-02-08 16:46:40', '2026-02-08 16:46:40', NULL, 10, 'app', 'opzUF43XlvnVUw5S9qS2cI6L7p9M', 0);
INSERT INTO `sys_user` VALUES (16, 2, 'Mars444', '$2a$10$zr4zrzbZasuckIfyahZiL.LUgzgoCARw9ocLTXV.HsV7fU9PxgMa.', 'Mars', 'http://m8e8f9e2.natappfree.cc/api/files/images/2026/02/08/3008954e93634f0ebbf0d78f2fe26fc0.jpg', NULL, NULL, 0, 1, 0, NULL, '2026-02-08 16:46:40', '2026-02-08 16:46:40', NULL, 10, 'app', 'opzUF43XlvnVUw5S9qS2cI6L7p9M', 0);
INSERT INTO `sys_user` VALUES (17, 2, 'Mars555', '$2a$10$zr4zrzbZasuckIfyahZiL.LUgzgoCARw9ocLTXV.HsV7fU9PxgMa.', 'Mars', 'http://m8e8f9e2.natappfree.cc/api/files/images/2026/02/08/3008954e93634f0ebbf0d78f2fe26fc0.jpg', NULL, NULL, 0, 1, 0, NULL, '2026-02-08 16:46:40', '2026-02-08 16:46:40', NULL, 10, 'app', 'opzUF43XlvnVUw5S9qS2cI6L7p9M', 0);

-- ----------------------------
-- Table structure for sys_user_blacklist
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_blacklist`;
CREATE TABLE `sys_user_blacklist`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `blocked_user_id` bigint NOT NULL COMMENT '被拉黑的用户ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉黑时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_blocked`(`user_id` ASC, `blocked_user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_blocked_user_id`(`blocked_user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户黑名单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_blacklist
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_notice`;
CREATE TABLE `sys_user_notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `notice_id` bigint NOT NULL COMMENT '通知ID',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '是否已读(0未读 1已读)',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_notice`(`user_id` ASC, `notice_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_notice_id`(`notice_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`is_read` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户通知关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_notice
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户岗位关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------
INSERT INTO `sys_user_post` VALUES (1, 11, 2);
INSERT INTO `sys_user_post` VALUES (3, 5, 3);
INSERT INTO `sys_user_post` VALUES (4, 5, 4);
INSERT INTO `sys_user_post` VALUES (5, 10, 4);
INSERT INTO `sys_user_post` VALUES (6, 1, 1);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (9, 4, 2);
INSERT INTO `sys_user_role` VALUES (11, 3, 2);
INSERT INTO `sys_user_role` VALUES (25, 7, 2);
INSERT INTO `sys_user_role` VALUES (30, 6, 2);
INSERT INTO `sys_user_role` VALUES (33, 2, 2);
INSERT INTO `sys_user_role` VALUES (34, 11, 2);
INSERT INTO `sys_user_role` VALUES (36, 5, 2);
INSERT INTO `sys_user_role` VALUES (37, 10, 2);
INSERT INTO `sys_user_role` VALUES (38, 1, 1);

SET FOREIGN_KEY_CHECKS = 1;
