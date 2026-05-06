# MasterLife V4.0 产品需求文档（PRD）

> **版本**：V4.0 rev.2（基于 V3.2 开发计划与真实代码状态校准）  
> **文档状态**：草案（待评审）  
> **编写日期**：2026-04-23  
> **编写人**：产品负责人 + Claude  
> **适用范围**：基于 `pengcheng-admin` 代码基座的新一代产品规划  
> **配套文档**：[`DEV-PLAN-V3.2.md`](./DEV-PLAN-V3.2.md)（当前在跑的 Sprint 5-7 开发计划，V4.0 在其之上承接）  
> **图例约定**：✔ 已实现 ｜ ◐ 部分实现 ｜ ✗ 未实现 ｜ 🏳️ Feature Flag 受控（默认关闭，按需开启）  

### rev.2 校准说明（2026-04-23）

本次修订基于两项真实事实重校：
1. **数据权限拦截器已存在**：`pengcheng-infra/pengcheng-db/.../DataPermissionInterceptor.java` + 单测已通过，`@DataScope` 注解已在 `Alliance/Customer/SysUser` 等 Mapper 使用。MVP 工作量由"从零建设"改为"覆盖面扩展"。
2. **微信/支付宝改为 Feature Flag 受控**：`pengcheng-common/.../FeatureFlags.java` 定义 `pengcheng.feature.{alipay,wechat.mp,wechat.mini,wechat.pay}`，V3.2 WP-S6-G-1 已完成接入。之前 PRD 标的 🔒 冻结修正为 🏳️ Feature Flag（默认关闭，按需按环境开启）。
3. **V3.2 Sprint 5-7 与 V4.0 MVP 合并**：V3.2 正在推进的防御层（参数校验/异常/错误码）、佣金闭环、密钥保险箱、部署安全、前端严格化、会议/AI/HR/uniapp 财务、微信支付宝加固、测试基线、可观测、CI/CD 等 —— 全部是 V4.0 MVP 的前置/共用能力，工作量抵扣详见 §4.0。

---

## 目录

1. 项目概述
2. 产品范围总览
3. **现有系统全景评估（现状盘点）**
4. **MVP 阶段详细需求（每个闭环：现状 → 目标 → 差距 → 工作量）**
5. **迭代阶段详细规划（每个 Phase：现状 → 目标 → 差距 → 工作量）**
6. 非功能需求
7. 技术架构与改造重点
8. 度量指标、风险、开放问题
9. 附录：模块改造总览表

---

## 一、项目概述

### 1.1 背景

现有代码基座（MasterLife V3.x）是一套"房产销售管理系统"，已具备：
- 成熟的 Spring Boot 3 + Vue 3 + UniApp 技术栈；
- 完整的系统管理、组织、消息、考勤、客户、佣金、回款、HR、多维表格、云文档等模块；
- AI 基座：**Spring AI 1.1.2 + Spring AI Alibaba 1.1.2.2**（DashScope starter + PGVector starter），自研多 Agent 编排层（`orchestration/ RouterService / OrchestratorService`），RAG、MCP 工具生态完整；
- 微信/支付宝登录支付：**Feature Flag 机制**（`pengcheng.feature.{alipay,wechat.mp,wechat.mini,wechat.pay}`，`FeatureFlags.java`）默认关闭，按环境/租户开启；V3.2 WP-S6-G 持续做稳定性加固（证书动态获取、token Redis 缓存、回调防重放、金额容差配置化），代码**绝对保留**（见 V3.2 §六"绝对不删清单"）。

但 V3.x 定位过窄、通用业务闭环未聚焦、移动端仅限微信小程序，难以直接面向中小企业通用市场。

### 1.2 V4.0 重新定位

> **「AI 驱动的中小企业智能协作平台」** —— 以"组织 + 客户 + AI 助手 + 移动办公"四根支柱构建完整业务闭环，房产行业作为首个垂直样板落地。

### 1.3 核心价值主张

| 维度 | 描述 |
|------|------|
| **一站式** | OA 办公 + CRM 客户 + AI 助手 + 移动办公 四合一，替代多套 SaaS |
| **AI 原生** | 每个业务模块均可由 AI Agent 驱动：自动跟进提醒、自动生成文案、自动填单 |
| **全端可用** | Web 管理后台 + 原生 APP（iOS/Android）+ 微信小程序，数据实时同步 |
| **开箱即用** | 私有化/SaaS 双模式，中小企业当天可上线使用 |

### 1.4 目标用户

- **核心用户**：10～500 人规模的销售驱动型中小企业（首波：房产中介、建材、家装、教培）
- **三类角色**：
  - **员工**：前线销售/顾问，主阵地 **手机 APP** —— 日常办公 + 客户跟进 + AI 助手
  - **管理层**：部门主管/老板，主阵地 **APP + Web** —— 看数据、审批、设目标
  - **管理员**：IT / HR，主阵地 **Web** —— 组织、权限、配置

### 1.5 V3 → V4 主要差异

| 维度 | V3.x | V4.0 |
|------|------|------|
| 行业定位 | 房产专属 | 通用 SMB + 房产垂直样板 |
| 移动端 | 仅微信小程序 | 原生 APP（iOS/Android）+ 小程序 |
| 业务闭环 | 模块罗列 | 以"闭环"为产品组织单位 |
| AI 定位 | 附加能力 | 每个闭环均内嵌 AI Copilot |
| 支付/微信登录 | 主推能力 | 🏳️ Feature Flag 默认关闭；V3.2 WP-S6-G 做证书/token/防重放等稳定性加固 |

---

## 二、产品范围总览

V4.0 的产品设计原则：**功能不以模块划分，而以"从开始到完成的完整业务闭环"划分**。

### 2.1 MVP 五大闭环

| # | 闭环 | 关键步骤 |
|---|------|---------|
| ① | 账户与组织 | 注册企业 → 邀请成员 → 建部门/岗位 → 分角色权限 → 员工登录 |
| ② | 日常办公 OA | 打卡 → 日报 → 审批 → 通知 → 待办/IM |
| ③ | 客户管理 CRM | 线索 → 分配 → 跟进 → 商机 → 成交 → 公海回收 |
| ④ | AI 智能助手 | 对话问答 → 查客户/数据 → 生成文案/日报 → 知识库 → 智能提醒 |
| ⑤ | 移动办公 | 原生 APP + 小程序，闭环 ②③④ 全部核心功能可用 |

### 2.2 迭代路线

| Phase | 主题 | 预计周期 |
|-------|------|---------|
| 2 | 财务闭环（合同/发票/提成） | 5～6 周 |
| 3 | 数据决策闭环（看板/BI/洞察） | 4 周 |
| 4 | 协作工具闭环（多维表/云文档/会议） | 5 周 |
| 5 | 房产垂直样板重新上架 | 4 周 |
| 6 | 生态集成与 SaaS 多租户 | 6～8 周 |

---

## 三、现有系统全景评估

本节基于对 `pengcheng-admin` 代码的深度调研，给出现有能力的完整清单，作为后续差距分析的基线。

### 3.1 后端业务模块完成度总览

| 模块 | 完成度 | 说明 |
|------|-------|------|
| pengcheng-system（用户/角色/菜单/部门/岗位） | ✔ 95% | RBAC 完整，缺数据权限表达式 |
| pengcheng-auth（登录策略） | ✔ 90% | 账密/短信/微信/支付宝/小程序均可，缺企业注册与邀请 |
| pengcheng-realty（房产业务） | ✔ 90% | 客户/项目/佣金/渠道/回款全链路，需要通用化重构 |
| pengcheng-ai（AI 能力） | ✔ 85% | Spring AI + Spring AI Alibaba + 自研 Orchestration + RAG + MCP + 实验治理齐备，前端 Copilot 入口未统一 |
| pengcheng-message（消息/公告/IM） | ✔ 90% | 单聊/群聊/撤回/引用/离线补偿已实现 |
| pengcheng-hr（人事/绩效） | ✔ 80% | 员工档案/异动/KPI 完整 |
| pengcheng-file（文件与预览） | ✔ 90% | MinIO + kkFileView |
| pengcheng-pay（微信/支付宝） | 🏳️ 90% | Feature Flag `pengcheng.feature.{alipay,wechat.pay}` 默认关闭；V3.2 WP-S6-G 正在做加固（证书动态获取/token 缓存/回调防重放/金额容差配置化） |
| pengcheng-wechat（微信公众号/小程序 OAuth） | 🏳️ 90% | Feature Flag `pengcheng.feature.wechat.{mp,mini}` 默认关闭 |
| pengcheng-social（第三方社交登录） | 🏳️ 80% | Feature Flag 受控 |
| pengcheng-push（极光/友盟/个推） | ✔ 85% | 三供应商工厂模式切换 |
| pengcheng-job（定时任务） | ✔ 90% | Quartz，公海回收/AI 心跳等已接入 |

### 3.2 关键业务表清单（已落地）

- **系统基础**：`sys_user / sys_dept / sys_post / sys_role / sys_menu / sys_role_menu / sys_notice / sys_job`
- **通讯协作**：`sys_chat_message / sys_chat_group_message / sys_chat_offline_message / sys_todo / sys_calendar_event`
- **CRM 核心**：`customer / customer_project / customer_visit / customer_deal / alliance / project / project_commission_rule`
- **财务回款**：`receivable_plan / receivable_record / receivable_alert / commission / payment_request / pay_notify_log`
- **人事考勤**：`attendance_record / leave_request / hr_employee_profile / hr_employee_change / hr_kpi_*`
- **销售运营**：`sys_daily_report / sys_sales_quality_score / sys_sales_visit / customer_pool_config`
- **AI 与协作**：`ai_memory / ai_memory_episode / smart_table* / sys_doc / sys_doc_space / sys_doc_version`
- **项目管理**：`pm_project / pm_task / pm_milestone / pm_task_dependency / pm_project_member`
- **会议与自动化**：`meeting_notification / meeting_minutes / meeting_file / sys_automation_rule`
- **AI 治理**：`sys_ai_heartbeat_log / ai_tool_audit_log / ai_experiment_config / alert_log`

### 3.3 前端与移动端现状

- **Web 管理后台（pengcheng-ui, Vue3 + Naive UI）**  
  已有业务视图：`dashboard / system / monitor / log / message / org / realty / project / hr / ai(chat/config/experiment/knowledge/mcp/memory/skills) / meeting / smarttable / doc / todo`。
- **uni-app（pengcheng-uniapp）**  
  已配置 `app-plus / mp-weixin / mp-alipay / mp-baidu / mp-toutiao`；页面覆盖：`login / index / workbench / chat / group-chat / contacts / group / customer / attendance / ai / approval / apply / profile`。  
  **当前实际主要运行在 mp-weixin**；原生 APP 打包流水线、原生插件（推送 / 定位 / OCR / 扫码）接入尚未就绪。

### 3.4 关键差距快照（指向 MVP 必须补齐）

1. **企业注册与成员邀请流程** ✗ —— 影响 SaaS 化与冷启动；
2. **数据权限覆盖面** ◐ —— 核心拦截器 `DataPermissionInterceptor` + `@DataScope` 注解已落地（单测通过，已在 Alliance/Customer/SysUser 使用），但其它高频 Mapper 尚未全量挂注解，前端仍缺管理员配置 UI；
3. **客户自定义字段 + 线索独立实体** ✗ —— 现仅有固定字段的 `customer`，行业扩展性受限；
4. **跟进记录多媒体（语音/图片/视频）** ◐ —— 当前只有文本 `remark`；
5. **前端统一 AI Copilot 入口（Web/APP/小程序三端组件）** ◐ —— 后端强、前端散；V3.2 WP-S6-B 正在补 `aiKnowledge/aiSkills/aiMcp` API 层；
6. **uni-app 原生 APP 打包流水线 + 原生能力接入** ✗ —— 当前只跑在小程序；
7. **审批流程引擎动态配置** ◐ —— 当前审批仅为固定模型；
8. **客户导入/导出 Excel** ✗ —— 批量录入入口缺失；
9. **参数校验 / 错误码字典 / 全局异常** ◐ —— V3.2 WP-S5-A 在跑，MVP 直接承接；
10. **代码废弃物** ◐ —— V3.2 WP-S5-F 已列清单（`pom.xml.bak ×2`、`smart-table/`、`test/test/`、`system/customer/`、`uniapp/utils/websocket.js`）尚未清理。

---

## 四、MVP 阶段详细需求

> **目标**：6～8 周内交付可商用版本，前期即可让员工在手机上完成日常办公、客户管理、AI 助手的所有核心动作。

### 4.0 V3.2 Sprint 与 V4.0 MVP 对齐（工作量抵扣表）

V3.2 已在跑 Sprint 5-7，下列任务属于 V4.0 MVP 的前置能力或共用基建。MVP 排期时**不重复计算**其工作量，直接承接 V3.2 结果；如 V3.2 该项未完成，则 V4.0 MVP 把它纳入补齐。

| V3.2 工作包 | 状态 | 对 V4.0 MVP 闭环的影响 |
|------------|------|----------------------|
| WP-S5-A 参数校验 + 全局异常 + 错误码 | 🟦 | 所有闭环的防御层共用 —— 直接承接 |
| WP-S5-B 佣金自动结算闭环 | 🟦 | 直接支撑 Phase 5 房产垂直，MVP 不直接覆盖 |
| WP-S5-C 敏感信息密钥保险箱（`sys_secret_vault`） | ⬜ | 账户与组织、支付加固的安全前置 |
| WP-S5-D 部署安全加固（nginx SSL/CSP/limit_req_zone/docker secrets） | ⬜ | 所有闭环的生产环境前置 |
| WP-S5-E 前端类型严格化（tsconfig strict / 401 并发竞态 / 重试退避） | ⬜ | 所有 Web 闭环的质量基建 |
| WP-S5-F 废弃文件清理（`pom.xml.bak`/`smart-table/`/`test/test/`/`system/customer/`/`uniapp/utils/websocket.js`） | ⬜ | MVP 启动前必须先做，避免后续 IDE 误导 |
| WP-S6-A 会议模块接真 API + 会议通知走 `ChannelPushService` | ⬜ | 闭环② 日常办公 —— 会议在 Phase 4 做深，MVP 仅保留基础 |
| WP-S6-B AI 三子模块 API 层（aiKnowledge/aiSkills/aiMcp） | ⬜ | **闭环④ AI 助手直接承接** |
| WP-S6-C AI 容错 + RAG 降级（BM25 fallback / 通用 agent 降级链） | ⬜ | 闭环④ 前置 |
| WP-S6-D HR 考勤 Excel 导入 + 360 绩效 | ⬜ | 闭环② 日常办公直接承接 |
| WP-S6-F uniapp 财务模块（佣金/回款/项目/成交确认） | ⬜ | Phase 5 房产垂直 + APP 端适配样板 |
| WP-S6-G 微信/支付宝加固（证书动态/token 缓存/回调防重放/金额容差） | 🟦 | 🏳️ Feature Flag 维护模式的"稳定性优化"，**不做新功能迭代** |
| WP-S7-A/B 测试基线（JaCoCo/集成/契约/E2E） | 🟦 | 所有闭环的质量门禁 |
| WP-S7-C 可观测性（Prometheus/Grafana/Loki） | ⬜ | 上线前置 |
| WP-S7-D CI/CD + 依赖锁定 | ⬜ | 上线前置 |
| WP-S7-E Spring AI 升版专项 | ⏸ | 延后，不阻断 MVP |
| WP-S7-G 文件预览 + Office 编辑（WOPI/OnlyOffice） | ⬜ | Phase 4 协作工具，MVP 不做 |

**V4.0 MVP 独立新增**（非 V3.2 范围）：闭环①企业注册/邀请、闭环③线索独立+自定义字段+多媒体跟进、闭环④统一 Copilot 前端组件+智能提醒主动推送、闭环⑤ uni-app APP 打包流水线 + 原生插件（推送/定位/OCR/扫码）。

---

### 闭环 ① 账户与组织

#### 1) 现状盘点

| 能力 | 现状 | 证据 |
|------|------|------|
| 账密登录 | ✔ | `AdminAuthController#/auth/login`，`LoginStrategyFactory` |
| 手机验证码登录 | ✔ | `SmsServiceFactory` + Redis 验证码缓存 |
| 微信/支付宝第三方登录 | 🏳️ | `WechatMpAuthController`、`SocialLoginService`；受 Feature Flag `pengcheng.feature.wechat.{mp,mini}` / `pengcheng.feature.alipay` 控制，默认关闭 |
| 部门/岗位树 | ✔ | `SysDeptController / SysPostController`，表 `sys_dept / sys_post` |
| 角色/菜单 RBAC | ✔ | `SysRoleController / SysMenuController`，表 `sys_role / sys_menu / sys_role_menu` |
| 数据权限核心 | ✔ | `pengcheng-infra/pengcheng-db/.../DataPermissionInterceptor.java` + 单测；`@DataScope` 注解在 `pengcheng-common`；`DataPermissionDeniedException` 在 realty 侧 |
| 数据权限覆盖面 | ◐ | 已接入：`AllianceMapper / RealtyCustomerMapper / SysUserMapper / SysUserServiceImpl`；其他高频 Mapper 未挂注解，前端无管理员配置页 |
| 多租户 | ◐ | 架构支持 MyBatis `TenantLine`，业务表未全量带 `tenant_id` |
| 企业注册 | ✗ | 无企业注册流程 |
| 成员邀请 | ✗ | 无 `invitation_record` 表 |
| 个人中心 | ◐ | `SysUserController` 有基础，设备管理缺 |

#### 2) 目标（MVP 交付）

- 新企业一分钟开通（管理员自动创建，默认部门/角色就位）；
- 手机号/二维码/Excel 三种成员邀请；
- 数据权限四档：**仅本人 / 本部门 / 本部门及下级 / 全部**；
- 预置 6 个行业角色：老板/主管/销售/HR/财务/管理员；可自定义；
- 个人中心：头像/密码/手机号/多端登录设备列表与踢下线。

#### 3) 差距与工作量

| 要做的事 | 端 | 工作量 | 备注 |
|---------|----|-------|------|
| 新增 `tenant` / `tenant_member_invite` 表与 Service | 后端 | 3 人日 | 带过期、状态、邀请人 |
| 企业注册向导页（含管理员创建、初始化默认部门/角色） | Web | 2 人日 | — |
| 成员邀请：短信邀请、邀请链接、二维码、Excel 批量导入 | Web + 后端 | 4 人日 | 导入失败行反馈 |
| 数据权限**覆盖面扩展**：给高频 Mapper 全量挂 `@DataScope`（核心已建，非从零） | 后端 | 2 人日 | 抵扣 V3.1 已完成的拦截器 + 单测 |
| 数据权限管理员配置 UI（按角色配置作用域）| Web | 2 人日 | — |
| 预置角色模板（SQL seed 数据） | 后端 | 0.5 人日 | — |
| 个人中心·设备管理（`user_login_device` 表，支持踢下线） | 后端 + Web + APP | 3 人日 | 复用 Sa-Token `kickout` |
| 多租户字段在业务表的全量回补（渐进） | 后端 | 计入各闭环 | — |

**小计：约 16.5 人日**（较 rev.1 减少 1.5 人日，因数据权限核心已落地）

---

### 闭环 ② 日常办公 OA

#### 1) 现状盘点

| 能力 | 现状 | 证据 |
|------|------|------|
| 考勤打卡（上下班） | ✔ | `AttendanceController`，表 `attendance_record`；`AttendanceServiceImpl` 单测已通过 |
| 外勤 GPS 打卡 | ◐ | `clockInLocation / clockOutLocation` 字段有，规则不完整 |
| 班次设置 | ✗ | 无 `shift_schedule` 表 |
| 补卡 | ✗ | 无补卡单据 |
| 考勤 Excel 导入 | ✗ | V3.2 WP-S6-D-1 规划中 |
| 月度考勤报表导出 | ✗ | V3.2 WP-S6-D-2 规划中 |
| 日报/周报 | ✔ | 表 `sys_daily_report`，`DailyReportController` |
| AI 日报草稿 | ✔ | `sys_daily_report.summary` + AI 生成 |
| 请假审批 | ✔ | 表 `leave_request`，`AppApprovalController` |
| 通用审批模板（外出/加班/报销） | ✗ | 仅请假模型 |
| 动态审批流程引擎 | ✗ | 未集成 Flowable/Activiti |
| 通知公告 | ✔ | 表 `sys_notice`，`SysNoticeController` |
| 待办中心 | ✔ | 表 `sys_todo`，`TodoController`，已聚合 |
| IM 单聊/群聊/撤回/引用/离线 | ✔ | V10 增强，`MessageWebSocketHandlerIntegrationTest` 端到端测试通过 |
| 任务管理 | ✔ | `pm_task`、`pm_milestone` 完整 |
| 会议模块前端接真 API | ◐ | V3.2 WP-S6-A 在跑（8 个 TODO 待接）|
| 360 绩效权重配置 | ✗ | V3.2 WP-S6-D-4 规划中 |

#### 2) 目标（MVP 交付）

- 员工一天完整工作流在手机上跑通：**打卡 → 看通知 → 处理待办/审批 → 回日报 → 下班打卡**；
- 审批模板扩展为 **请假 / 外出 / 加班 / 报销 / 通用** 5 类；
- 轻量流程引擎：单级/多级串行审批（并行放迭代期）；
- 考勤：班次模板、迟到/早退规则、补卡申请单 → 月度考勤报表。

#### 3) 差距与工作量

| 要做的事 | 端 | 工作量 | 备注 |
|---------|----|-------|------|
| `attendance_shift` 班次表 + 规则引擎（迟到/早退判定重构） | 后端 | 3 人日 | — |
| `attendance_correction`（补卡单）+ 审批接入 | 后端 + 三端 | 2 人日 | — |
| 4 个审批模板（外出/加班/报销/通用）表结构 + Service | 后端 | 3 人日 | — |
| 轻量串行审批流程引擎（`approval_flow_def` / `approval_flow_node` / `approval_instance`） | 后端 | 6 人日 | 暂不做 BPMN |
| 审批流配置页（管理员可视化配置审批人） | Web | 3 人日 | — |
| 三端审批卡片 UI 优化（统一列表/详情/处理组件） | Web + APP + MP | 3 人日 | — |
| 日报移动端体验优化（语音输入、AI 草稿按钮） | APP + MP | 2 人日 | — |
| ⇢ 考勤 Excel 导入 / 月度报表导出 | — | — | **承接 V3.2 WP-S6-D-1/2** |
| ⇢ 会议前端真 API 接入 | — | — | **承接 V3.2 WP-S6-A** |

**小计：约 22 人日**（V3.2 在跑任务不计入）

---

### 闭环 ③ 客户管理 CRM

#### 1) 现状盘点

| 能力 | 现状 | 证据 |
|------|------|------|
| 客户档案（公海/私海） | ✔ | 表 `customer`，`pool_type` 字段 |
| 独立线索池（Lead） | ✗ | 无 `lead` 表，线索和客户未分离 |
| 跟进记录 | ✔ | 表 `customer_visit`，`SalesVisitController` |
| 跟进多媒体（语音/图片/视频） | ✗ | 仅文本 `remark` |
| 商机管理（阶段推进） | ◐ | 通过 `customer_deal` 签约状态替代，无独立 Opportunity |
| 成交归档 | ✔ | 表 `customer_deal`（签约/网签/备案/贷款/回款全链路） |
| 公海回收规则 | ✔ | `customer_pool_config` + 定时任务 `customerPoolRecycleTask` |
| 手机号查重 | ✔ | `customer.phone` 唯一键 + AES |
| 客户导入/导出 | ✗ | 无 import/export 接口 |
| 标签管理 | ◐ | 有 `sys_sales_visit_tag`，客户标签自定义未完成 |
| 自定义字段 | ✗ | 无动态字段模型 |
| 房产专属字段混入 | 问题 | `customer` 表含 `visit_count / visit_time / alliance_id / deal_probability` 等行业字段 |

#### 2) 目标（MVP 交付）

- **线索 → 客户 → 商机 → 成交 → 回款**一条主干跑通；
- 线索独立实体（支持表单/二维码线索采集）；
- 跟进记录支持文字 + 语音 + 图片 + 视频；
- 客户自定义字段（文本/数字/日期/选项/多选/文件），行业字段即可配置；
- 客户/线索 Excel 批量导入导出（带失败行反馈）；
- 客户标签分组管理；
- 通用化重构：把现有 `customer` 中的房产专属字段下沉到 `customer_realty_ext`，主表只保留通用字段。

#### 3) 差距与工作量

| 要做的事 | 端 | 工作量 | 备注 |
|---------|----|-------|------|
| 新增 `lead`（线索）主表 + 分配 + 转客户 | 后端 | 4 人日 | — |
| 线索采集：表单构建器 + 公开 URL + 二维码 | Web + 后端 | 5 人日 | 可先做简化版 |
| `customer_visit` 扩展：`media_type / media_urls / voice_duration` | 后端 | 1.5 人日 | — |
| 跟进多媒体上传（MinIO 直传） + APP 录音/录像 | APP + MP | 4 人日 | — |
| 自定义字段：`custom_field_def / custom_field_value`（EAV 模型） | 后端 | 5 人日 | — |
| 自定义字段可视化配置（管理员配字段、校验规则） | Web | 4 人日 | — |
| 客户详情页动态渲染自定义字段 | 三端 | 4 人日 | — |
| Excel 导入/导出模板 + 失败行反馈 | Web + 后端 | 3 人日 | EasyExcel |
| 客户标签 `customer_tag / customer_tag_rel` | 后端 + Web | 2 人日 | — |
| `customer_realty_ext` 行业扩展表迁移 + 数据平滑迁移脚本 | 后端 | 3 人日 | 不破坏现有房产数据 |
| 商机独立：`opportunity` 表 + 阶段推进看板（可选，可放迭代） | 后端 + Web | 5 人日 | **MVP 可选** |

**小计：约 35.5 人日**（商机独立如放到迭代 Phase 5，则为 30.5 人日）

---

### 闭环 ④ AI 智能助手

#### 1) 现状盘点

| 能力 | 现状 | 证据 |
|------|------|------|
| 多模型接入 | ✔ | DashScope、智谱、OpenAI 兼容 |
| Agent 编排 | ✔ | 自研 `orchestration/` 包：`RouterService / OrchestratorService`，基于 Spring AI（无 AgentScope 依赖） |
| MCP 工具生态 | ✔ | 6 大 Agent 工具类 |
| 对话/流式/多轮 | ✔ | `AiChatController`，`ai_memory` 表 |
| 业务问答（查库） | ✔ | `CustomerAgentTool`、`RealtyDataTools` 等 |
| 文案生成 | ✔ | `AiContentController / AiSkillController` |
| 知识库 RAG（PDF/Word/Excel/PPT） | ✔ | Apache Tika + PGVector + DashScope Embedding |
| 智能提醒 | ✔ | `sys_ai_heartbeat_log` + 定时任务 |
| 实验治理、工具审计 | ✔ | `ai_experiment_config / ai_tool_audit_log / alert_log` |
| 前端统一 Copilot 入口 | ◐ | Web 有 `views/ai`，但无全局悬浮入口 |
| AI 前端 API 层（aiKnowledge / aiSkills / aiMcp） | ◐ | V3.2 WP-S6-B 在跑 |
| RAG BM25 fallback / 通用 Agent 降级链 | ✗ | V3.2 WP-S6-C 规划中，MVP 直接承接 |
| 索引失败重试队列 + DLQ | ✗ | V3.2 WP-S6-C-4 |
| APP 端 AI 入口 | ◐ | `pages/ai` 存在，约 70% 完成度 |
| 语音输入 | ◐ | 权限已配置，未落地 |

#### 2) 目标（MVP 交付）

- **全端统一的悬浮 Copilot 入口**（Web/APP/小程序右下角 FAB）；
- 自然语言问答能打通：
  - "这个月我跟了几个客户？" → 业务查询
  - "帮我写条给王总的回访话术" → 文案生成 + 客户档案上下文
  - "AI 知识库里 XX 合同怎么写" → RAG 问答
  - "今天我要做什么" → 综合待办+跟进+审批
- 智能主动推送：
  - 每日 9:00 推送"今日待跟进客户"；
  - 审批堆积 2h 提醒审批人；
  - 公海即将掉库前 1 天提醒归属销售。
- 语音输入：APP/小程序语音 → 文字 → 提交；
- 对话中可直接触发业务动作（新建跟进、创建待办、提交审批）。

#### 3) 差距与工作量

| 要做的事 | 端 | 工作量 | 备注 |
|---------|----|-------|------|
| 统一 Copilot 前端组件库（悬浮 FAB、对话抽屉、消息气泡、Markdown/流式） | Web + APP + MP | 6 人日 | 一次开发三端复用 |
| 全局上下文注入（当前用户、当前页面、可执行动作） | 前后端 | 3 人日 | — |
| 对话驱动的业务动作（Tool Call → 前端确认 → 提交） | 前后端 | 4 人日 | 跟进/待办/审批 3 个动作 |
| 智能提醒调度规则表 `ai_reminder_rule` + 定时任务扩展 | 后端 | 3 人日 | — |
| 智能推送触发（集成现有 Push 通道） | 后端 | 2 人日 | — |
| 语音输入（ASR）：APP 原生录音 → 转文字（对接 DashScope ASR） | APP + 后端 | 3 人日 | — |
| APP `pages/ai` 剩余 30% 补齐 | APP | 3 人日 | — |
| AI 成本控制：分级路由（小模型兜底 + 大模型按需）+ 企业配额 | 后端 | 4 人日 | 放在 MVP 后段 |
| ⇢ AI 前端 API 层（aiKnowledge/aiSkills/aiMcp） | — | — | **承接 V3.2 WP-S6-B** |
| ⇢ RAG BM25 fallback + Agent 降级链 + DLQ | — | — | **承接 V3.2 WP-S6-C** |

**小计：约 28 人日**（V3.2 在跑任务不计入）

---

### 闭环 ⑤ 移动办公（APP + 小程序）

#### 1) 现状盘点

| 能力 | 现状 | 证据 |
|------|------|------|
| uni-app 多端配置 | ✔ | `manifest.json` 含 `app-plus / mp-weixin / mp-alipay / mp-baidu / mp-toutiao` |
| 小程序主力运行 | ✔ | 目前实际主要跑在 `mp-weixin` |
| 页面覆盖 | ✔ | 12 个核心页面已就位 |
| APP 打包流水线 | ✗ | 无 CI 打包脚本（需 HBuilderX 或 CLI 手动） |
| 原生插件 · 推送 | ◐ | 后端 JPush/Umeng/GeTui ✔；uni-app 原生插件未接入 |
| 原生插件 · 定位 | ◐ | 权限配置 ✔；外勤精准定位未封装 |
| 原生插件 · 相机/扫码 | ◐ | uni API 可用；未封装业务组件 |
| OCR 名片识别 | ✗ | 未集成 |
| 小程序订阅消息 | ◐ | 权限 ✔，模板消息集中调度未实现 |
| 离线缓存策略 | ◐ | 零散 `uni.setStorage`，无统一方案 |
| APP 签名与上架 | ✗ | 无开发者账号/证书配置 |

#### 2) 目标（MVP 交付）

- **原生 APP（iOS + Android）**通过 uni-app 打包上架（iOS 企业签/TestFlight 起步，Android 应用市场）；
- **小程序**功能与 APP 对齐（部分 APP 独占功能如实时定位、OCR 在小程序以引导方式降级）；
- APP 支持推送（极光/友盟/个推任选一）、精准定位、相机、扫一扫；
- OCR 名片扫描入库（对接第三方，如百度 OCR / 腾讯 OCR）；
- 离线缓存策略：客户列表、通知、待办、日报草稿可离线查看/编辑；
- 消息推送分渠道：APP 推送 + 小程序订阅消息 + Web 站内信，统一调度。

#### 3) 差距与工作量

| 要做的事 | 端 | 工作量 | 备注 |
|---------|----|-------|------|
| uni-app APP 打包流水线（HBuilderX CLI + CI 脚本） | 工程 | 3 人日 | — |
| iOS 企业签证书配置 + TestFlight 发布流程文档 | 工程 | 2 人日 | 需企业开发者账号 |
| Android 多渠道打包（华为/小米/OPPO/vivo） | 工程 | 2 人日 | — |
| 原生推送插件接入（选定一家：极光推荐） | APP | 3 人日 | — |
| 定位/相机/扫一扫 统一业务组件封装 | APP + MP | 3 人日 | — |
| OCR 名片识别（对接第三方）+ 自动填充客户 | APP + 后端 | 4 人日 | — |
| 小程序订阅消息模板 + 统一调度 Service | 后端 + MP | 2 人日 | — |
| 离线缓存统一方案（白名单 + TTL + 冲突合并） | 三端 | 4 人日 | — |
| 消息推送统一调度（Web 站内/APP/小程序 三通道决策） | 后端 | 3 人日 | — |
| APP 启动页/引导页/版本更新（热更新模块） | APP | 3 人日 | — |
| ⇢ uniapp 财务页面（佣金/回款/项目/成交确认） | — | — | **承接 V3.2 WP-S6-F**，样板复用 |

**小计：约 29 人日**（V3.2 WP-S6-F 不计入）

---

### MVP 合计工作量（已扣除 V3.2 在跑任务）

| 闭环 | 工作量 | 备注 |
|------|-------|------|
| ① 账户与组织 | 16.5 人日 | 数据权限核心已就位，改为覆盖扩展 |
| ② 日常办公 | 22 人日 | 承接 V3.2 WP-S6-A/D 不计入 |
| ③ 客户管理 | 30.5 人日 | 商机独立放到 Phase 5 |
| ④ AI 助手 | 28 人日 | 承接 V3.2 WP-S6-B/C 不计入 |
| ⑤ 移动办公 | 29 人日 | 承接 V3.2 WP-S6-F 不计入 |
| **V4.0 MVP 新增合计** | **~ 126 人日** | |
| V3.2 并行任务 | 按 V3.2 排期独立推进 | Sprint 5-7，约 6 周 |

按 **3 名后端 + 2 名前端 + 1 名移动** 6 人团队计：V4.0 新增部分**约 21～28 工作日 / ≈ 5～6 周纯开发**；V3.2 Sprint 5-7 与之并行推进，联调、灰度、上架、验收统一对齐 **8 周交付**。

---

## 五、迭代阶段详细规划

> 每个 Phase 4～6 周，可按市场反馈调整顺序。

### Phase 2：财务闭环（合同 / 发票 / 提成）

#### 1) 现状盘点

| 能力 | 现状 | 证据 |
|------|------|------|
| 回款计划 | ✔ | 表 `receivable_plan`（分期/金额/日期） |
| 到账流水 | ✔ | 表 `receivable_record` |
| 逾期告警 | ✔ | 表 `receivable_alert` |
| 佣金规则 | ✔ | 表 `project_commission_rule`（跳点/奖励） |
| 佣金审核 | ✔ | `commission.audit_status` |
| 合同管理 | ✗ | 无 `contract` 表 |
| 电子签署 | ✗ | 无对接 |
| 发票/开票 | ✗ | 无 `invoice` 表 |

#### 2) 目标

- 合同主流程：**模板 → 起草 → 审批 → 在线签署（对接 e 签宝/法大大） → 履约跟踪 → 归档**；
- 开票闭环：**申请 → 审批 → 开具 → 快递寄送 → 客户签收**；
- 通用销售提成规则（非房产行业）：按成交金额比例/阶梯/团队分成；
- 报销 + 合同 + 开票三向联动。

#### 3) 差距与工作量

| 要做的事 | 工作量 |
|---------|-------|
| `contract / contract_template / contract_version / contract_sign_record` 表 + Service | 8 人日 |
| 合同模板引擎（变量替换，导出 PDF） | 5 人日 |
| 对接 e 签宝（或法大大）API，签署状态回调 | 5 人日 |
| `invoice / invoice_item / invoice_delivery` 表 + 申请审批流 | 5 人日 |
| 税控/开票 API 对接（如诺诺/百望云） | 5 人日 |
| 通用提成规则引擎（表达式 DSL）+ 自动计算 | 6 人日 |
| Web 端合同/发票/提成管理页 | 6 人日 |
| APP/小程序端查看与签署 | 4 人日 |

**小计：约 44 人日**

---

### Phase 3：数据决策闭环（看板 / BI / AI 洞察）

#### 1) 现状盘点

| 能力 | 现状 | 证据 |
|------|------|------|
| 基础看板 | ✔ | `pengcheng-ui/src/views/dashboard`，`RealtyDashboardController` |
| 业务快照 | ✔ | 表 `sys_sales_daily_snapshot` |
| AI 分析接口 | ✔ | `AiAnalysisController` |
| 自定义卡片 | ◐ | 容器在，拖拽/编辑器无 |
| BI 多维分析 | ◐ | SQL 级别，无 OLAP |
| AI 周/月洞察报告 | ◐ | 可生成，但未成规则 |
| 目标管理 OKR/KPI | ◐ | HR 模块有 KPI，未做 OKR |

#### 2) 目标

- 可配置看板：卡片市场（20+ 预置卡片）+ 拖拽布局 + 权限绑定；
- BI 自助分析：多维查询（维度/指标/过滤/排序）、导出、图表（折线/柱状/饼/漏斗/热力）；
- AI 自动洞察周/月报：按角色定制（老板看全局、主管看团队、销售看个人）；
- OKR：目标设定 + 关键结果 + 进度跟踪 + AI 辅助拆解。

#### 3) 差距与工作量

| 要做的事 | 工作量 |
|---------|-------|
| 卡片注册表 + 卡片配置表 `dashboard_card_def / dashboard_layout` | 4 人日 |
| 20 个预置卡片（销售漏斗/团队排行/转化/客户健康等） | 8 人日 |
| 拖拽布局编辑器（Web） | 5 人日 |
| 多维查询引擎（基于视图模型 + 动态 SQL） | 8 人日 |
| 图表组件库统一（ECharts / AntV） | 3 人日 |
| AI 洞察报告模板（按角色）+ 定时生成 + 推送 | 5 人日 |
| OKR 表 `okr_objective / okr_key_result / okr_checkin` + UI | 8 人日 |

**小计：约 41 人日**

---

### Phase 4：协作工具闭环（多维表格 / 云文档 / 会议）

#### 1) 现状盘点

| 能力 | 现状 | 证据 |
|------|------|------|
| 多维表格 | ✔ | 表 `smart_table / smart_table_field / smart_table_record`，14 字段类型 + 4 视图 |
| 多维表格完成度 | ◐ | 基础完整，公式/关联字段不全 |
| 云文档 | ✔ | 表 `sys_doc / sys_doc_space / sys_doc_version` |
| 云文档完成度 | ◐ | 版本 ✔，实时协作编辑 ✗ |
| 项目/任务 | ✔ | `pm_project / pm_task / pm_milestone / pm_task_dependency` |
| 甘特图 | ✗ | 未实现 |
| 看板视图 | ◐ | `pm_project_status_column` 有，UI 不完整 |
| 会议管理 | ◐ | `meeting_notification / meeting_minutes / meeting_file`，约 50% |

#### 2) 目标

- 多维表格：公式字段、关联字段、自动化、API 开放；
- 云文档：基于 Y.js / CRDT 的实时协作、评论、@、权限分享；
- 项目：看板 + 列表 + 甘特 三视图切换；
- 会议：预约（含会议室资源）、签到、AI 自动纪要（接 ASR + 摘要）、行动项联动待办。

#### 3) 差距与工作量

| 要做的事 | 工作量 |
|---------|-------|
| 多维表格公式引擎（DSL + 求值） | 8 人日 |
| 多维表格关联字段（跨表查询） | 5 人日 |
| 多维表格自动化（触发器 + 动作） | 5 人日 |
| 云文档实时协作（Y.js + WebSocket 同步） | 10 人日 |
| 云文档评论、@、权限分享 | 5 人日 |
| 项目看板/甘特图 UI | 6 人日 |
| 会议预约 + 会议室资源表 `meeting_room` | 3 人日 |
| 会议 AI 纪要（ASR + LLM 摘要）+ 行动项转待办 | 5 人日 |

**小计：约 47 人日**

---

### Phase 5：房产垂直样板重新上架

#### 1) 现状盘点

| 能力 | 现状 | 证据 |
|------|------|------|
| 楼盘管理 | ✔ | 表 `project`，`ProjectController` |
| 项目佣金规则 | ✔ | 表 `project_commission_rule` |
| 渠道经销（联盟商） | ✔ | 表 `alliance` |
| 佣金结算 | ✔ | 表 `commission` + 审核 |
| 看房/带看 | ✔ | `customer_visit` + `customer.visit_*` |
| 认购（小订/大定） | ✔ | `customer_deal.subscribe_type` |
| 签约/网签/备案/贷款/回款 | ✔ | `customer_deal` 全状态 |
| 通用化后的重接入 | ✗ | 需要把 V3 能力以"行业插件"形式在 V4 上架 |

#### 2) 目标

- 房产行业包（可按租户开启）：楼盘、户型、房源状态图、联盟商、佣金规则、带看 SOP、认购签约流程；
- 行业专属字段：复用闭环 ③ 的自定义字段机制 + 行业预置模板；
- 行业看板/报表：房源去化、回款健康度、渠道 ROI。

#### 3) 差距与工作量

| 要做的事 | 工作量 |
|---------|-------|
| 行业插件框架（插件元数据、启用/禁用、菜单注入） | 5 人日 |
| 房产行业包封装（V3 代码重新组织） | 5 人日 |
| 行业预置字段模板（线索/客户/成交单） | 2 人日 |
| 户型 `house_type` + 房源状态图 `unit_status` 表 + UI | 5 人日 |
| 带看 SOP：带看确认书/佣金三方单（电子版） | 4 人日 |
| 房产专属看板（去化率、客户到访漏斗、渠道 ROI） | 4 人日 |
| APP/小程序端行业插件 UI（楼盘列表、房源状态） | 4 人日 |

**小计：约 29 人日**

---

### Phase 6：生态集成与 SaaS 多租户

#### 1) 现状盘点

| 能力 | 现状 | 证据 |
|------|------|------|
| 企业微信集成 | ✗ | — |
| 钉钉集成 | ✗ | — |
| 飞书集成 | ✗ | — |
| OpenAPI 规范文档 | ◐ | RESTful 完整但无 OpenAPI 3.0 文档 |
| Webhook | ✗ | — |
| 多租户 | ◐ | `TenantLine` 支持，无租户库/订阅/计费表 |
| SaaS 计费 | ✗ | — |
| 国际化 | ✗ | — |
| 微信/支付宝支付 | 🏳️ | Feature Flag 默认关闭（见 `FeatureFlags.java`）；V3.2 WP-S6-G 做稳定性加固；SaaS 计费场景按需开启 |

#### 2) 目标

- 企业微信/钉钉/飞书三选一首发：SSO、通讯录同步、消息通道、审批同步；
- OpenAPI：Swagger/OpenAPI 3.0 标准文档 + API Key 管理 + 用量统计；
- Webhook：事件订阅中心（客户新增、成交、审批、回款）+ 重试队列；
- SaaS 多租户：租户库、套餐/版本、计费（按人/功能/容量）、账单、续费；
- 国际化：中/英，预留多时区、多货币；
- SaaS 计费场景下可能重启支付模块：此时解除 `pengcheng-pay` 冻结。

#### 3) 差距与工作量

| 要做的事 | 工作量 |
|---------|-------|
| 企业微信集成（SSO + 通讯录 + 消息 + 审批） | 10 人日 |
| 钉钉集成（同上，复用抽象层） | 6 人日 |
| 飞书集成（同上） | 6 人日 |
| OpenAPI 3.0 文档生成（springdoc）+ API Key 管理 + 限流 | 5 人日 |
| 事件总线 + Webhook 订阅中心 + 投递重试 | 8 人日 |
| 多租户彻底贯穿（数据隔离审计 + 跨租户管理端） | 10 人日 |
| SaaS 套餐/订阅/账单/续费（`plan / subscription / bill`） | 10 人日 |
| 计费计量（MAU/API 次数/存储容量） | 5 人日 |
| 国际化 i18n 框架 + 词条抽取 + 两语言版本 | 8 人日 |
| （按需）pengcheng-pay 解冻，对接 SaaS 付款 | 5 人日 |

**小计：约 73 人日**

---

### 迭代工作量总览

| Phase | 主题 | 工作量 | 预估周期（6 人团队） |
|-------|------|-------|-------------------|
| 2 | 财务闭环 | 44 人日 | 4～5 周 |
| 3 | 数据决策 | 41 人日 | 4 周 |
| 4 | 协作工具 | 47 人日 | 5 周 |
| 5 | 房产垂直 | 29 人日 | 3 周 |
| 6 | 生态与 SaaS | 73 人日 | 7～8 周 |
| 合计 | — | **234 人日** | **约 6 个月** |

---

## 六、非功能需求

| 维度 | 指标 |
|------|------|
| **性能** | 后端 P95 < 500ms；APP 启动 < 2s；消息端到端 < 1s |
| **可用性** | 生产环境 SLA ≥ 99.5%；支持灰度发布、回滚 |
| **安全** | 等保 2.0 三级对齐；敏感字段加密（复用 `pengcheng-crypto`）；操作审计 100% 覆盖；越权防护 |
| **AI 治理** | 工具调用审计、A/B 实验、自动回滚（复用 V3）；幻觉率 < 5%；成本分级路由 |
| **兼容** | iOS 13+、Android 8+、主流浏览器最近两个大版本、微信小程序基础库 2.25+ |
| **可观测** | 日志、指标、链路追踪；告警（企业微信/邮件） |
| **合规** | 个人信息保护法 / GDPR（海外）就绪；数据导出与删除接口 |

---

## 七、技术架构与改造重点

```
┌──────────────────────────────────────────────────────────┐
│  客户端：Vue3 Web 管理后台 │ uni-app APP(iOS/Android) │ 小程序 │
└──────────────────────────────────────────────────────────┘
                           │ HTTPS / WebSocket
┌──────────────────────────────────────────────────────────┐
│  Nginx / 网关（鉴权/限流/灰度）                            │
└──────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────────────────────────────────────┐
│  Spring Boot 3.3 应用（pengcheng-api）                     │
│  ├─ core（system/auth/crm/oa/ai/file/message/hr/fin/...）  │
│  ├─ infra（db/redis/oss/ws/sms/push/pay🏳️/wechat🏳️/...）   │
│  └─ job（公海回收、AI 提醒、对账…）                          │
└──────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────────────────────────────────────┐
│  MySQL 8 │ Redis 7 │ PostgreSQL 16 + PGVector │ MinIO │ kkFileView │
└──────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────────────────────────────────────┐
│  AI 基座：Spring AI + Spring AI Alibaba │ DashScope │ PGVector │ MCP │ 自研 Orchestration │
└──────────────────────────────────────────────────────────┘
```

**五个改造重点**：
1. **通用化重构**：`pengcheng-realty` 通用部分（客户/跟进/商机）抽到 `pengcheng-crm`；行业特征下沉到 `pengcheng-realty-vertical`；
2. **统一 AI Copilot 组件库**：Web/APP/小程序三套 UI + 一套后端协议；
3. **uni-app 原生 APP 打包流水线**：HBuilderX CLI + CI 脚本 + 推送/定位/OCR/扫码原生插件；
4. **数据权限与多租户**：注解 + 拦截器 + `tenant_id` 全量贯穿；
5. **审批流程引擎**：轻量串行引擎 → 迭代 Phase 2 升级为 BPMN（可选）。

---

## 八、度量、风险、开放问题

### 8.1 北极星指标（MVP 上线后 3 个月）

| 指标 | 目标 |
|------|------|
| 企业注册数 | ≥ 50 家 |
| 日活员工数（DAU） | ≥ 1,000 |
| APP 次日留存 | ≥ 60% |
| CRM 客户录入数 | ≥ 20,000 |
| AI 对话日调用量 | ≥ 5,000 次 |
| AI 问题解决率（用户点赞） | ≥ 70% |
| 核心流程崩溃率 | < 0.5% |
| 付费转化率（试用→付费） | ≥ 15% |

### 8.2 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| uni-app 打包原生 APP 性能不达预期 | 中 | 关键页（消息、IM）预留 nvue / 原生插件方案；必要时引入 Flutter 模块 |
| iOS 上架审核超时 | 中 | 企业签/TestFlight 兜底；提前 4 周开始上架 |
| AI 调用成本失控 | 高 | 分级路由：小模型兜底 + 大模型精用；用量上限 + 企业配额 |
| 通用化重构破坏 V3 已有客户 | 高 | V3 → V4 平滑升级路径；数据迁移脚本 + 灰度切换 |
| 自定义字段（EAV）性能问题 | 中 | 热字段冗余到主表；列式存储可选 |
| 微信/支付宝策略变化 | 低 | Feature Flag 默认关闭兜底；V3.2 WP-S6-G 的证书/token/重放加固完成后，解冻可控 |

### 8.3 开放问题（待评审决策）

1. **商业模式**：按人数 / 功能 / 行业版本三种计费，哪种先推？
2. **APP 分发**：iOS 立项企业开发者账号？Android 走华为/小米/OPPO/vivo 全渠道？
3. **AI 模型策略**：优先通义千问？是否引入 DeepSeek/Kimi 做比价？
4. **多租户**：MVP 是否就上多租户？（建议 MVP 保持单租户私有化，Phase 6 做 SaaS 多租户）
5. **V3 老客户**：是否有已落地的房产客户需要兼容？兼容策略？
6. **商机独立实体**：MVP 是否做？（建议放到 Phase 5 房产垂直时统一处理）

---

## 九、附录：模块改造总览

| 现有模块 | MVP 阶段 | 迭代阶段 |
|---------|---------|---------|
| pengcheng-system | 补：数据权限表达式、企业注册、成员邀请、设备管理 | — |
| pengcheng-auth | 补：企业注册向导登录路径 | — |
| pengcheng-realty | 拆：通用字段 → `pengcheng-crm`；行业字段 → `pengcheng-realty-vertical`；客户多媒体跟进；自定义字段；线索 Lead 实体 | Phase 5：房产行业插件重新上架 |
| pengcheng-ai | 补：统一 Copilot 前端组件、智能提醒规则、对话触发动作、语音输入、成本分级路由 | Phase 3：AI 洞察周/月报 |
| pengcheng-message | 补：通知统一分渠道调度（APP/MP/站内） | — |
| pengcheng-hr | — | Phase 3：OKR；Phase 2：报销与合同/提成联动 |
| pengcheng-pay | 🏳️ Feature Flag 默认关；V3.2 WP-S6-G 加固中（证书/token/重放/金额容差） | Phase 6 SaaS 计费按需开启 |
| pengcheng-wechat / social | 🏳️ Feature Flag 默认关；加固维护 | Phase 6 按需 |
| pengcheng-push | 补：原生插件接入、统一调度 | Phase 6：多租户推送配额 |
| pengcheng-uniapp | 补：APP 打包流水线、原生插件（推送/定位/OCR/扫码）、小程序对齐、离线缓存策略 | Phase 4：协作工具三端适配 |
| pengcheng-ui | 补：企业注册/邀请、数据权限、CRM 通用页面、Copilot 组件、审批配置 | Phase 3：看板编辑器、BI；Phase 4：多维表格/云文档；Phase 6：OpenAPI 管理 |
| — | 新增：`pengcheng-crm`（通用 CRM） | — |
| — | 新增：`pengcheng-oa`（聚合日报/任务/审批） | — |
| — | 新增：`pengcheng-approval-flow`（轻量流程引擎） | Phase 2 升级为 BPMN |
| — | — | 新增：`pengcheng-contract`（合同）Phase 2 |
| — | — | 新增：`pengcheng-invoice`（发票）Phase 2 |
| — | — | 新增：`pengcheng-finance`（提成规则引擎）Phase 2 |
| — | — | 新增：`pengcheng-bi`（数据分析）Phase 3 |
| — | — | 新增：`pengcheng-tenant`（多租户 + SaaS 计费）Phase 6 |
| — | — | 新增：`pengcheng-openapi`（OpenAPI + Webhook）Phase 6 |
| — | — | 新增：`pengcheng-integration`（企微/钉钉/飞书适配层）Phase 6 |

---

> **下一步行动**：  
> 1. 产品/研发/业务三方评审本 PRD（本周内）；  
> 2. 确认开放问题答案（尤其是 APP 分发策略、多租户时机）；  
> 3. 进入 MVP 技术详细设计（SD 文档），重点攻关：数据权限 + 自定义字段 + Copilot 组件 + APP 打包流水线；  
> 4. 启动 W1 立项与 6 人团队组建。
