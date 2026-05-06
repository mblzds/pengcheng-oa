-- V22: 人事与绩效模块
-- 人事：员工档案扩展、入职/离职/调岗异动
-- 绩效：考核周期、KPI 指标模板、考核记录

-- 1. 员工档案扩展表（关联 sys_user，存人事专用字段）
CREATE TABLE IF NOT EXISTS hr_employee_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '关联 sys_user.id',
    employee_no VARCHAR(32) NULL COMMENT '工号',
    join_date DATE NULL COMMENT '入职日期',
    formal_date DATE NULL COMMENT '转正日期',
    contract_start DATE NULL COMMENT '合同开始',
    contract_end DATE NULL COMMENT '合同结束',
    job_level VARCHAR(20) NULL COMMENT '职级',
    work_location VARCHAR(100) NULL COMMENT '工作地点',
    emergency_contact VARCHAR(50) NULL COMMENT '紧急联系人',
    emergency_phone VARCHAR(20) NULL COMMENT '紧急联系电话',
    remark VARCHAR(500) NULL COMMENT '备注',
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0-未删除 1-已删除',
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_employee_no (employee_no),
    KEY idx_join_date (join_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工档案扩展表';

-- 2. 人事异动表（入职/离职/调岗/调薪等）
CREATE TABLE IF NOT EXISTS hr_employee_change (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '员工 user_id',
    change_type TINYINT NOT NULL COMMENT '1-入职 2-离职 3-调岗 4-调薪 5-其他',
    change_date DATE NOT NULL COMMENT '异动日期',
    before_dept_id BIGINT NULL COMMENT '调岗前部门',
    after_dept_id BIGINT NULL COMMENT '调岗后部门',
    before_post_id BIGINT NULL COMMENT '调岗前岗位',
    after_post_id BIGINT NULL COMMENT '调岗后岗位',
    reason VARCHAR(500) NULL COMMENT '原因说明',
    attachment VARCHAR(500) NULL COMMENT '附件路径',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1-草稿 2-已生效',
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_user_id (user_id),
    KEY idx_change_type (change_type),
    KEY idx_change_date (change_date),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人事异动表';

-- 3. 考核周期表
CREATE TABLE IF NOT EXISTS hr_kpi_period (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '周期名称，如 2026年Q1考核',
    period_type TINYINT NOT NULL COMMENT '1-月度 2-季度 3-年度',
    year INT NOT NULL COMMENT '年',
    quarter TINYINT NULL COMMENT '季度 1-4，月度可为 0',
    month TINYINT NULL COMMENT '月份 1-12，季度/年度可为 0',
    start_date DATE NOT NULL COMMENT '周期开始',
    end_date DATE NOT NULL COMMENT '周期结束',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1-未开始 2-考核中 3-已结束',
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_year_period (year, period_type),
    KEY idx_status (status),
    KEY idx_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考核周期表';

-- 4. KPI 指标模板表
CREATE TABLE IF NOT EXISTS hr_kpi_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '指标名称',
    code VARCHAR(50) NOT NULL COMMENT '编码，如 deal_count, attendance_rate',
    category TINYINT NOT NULL COMMENT '1-销售业绩 2-考勤 3-过程质量 4-综合',
    weight DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '权重 0-100',
    data_source VARCHAR(50) NULL COMMENT 'manual, auto_commission, auto_attendance, auto_quality',
    formula VARCHAR(200) NULL COMMENT '计算公式或说明',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_code (code),
    KEY idx_category (category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='KPI 指标模板表';

-- 5. 考核记录表（周期+员工+指标 维度）
CREATE TABLE IF NOT EXISTS hr_kpi_score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    period_id BIGINT NOT NULL COMMENT '考核周期 id',
    user_id BIGINT NOT NULL COMMENT '被考核人 user_id',
    template_id BIGINT NOT NULL COMMENT '指标模板 id',
    target_value DECIMAL(12,2) NULL COMMENT '目标值',
    actual_value DECIMAL(12,2) NULL COMMENT '实际值',
    score DECIMAL(5,2) NULL COMMENT '得分',
    weighted_score DECIMAL(5,2) NULL COMMENT '加权得分',
    remark VARCHAR(500) NULL COMMENT '备注',
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_period_user_template (period_id, user_id, template_id),
    KEY idx_period_id (period_id),
    KEY idx_user_id (user_id),
    KEY idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考核记录表';

-- 预置 KPI 指标模板（与销售/考勤/质检衔接）
INSERT INTO hr_kpi_template (name, code, category, weight, data_source, formula, sort_order, status) VALUES
('签约套数', 'deal_count', 1, 30.00, 'auto_commission', '统计周期内签约单数', 1, 1),
('签约金额', 'deal_amount', 1, 30.00, 'auto_commission', '统计周期内签约金额', 2, 1),
('出勤率', 'attendance_rate', 2, 20.00, 'auto_attendance', '出勤天数/应出勤天数', 3, 1),
('过程质量综合分', 'quality_overall', 3, 20.00, 'auto_quality', '取自 sys_sales_quality_score 周期内综合分', 4, 1);
