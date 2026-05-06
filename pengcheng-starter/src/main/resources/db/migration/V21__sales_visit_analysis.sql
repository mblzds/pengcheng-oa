-- ==============================================
-- V21: 销售拜访记录与分析
-- ==============================================

-- 销售拜访记录表（扩展原有 customer_visit，增加 AI 分析能力）
CREATE TABLE IF NOT EXISTS `sys_sales_visit` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL COMMENT '销售人员 ID',
    `customer_id`    BIGINT       DEFAULT NULL COMMENT '关联客户 ID',
    `customer_name`  VARCHAR(100) DEFAULT NULL COMMENT '客户姓名（冗余）',
    `project_id`     BIGINT       DEFAULT NULL COMMENT '关联项目 ID',
    `project_name`   VARCHAR(200) DEFAULT NULL COMMENT '项目名称（冗余）',
    `visit_type`     VARCHAR(20)  NOT NULL DEFAULT 'field' COMMENT '拜访类型: field=实地/phone=电话/online=线上',
    `visit_time`     DATETIME     NOT NULL COMMENT '拜访时间',
    `duration`       INT          DEFAULT 0 COMMENT '拜访时长（分钟）',
    `location`       VARCHAR(300) DEFAULT NULL COMMENT '拜访地点',
    `purpose`        VARCHAR(500) DEFAULT NULL COMMENT '拜访目的',
    `summary`        TEXT         DEFAULT NULL COMMENT '拜访总结（人工或 AI 生成）',
    `audio_url`      VARCHAR(500) DEFAULT NULL COMMENT '录音文件 URL',
    `transcript`     TEXT         DEFAULT NULL COMMENT 'ASR 转写文本',
    `ai_analysis`    JSON         DEFAULT NULL COMMENT 'AI 分析结果（需求/异议/承诺/竞品等）',
    `ai_score`       INT          DEFAULT NULL COMMENT 'AI 拜访质量评分（0-100）',
    `follow_up`      VARCHAR(500) DEFAULT NULL COMMENT '跟进事项',
    `next_plan`      VARCHAR(500) DEFAULT NULL COMMENT '下次拜访计划',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=草稿 1=已完成 2=已取消',
    `dept_id`        BIGINT       DEFAULT NULL COMMENT '所属部门',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_user_time` (`user_id`, `visit_time`),
    INDEX `idx_customer` (`customer_id`),
    INDEX `idx_project` (`project_id`),
    INDEX `idx_dept_time` (`dept_id`, `visit_time`),
    FULLTEXT INDEX `ft_visit_content` (`summary`, `transcript`, `follow_up`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售拜访记录';

-- 拜访分析标签表（AI 从拜访中提取的结构化标签）
CREATE TABLE IF NOT EXISTS `sys_sales_visit_tag` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `visit_id`    BIGINT       NOT NULL COMMENT '拜访记录 ID',
    `tag_type`    VARCHAR(30)  NOT NULL COMMENT '标签类型: need=需求/objection=异议/commitment=承诺/competitor=竞品/risk=风险',
    `tag_content` VARCHAR(500) NOT NULL COMMENT '标签内容',
    `confidence`  DECIMAL(3,2) DEFAULT 0.80 COMMENT '置信度',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_visit` (`visit_id`),
    INDEX `idx_type` (`tag_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拜访分析标签';
