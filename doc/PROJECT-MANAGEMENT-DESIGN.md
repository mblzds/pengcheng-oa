# 通用项目管理模块设计（B 方案）

**版本**: V1.0  
**日期**: 2026-03-02  
**目标**: 在现有智能表格、待办、日历基础上，新增**通用项目管理模块**，对标飞书项目/Jira/Monday.com，支持项目空间、任务管理、里程碑、任务依赖、多视图（列表/看板/甘特图/日历）。

---

## 1. 现状与范围

### 1.1 已有可复用能力

| 模块           | 说明 | 与项目管理的衔接 |
|----------------|------|------------------|
| 智能表格       | 看板/甘特图/日历视图、动态字段 | 视图组件与交互可复用，数据独立 |
| 待办 (sys_todo)| 简单待办 CRUD、优先级 | 可选：项目任务同步到「我的待办」 |
| 销售日历       | 月/周/日视图、事件 CRUD、团队日程 | 任务截止日期可展示在日历；接口风格可参考 |
| 云文档         | 空间 + 文档树 | 项目可关联文档空间（后续扩展） |
| 自动化规则引擎 | 定时/事件触发、通知/分配 | 任务逾期提醒、里程碑到期通知 |
| 组织架构       | sys_user、sys_dept | 项目成员、任务指派基于现有用户体系 |

### 1.2 本模块补齐范围

- **项目空间**：项目创建、成员与角色、状态、时间范围。
- **任务管理**：任务 CRUD、子任务、指派、状态、优先级、进度、截止日期。
- **任务依赖**：前置/后置任务，依赖类型（完成-开始等）。
- **里程碑**：项目级关键节点，可选关联任务。
- **多视图**：列表、看板（按状态列）、甘特图、日历。
- **数据权限**：项目成员可见、按部门/角色扩展（可选）。

---

## 2. 数据模型设计

### 2.1 项目（pm_project）

项目作为顶层容器，归集任务、里程碑与成员。

| 字段           | 类型         | 说明 |
|----------------|--------------|------|
| id             | BIGINT PK    | 主键 |
| name           | VARCHAR(200) | 项目名称 |
| description    | TEXT         | 项目描述 |
| owner_id       | BIGINT       | 负责人（关联 sys_user.id） |
| status         | TINYINT      | 1-未开始 2-进行中 3-已暂停 4-已完成 5-已归档 |
| start_date     | DATE         | 计划开始日期 |
| end_date       | DATE         | 计划结束日期 |
| visibility     | VARCHAR(20)  | private-仅成员 dept-本部门 all-全公司 |
| color          | VARCHAR(20)  | 主题色（如 #1890ff），看板/甘特图区分 |
| sort_order     | INT          | 排序 |
| create_time    | DATETIME     | 创建时间 |
| update_time    | DATETIME     | 更新时间 |
| create_by      | BIGINT       | 创建人 |
| deleted        | TINYINT      | 逻辑删除 |

- **索引**：owner_id, status, create_time, (owner_id, deleted)。

### 2.2 项目成员（pm_project_member）

项目参与人及角色，用于权限与「我的项目」筛选。

| 字段        | 类型         | 说明 |
|-------------|--------------|------|
| id          | BIGINT PK    | 主键 |
| project_id  | BIGINT       | 项目 id |
| user_id     | BIGINT       | 用户 id |
| role        | VARCHAR(20)  | owner-负责人 admin-管理员 member-成员 |
| join_time   | DATETIME     | 加入时间 |
| create_time | DATETIME     | 创建时间 |
| deleted     | TINYINT      | 逻辑删除 |

- **唯一约束**：(project_id, user_id)。
- **索引**：project_id, user_id。

### 2.3 任务（pm_task）

支持多级子任务（parent_id 自关联），与项目、指派、状态、进度、日期绑定。

| 字段           | 类型         | 说明 |
|----------------|--------------|------|
| id             | BIGINT PK    | 主键 |
| project_id     | BIGINT       | 所属项目 |
| parent_id      | BIGINT       | 父任务 id，0 表示顶层任务 |
| title          | VARCHAR(500) | 任务标题 |
| description    | TEXT         | 任务描述（富文本可选） |
| assignee_id    | BIGINT       | 执行人（sys_user.id） |
| status         | VARCHAR(30)  | 任务状态：待办/进行中/已完成 等，支持项目自定义列（见 2.6） |
| priority       | TINYINT      | 0-无 1-低 2-中 3-高 4-紧急 |
| progress       | TINYINT      | 进度 0-100 |
| start_date     | DATE         | 计划开始 |
| due_date      | DATE         | 截止日期 |
| estimated_hours| DECIMAL(8,2) | 预估工时（小时） |
| actual_hours   | DECIMAL(8,2) | 实际工时（小时） |
| sort_order     | INT          | 同层级内排序 |
| create_time    | DATETIME     | 创建时间 |
| update_time    | DATETIME     | 更新时间 |
| create_by      | BIGINT       | 创建人 |
| deleted        | TINYINT      | 逻辑删除 |

- **索引**：project_id, parent_id, assignee_id, status, due_date, (project_id, deleted)。

### 2.4 任务依赖（pm_task_dependency）

任务间前后依赖，用于甘特图关键路径与排期校验。

| 字段             | 类型         | 说明 |
|------------------|--------------|------|
| id               | BIGINT PK    | 主键 |
| task_id          | BIGINT       | 依赖方任务（后置） |
| depends_on_task_id | BIGINT     | 被依赖任务（前置） |
| type             | VARCHAR(10)  | fs-完成开始 ff-完成完成 ss-开始开始 sf-开始完成，默认 fs |
| create_time      | DATETIME     | 创建时间 |

- **唯一约束**：(task_id, depends_on_task_id)，且禁止 task_id = depends_on_task_id。
- **校验**：循环依赖检测（应用层或触发器）。
- **索引**：task_id, depends_on_task_id。

### 2.5 里程碑（pm_milestone）

项目级关键节点，可独立于任务存在，也可在甘特图中与任务一起展示。

| 字段         | 类型         | 说明 |
|--------------|--------------|------|
| id           | BIGINT PK    | 主键 |
| project_id   | BIGINT       | 项目 id |
| name         | VARCHAR(200) | 里程碑名称 |
| description  | VARCHAR(500) | 说明 |
| due_date     | DATE         | 目标日期 |
| status       | TINYINT      | 0-未完成 1-已完成 |
| sort_order   | INT          | 排序 |
| create_time  | DATETIME     | 创建时间 |
| update_time  | DATETIME     | 更新时间 |
| deleted      | TINYINT      | 逻辑删除 |

- **索引**：project_id。

### 2.6 项目状态列（pm_project_status_column）（可选）

若需支持项目自定义看板列（如「需求评审」「开发中」「测试」），可增加本表；否则使用系统默认 3 列：待办、进行中、已完成。

| 字段         | 类型         | 说明 |
|--------------|--------------|------|
| id           | BIGINT PK    | 主键 |
| project_id   | BIGINT       | 项目 id |
| name         | VARCHAR(50)  | 列名 |
| status_value | VARCHAR(30)  | 对应 pm_task.status 取值 |
| sort_order   | INT          | 看板列顺序 |
| is_done      | TINYINT      | 是否视为「已完成」列（用于统计） |
| create_time  | DATETIME     | 创建时间 |
| deleted      | TINYINT      | 逻辑删除 |

- **索引**：project_id。

---

## 3. 数据库迁移（Flyway）

- **脚本**：`V23__project_management.sql`
- **内容**：
  - 创建 pm_project、pm_project_member、pm_task、pm_task_dependency、pm_milestone 五张表及索引。
  - 可选：创建 pm_project_status_column，并预置默认列（待办/进行中/已完成）。
  - 外键：可不建物理外键，用应用层保证一致性；若建则 project_id → pm_project(id)，task 表 project_id/parent_id/assignee_id 等。

---

## 4. 后端模块与 API 规划

### 4.1 包结构建议

- **包路径**：`com.pengcheng.admin.controller.project`、`entity/project`、`mapper/project`、`service/project`
- **实体**：PmProject、PmProjectMember、PmTask、PmTaskDependency、PmMilestone（及可选的 PmProjectStatusColumn）
- **Service**：PmProjectService、PmTaskService、PmMilestoneService
- **Controller**：PmProjectController、PmTaskController、PmMilestoneController（或合并为 ProjectController 下多路径）

### 4.2 项目 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET    | /api/project/list | 项目列表（分页、按我创建的/我参与的/全部、状态筛选） |
| GET    | /api/project/{id} | 项目详情（含成员列表、统计：任务总数/已完成/逾期） |
| POST   | /api/project | 创建项目 |
| PUT    | /api/project/{id} | 更新项目 |
| DELETE | /api/project/{id} | 删除/归档项目（软删） |
| GET    | /api/project/{id}/members | 项目成员列表 |
| POST   | /api/project/{id}/members | 添加成员 |
| PUT    | /api/project/{id}/members/{userId} | 修改成员角色 |
| DELETE | /api/project/{id}/members/{userId} | 移除成员 |
| GET    | /api/project/{id}/stats | 项目统计（任务数、完成率、逾期数、里程碑进度） |

### 4.3 任务 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET    | /api/project/{projectId}/tasks | 任务列表（分页、parent_id/assignee_id/status/优先级/日期范围筛选、排序） |
| GET    | /api/project/{projectId}/tasks/tree | 任务树（含子任务层级） |
| GET    | /api/project/task/{taskId} | 任务详情（含子任务、依赖、评论可选） |
| POST   | /api/project/{projectId}/tasks | 创建任务 |
| PUT    | /api/project/task/{taskId} | 更新任务 |
| DELETE | /api/project/task/{taskId} | 删除任务（软删，子任务可级联或禁止） |
| PUT    | /api/project/task/{taskId}/status | 更新任务状态（看板拖拽） |
| PUT    | /api/project/task/{taskId}/assignee | 变更执行人 |
| PUT    | /api/project/task/{taskId}/progress | 更新进度 |
| GET    | /api/project/task/{taskId}/dependencies | 依赖列表（前置/后置） |
| POST   | /api/project/task/{taskId}/dependencies | 添加依赖（校验循环） |
| DELETE | /api/project/task/{taskId}/dependencies/{depId} | 删除依赖 |

### 4.4 里程碑 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET    | /api/project/{projectId}/milestones | 里程碑列表 |
| POST   | /api/project/{projectId}/milestones | 创建里程碑 |
| PUT    | /api/project/milestone/{id} | 更新里程碑 |
| DELETE | /api/project/milestone/{id} | 删除里程碑 |
| PUT    | /api/project/milestone/{id}/complete | 标记完成 |

### 4.5 视图/统计 API（便于前端多视图）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET    | /api/project/{projectId}/board | 看板数据（按状态分列，每列任务列表） |
| GET    | /api/project/{projectId}/gantt | 甘特图数据（任务+起止日期+依赖，含里程碑） |
| GET    | /api/project/{projectId}/calendar | 日历数据（任务 due_date + 里程碑 due_date，按日聚合） |

### 4.6 权限与数据权限

- **项目可见性**：private 仅成员；dept 本部门；all 全公司（需结合 sys_dept）。
- **操作权限**：owner/admin 可编辑项目、管理成员、删除；member 可创建/编辑/删除自己的任务，可查看全部任务。
- **接口**：所有接口需登录；列表接口按可见性过滤；修改/删除前校验角色。

---

## 5. 前端规划

### 5.1 路由与菜单

- **菜单**：在「协作办公」或「工作台」下增加「项目管理」一级菜单。
- **路由**：
  - `/project` — 项目列表（卡片或表格，筛选：我创建的/我参与的/全部，状态）
  - `/project/:id` — 项目详情（Tab：任务列表 / 看板 / 甘特图 / 日历 / 里程碑 / 设置）

### 5.2 页面与组件

| 页面/组件 | 说明 |
|-----------|------|
| 项目列表 | 项目卡片或表格，创建项目入口，按状态/参与角色筛选 |
| 项目详情-任务列表 | 表格：标题、执行人、状态、优先级、进度、截止日、操作；支持筛选、排序、分页；新建/编辑任务抽屉或弹窗 |
| 项目详情-看板 | 按状态分列，任务卡片拖拽变更状态（可复用智能表格看板交互） |
| 项目详情-甘特图 | 任务条+起止日期+依赖线+里程碑标记（可复用智能表格甘特图或 ECharts custom） |
| 项目详情-日历 | 月/周视图，任务与里程碑按 due_date 展示（可复用销售日历组件思路） |
| 项目详情-里程碑 | 里程碑列表，新建/编辑/完成/删除 |
| 项目详情-设置 | 项目基本信息编辑、成员管理、状态列配置（若启用 2.6） |
| 任务详情抽屉 | 标题、描述、执行人、状态、优先级、进度、日期、工时、依赖列表、子任务列表 |

### 5.3 与现有模块的集成

- **日历**：在「销售日历」或统一日历中，可选展示「项目任务」来源（按用户筛选本人负责任务的 due_date）。
- **待办**：可选「同步到我的待办」：将「指派给我」且状态非完成的任务同步到 sys_todo，便于在待办页统一处理。
- **工作台**：角色化门户中增加「我的任务」卡片（逾期/今日截止/进行中数量）及「项目管理」入口。
- **全局搜索**：项目名称、任务标题纳入 GlobalSearch（需扩展索引与接口）。

---

## 6. 与自动化、通知的衔接

- **自动化规则**：任务逾期提醒（due_date < 今日且 status 未完成 → 通知 assignee）；里程碑到期提醒（due_date 临近 → 通知项目成员）。
- **渠道推送**：重要任务指派、任务逾期、里程碑完成可推送钉钉/飞书/企微（复用 ChannelPushService）。

---

## 7. 实施顺序建议

| 阶段 | 内容 | 预估 |
|------|------|------|
| **Phase A** | V23 迁移脚本；PmProject/PmProjectMember/PmTask/PmTaskDependency/PmMilestone 实体与 Mapper；PmProjectService、PmTaskService、PmMilestoneService 基础 CRUD；PmProjectController、PmTaskController、PmMilestoneController 上述 API | 1 周 |
| **Phase B** | 任务依赖循环检测；看板/甘特图/日历数据接口（board/gantt/calendar）；项目统计接口 | 3 天 |
| **Phase C** | 前端：项目列表、项目详情（任务列表 + 看板 + 甘特图 + 日历 + 里程碑 + 设置）、任务 CRUD、依赖编辑、成员管理 | 1.5 周 |
| **Phase D** | 工作台「我的任务」卡片；日历/待办可选集成；自动化任务逾期与里程碑提醒；全局搜索扩展（可选） | 3 天 |

**合计约 2.5～3 周**（1 人全职）。

---

## 8. 与 TASKS-V3.0 的对应

在 `TASKS-V3.0.md` 中新增「3.14 通用项目管理模块」任务条，与本文档 2～7 节一一对应，便于迭代开发与验收。

---

## 9. 实现状态

当前实现情况以 **TASKS-V3.0.md** 中 3.14 为准，摘要如下：

| 类别 | 状态 | 说明 |
|------|------|------|
| 数据库 | ✅ | V23 迁移脚本已就绪：pm_project、pm_project_member、pm_task、pm_task_dependency、pm_milestone |
| 后端 | ✅ | pengcheng-system.project 包：实体/Mapper/Service/Impl；pengcheng-admin-api 下 PmProjectController、PmTaskController、PmMilestoneController；board/gantt/calendar、my-tasks、依赖循环检测 |
| 前端 | ✅ | 项目列表、项目详情（任务列表/看板/甘特图/日历/里程碑/设置）、工作台「我的任务」卡片与入口 |
| 集成 | ✅ | Heartbeat 任务逾期与里程碑到期提醒；全局搜索项目与任务；可选同步到 sys_todo |
| 文档 | ✅ | API-REFERENCE.md 3.2 节、USER-MANUAL.md 5.3 节已补充 |

---

## 10. 自定义看板列（V24 已实现）

支持**项目级自定义看板列**（如「需求评审」「开发中」「测试」）。已新增迁移 V24、表 `pm_project_status_column`，看板接口按列配置聚合任务并保持列顺序。

### 10.1 表结构（V24）

```sql
-- V24__project_status_columns.sql（可选）
CREATE TABLE IF NOT EXISTS pm_project_status_column (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目 id',
    name VARCHAR(50) NOT NULL COMMENT '列名（展示用）',
    status_value VARCHAR(30) NOT NULL COMMENT '对应 pm_task.status 取值',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '看板列顺序',
    is_done TINYINT NOT NULL DEFAULT 0 COMMENT '是否视为已完成列（用于统计）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目看板状态列配置';
```

- 新建项目时可复制「默认列」或从模板创建；未配置时沿用系统默认三列：待办、进行中、已完成（对应 status_value）。
- 前端看板按 `pm_project_status_column.sort_order` 排序列，每列展示 `pm_task.status = status_value` 的任务。
