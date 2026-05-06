# MasterLife V3.2 开发计划（基于当前真实状态校准）

> 基线校准日期：2026-04-22
> 说明：本计划以 Claude 提议的 V3.2 清单为母版，但已按当前代码状态回填。V3.1 中已经完成的质量基线、集成测试与业务闭环项不再重复视为 `⬜`。
> 图例：`⬜` 未开始 / `🟦` 部分完成 / `✅` 已完成 / `⏸` 延后 / `🔒` 保留不删

---

## 一、继承完成项（来自 V3.1，不再重复排期）

| 继承项 | 当前状态 | 说明 |
|---|---|---|
| P3-1.1 JaCoCo + CI 45% 行覆盖率门禁 | ✅ | `pengcheng-realty` / `pengcheng-hr` 已接入并本地 `verify` 通过 |
| P3-1.2 `CommissionCalculator` 单测 | ✅ | 行覆盖率 98.82% |
| P3-1.3 `CustomerService` 相关服务层单测 | ✅ | `CustomerService` / `CustomerPoolService` / `CustomerDealService` / `CustomerVisitService` / `ProjectService` 已补 |
| P3-1.4 `DataPermissionInterceptor` SQL 改写单测 | ✅ | 已补 `DataPermissionInterceptorTest` |
| P3-1.5 `AttendanceServiceImpl` 单测 | ✅ | 已补 `AttendanceServiceImplTest` |
| P3-2.1 支付回调幂等集成测试 | ✅ | `PaymentCallbackIdempotencyTest` 已实跑通过；默认 Testcontainers，本机支持 `-Dit.mysql.*` 回退 |
| P3-2.2 WebSocket 端到端集成测试 | ✅ | `MessageWebSocketHandlerIntegrationTest` 已验证 `/ws/message` 的 connected / ping-pong / 私聊投递 |
| P1-3.2 佣金自动结算主干 | ✅ | 月末自动结算任务已在现有佣金链路落地 |
| P0-1.6 小程序支付下单 | ✅ | `/app/pay/create` 已落地，复用 `payment_request.order_no` |

---

## 二、Sprint 5（第 1–2 周）— 防御层 + 业务收尾

### WP-S5-A｜参数校验 + 全局异常处理（后端 Lead，5d）

| # | 任务 | 状态 | 备注 |
|---|------|------|------|
| A-1 | 梳理 DTO，补 `@NotNull / @NotBlank / @Size / @Pattern / @Min` | ⬜ | 现有校验注解覆盖不完整 |
| A-2 | 所有业务 Controller 入参加 `@Valid` / `@Validated` | ⬜ | 需要系统性补齐 |
| A-3 | 统一 `GlobalExceptionHandler` + 错误码收口 | 🟦 | `GlobalExceptionHandler` 已存在，但仍缺错误码体系与分层治理 |
| A-4 | 错误码字典 `error_code.md` + `BizErrorCode` | ⬜ | 尚未建立统一字典 |
| A-5 | 处理 `MethodArgumentNotValidException / BindException / ConstraintViolationException` | 🟦 | 前两者已处理，`ConstraintViolationException` 未补 |
| A-6 | 单测：非法入参/业务异常/未知异常 | ⬜ | 需要新增 |
| A-7 | 前端 `request.ts` 统一按错误码提示 | 🟦 | 现有拦截器已统一报错，但未按错误码字典收敛 |

### WP-S5-B｜佣金自动结算闭环（后端 #2，6d）

| # | 任务 | 状态 | 备注 |
|---|------|------|------|
| B-1 | `CommissionCalculator` 升级为 五层计算链 | 🟦 | 当前已有计算器和高覆盖测试，但未到“五层计算”目标 |
| B-2 | 月末自动结算 + 幂等重跑 | 🟦 | 已有月末自动结算主干，cron/补跑策略仍需升级 |
| B-3 | 佣金审批流：业务员 → 主管 → 财务 → 放款 | ⬜ | 现有审批闭环不完整 |
| B-4 | 前端审批列表 + 审批链可视化 + 驳回理由 | ⬜ | 当前仅有基础审批页面 |
| B-5 | 联盟商佣金结算对账表导出 | ⬜ | 未开始 |
| B-6 | 单测：分账/跳点/补贴/扣费/税 覆盖率 ≥ 90% | 🟦 | 现有 `CommissionCalculatorTest` 已高覆盖，但新链路完成后需回归扩展 |

### WP-S5-C｜敏感信息加密修复（后端 #3，3d）

| # | 任务 | 状态 |
|---|------|------|
| C-1 | `sys_secret_vault` + `V43__secret_vault.sql` | ⬜ |
| C-2 | `SecretVaultService`：AES-256-GCM + 随机 IV + 版本号 | ⬜ |
| C-3 | `SysServerServiceImpl` 服务器密码改走 Vault | ⬜ |
| C-4 | `sys_secret_access_log` + 审计切面 | ⬜ |
| C-5 | `scripts/rotate-secrets.sh` | ⬜ |

### WP-S5-D｜部署安全资产加固（DevOps，3d）

| # | 任务 | 状态 |
|---|------|------|
| D-1 | `nginx.conf` 增加 SSL / HSTS / CSP 等安全头 | ⬜ |
| D-2 | `limit_req_zone` + 大文件超时 | ⬜ |
| D-3 | `application-prod.yml` 移除 `changeit` 默认值 | ⬜ |
| D-4 | `docker-compose.yml` 改 `secrets:` | ⬜ |
| D-5 | `scripts/deploy-cloud.sh` 去硬编码示例密码 | ⬜ |
| D-6 | `start.sh` 轮询 `/actuator/health` 替代固定 `sleep` | ⬜ |
| D-7 | `.env.example` 只保留外部 secrets 注入说明 | ⬜ |

### WP-S5-E｜前端类型严格化 + 拦截器加固（前端 Lead，4d）

| # | 任务 | 状态 | 备注 |
|---|------|------|------|
| E-1 | `tsconfig.json` 开 `strict / noImplicitAny / strictNullChecks` | ⬜ | 当前未开启严格模式 |
| E-2 | 清理 `any / @ts-ignore`，补 API `interface` | ⬜ | 仍有较多存量 |
| E-3 | `request.ts` 指数退避重试 | ⬜ | 未实现 |
| E-4 | 修复 `isLoggingOut` 401 并发竞态 | ⬜ | 当前仍为单布尔位方案 |
| E-5 | AES 解密失败降级日志 + sentry-ready 埋点 | 🟦 | 解密失败日志已存在，但未接统一埋点 |
| E-6 | `vue-tsc --noEmit` 零错误入 CI | ⬜ | 当前仍被仓库既有类型基线阻塞 |

### WP-S5-F｜废弃文件清理（前端 #2，1d）

| # | 任务 | 状态 | 备注 |
|---|------|------|------|
| F-1 | 删 `pengcheng-core/pom.xml.bak`、`pengcheng-infra/pom.xml.bak` | ✅ | 已删，`.gitignore` 新增 `*.bak` 兜底 |
| F-2 | 删 `pengcheng-ui/src/views/smart-table/` | ✅ | 路由实际指向 `views/smarttable/`，旧目录已删 |
| F-3 | 删 `pengcheng-ui/src/views/test/test/` | ✅ | 已删 |
| F-4 | 删 `pengcheng-ui/src/views/system/customer/` | ✅ | 路由未引用，已删 |
| F-5 | 删 `pengcheng-uniapp/utils/websocket.js` | ❌ 取消 | **误列**：该文件是唯一的 WS 客户端实现，`App.vue` 及 `pages/{chat,group-chat,profile,profile/password,index,login}` 共 7 处在用，无替代实现。仅允许按 V3.2 保留资产原则做性能/稳定性优化 |
| F-6 | `temp-disabled/README.md` 保留说明 | ✅ | 已写，说明微信/支付宝 Feature Flag 保留原则 |
| F-7 | `.gitignore` 加 `*.bak` | ✅ | 已加（`*.bak` + `**/*.bak`） |
| F-8 | 保留微信/支付宝相关资产 | 🔒 | 不删，只加开关与文档 |
| F-9 | `doc/` 历史文档归档至 `doc/archive/v3.x-plans/`、`doc/archive/v3.0-delivery/` | ✅ | 2026-04-23 执行，git 保留历史 |
| F-10 | 删 `doc/AGENTSCOPE-REFERENCE-AND-BORROWING.md`、`doc/agentscope-b-upgrade-plan.md` | ✅ | 代码无 AgentScope 依赖，文档直接删除 |

**Sprint 5 DoD**：所有 POST/PUT 有统一校验与错误码；佣金月结试跑成功；`git grep changeit` 零结果；`vue-tsc --noEmit` 零错。

---

## 三、Sprint 6（第 3–4 周）— 功能闭环 + 微信支付宝加固

### WP-S6-A｜会议模块接真 API（前端 Lead + 后端 Lead，5d）

| # | 任务 | 状态 |
|---|------|------|
| A-1 | `MeetingCalendar.vue` 8 个 TODO 接真 API | ⬜ |
| A-2 | 会议创建/取消/改期/纪要端点补全 | ⬜ |
| A-3 | 会议 → 任务/待办联动 | ⬜ |
| A-4 | `api/calendar.ts` 补齐会议端点 | ⬜ |
| A-5 | 会议通知走 `ChannelPushService` | ⬜ |

### WP-S6-B｜AI 三子模块 API 层补齐（前端 Lead，4d）

| # | 任务 | 状态 |
|---|------|------|
| B-1 | `aiKnowledge.ts` | ⬜ |
| B-2 | `aiSkills.ts` | ⬜ |
| B-3 | `aiMcp.ts` | ⬜ |
| B-4 | 知识库页接真：上传 → 向量化 → 检索引用 | ⬜ |
| B-5 | Skills 配置页 + MCP 工具列表联调 | ⬜ |

### WP-S6-C｜AI 容错 + RAG 降级（后端 Lead，5d）

| # | 任务 | 状态 |
|---|------|------|
| C-1 | `RagSearchEnhancer` BM25 fallback | ⬜ |
| C-2 | `AgentOrchestratorService` 通用 agent 降级链 | ⬜ |
| C-3 | `CustomerAgentTool / ReportAgentTool` null-check + 日志 | ⬜ |
| C-4 | 索引失败重试队列 + DLQ | ⬜ |
| C-5 | 三路径单测 | ⬜ |

### WP-S6-D｜HR 考勤导入 + 绩效完善（后端 #2，5d）

| # | 任务 | 状态 |
|---|------|------|
| D-1 | Excel 考勤模板导入 | ⬜ |
| D-2 | 月度考勤报表导出 | ⬜ |
| D-3 | 考勤异常批量更正 | ⬜ |
| D-4 | 360 权重配置 UI + 合计=1 校验 | ⬜ |
| D-5 | 绩效结果批量导出 | ⬜ |

### WP-S6-F｜uniapp 财务模块（前端 #2，5d）

| # | 任务 | 状态 |
|---|------|------|
| F-1 | `pages/commission/list + detail` | ⬜ |
| F-2 | `pages/receivable/list + detail + overdue` | ⬜ |
| F-3 | `pages/project/list + detail` | ⬜ |
| F-4 | `utils/api.js` 补财务/项目 API | ⬜ |
| F-5 | 成交确认移动端页面 + 推送提醒 | ⬜ |

### WP-S6-G｜微信/支付宝加固 + Feature Flag（后端 #3，8d）

| # | 任务 | 状态 | 备注 |
|---|------|------|------|
| G-1 | 为微信/支付宝相关 bean 增加 `@ConditionalOnProperty` 开关，默认 `false` | ✅ | 已新增 `pengcheng.feature.alipay / wechat.mp / wechat.mini / wechat.pay`，并挂到 pay/social/controller/login strategy 链路 |
| G-2 | root `pom.xml` 锁 `alipay-sdk-java` 到 `4.40.272.ALL` | ⬜ | 当前仍为 `4.39.79.ALL` |
| G-3 | 微信平台证书动态获取 + Redis 缓存 + 本地 fallback | ⬜ | 未开始 |
| G-4 | 支付宝 `notify_id` Redis 幂等 | ⬜ | 当前已有基础幂等，未按新方案收口 |
| G-5 | `WechatAccessTokenHolder` Redis token 缓存与自动刷新 | ⬜ | 未开始 |
| G-6 | `AlipayClient / NativePayService` 单例化 + 超时池化 | ⬜ | 当前仍是方法内构造 |
| G-7 | 回调防重放 + 日志脱敏 | ⬜ | 未开始 |
| G-8 | 金额容差改配置 `pay.tolerance-amount` | ⬜ | 当前仍为代码内固定值 |
| G-9 | 验签/重放/幂等/金额不符单测与压测 | ⬜ | 未开始 |
| G-10 | `WECHAT-ALIPAY-ENABLE.md` 一页纸启用文档 | ⬜ | 未开始 |

**Sprint 6 DoD**：会议全链路可用；AI 知识库对话带引用；uniapp 销售可看佣金；微信/支付宝在“全关”状态下应用正常启动，“按需开启”后可进入沙箱联调。

---

## 四、Sprint 7（第 5–6 周）— 质量基线 + 可观测 + 发版

### WP-S7-A｜测试基线（继承完成）

| # | 任务 | 状态 | 备注 |
|---|------|------|------|
| A-1 | JaCoCo + CI 门禁行覆盖率 ≥ 45% | ✅ | 已完成 |
| A-2 | `CommissionCalculator` 单测覆盖 ≥ 90% | ✅ | 已完成 |
| A-3 | `CustomerService` 保护期/状态机/公海 单测 | ✅ | 已完成 |
| A-4 | `DataPermissionInterceptor` SQL 改写单测 | ✅ | 已完成 |
| A-5 | `AttendanceServiceImpl` 单测 | ✅ | 已完成 |

### WP-S7-B｜集成/契约/E2E 测试

| # | 任务 | 状态 | 备注 |
|---|------|------|------|
| B-1 | TestContainers 支付回调幂等集成测试（含微信+支付宝） | 🟦 | 当前已有 `PaymentCallbackIdempotencyTest` 打通 PaymentService 幂等主链；控制器级“双通道回调”仍需扩展 |
| B-2 | WebSocket 端到端集成测试 | ✅ | `/ws/message` E2E 已完成 |
| B-3 | REST Assured 契约测试 30 核心端点 | ⬜ | 未开始 |
| B-4 | Playwright E2E 5 条核心旅程 | ⬜ | 未开始 |

### WP-S7-C｜可观测性（DevOps，4d）

| # | 任务 | 状态 |
|---|------|------|
| C-1 | Micrometer + Prometheus + Grafana | ⬜ |
| C-2 | Loki 日志集中 + trace-id | ⬜ |
| C-3 | k6 压测 3 场景 | ⬜ |
| C-4 | 支付/登录专项监控面板 | ⬜ |

### WP-S7-D｜CI/CD + 依赖锁定（DevOps，3d）

| # | 任务 | 状态 |
|---|------|------|
| D-1 | `backend-ci.yml`（Maven + JaCoCo + Spotless） | ⬜ |
| D-2 | `frontend-ci.yml`（vue-tsc + eslint + vitest + build） | ⬜ |
| D-3 | `release.yml`（多架构 Docker 镜像 + 版本 tag） | ⬜ |
| D-4 | `npm ci` + Renovate 锁依赖 | ⬜ |

### WP-S7-E｜Spring AI 升版专项

| # | 任务 | 状态 | 备注 |
|---|------|------|------|
| E-1 | `spring-ai 1.1.2` + `alibaba 1.1.2.2` 升版 | ⏸ | 当前继续延后，不是阻断项 |
| E-2 | ChatMemory artifact 重命名 + 包名重构 | ⏸ | 继续延后 |
| E-3 | Advisor 模板占位符升级 | ⏸ | 继续延后 |
| E-4 | `KnowledgeBaseService` 走 `VectorStore.similaritySearch` | ⏸ | 继续延后 |
| E-5 | 房源/客户/佣金查询挂 Function Calling | ⏸ | 继续延后 |
| E-6 | 端到端“本月签几单”工具调用 DB | ⏸ | 继续延后 |

### WP-S7-G｜文件预览 + Office 编辑闭环

| # | 任务 | 状态 |
|---|------|------|
| G-1 | WOPI 完整协议 | ⬜ |
| G-2 | OnlyOffice JWT + nginx 反代证书链 | ⬜ |
| G-3 | 断点续传 resume | ⬜ |
| G-4 | 文件版本控制 `sys_file_version` | ⬜ |
| G-5 | 病毒扫描 API 对接 | ⬜ |

### WP-S7-F｜RELEASE-CHECKLIST 走完 + 发版

| # | 任务 | 状态 |
|---|------|------|
| F-1 | `RELEASE-CHECKLIST-V3.0.md` 逐项验证 | ⬜ |
| F-2 | 增加“禁用态功能完整性自检”章节 | ⬜ |
| F-3 | 归档过时报告到 `doc/archive/v3.0-delivery/`、`doc/archive/v3.x-plans/` | ✅ | 2026-04-23 执行，13 份 V3.0 交付报告 + 5 份 V3.x 计划已归档（commit `4b18b4d`） |
| F-4 | `README.md` 版本基线到 V3.2 | ⬜ |
| F-5 | 用户手册 / API-REFERENCE 对齐 | ⬜ |
| F-6 | `DELIVERY-CHECKLIST.md` 签字 + `cloudbaserc.json` 更新 | ⬜ |

---

## 五、当前推进顺序（校准后）

1. `WP-S6-G-1` 已完成，先用 Feature Flag 把微信/支付宝相关链路安全包起来。
2. 下一优先级建议切到 `WP-S5-A`，把“参数校验 + 全局异常 + 错误码”做成统一防御层。
3. Spring AI 升版继续保持 `⏸`，仅在前面主线清掉后再拉起。

---

## 六、绝对不删清单

- `pengcheng-infra/pengcheng-pay/**`
- `pengcheng-infra/pengcheng-social/**`
- `pengcheng-api/**/pay/**`
- `pengcheng-api/**/auth/Wechat*`
- `temp-disabled/*.archived`
- `pengcheng-ui/src/api/wechat.ts`
