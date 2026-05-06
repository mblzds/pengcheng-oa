-- V19: AI 日报 + 销售质检 + 场景模板

-- AI 日报
CREATE TABLE IF NOT EXISTS sys_daily_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '日报归属人',
    report_date DATE NOT NULL,
    summary TEXT COMMENT 'AI 生成的日报摘要',
    customer_follow_up INT DEFAULT 0 COMMENT '当日跟进客户数',
    new_customers INT DEFAULT 0 COMMENT '新增客户数',
    deal_count INT DEFAULT 0 COMMENT '签约单数',
    deal_amount DECIMAL(14,2) DEFAULT 0 COMMENT '签约金额',
    payment_received DECIMAL(14,2) DEFAULT 0 COMMENT '回款金额',
    attendance_status VARCHAR(20) COMMENT '考勤状态',
    chat_summary TEXT COMMENT '聊天要点摘要',
    todo_completed INT DEFAULT 0 COMMENT '完成待办数',
    todo_pending INT DEFAULT 0 COMMENT '剩余待办数',
    ai_suggestions TEXT COMMENT 'AI 改进建议',
    pushed TINYINT DEFAULT 0 COMMENT '是否已推送',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, report_date),
    KEY idx_date (report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 日报';

-- 销售质检评分
CREATE TABLE IF NOT EXISTS sys_sales_quality_score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '被评人',
    score_date DATE NOT NULL,
    communication_score INT DEFAULT 0 COMMENT '沟通完整性 0-100',
    demand_mining_score INT DEFAULT 0 COMMENT '需求挖掘 0-100',
    objection_handling_score INT DEFAULT 0 COMMENT '异议处理 0-100',
    closing_ability_score INT DEFAULT 0 COMMENT '闭合能力 0-100',
    follow_up_frequency_score INT DEFAULT 0 COMMENT '跟进频率 0-100',
    response_time_score INT DEFAULT 0 COMMENT '响应时效 0-100',
    overall_score INT DEFAULT 0 COMMENT '综合评分 0-100',
    ai_comment TEXT COMMENT 'AI 评语',
    ai_suggestion TEXT COMMENT 'AI 改进建议',
    evaluated_records INT DEFAULT 0 COMMENT '评估的跟进记录数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_date (user_id, score_date),
    KEY idx_overall (overall_score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售质检评分';

-- 场景模板
CREATE TABLE IF NOT EXISTS sys_scene_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    category VARCHAR(50) NOT NULL COMMENT 'visit_memo/demand_analysis/competitor/survey',
    description VARCHAR(500),
    template_content TEXT NOT NULL COMMENT '模板内容（Markdown 格式，含 {{placeholder}}）',
    fields JSON COMMENT '模板字段定义 [{name, label, type, required}]',
    icon VARCHAR(50) DEFAULT '📋',
    sort_order INT DEFAULT 0,
    usage_count INT DEFAULT 0,
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景模板';

-- 模板使用记录
CREATE TABLE IF NOT EXISTS sys_scene_template_usage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    filled_content TEXT COMMENT '填充后的内容',
    customer_id BIGINT COMMENT '关联客户',
    project_id BIGINT COMMENT '关联项目',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_template (template_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板使用记录';

-- 预置 4 个场景模板
INSERT INTO sys_scene_template (name, category, description, template_content, fields, icon, sort_order) VALUES
('楼盘推介纪要', 'visit_memo', '记录带看楼盘推介过程和客户反馈', '# 楼盘推介纪要\n\n## 基本信息\n- **日期**：{{date}}\n- **客户**：{{customerName}}\n- **项目**：{{projectName}}\n- **置业顾问**：{{salesName}}\n\n## 推介概况\n- **到访方式**：{{visitType}}\n- **看房户型**：{{houseType}}\n- **客户意向度**：{{intentLevel}}\n\n## 客户反馈\n{{customerFeedback}}\n\n## 价格沟通\n{{priceDiscussion}}\n\n## 竞品提及\n{{competitorMention}}\n\n## 后续跟进计划\n{{followUpPlan}}\n\n## 备注\n{{notes}}', '[{"name":"date","label":"日期","type":"date","required":true},{"name":"customerName","label":"客户姓名","type":"text","required":true},{"name":"projectName","label":"项目名称","type":"text","required":true},{"name":"salesName","label":"置业顾问","type":"text","required":true},{"name":"visitType","label":"到访方式","type":"select","required":false},{"name":"houseType","label":"看房户型","type":"text","required":false},{"name":"intentLevel","label":"意向度","type":"select","required":false},{"name":"customerFeedback","label":"客户反馈","type":"textarea","required":false},{"name":"priceDiscussion","label":"价格沟通","type":"textarea","required":false},{"name":"competitorMention","label":"竞品提及","type":"textarea","required":false},{"name":"followUpPlan","label":"跟进计划","type":"textarea","required":false},{"name":"notes","label":"备注","type":"textarea","required":false}]', '🏠', 1),

('客户需求分析', 'demand_analysis', '深度分析客户购房需求和画像', '# 客户需求分析报告\n\n## 客户信息\n- **客户姓名**：{{customerName}}\n- **联系方式**：{{phone}}\n- **家庭结构**：{{familyStructure}}\n- **目前居住**：{{currentLiving}}\n\n## 购房需求\n- **购房目的**：{{purpose}}\n- **预算范围**：{{budget}}\n- **面积需求**：{{areaRequirement}}\n- **户型偏好**：{{layoutPreference}}\n- **区域偏好**：{{areaPreference}}\n- **楼层偏好**：{{floorPreference}}\n\n## 关注要素（按优先级排序）\n{{priorityFactors}}\n\n## 决策因素分析\n{{decisionFactors}}\n\n## 潜在顾虑\n{{concerns}}\n\n## 推荐方案\n{{recommendation}}\n\n## 跟进策略\n{{followUpStrategy}}', '[{"name":"customerName","label":"客户姓名","type":"text","required":true},{"name":"phone","label":"联系方式","type":"text","required":false},{"name":"familyStructure","label":"家庭结构","type":"text","required":false},{"name":"currentLiving","label":"目前居住","type":"text","required":false},{"name":"purpose","label":"购房目的","type":"select","required":true},{"name":"budget","label":"预算范围","type":"text","required":true},{"name":"areaRequirement","label":"面积需求","type":"text","required":false},{"name":"layoutPreference","label":"户型偏好","type":"text","required":false},{"name":"areaPreference","label":"区域偏好","type":"text","required":false},{"name":"floorPreference","label":"楼层偏好","type":"text","required":false},{"name":"priorityFactors","label":"关注要素","type":"textarea","required":false},{"name":"decisionFactors","label":"决策因素","type":"textarea","required":false},{"name":"concerns","label":"潜在顾虑","type":"textarea","required":false},{"name":"recommendation","label":"推荐方案","type":"textarea","required":false},{"name":"followUpStrategy","label":"跟进策略","type":"textarea","required":false}]', '🎯', 2),

('竞品对比报告', 'competitor', '对比分析竞品楼盘优劣势', '# 竞品对比报告\n\n## 我方项目\n- **项目名称**：{{ourProject}}\n- **均价**：{{ourPrice}}\n- **主力户型**：{{ourLayout}}\n\n## 竞品信息\n### 竞品一：{{comp1Name}}\n- **均价**：{{comp1Price}}\n- **主力户型**：{{comp1Layout}}\n- **优势**：{{comp1Advantage}}\n- **劣势**：{{comp1Disadvantage}}\n\n### 竞品二：{{comp2Name}}\n- **均价**：{{comp2Price}}\n- **主力户型**：{{comp2Layout}}\n- **优势**：{{comp2Advantage}}\n- **劣势**：{{comp2Disadvantage}}\n\n## 对比维度分析\n| 维度 | 我方 | 竞品一 | 竞品二 |\n|------|------|--------|--------|\n| 价格 | {{ourPriceComp}} | {{comp1PriceComp}} | {{comp2PriceComp}} |\n| 位置 | {{ourLocationComp}} | {{comp1LocationComp}} | {{comp2LocationComp}} |\n| 品质 | {{ourQualityComp}} | {{comp1QualityComp}} | {{comp2QualityComp}} |\n\n## 话术建议\n{{talkingPoints}}\n\n## 总结\n{{summary}}', '[{"name":"ourProject","label":"我方项目","type":"text","required":true},{"name":"ourPrice","label":"我方均价","type":"text","required":true},{"name":"ourLayout","label":"我方户型","type":"text","required":false},{"name":"comp1Name","label":"竞品一名称","type":"text","required":true},{"name":"comp1Price","label":"竞品一均价","type":"text","required":false},{"name":"comp1Layout","label":"竞品一户型","type":"text","required":false},{"name":"comp1Advantage","label":"竞品一优势","type":"textarea","required":false},{"name":"comp1Disadvantage","label":"竞品一劣势","type":"textarea","required":false},{"name":"comp2Name","label":"竞品二名称","type":"text","required":false},{"name":"comp2Price","label":"竞品二均价","type":"text","required":false},{"name":"comp2Layout","label":"竞品二户型","type":"text","required":false},{"name":"comp2Advantage","label":"竞品二优势","type":"textarea","required":false},{"name":"comp2Disadvantage","label":"竞品二劣势","type":"textarea","required":false},{"name":"talkingPoints","label":"话术建议","type":"textarea","required":false},{"name":"summary","label":"总结","type":"textarea","required":false}]', '⚔️', 3),

('项目踩盘报告', 'survey', '实地考察竞品项目的详细记录', '# 项目踩盘报告\n\n## 踩盘信息\n- **踩盘日期**：{{date}}\n- **项目名称**：{{projectName}}\n- **开发商**：{{developer}}\n- **踩盘人**：{{surveyor}}\n\n## 项目概况\n- **项目位置**：{{location}}\n- **占地面积**：{{landArea}}\n- **建筑面积**：{{buildingArea}}\n- **容积率**：{{plotRatio}}\n- **绿化率**：{{greenRate}}\n- **总户数**：{{totalUnits}}\n- **车位比**：{{parkingRatio}}\n\n## 产品信息\n- **在售户型**：{{sellingLayouts}}\n- **均价范围**：{{priceRange}}\n- **付款方式**：{{paymentMethod}}\n- **交房时间**：{{deliveryDate}}\n\n## 现场观感\n### 售楼处\n{{salesOffice}}\n### 样板间\n{{modelRoom}}\n### 周边配套\n{{surroundings}}\n\n## 销售情况\n- **去化率**：{{sellRate}}\n- **客群画像**：{{customerProfile}}\n- **促销活动**：{{promotion}}\n\n## 对我方项目的启示\n{{insights}}\n\n## 照片记录\n{{photos}}', '[{"name":"date","label":"踩盘日期","type":"date","required":true},{"name":"projectName","label":"项目名称","type":"text","required":true},{"name":"developer","label":"开发商","type":"text","required":false},{"name":"surveyor","label":"踩盘人","type":"text","required":true},{"name":"location","label":"项目位置","type":"text","required":false},{"name":"priceRange","label":"均价范围","type":"text","required":false},{"name":"sellingLayouts","label":"在售户型","type":"text","required":false},{"name":"salesOffice","label":"售楼处观感","type":"textarea","required":false},{"name":"modelRoom","label":"样板间观感","type":"textarea","required":false},{"name":"surroundings","label":"周边配套","type":"textarea","required":false},{"name":"sellRate","label":"去化率","type":"text","required":false},{"name":"customerProfile","label":"客群画像","type":"textarea","required":false},{"name":"promotion","label":"促销活动","type":"textarea","required":false},{"name":"insights","label":"启示","type":"textarea","required":false}]', '🔍', 4);
