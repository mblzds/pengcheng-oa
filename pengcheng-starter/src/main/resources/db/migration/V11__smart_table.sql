-- ============================================================
-- V11: 智能表格系统
-- ============================================================

-- 智能表格主表
CREATE TABLE IF NOT EXISTS `smart_table` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '表格名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '表格描述',
    `icon` VARCHAR(50) DEFAULT '📊' COMMENT '表格图标',
    `template_id` BIGINT DEFAULT NULL COMMENT '模板来源ID',
    `owner_id` BIGINT NOT NULL COMMENT '创建者ID',
    `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门ID',
    `visibility` VARCHAR(20) NOT NULL DEFAULT 'private' COMMENT '可见范围：private/dept/all',
    `record_count` INT NOT NULL DEFAULT 0 COMMENT '记录数（冗余缓存）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_owner` (`owner_id`),
    KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能表格';

-- 智能表格字段定义
CREATE TABLE IF NOT EXISTS `smart_table_field` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `table_id` BIGINT NOT NULL COMMENT '所属表格ID',
    `name` VARCHAR(100) NOT NULL COMMENT '字段名称',
    `field_key` VARCHAR(50) NOT NULL COMMENT '字段标识（JSON Key）',
    `field_type` VARCHAR(30) NOT NULL COMMENT '字段类型：text/number/select/multi_select/date/datetime/checkbox/url/email/phone/rating/progress/member/attachment',
    `required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必填',
    `options` JSON DEFAULT NULL COMMENT '字段配置（选项列表、数值范围、日期格式等）',
    `default_value` VARCHAR(500) DEFAULT NULL COMMENT '默认值',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `width` INT DEFAULT 150 COMMENT '列宽（像素）',
    `hidden` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否隐藏',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_table_sort` (`table_id`, `sort_order`),
    UNIQUE KEY `uk_table_key` (`table_id`, `field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能表格字段';

-- 智能表格记录
CREATE TABLE IF NOT EXISTS `smart_table_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `table_id` BIGINT NOT NULL COMMENT '所属表格ID',
    `data` JSON NOT NULL COMMENT '行数据（JSON 格式，key 对应 field_key）',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_table_sort` (`table_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能表格记录';

-- 智能表格视图
CREATE TABLE IF NOT EXISTS `smart_table_view` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `table_id` BIGINT NOT NULL COMMENT '所属表格ID',
    `name` VARCHAR(100) NOT NULL COMMENT '视图名称',
    `view_type` VARCHAR(20) NOT NULL DEFAULT 'grid' COMMENT '视图类型：grid/kanban/gantt/calendar',
    `config` JSON DEFAULT NULL COMMENT '视图配置（筛选条件、排序、分组、隐藏列等）',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认视图',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_table` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能表格视图';

-- 智能表格模板
CREATE TABLE IF NOT EXISTS `smart_table_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '模板描述',
    `category` VARCHAR(50) NOT NULL DEFAULT 'general' COMMENT '分类：general/realty/sales/hr/finance',
    `icon` VARCHAR(50) DEFAULT '📋' COMMENT '模板图标',
    `fields_config` JSON NOT NULL COMMENT '字段定义（JSON 数组）',
    `sample_data` JSON DEFAULT NULL COMMENT '示例数据',
    `built_in` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否内置模板',
    `usage_count` INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能表格模板';

-- 内置房产行业模板
INSERT INTO `smart_table_template` (`name`, `description`, `category`, `icon`, `fields_config`, `built_in`) VALUES
('客户跟进记录表', '记录销售人员每日客户跟进情况', 'realty', '👥',
 '[{"name":"客户姓名","field_key":"customer_name","field_type":"text","required":true},{"name":"跟进状态","field_key":"status","field_type":"select","options":{"items":["初次接触","跟进中","已预约","已到访","已认购","已签约","流失"]}},{"name":"跟进方式","field_key":"method","field_type":"select","options":{"items":["电话","微信","面谈","带看"]}},{"name":"跟进内容","field_key":"content","field_type":"text"},{"name":"下次跟进日期","field_key":"next_date","field_type":"date"},{"name":"跟进人","field_key":"follower","field_type":"member"},{"name":"意向度","field_key":"intention","field_type":"rating"}]', 1),

('楼盘对比分析表', '多维度对比竞品楼盘信息', 'realty', '🏢',
 '[{"name":"楼盘名称","field_key":"project_name","field_type":"text","required":true},{"name":"均价(元/㎡)","field_key":"avg_price","field_type":"number"},{"name":"面积段","field_key":"area_range","field_type":"text"},{"name":"装修标准","field_key":"decoration","field_type":"select","options":{"items":["毛坯","简装","精装","豪装"]}},{"name":"交房日期","field_key":"delivery_date","field_type":"date"},{"name":"优势","field_key":"advantages","field_type":"text"},{"name":"劣势","field_key":"disadvantages","field_type":"text"},{"name":"综合评分","field_key":"score","field_type":"rating"}]', 1),

('佣金结算跟踪表', '跟踪渠道佣金结算进度', 'finance', '💰',
 '[{"name":"客户","field_key":"customer","field_type":"text","required":true},{"name":"成交金额","field_key":"deal_amount","field_type":"number"},{"name":"佣金比例","field_key":"commission_rate","field_type":"number"},{"name":"应付佣金","field_key":"commission_amount","field_type":"number"},{"name":"结算状态","field_key":"status","field_type":"select","options":{"items":["待结算","审批中","已付款","已完成"]}},{"name":"结算进度","field_key":"progress","field_type":"progress"},{"name":"渠道","field_key":"channel","field_type":"text"},{"name":"备注","field_key":"remark","field_type":"text"}]', 1),

('带看登记表', '记录销售带客看房情况', 'realty', '🏠',
 '[{"name":"客户姓名","field_key":"customer_name","field_type":"text","required":true},{"name":"带看日期","field_key":"visit_date","field_type":"datetime","required":true},{"name":"带看房源","field_key":"property","field_type":"text"},{"name":"带看人","field_key":"agent","field_type":"member"},{"name":"客户反馈","field_key":"feedback","field_type":"select","options":{"items":["满意","一般","不满意","需再看"]}},{"name":"是否有意向","field_key":"interested","field_type":"checkbox"},{"name":"备注","field_key":"remark","field_type":"text"}]', 1);
