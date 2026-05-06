-- V23: 通用项目管理模块
-- 项目、成员、任务、任务依赖、里程碑

-- 1. 项目表
CREATE TABLE IF NOT EXISTS pm_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '项目名称',
    description TEXT NULL COMMENT '项目描述',
    owner_id BIGINT NULL COMMENT '负责人 sys_user.id',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1-未开始 2-进行中 3-已暂停 4-已完成 5-已归档',
    start_date DATE NULL COMMENT '计划开始',
    end_date DATE NULL COMMENT '计划结束',
    visibility VARCHAR(20) NOT NULL DEFAULT 'private' COMMENT 'private/dept/all',
    color VARCHAR(20) NULL COMMENT '主题色',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_owner (owner_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    KEY idx_owner_deleted (owner_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- 2. 项目成员表
CREATE TABLE IF NOT EXISTS pm_project_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目 id',
    user_id BIGINT NOT NULL COMMENT '用户 id',
    role VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT 'owner/admin/member',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_project_user (project_id, user_id),
    KEY idx_project_id (project_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员表';

-- 3. 任务表
CREATE TABLE IF NOT EXISTS pm_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '所属项目',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父任务 id，0 为顶层',
    title VARCHAR(500) NOT NULL COMMENT '任务标题',
    description TEXT NULL COMMENT '任务描述',
    assignee_id BIGINT NULL COMMENT '执行人',
    status VARCHAR(30) NOT NULL DEFAULT '待办' COMMENT '任务状态',
    priority TINYINT NOT NULL DEFAULT 1 COMMENT '0-无 1-低 2-中 3-高 4-紧急',
    progress TINYINT NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    start_date DATE NULL COMMENT '计划开始',
    due_date DATE NULL COMMENT '截止日期',
    estimated_hours DECIMAL(8,2) NULL COMMENT '预估工时',
    actual_hours DECIMAL(8,2) NULL COMMENT '实际工时',
    sort_order INT DEFAULT 0 COMMENT '同层级排序',
    create_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_project (project_id),
    KEY idx_parent (parent_id),
    KEY idx_assignee (assignee_id),
    KEY idx_status (status),
    KEY idx_due_date (due_date),
    KEY idx_project_deleted (project_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- 4. 任务依赖表
CREATE TABLE IF NOT EXISTS pm_task_dependency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '依赖方任务（后置）',
    depends_on_task_id BIGINT NOT NULL COMMENT '被依赖任务（前置）',
    type VARCHAR(10) NOT NULL DEFAULT 'fs' COMMENT 'fs/ff/ss/sf',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_depends (task_id, depends_on_task_id),
    KEY idx_task_id (task_id),
    KEY idx_depends_on (depends_on_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务依赖表';

-- 5. 里程碑表
CREATE TABLE IF NOT EXISTS pm_milestone (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目 id',
    name VARCHAR(200) NOT NULL COMMENT '里程碑名称',
    description VARCHAR(500) NULL COMMENT '说明',
    due_date DATE NULL COMMENT '目标日期',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-未完成 1-已完成',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='里程碑表';
