-- V20: 扩展智能表格模板 + AI 巡检记录表

-- 新增房产行业预置模板
INSERT INTO `smart_table_template` (`name`, `description`, `category`, `icon`, `fields_config`, `built_in`) VALUES
('回款跟踪表', '跟踪项目回款进度与账期', 'finance', '💵',
 '[{"name":"客户姓名","field_key":"customer_name","field_type":"text","required":true},{"name":"项目名称","field_key":"project_name","field_type":"text"},{"name":"合同金额","field_key":"contract_amount","field_type":"number"},{"name":"已回款金额","field_key":"paid_amount","field_type":"number"},{"name":"回款进度","field_key":"progress","field_type":"progress"},{"name":"最近回款日期","field_key":"last_pay_date","field_type":"date"},{"name":"下期应付日期","field_key":"next_pay_date","field_type":"date"},{"name":"付款方式","field_key":"pay_method","field_type":"select","options":{"items":["全款","按揭","分期","公积金"]}},{"name":"逾期天数","field_key":"overdue_days","field_type":"number"},{"name":"跟进人","field_key":"follower","field_type":"member"},{"name":"备注","field_key":"remark","field_type":"text"}]', 1),

('签约台账表', '管理签约合同全生命周期', 'realty', '📝',
 '[{"name":"合同编号","field_key":"contract_no","field_type":"text","required":true},{"name":"客户姓名","field_key":"customer_name","field_type":"text","required":true},{"name":"房号","field_key":"room_no","field_type":"text"},{"name":"面积(㎡)","field_key":"area","field_type":"number"},{"name":"成交单价","field_key":"unit_price","field_type":"number"},{"name":"成交总价","field_key":"total_price","field_type":"number"},{"name":"签约日期","field_key":"sign_date","field_type":"date"},{"name":"交房日期","field_key":"delivery_date","field_type":"date"},{"name":"合同状态","field_key":"status","field_type":"select","options":{"items":["草签","正签","备案中","已备案","退房"]}},{"name":"置业顾问","field_key":"agent","field_type":"member"},{"name":"是否网签","field_key":"online_signed","field_type":"checkbox"}]', 1),

('渠道拓客登记表', '渠道带客与成交统计', 'sales', '🤝',
 '[{"name":"渠道名称","field_key":"channel_name","field_type":"text","required":true},{"name":"渠道经理","field_key":"channel_manager","field_type":"text"},{"name":"带客日期","field_key":"visit_date","field_type":"date"},{"name":"带客数量","field_key":"visit_count","field_type":"number"},{"name":"认购数量","field_key":"subscribe_count","field_type":"number"},{"name":"签约数量","field_key":"sign_count","field_type":"number"},{"name":"转化率","field_key":"conversion_rate","field_type":"number"},{"name":"佣金结算","field_key":"commission_status","field_type":"select","options":{"items":["未结","部分结","已结清"]}},{"name":"合作评级","field_key":"rating","field_type":"rating"},{"name":"联系电话","field_key":"phone","field_type":"text"}]', 1),

('日常巡盘记录表', '记录项目巡盘检查结果', 'realty', '🔍',
 '[{"name":"巡盘日期","field_key":"patrol_date","field_type":"date","required":true},{"name":"项目名称","field_key":"project_name","field_type":"text","required":true},{"name":"巡盘人","field_key":"inspector","field_type":"member"},{"name":"售楼处卫生","field_key":"clean_score","field_type":"rating"},{"name":"沙盘维护","field_key":"model_score","field_type":"rating"},{"name":"样板间状态","field_key":"showroom_score","field_type":"rating"},{"name":"物料齐全度","field_key":"material_score","field_type":"rating"},{"name":"整体评分","field_key":"overall_score","field_type":"rating"},{"name":"发现问题","field_key":"issues","field_type":"text"},{"name":"整改期限","field_key":"fix_deadline","field_type":"date"},{"name":"是否已整改","field_key":"fixed","field_type":"checkbox"}]', 1);

-- AI 巡检记录表
CREATE TABLE IF NOT EXISTS `sys_ai_heartbeat_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `check_type` VARCHAR(50) NOT NULL COMMENT '巡检类型：customer_followup/commission/contract/overdue',
    `user_id` BIGINT DEFAULT NULL COMMENT '关联用户',
    `target_id` BIGINT DEFAULT NULL COMMENT '关联业务记录ID',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT '业务类型：customer/commission/contract',
    `severity` VARCHAR(20) NOT NULL DEFAULT 'info' COMMENT '严重程度：info/warn/critical',
    `title` VARCHAR(200) NOT NULL COMMENT '巡检标题',
    `content` TEXT COMMENT '巡检详情',
    `suggestion` TEXT COMMENT 'AI 建议',
    `handled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已处理',
    `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_heartbeat_type` (`check_type`),
    INDEX `idx_heartbeat_user` (`user_id`),
    INDEX `idx_heartbeat_severity` (`severity`),
    INDEX `idx_heartbeat_handled` (`handled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 巡检记录';
