# 鹏程房产销售系统 — 后续开发计划 V3.1

**起算日期**：2026-04-21
**基线**：综合真实完成度 62%（见《项目开发情况分析报告》代码级核查）
**目标**：把商用阻断项清零、管理功能补齐、AI 基础设施真实化、质量基线建立

---

## 一、迭代路线图

| 迭代 | 周期 | 目标 | 出口标准 |
|-----|------|------|---------|
| **Sprint 0** | 准备周 | 环境、分支、CI 门禁骨架 | feature 分支可 PR、CI 绿 |
| **Sprint 1 (P0)** | 2 周 | 支付回调、回款模块、种子数据 | 支付三端贯通、回款 UI 可用、开箱可跑 |
| **Sprint 2 (P1)** | 2 周 | 360 评估、自动化规则、权限收口 | 360 评估闭环、规则引擎跑 4 条、业务 Controller 全部鉴权 |
| **Sprint 3 (P2)** | 3 周 | Spring AI 升版、PGVector、Function Calling、经营洞察 | AI RAG 端到端、AI 升版通过 E2E |
| **Sprint 4 (P3)** | 2 周 | 测试基线、云文档版本、可观测性 | 行覆盖率 ≥ 45%、E2E ≥ 5 条、Grafana 上线 |

总工期：**10 周**（~2.5 个月）

---

## 二、任务拆解与进度跟踪表

图例：⬜ 未开始 / 🟦 进行中 / ✅ 已完成 / ⚠️ 阻塞 / ⏸ 暂停

### Sprint 1 — P0（商业闭环）

| # | 任务 | 负责域 | 预估 | 状态 | 产出 | 备注 |
|---|------|--------|------|------|------|------|
| P0-1.1 | 在 `PaymentService` 补 `updatePayStatus(orderNo, tradeNo, channel, amount)` + 重载含 notifyId | pengcheng-realty/payment | 0.5d | ✅ | [PaymentService.java](../pengcheng-core/pengcheng-realty/src/main/java/com/pengcheng/realty/payment/service/PaymentService.java) 金额/状态/幂等校验 |  |
| P0-1.2 | 扩展 `payment_request`（order_no/pay_channel/...）+ 建 `pay_notify_log` 表 | sql | 0.5d | ✅ | [V37__pay_gateway.sql](../pengcheng-starter/src/main/resources/db/migration/V37__pay_gateway.sql) |  |
| P0-1.3 | 支付宝回调 Controller 从 temp-disabled 迁回并适配新签名 | pengcheng-admin-api | 1d | ✅ | [pay/AlipayNotifyController.java](../pengcheng-api/pengcheng-admin-api/src/main/java/com/pengcheng/admin/controller/pay/AlipayNotifyController.java) | 验签、幂等、失败重试 |
| P0-1.4 | 微信支付回调 Controller 同上 | pengcheng-admin-api | 1d | ✅ | [pay/WechatPayNotifyController.java](../pengcheng-api/pengcheng-admin-api/src/main/java/com/pengcheng/admin/controller/pay/WechatPayNotifyController.java) |  |
| P0-1.5 | 微信公众号登录回调 Controller 迁回（移除已废弃的 unionId 字段） | pengcheng-admin-api | 0.5d | ✅ | [auth/WechatMpAuthController.java](../pengcheng-api/pengcheng-admin-api/src/main/java/com/pengcheng/admin/controller/auth/WechatMpAuthController.java) |  |
| P0-1.6 | 支付下单接口补全（当前 `AppPaymentController` 仅 CRUD） | pengcheng-app-api | 1.5d | ✅ | [AppPayController.java](../pengcheng-api/pengcheng-app-api/src/main/java/com/pengcheng/app/controller/AppPayController.java) | `/app/pay/create` 复用 `payment_request.order_no` 与 `relatedDealId` |
| P0-1.7 | 单元测试：订单缺失/重复支付/未审批/金额不一致/正常/幂等兜底/订单号格式 | test | 1d | ✅ | 7/7 全部通过 | `PaymentServiceUpdatePayStatusTest` |
| P0-1.8 | 升级 Lombok 1.18.36 → 1.18.38（解决 JDK 17.0.18 兼容 bug） | pom | - | ✅ | 根 [pom.xml](../pom.xml):32 | 附带修复 |
| P0-2.1 | 新建 DB 表 `receivable_plan` / `receivable_record` / `receivable_alert` | sql | 1d | ✅ | [V38__receivable.sql](../pengcheng-starter/src/main/resources/db/migration/V38__receivable.sql) | 与 customer_deal 1:N |
| P0-2.2 | Entity + Mapper（3 个实体 3 个 Mapper） | pengcheng-realty | 1d | ✅ | [realty/receivable/**](../pengcheng-core/pengcheng-realty/src/main/java/com/pengcheng/realty/receivable/) | |
| P0-2.3 | `ReceivableService`：createPlan / registerRecord / resolveStatus / runOverdueCheck / stats | pengcheng-realty | 2d | ✅ | [ReceivableService.java](../pengcheng-core/pengcheng-realty/src/main/java/com/pengcheng/realty/receivable/service/ReceivableService.java) | 含状态机 + 告警去重 |
| P0-2.4 | `ReceivableController`：7 个 REST 端点 | pengcheng-admin-api | 1d | ✅ | [ReceivableController.java](../pengcheng-api/pengcheng-admin-api/src/main/java/com/pengcheng/admin/controller/realty/ReceivableController.java) | |
| P0-2.5 | `AutomationScheduler` 加 `@Scheduled(cron="0 30 8 * * ?")` 回款逾期扫描 | pengcheng-admin-api/schedule | 0.5d | ✅ | [AutomationScheduler.java](../pengcheng-api/pengcheng-admin-api/src/main/java/com/pengcheng/admin/schedule/AutomationScheduler.java) | 每天 08:30 |
| P0-2.6 | 前端 `views/realty/receivable/`：计划列表 / 回款登记 / 逾期看板 | pengcheng-ui | 3d | ✅ | [views/realty/receivable/**](../pengcheng-ui/src/views/realty/receivable/) | 路由 `/realty/receivable` 已接入 |
| P0-2.7 | 前端 API 层 `api/realty/receivable.ts` | pengcheng-ui | 0.5d | ✅ | [api/receivable.ts](../pengcheng-ui/src/api/receivable.ts) | 受现有 `api/realty.ts` 文件占位影响，落地为独立 API 文件 |
| P0-2.8 | 单元测试：逾期判定、分期对账、告警去重 | test | 1d | ✅ | `ReceivableServiceTest` 11/11 全绿 | |
| P0-3.1 | 种子数据脚本：联盟商×3 / 项目×3 / 佣金规则×3 / 客户×4 / 到访×3 / 成交×1 / 回款计划×3 / 回款流水×1 | sql | 1d | ✅ | [V39__seed_demo.sql](../pengcheng-starter/src/main/resources/db/migration/V39__seed_demo.sql) | 全部 INSERT IGNORE，可重复执行 |
| P0-3.2 | `README.md` 追加"开箱即用 Demo 数据"章节 | doc | 0.5d | ✅ | [README.md](../README.md):370 | 展示 P0-2 回款演示路径 |

**Sprint 1 小结**：工作量 ≈ 17.5 人日

### Sprint 2 — P1（管理侧硬伤）

| # | 任务 | 负责域 | 预估 | 状态 | 备注 |
|---|------|--------|------|------|------|
| P1-1.1 | `KpiReview360Service:213` 生成上/同/下级评估任务（四向全覆盖+去重） | pengcheng-hr | 2d | ✅ | [KpiReview360Service.java](../pengcheng-core/pengcheng-hr/src/main/java/com/pengcheng/hr/performance/service/KpiReview360Service.java) `createReviewTasks` + `upsertTask` |
| P1-1.2 | 评估关系表复用 V33 已有 `kpi_review_relation` / `kpi_peer_review` → 新增 Entity + Mapper | pengcheng-hr | 0.5d | ✅ | [KpiReviewRelation.java](../pengcheng-core/pengcheng-hr/src/main/java/com/pengcheng/hr/performance/entity/KpiReviewRelation.java) / [KpiPeerReview.java](../pengcheng-core/pengcheng-hr/src/main/java/com/pengcheng/hr/performance/entity/KpiPeerReview.java) |
| P1-1.3 | `KpiReview360Service:245` 权重配置落 `sys_config_group`（含合计=1 校验） | pengcheng-hr | 1d | ✅ | `getWeightConfig` / `updateWeightConfig` / `parseWeightConfigJson` |
| P1-1.4 | `KpiReview360Service:286` 换 `StpUtil.getLoginIdAsLong()`（未登录抛 IllegalStateException） | pengcheng-hr | 0.5d | ✅ | `getCurrentUserId` 安全上下文读取 |
| P1-1.5 | 前端 `Review360.vue` 对接任务分发、打分、汇总 | pengcheng-ui | 2d | ✅ | [Review360.vue](../pengcheng-ui/src/views/hr/Review360.vue) 任务分发/分页待办/结果汇总全部接通 |
| P1-1.6 | 单元测试：四向任务生成 / 权重 JSON 读写 / 合计校验 / getCurrentUserId 未登录 | test | 1d | ✅ | `KpiReview360ServiceTest` 8/8 全绿 |
| P1-2.1 | 抽象 `RuleActionHandler` SPI（Spring 收集所有实现） | automation/handler | 2d | ✅ | [RuleActionHandler.java](../pengcheng-core/pengcheng-system/src/main/java/com/pengcheng/system/automation/handler/RuleActionHandler.java) |
| P1-2.2 | 4 个动作 Handler：notify（真实调 ChannelPushService）/ update_status（白名单校验防注入）/ assign / create_task | automation/handler | 3d | ✅ | NotifyActionHandler + UpdateStatusActionHandler + AssignActionHandler + CreateTaskActionHandler |
| P1-2.3 | DB 表已存在（V35），预置 2 条种子规则（超 7 天未跟进 / 合同 30 天到期） | sql | 0.5d | ✅ | [V39__seed_demo.sql](../pengcheng-starter/src/main/resources/db/migration/V39__seed_demo.sql) 追加 sys_automation_rule 2 条 |
| P1-2.4 | 前端 `AutomationRule.vue` 7 处 TODO 全部对接引擎 API | pengcheng-ui | 2d | ✅ | 7/7 修复：listRules/getLogs/editRule/toggleRule/copyRule/submitRule/useTemplate |
| P1-2.5 | 单元测试 8 条（notify 渲染/异常容错 + update_status 白名单/注入防护/缺参 + assign + create_task 成功/失败） | test | 1d | ✅ | `AutomationEngineTest` 8/8 全绿 |
| P1-2.6 | `AutomationService.executeAction` 重构为 SPI 分发表 | automation/service | - | ✅ | 附带：构造器注入 List<RuleActionHandler> |
| P1-2.7 | 新建前端 API 层 `api/automation.ts` | pengcheng-ui | - | ✅ | 7 个端点封装 + 完整 TypeScript 类型 |
| P1-3.1 | 核心业务 Controller 补 `@SaCheckPermission`：realty 5 + hr 3 + ai 1 + automation 1 = 10 Controller | pengcheng-api/** | 2d | ✅ | 覆盖全部写操作，40+ 端点注入权限码 |
| P1-3.2 | 佣金自动结算定时任务 + 审批流前端 | commission | 2d | ✅ | 月末 `@Scheduled` + 复用现有 [CommissionManage.vue](../pengcheng-ui/src/views/realty/commission/CommissionManage.vue) |

**Sprint 2 小结**：工作量 ≈ 19.5 人日

### Sprint 3 — P2（AI 升版与真实落地）

| # | 任务 | 负责域 | 预估 | 状态 | 备注 |
|---|------|--------|------|------|------|
| P2-0.1 | 依赖兼容矩阵核查（Sa-Token/MyBatis-Plus/Flyway/Hutool/Spring AI） | pom | 0.5d | ✅ | 所有关键依赖均已适配 Spring Boot 3.3 |
| P2-0.2 | `pom.xml` 升 Spring Boot 3.2.2 → 3.3.5（LTS 兼容路径） | pom | 0.5d | ✅ | 根 [pom.xml](../pom.xml):28 |
| P2-0.3 | 全量 `clean compile` + 4 模块单测回归 | CI | 1d | ✅ | BUILD SUCCESS 14s / 34/34 测试零回归 |
| P2-1.1 | PGVector starter 启用 — artifact 升级为 1.0.0 GA 新规范 `spring-ai-starter-vector-store-pgvector` | pengcheng-ai | 0.5d | ✅ | [pom.xml](../pengcheng-core/pengcheng-ai/pom.xml):23 + dev 配置 initialize-schema=false 避免无运行时副作用 |
| P2-0.4 | `pom.xml` 升 Spring AI 1.0.0 → 1.1.2、Alibaba 1.0.0.2 → 1.1.2.2 | pom | 0.5d | ⏸ 延后 | **P4 迭代**：当前 Function Calling / 多 Agent 高级特性收益有限，升级 2~3 人日适配成本不划算 |
| P2-0.5 | 处理 Breaking Changes：ChatMemory 包名/配置、Advisor 占位符、TOP_K 常量 | pengcheng-ai | 2d | ⏸ 延后 | 同上 |
| P2-0.6 | 验证 Jackson 冲突（`issue #4486`）解法 | pom | 0.5d | ⏸ 延后 | 同上 |
| P2-1.2 | `KnowledgeBaseService` 改走 `VectorStore.similaritySearch` | pengcheng-ai | 2d | ⏸ | 等真实启用 PGVector 环境后做 |
| P2-1.3 | `MemoryServiceImpl` FULLTEXT 降级改回 VectorStore | pengcheng-ai | 1d | ⏸ | 同上 |
| P2-1.4 | 集成测试：上传文档 → 向量化 → 问答引用 | test | 1d | ⏸ | Testcontainers pgvector |
| P2-2.1 | 现有房源/客户/佣金查询包装成 `@Tool` | pengcheng-ai | 2d | ⏸ | 依赖 Spring AI 1.1.x 的 ToolCallback API |
| P2-2.2 | `SkillEnableRegistry` 挂 Function Calling 到 AiChat | pengcheng-ai | 1d | ⏸ | 同上 |
| P2-2.3 | 端到端："本月签几单"触发工具调用 DB | test | 1d | ⏸ | 同上 |
| P2-3.1 | 经营洞察：LLM 生成文字推入工作台 | pengcheng-core/dashboard | 1.5d | ⏸ | 独立任务，可不依赖升版；拆到 P4 |
| P2-3.2 | 云文档版本管理：`doc_version` 表 + diff + rollback | pengcheng-core/doc | 2d | ⏸ | 独立任务，拆到 P4 |
| P2-3.3 | 智能表格 JSON 导入导出端到端 | pengcheng-core | 1d | ⏸ | 独立任务，拆到 P4 |

**Sprint 3 小结**：工作量 ≈ 18 人日

### Sprint 4 — P3（质量与可运维）

| # | 任务 | 负责域 | 预估 | 状态 | 备注 |
|---|------|--------|------|------|------|
| P3-1.1 | JaCoCo 配置 + CI 门禁（行覆盖率 45%） | pom / CI | 0.5d | ✅ | 首批对 `pengcheng-realty` / `pengcheng-hr` 模块落地 45% 行覆盖率门禁，CI 切换到 `mvn verify` |
| P3-1.2 | 补 `CommissionCalculator` 单测（分账/跳点/奖励） | test | 1d | ✅ | `CommissionCalculator` 行覆盖率 98.82%（84/85） |
| P3-1.3 | 补 `CustomerService` 单测（保护期/状态机/公海） | test | 1.5d | ✅ | `CustomerService` / `CustomerPoolService` / `CustomerDealService` / `CustomerVisitService` 已补服务层单测 |
| P3-1.4 | 补 `DataPermissionInterceptor` SQL 改写单测 | test | 1d | ✅ | `pengcheng-db` 已补 `DataPermissionInterceptorTest` 覆盖房产业务/通用数据权限过滤规则 |
| P3-1.5 | 补 `AttendanceServiceImpl` 单测 | test | 1d | ✅ | `AttendanceServiceImplTest` 已覆盖打卡/请假/签到/月度汇总核心分支 |
| P3-2.1 | Testcontainers 集成测试：支付回调幂等 | test | 1d | ✅ | 新增 `PaymentCallbackIdempotencyTest`；默认走 Testcontainers，本机支持 `-Dit.mysql.*` 外部库回退，已在临时 schema 实跑通过 |
| P3-2.2 | WebSocket 端到端集成测试 | test | 1d | ✅ | 新增 `MessageWebSocketHandlerIntegrationTest`，在最小 WebSocket 应用中验证 `/ws/message` 的 connected / ping-pong / 私聊投递链路 |
| P3-2.3 | REST Assured 契约测试 30 端点 | test | 2d | ⬜ | |
| P3-3.1 | Playwright E2E 初始化 + 5 条核心旅程 | e2e | 3d | ⬜ | |
| P3-4.1 | Prometheus Micrometer + Grafana 仪表盘 | infra | 2d | ⬜ | |
| P3-4.2 | 日志集中（ELK 或 Loki） | infra | 1d | ⬜ | |
| P3-4.3 | 性能压测：客户列表/消息推送/AI 对话 | perf | 1.5d | ⬜ | |

**Sprint 4 小结**：工作量 ≈ 17.5 人日

---

## 三、可砍清单（MVP 非必需）

若工期紧张，以下可推迟或取消，不影响上线：

- 会议日历模块（`MeetingCalendar.vue`）— 7 处 TODO，业务优先级低
- 云文档版本管理 — 先上最小可用编辑版
- MCP 工具扩展 — 现有 Function Calling 可替代
- 智能表格甘特视图完全渲染 — 保留看板+表格即可

---

## 四、Spring AI Alibaba 升版兼容性报告

**目标版本**：`spring-ai-alibaba 1.1.2.2` + `spring-ai 1.1.2`
**前置条件**：Spring Boot 3.2.2 → 3.3.5
**风险清单**：

| 风险 | 影响范围 | 缓解 |
|-----|---------|------|
| ChatMemory artifact 改名 (`spring-ai-model-chat-memory-*` → `*-repository-*`) | pengcheng-ai 记忆模块 | 改 pom + 重构 `MemoryAdvisor` import |
| 配置 key 改名 (`spring.ai.chat.memory.jdbc` → `spring.ai.chat.memory.repository.jdbc`) | application.yml | 同步调整 |
| Advisor 模板占位符改为 `${query}` / `${question_answer_context}` | RAG 代码 | 重写 prompt 模板 |
| `CHAT_MEMORY_RETRIEVE_SIZE_KEY` → `TOP_K` | `KnowledgeBaseService` | 按常量替换 |
| TTS API 全量重命名 | **本项目未使用** | 无影响 |
| Jackson 冲突 [#4486](https://github.com/alibaba/spring-ai-alibaba/issues/4486) | 全局 | pom 强制 jackson-bom 版本 |
| Spring Boot 3.3 的 RestClient 变更 | `ChannelPushService` | 回归测试 |

**缓解总工期**：≈ 3 人日（含回归）。**不升版 AI 也可用现版完成 P0/P1**，升版是 AI 能力增强而非阻断项。

---

## 五、验收标准（每个 Sprint 出口）

### Sprint 1 DoD
- [ ] 模拟支付宝异步通知，订单状态更新成功；重复 notify_id 不重复处理
- [ ] 新建回款计划 → 到账登记 → 逾期判定全链路
- [ ] `git clone && docker compose up` 后首次登录能看到样例数据

### Sprint 2 DoD
- [ ] 启动 360 评估周期，自动为某员工生成 N 个评估任务（上/同/下级）
- [ ] 创建一条"超 7 天未跟进自动提醒"规则，调度器跑出推送
- [ ] 未登录或越权访问业务 Controller 返回 401/403

### Sprint 3 DoD
- [ ] `./mvnw clean verify` 全绿，版本为 `1.1.2.2`
- [ ] 上传 PDF → 对话引用 → 答案含文档内容
- [ ] "本月签几单"触发 Function Calling 真查 DB

### Sprint 4 DoD
- [ ] `mvn verify` 行覆盖率 ≥ 45%
- [ ] Playwright 5 条 E2E 绿
- [ ] Grafana 能看到接口 QPS / 延迟 / JVM 指标

---

## 六、进度汇总（每次任务完成后回填）

| 日期 | 任务编号 | 状态 | 变更摘要 | 提交 |
|-----|---------|------|---------|------|
| 2026-04-21 | 计划制定 | ✅ | 创建 DEV-PLAN-V3.1.md | — |
| 2026-04-21 | P0-1.2 | ✅ | V37__pay_gateway.sql 迁移（payment_request +5 列 + pay_notify_log 表） | — |
| 2026-04-21 | P0-1.1 | ✅ | PaymentService.updatePayStatus + 幂等日志 + 5 种分支校验 | — |
| 2026-04-21 | P0-1.7 | ✅ | PaymentServiceUpdatePayStatusTest 7 测试全绿 | — |
| 2026-04-21 | P0-1.3 / 1.4 / 1.5 | ✅ | 3 Controller 迁回正式包，修复 Result.fail/unionId 漂移，BUILD SUCCESS | — |
| 2026-04-21 | P0-1.6 | ✅ | 新增 `/app/pay/create`：基于已审批付款申请发起微信小程序预下单，复用 `payment_request.order_no`，返回 `timeStamp/nonceStr/package/paySign` 等支付参数 | — |
| 2026-04-21 | P0-1.8 | ✅ | Lombok 1.18.36 → 1.18.38（JDK 17.0.18 兼容修复） | pom.xml |
| 2026-04-21 | P0-2.1 | ✅ | V38__receivable.sql（3 表：plan/record/alert） | — |
| 2026-04-21 | P0-2.2 | ✅ | 3 Entity + 3 Mapper + 3 DTO + 1 VO 落位 | — |
| 2026-04-21 | P0-2.3 | ✅ | ReceivableService 5 核心方法完成（创建/登记/状态机/巡检/统计） | — |
| 2026-04-21 | P0-2.4 | ✅ | ReceivableController 7 端点：plan/record/alert/stats/overdue-check | — |
| 2026-04-21 | P0-2.5 | ✅ | AutomationScheduler 新增 `0 30 8 * * ?` 每日回款巡检 | — |
| 2026-04-21 | P0-2.8 | ✅ | ReceivableServiceTest 11/11 全绿 | — |
| 2026-04-21 | 构建 | ✅ | pengcheng-admin-api BUILD SUCCESS（含回款模块） | — |
| 2026-04-21 | P0-3.1 | ✅ | V39__seed_demo.sql 种子数据：联盟商/项目/客户/成交/回款计划全链路 | — |
| 2026-04-21 | P0-3.2 | ✅ | README 追加"开箱即用 Demo 数据"章节 | — |
| 2026-04-21 | **Sprint 1 P0 全部完成** | ✅ | P0-1 / P0-2 / P0-3 三大阻断项清零 | — |
| 2026-04-21 | P1-1.1 | ✅ | createReviewTasks 四向任务生成（自/上/同/下）+ upsertTask 去重 | — |
| 2026-04-21 | P1-1.2 | ✅ | KpiReviewRelation / KpiPeerReview 实体+Mapper 落位 | — |
| 2026-04-21 | P1-1.3 | ✅ | 权重配置持久化至 sys_config_group（JSON + 合计 1.0 校验） | — |
| 2026-04-21 | P1-1.4 | ✅ | getCurrentUserId 改走 StpUtil，去掉硬编码 1L | — |
| 2026-04-21 | P1-1.6 | ✅ | KpiReview360ServiceTest 8/8 全绿 | — |
| 2026-04-21 | P1-2.1/2.6 | ✅ | RuleActionHandler SPI + AutomationService 构造器注入分发表 | — |
| 2026-04-21 | P1-2.2 | ✅ | 4 动作 Handler 落地（含 SQL 注入防护白名单） | — |
| 2026-04-21 | P1-2.3 | ✅ | V39__seed_demo.sql 追加 2 条预置规则 | — |
| 2026-04-21 | P1-2.4/2.7 | ✅ | api/automation.ts + AutomationRule.vue 7 处 TODO 全部对接 | — |
| 2026-04-21 | P1-2.5 | ✅ | AutomationEngineTest 8/8 全绿 | — |
| 2026-04-21 | P1-3 | ✅ | 10 Controller 补 @SaCheckPermission（realty/hr/ai/automation），权限码规范 `{模块}:{资源}:{操作}`，admin-api BUILD SUCCESS，34/34 测试无回归 | — |
| 2026-04-22 | P1-3.2 | ✅ | 月末自动结算任务：扫描全款到账/按揭放款成交，按项目佣金规则自动生成待审核佣金单；审批继续复用 `CommissionManage.vue` | — |
| 2026-04-22 | P3-1.1 | ✅ | 根 `pom.xml` 接入 JaCoCo，CI 切换到 `mvn clean verify` 并上传覆盖率报告；`pengcheng-realty` / `pengcheng-hr` 模块本地 `verify` 已通过 45% 行覆盖率门禁 | — |
| 2026-04-21 | P2-0.1 | ✅ | 依赖兼容矩阵核查：Sa-Token/MyBatis-Plus/Flyway 均已适配 Spring Boot 3.3 | — |
| 2026-04-21 | P2-0.2 | ✅ | Spring Boot 3.2.2 → 3.3.5 升版（Micrometer 1.13.6 / Hibernate Validator 8.0.1） | — |
| 2026-04-21 | P2-0.3 | ✅ | 全量 clean compile 14s + 34/34 测试零回归 | — |
| 2026-04-21 | P2-1.1 | ✅ | PGVector starter `spring-ai-starter-vector-store-pgvector:1.0.0` 启用 + dev initialize-schema=false 默认关闭 | — |
| 2026-04-21 | P2 小结 | 🎯 | Spring AI 1.1.x 全链路升版（P2-0.4~0.6/1.2~1.4/2/3）**延后到 P4 专项迭代**：适配成本 2~3 人日，当前无紧迫收益；Sprint 3 实际完成 Boot 3.3 + PGVector 准备 | — |
| 2026-04-21 | P0-2.6 / P0-2.7 | ✅ | 回款前端闭环：新增 `api/receivable.ts`、`views/realty/receivable/` 三子视图、路由 `/realty/receivable`、计划创建/到账登记/逾期看板全部接通 | — |
| 2026-04-21 | P1-1.5 | ✅ | `Review360.vue` 对接任务分发、分页待办、真实用户/周期展示、结果汇总；同步修复 `/review/360/pending` 未读取当前登录人导致待办为空的问题 | — |
| 2026-04-21 | 验证 | ✅ | `pengcheng-ui` 构建通过；`pengcheng-admin-api` compile 通过 | — |
| 2026-04-21 | 验证 | ⚠️ | `vue-tsc --noEmit` 仍被仓库既有 `RequestConfig/axios` 类型基线问题阻塞 | — |
| 2026-04-22 | P3-1.2 | ✅ | 新增 `CommissionCalculatorTest`，`CommissionCalculator` 行覆盖率提升到 98.82%（84/85） | — |
| 2026-04-22 | P3-1.3 | ✅ | 新增 `CustomerServiceTest` / `CustomerPoolServiceTest` / `CustomerDealServiceTest` / `CustomerVisitServiceTest` / `ProjectServiceTest` 等服务层单测 | — |
| 2026-04-22 | P3-1.4 | ✅ | 新增 `DataPermissionInterceptorTest`，验证驻场/联盟商负责人/总监/本部门/仅本人等数据权限 SQL 过滤规则 | — |
| 2026-04-22 | P3-1.5 | ✅ | 新增 `AttendanceServiceImplTest`，覆盖打卡状态计算、请假校验、签到创建、月度汇总统计 | — |
| 2026-04-22 | P3-2.1 | ✅ | 新增 `PaymentCallbackIdempotencyTest`，默认走 Testcontainers；针对本机 Docker Desktop 握手异常补了 `-Dit.mysql.*` 外部库回退，并在临时 `pengcheng_it_payment` schema 实跑验证重复 `notifyId` 只落一条审计日志、订单维持已付款 | — |
| 2026-04-22 | P3-2.2 | ✅ | 新增 `MessageWebSocketHandlerIntegrationTest`，在最小 WebSocket 测试应用中验证 `/ws/message` 握手建立、`connected` 返回、`ping/pong` 和 A→B 私聊投递；同时剥离 DashScope / DataSource / PGVector 等无关自动配置噪音 | — |
| 2026-04-22 | 验证 | ✅ | `pengcheng-realty` / `pengcheng-hr` 模块 `mvn verify` 已通过 45% 行覆盖率门禁；CI 覆盖率产物上传包含 `realty` / `hr`；Mockito 测试基线已切换为 subclass mock maker 并恢复 HR/Realty/AI 本地单测 | — |

> 开发者每完成一个任务，在本表末尾追加一行；同时把上表对应 # 的"状态"列从 ⬜ 改为 ✅。
