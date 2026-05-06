# 人事与绩效模块补齐设计

**版本**: V1.1  
**日期**: 2026-03-02  
**定位**: **公司级公共服务模块**，不隶属于房产业务或单一业务线。与飞书、钉钉一致：人事与绩效作为全公司通用能力，服务销售、支持、职能等所有部门。

**技术实现**: 独立模块 `pengcheng-hr`（`com.pengcheng.hr`），不放在 `pengcheng-realty` 下；接口路径 `/admin/hr/*`、假勤保持 `/admin/attendance` 兼容；控制器包 `com.pengcheng.admin.controller.hr`。**假勤**（考勤打卡、请假、调休、签到）已迁入 `hr.attendance`，与人事、绩效同属公司级公共服务。

---

## 1. 现状与范围

### 1.1 已有能力

| 模块     | 说明 |
|----------|------|
| 考勤     | attendance_record / leave_request / realty_compensate_request，打卡、请假、调休、月度汇总、迟到早退判定 |
| 组织架构 | sys_user（含 is_quit）、sys_dept、sys_post、sys_user_post |
| 审批     | 请假/调休/付款等走统一审批（AppApprovalController），可复用于人事审批 |
| 销售质检 | sys_sales_quality_score 六维评分、综合分、AI 评语、团队排行（可作为绩效数据源之一） |

### 1.2 补齐范围

- **人事**：员工档案扩展、入职/离职/调岗异动、与组织架构/审批联动（全公司适用）。
- **绩效**：考核周期、KPI 指标模板、考核记录；数据来源可插拔（手工 + 各业务模块可选对接，如房产的佣金/考勤/质检仅为一类数据源）。

---

## 2. 人事模块设计

### 2.1 员工档案扩展（hr_employee_profile）

在保留 `sys_user` 主表前提下，用扩展表存人事专用字段，避免改动核心用户表。

| 字段           | 类型         | 说明 |
|----------------|--------------|------|
| id             | BIGINT PK    | 主键 |
| user_id        | BIGINT UK    | 关联 sys_user.id |
| employee_no    | VARCHAR(32)  | 工号 |
| join_date      | DATE         | 入职日期 |
| formal_date    | DATE         | 转正日期（可选） |
| contract_start | DATE         | 合同开始 |
| contract_end   | DATE         | 合同结束 |
| job_level      | VARCHAR(20)  | 职级（可字典） |
| work_location  | VARCHAR(100) | 工作地点 |
| emergency_contact | VARCHAR(50) | 紧急联系人 |
| emergency_phone   | VARCHAR(20) | 紧急联系电话 |
| remark         | VARCHAR(500) | 备注 |
| create_time / update_time / create_by / update_by / deleted | 标准字段 |

- **与现有系统**：user_id 唯一关联 sys_user，部门/岗位仍用 sys_dept、sys_post、sys_user_post。

### 2.2 人事异动（hr_employee_change）

记录入职、离职、调岗、调薪等异动，便于审计与统计。

| 字段        | 类型         | 说明 |
|-------------|--------------|------|
| id          | BIGINT PK    | 主键 |
| user_id     | BIGINT       | 员工 user_id |
| change_type | TINYINT      | 1-入职 2-离职 3-调岗 4-调薪 5-其他 |
| change_date | DATE         | 异动日期 |
| before_dept_id / after_dept_id | BIGINT | 调岗前后部门（调岗时用） |
| before_post_id / after_post_id | BIGINT | 调岗前后岗位（调岗时用） |
| reason      | VARCHAR(500) | 原因说明（离职原因等） |
| attachment  | VARCHAR(500) | 附件路径（如离职证明） |
| status      | TINYINT      | 1-草稿 2-已生效 |
| create_by / create_time / update_time / deleted | 标准字段 |

- **离职**：异动生效时，可同步更新 `sys_user.is_quit = 1`（或走审批流后更新）。
- **考勤**：离职后不再参与考勤统计；报表按 `join_date`/异动日期过滤在岗人员。

### 2.3 与考勤、审批的衔接

- **考勤**：考勤统计、月度汇总、异常检测仅针对「在岗」员工（sys_user.is_quit=0，且可选：存在 hr_employee_profile 且无「离职」生效异动）。
- **审批**：请假/调休/补卡仍走现有 AppApprovalController；若后续做「离职申请」「调岗申请」，可复用同一审批流，审批通过后写 hr_employee_change 并更新 sys_user / sys_user_post。

---

## 3. 绩效模块设计

### 3.1 考核周期（hr_kpi_period）

按周期（月/季/年）发起考核，便于与销售月报、质检周期对齐。

| 字段        | 类型         | 说明 |
|-------------|--------------|------|
| id          | BIGINT PK    | 主键 |
| name        | VARCHAR(100) | 周期名称，如「2026年Q1考核」 |
| period_type | TINYINT      | 1-月度 2-季度 3-年度 |
| year        | INT          | 年 |
| quarter     | TINYINT      | 季度（1-4），月度时可为 0 |
| month       | TINYINT      | 月份（1-12），季度/年度可为 0 |
| start_date  | DATE         | 周期开始 |
| end_date    | DATE         | 周期结束 |
| status      | TINYINT      | 1-未开始 2-考核中 3-已结束 |
| create_time / update_time / deleted | 标准字段 |

### 3.2 KPI 指标模板（hr_kpi_template）

可配置的指标项，支持销售与通用两类，便于复用。

| 字段          | 类型         | 说明 |
|---------------|--------------|------|
| id            | BIGINT PK    | 主键 |
| name          | VARCHAR(100) | 指标名称 |
| code          | VARCHAR(50)  | 编码，如 deal_count、attendance_rate |
| category      | TINYINT      | 1-销售业绩 2-考勤 3-过程质量 4-综合 |
| weight        | DECIMAL(5,2) | 权重（0-100），同一周期内合计建议 100 |
| data_source   | VARCHAR(50)  | 数据来源：manual-手工录入，auto_commission-佣金，auto_attendance-考勤，auto_quality-质检 |
| formula       | VARCHAR(200) | 计算公式或说明（可选） |
| sort_order    | INT          | 排序 |
| status        | TINYINT      | 0-停用 1-启用 |
| create_time / update_time / deleted | 标准字段 |

- **与现有数据**：data_source 为 auto_* 时，绩效打分可从 commission、attendance_record、sys_sales_quality_score 等表汇总计算。

### 3.3 考核记录（hr_kpi_score）

按「周期 + 员工」维度记录各指标得分及综合分。

| 字段          | 类型         | 说明 |
|---------------|--------------|------|
| id            | BIGINT PK    | 主键 |
| period_id     | BIGINT       | 考核周期 id |
| user_id       | BIGINT       | 被考核人 |
| template_id   | BIGINT       | 指标模板 id |
| target_value  | DECIMAL(12,2)| 目标值（可选） |
| actual_value  | DECIMAL(12,2)| 实际值 |
| score         | DECIMAL(5,2) | 得分（或由公式计算） |
| weighted_score| DECIMAL(5,2)| 加权得分 |
| remark        | VARCHAR(500) | 备注 |
| create_time / update_time / create_by / update_by / deleted | 标准字段 |

- **唯一约束**：(period_id, user_id, template_id) 保证同一周期同一人同一指标一条记录。
- **综合分**：可在应用层按周期+用户汇总 weighted_score，或增加汇总表/视图。

### 3.4 与销售、考勤、质检的衔接

- **销售业绩**：从 commission、customer_deal 等表按 user_id、周期统计签约额/套数，写入 actual_value 或通过 data_source=auto_commission 自动拉取。
- **考勤**：从 attendance_record、leave_request 等统计出勤率、迟到早退次数，对应 auto_attendance 指标。
- **过程质量**：从 sys_sales_quality_score 按周期取综合分或维度分，对应 auto_quality。
- **实现说明**：当前由 KpiScoreServiceImpl.autoFillByDataSource 内联 SQL 拉取上述三类数据；接口 KpiDataSourceProvider 已定义在 pengcheng-hr，供后续各业务模块实现 SPI 扩展时使用（可选）。
- **AI 日报/Heartbeat**：已有「考勤异常检测」等，可扩展为绩效预警（如连续迟到、业绩未达标提醒）。

---

## 4. 数据库迁移（Flyway）

- 脚本：`V22__hr_and_performance.sql`（见下一节）。
- 内容：创建 hr_employee_profile、hr_employee_change、hr_kpi_period、hr_kpi_template、hr_kpi_score 表及必要索引；可选增加 sys_dict_type/sys_dict_data 字典（如异动类型、考核周期类型、指标分类）。

---

## 5. 后端模块与 API 规划

### 5.1 包结构（已实现：pengcheng-hr 独立模块）

- **模块**：`pengcheng-core/pengcheng-hr`，包前缀 `com.pengcheng.hr`。
- **人事**：`hr.employee` — EmployeeProfile、EmployeeChange 实体/Mapper/Service，HrEmployeeController 在 admin-api 的 `controller.hr` 包。
- **绩效**：`hr.performance` — KpiPeriod、KpiTemplate、KpiScore 实体/Mapper/Service，HrKpiController 在 admin-api 的 `controller.hr` 包。

### 5.2 主要 API（已实现）

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 人事档案 | GET | /admin/hr/employee/profile/{userId} | 查询档案 |
|         | PUT | /admin/hr/employee/profile | 创建/更新档案 |
| 人事异动 | GET | /admin/hr/employee/changes | 异动列表（分页） |
|         | GET | /admin/hr/employee/changes/{id} | 异动详情 |
|         | POST | /admin/hr/employee/changes | 发起异动 |
|         | POST | /admin/hr/employee/changes/{id}/effective | 异动生效（离职同步 is_quit） |
| 考核周期 | CRUD | /admin/hr/kpi/periods | 周期管理 |
| KPI 模板 | CRUD | /admin/hr/kpi/templates | 指标模板；GET /templates/list 启用列表 |
| 考核记录 | GET | /admin/hr/kpi/scores?periodId=&userId= | 按周期/人查询得分 |
|         | POST | /admin/hr/kpi/scores | 单条保存 |
|         | POST | /admin/hr/kpi/scores/batch | 批量保存 |

- 权限：建议复用现有 RBAC，增加如 `hr:employee:list`、`hr:kpi:manage` 等权限点；HR 角色可配置为具备上述权限。

---

## 6. 前端与菜单

- **菜单**：在「房产业务」或单独「人事管理」下增加：
  - 人事档案（列表/详情/编辑，关联用户）
  - 人事异动（列表、发起异动）
  - 考核周期（列表、新建/编辑）
  - KPI 指标（模板列表、新建/编辑）
  - 绩效考核（按周期查看得分、填写/自动拉数、汇总）
- **工作台**：HR 角色门户中展示待审批（请假/调休/离职等）、考勤异常、考核进度等入口，与现有 2.10 智能工作台衔接。

---

## 7. 实施顺序建议

1. **Phase A — 人事基础**：V22 迁移脚本 → 员工档案 + 人事异动 实体/Service/Controller → 基础 API → 档案与异动前端页。
2. **Phase B — 绩效基础**：KPI 周期 + 模板 + 考核记录 实体/Service/Controller → 周期与模板管理 API 与前端。
3. **Phase C — 绩效数据与报表**：自动拉取销售/考勤/质检数据写入 actual_value 与 score → 汇总查询与简单报表 → 工作台 HR 卡片。

---

## 8. 与 TASKS-V3.0 的对应

在 `TASKS-V3.0.md` 中新增「Phase 3.x 人事与绩效」任务条，与本文档 2～7 节一一对应，便于迭代开发与验收。
