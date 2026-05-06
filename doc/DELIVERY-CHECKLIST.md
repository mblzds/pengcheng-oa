# 交付前检查清单

本文档说明各功能模块与后端接口的对接状态，便于客户部署及员工使用前验收。

## 一、已对接真实接口的模块（可交付）

| 模块 | 前端入口 | 后端接口 | 说明 |
|------|----------|----------|------|
| **工作台** | `/` (dashboard) | `/admin/dashboard/overview`、`/calendar/today`、`/todo/list`、`/sys/notice/my`、`/sys/file/page` | 待办/会议/公告/最近文件均来自接口 |
| **会议日程** | `/meeting` | `/calendar/month`、`/calendar/event` (POST) | 会议列表与预约会议持久化到日历事件 |
| **销售日历** | `/realty/calendar` | `/calendar/merged`、`/calendar/month`、`/calendar/event` | 月/周/日视图与事件 CRUD |
| **客户管理** | `/realty/customer` | `/admin/customer/*` (page/create/visit/deal/project/search/alliance/search 等) | 报备、到访、成交、公海池 |
| **项目楼盘** | `/realty/project` | `/admin/project/*` (page/detail/create/update/commission-rule) | 项目与佣金规则 |
| **联盟商** | `/realty/alliance` | `/admin/alliance/*` (page/detail/create/update/enable/disable/stats) | 联盟商与运营统计 |
| **成交佣金** | `/realty/commission` | `/admin/commission/*` (page/create/audit/changelog) | 佣金录入与审核 |
| **付款审批** | `/realty/payment` | `/admin/payment/*` (page/create/approve/approvals) | 付款申请与审批流 |
| **数据统计** | `/realty/stats` | `/admin/dashboard/overview`、`/funnel`、`/ranking` | 概览、漏斗、排行榜 |
| **经营分析** | `/realty/analysis` | `/admin/dashboard/overview` | 顶部卡片来自仪表盘接口 |
| **考勤打卡** | `/realty/attendance` | `/admin/attendance/*` (records/monthly/leave/list/compensate/list) | 考勤记录与请假/调休（使用 `attendanceApi`） |
| **销售拜访** | `/realty/visit` | `/realty/visit/*` (list/detail/create/update/stats/ranking 等) | 拜访记录与统计 |
| **待办** | 工作台 + `/todo` | `/todo/list`、`/todo/create`、`/todo/complete`、`/todo/update`、`/todo/cancel`、`/todo/extract` | 待办列表、创建、完成、提取（含 LLM） |
| **智能表格** | `/smart-table` | 用户选项来自 `/sys/user/page` | 负责人等选项为真实用户列表 |
| **AI 知识库** | `/ai/knowledge` | `/admin/ai/knowledge/docs`、`/upload`、`/docs/{id}` (DELETE) | 文档列表、上传、删除 |
| **AI 对话** | `/ai/chat` | `/admin/ai/chat` (POST) | 同步对话，会话前端缓存 |
| **系统文件** | `/system/file`、工作台最近文件 | `/sys/file/page`、`/sys/file/upload` 等 | 文件列表与上传 |

## 二、部署与使用前需确认项

1. **环境与权限**
   - 后端 API 基地址：前端通过 `/api` 代理到实际后端（见 `vite.config` / nginx 配置）。
   - 登录态：请求头携带 `Authorization`，确保登录后 token 有效。
   - 菜单与权限：`workspace:meeting:list`、`sys:file:list` 等权限与角色配置正确，避免接口 403。

2. **数据初始化**
   - 若为新环境，需执行数据库迁移（如 Flyway `db/migration` 下脚本），并确认 `sys_calendar_event`、`realty_*` 等表已创建。
   - 工作台「今日会议」与「会议日程」页依赖日历事件（`event_type=meeting`），无数据时为空状态属正常。

3. **可选/后续扩展**
   - **客户成交概率**：`realtyApi.customerDealProbability` 当前返回 0，若后端有单独接口可再对接。
   - **智能表格任务数据**：表格内任务列表仍为前端示例数据，若需持久化可对接 `/smart-table/tables/{id}/records` 等接口。

## 三、前端假数据已移除的说明

- **会议**：原会议页与工作台今日会议为前端写死数据，已改为 `/calendar/*` 接口。
- **工作台**：今日会议、会议数、公告、最近文件均改为接口拉取；待办与业务概览此前已接接口。
- **房产**：`api/realty.ts` 已对接客户/项目/联盟/佣金/付款/仪表盘、考勤记录与请假调休列表、AI 知识库（`/admin/ai/knowledge`）、AI 对话（`/admin/ai/chat`）。
- **数据分析**：经营分析页顶部卡片改为使用 `realtyApi.dashboardOverview()`，与仪表盘数据一致。
- **待办页**：统一使用 `request()`，响应正确解包；列表/创建/完成/开始/取消/删除/提取均走 `/todo/*` 接口。
- **智能表格**：用户选项由 `userApi.page` 加载，不再使用模拟用户列表。
- **AI 聊天**：发送消息调用 `realtyApi.aiChat` 对接 `/admin/ai/chat`，会话在前端按会话 id 缓存。

按上述清单核对后，即可进行客户部署与员工使用验收。

---

## 四、下一步建议（交付后）

1. **部署与联调**
   - 将前端构建产物（`pengcheng-starter/src/main/resources/static/`）随后端一起部署，或单独用 Nginx 托管并配置 `/api` 反向代理。
   - 在测试环境完整走一遍：登录 → 工作台 → 会议/待办/公告/文件 → 客户/项目/联盟/佣金/付款 → 考勤 → AI 知识库与对话 → 待办提取。

2. **可选功能增强**
   - **客户成交概率**：若业务需要，后端提供接口后在前端 `realtyApi.customerDealProbability` 中对接。
   - **智能表格任务持久化**：当前「项目任务进度表」为前端示例数据；若需持久化，可新建一张智能表格（或使用模板），将任务列表改为调用 `smartTableApi.listRecords(tableId)` / `addRecord` / `updateRecord`，并做字段映射。

3. **体验与性能**
   - 大 chunk 提示（如 500KB+）：可考虑按路由或模块做 `import()` 动态拆分，降低首屏体积。
   - Sass 弃用告警：后续可升级为 `sass` 的现代 API，消除 `legacy-js-api` 警告。
