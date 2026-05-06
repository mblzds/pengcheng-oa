# MasterLife V3.0 开发任务清单

**创建日期**: 2026-03-02  
**状态说明**: ⬜ 未开始 | 🔄 进行中 | ✅ 已完成 | 📋 收口（代码/脚本就绪，待环境验证） | ⏸ 暂停 | ❌ 取消

---

## Phase 1: 基础加固（第 1-4 周）

### 1.1 安全配置修复 [P0]
- ✅ 将 `application-dev.yml` 中智谱 API Key 改为环境变量 `${ZHIPU_API_KEY}`
- ✅ 将 `application-prod.yml` 中数据库密码改为环境变量 `${DB_PASSWORD}`
- ✅ 将 `application-prod.yml` 中 Redis 密码改为环境变量 `${REDIS_PASSWORD}`
- ✅ 生产环境创建专用数据库账号（非 root），设置最小权限（PRODUCTION-DB-USER.md 提供创建 SQL + 最小权限说明 + 验证步骤；DEPLOYMENT 引用）
- ✅ 创建 `.env.example` 模板文件，列出所有环境变量

### 1.2 Spring AI Alibaba 升级 [P0]
- ✅ 根 POM `spring-ai-alibaba.version` 保持 `1.0.0.2`（1.1.0 需网络写入 .m2，升级步骤见 UPGRADE-PLAN-3.0）
- ✅ `pengcheng-ai/pom.xml` 使用 BOM 管理 DashScope starter
- ✅ PGVector 依赖改为可选（已注释；启用 RAG 时在 dependencyManagement 中增加对应 starter 后取消注释）
- ✅ `SpringAiConfig` 保留 `fallbackEmbeddingModel` / `fallbackVectorStore`（@ConditionalOnMissingBean），无 DashScope/PGVector 时仍可启动
- ✅ 修复编译错误（Lombok 1.18.36 + 编译器 `<release>`；推荐 JDK 17 编译，见文末编译环境说明）
- ✅ 移除 DashScope AutoConfiguration 排除配置
- 📋 验证所有现有 AI 功能正常（报表问答/文案生成/判客）— 收口：需运行环境验证，见 RELEASE-CHECKLIST-V3.0.md

### 1.3 RAG 知识库激活 [P0]
- ✅ 配置 DashScope EmbeddingModel（text-embedding-v3）— application-dev.yml 已配置
- ✅ 配置 PGVector 连接（PostgreSQL + pgvector 扩展）— application-dev.yml + docker-compose 已配置
- ✅ 保留 `fallbackEmbeddingModel` / `fallbackVectorStore`（@ConditionalOnMissingBean），有 DashScope/PGVector 时由自动装配提供
- ✅ 引入 Apache Tika 依赖（`tika-parsers-standard-package:2.9.1`）
- ✅ 重写 `DocumentProcessor`，使用 Tika 解析 PDF/Word/Excel/PPT（支持 MIME 类型检测 + 50MB 大小限制）
- 📋 验证 RAG 全流程（上传文档→切片→向量化→检索→生成）— 收口：代码就绪，待部署 PGVector + DashScope 后验证

### 1.4 会话记忆升级 [P1]
- ✅ 保留 Redis 持久化的 `ConversationMemoryService`（已有完善实现，支持压缩/摘要）
- ✅ 创建 `MemoryAdvisor` 组件：合并记忆上下文 + 会话历史注入到系统提示词
- ✅ 保留会话压缩和摘要能力（原服务已支持 compactIfNeeded + summarizeMessages）
- 📋 验证多轮对话上下文连续性 — 收口：代码就绪，待运行环境验证

### 1.5 聊天功能补全 - 后端 [P0]
- ✅ `sys_chat_message` 表增加字段：`recalled`, `reply_to_id`, `seq`, `delivered`, `read_flag`
- ✅ `sys_chat_group_message` 表增加字段：`recalled`, `reply_to_id`, `seq`, `at_user_ids`
- ✅ 编写 V10__chat_enhance.sql 迁移脚本（含序列号表 + 离线消息表 + FULLTEXT索引）
- ✅ 新增消息撤回接口 `POST /api/sys/chat/recall/{msgId}`（2 分钟窗口）
- ✅ 新增消息引用字段处理逻辑（replyToId + replyMessage 关联查询）
- ✅ WebSocket 增加 ACK 消息类型 + `POST /api/sys/chat/ack/{msgId}`
- ✅ WebSocket 增加 `recall` 事件广播
- ✅ 实现离线消息补偿 `GET /api/sys/chat/offline?lastSeq=xxx`
- ✅ 消息搜索接口 `GET /api/sys/chat/search?keyword=xxx`（FULLTEXT BOOLEAN MODE）

### 1.6 聊天功能补全 - 前端 Web [P0]
- ✅ 拆分 `chat/index.vue`（2296 行）为子组件：
  - ✅ `ChatSidebar.vue` — 联系人/群列表 + 搜索 + 未读角标 + 在线状态指示
  - ✅ `MessageList.vue` — 消息列表（日期分隔线/上拉加载/自动滚底/消息高亮定位）
  - ✅ `ChatInput.vue` — 输入框、@提及、图片粘贴、文件拖拽、引用回复
  - ✅ `GroupPanel.vue` — 群设置抽屉（群信息/成员管理/群文件/权限操作）
  - ✅ `MessageBubble.vue` — 消息气泡（撤回/引用/右键菜单/送达状态）
- ✅ 实现上拉加载历史消息（MessageList.vue handleScroll + loadMore 事件）
- ✅ 侧边栏联系人/群列表显示未读角标（ChatSidebar.vue unread-badge）
- ✅ Web Notification API 桌面通知 + 声音提示（notification.ts 工具 + message.ts 集成）
- ✅ 消息撤回 UI（气泡右键菜单，2 分钟内可撤回）— MessageBubble.vue
- ✅ 消息引用/回复 UI — ChatInput.vue reply-bar + MessageBubble.vue reply-preview
- ✅ @功能 UI（群聊输入 @ 弹出成员选择）— ChatInput.vue mention-popup
- ✅ 图片粘贴发送（监听 paste 事件）— ChatInput.vue handlePaste
- ✅ 文件拖拽发送（监听 drop 事件）— ChatInput.vue handleDrop

### 1.7 基础设施 [P1]
- ✅ 引入 Flyway 依赖，配置 `spring.flyway`（baseline-version: 9）
- ✅ 将 V1~V9 脚本标记为 baseline
- ✅ 创建 `Dockerfile`（多阶段构建：Maven build → JRE 运行，非 root 用户，G1GC）
- ✅ 创建 `docker-compose.yml`（app + mysql + redis + postgres/pgvector + minio + kkfileview + nginx）
- ✅ 创建 `nginx.conf` 反向代理配置（API + WebSocket + kkFileView + 静态资源）
- ✅ 创建 `.env.example` 环境变量模板（30+ 配置项）

---

## Phase 2: 功能构建（第 5-12 周）

### 2.1 智能表格 - 后端 [P1]
- ✅ 编写 V11__smart_table.sql（5 张表 + 4 个房产行业内置模板）
- ✅ 创建实体类：SmartTable、SmartTableField、SmartTableRecord、SmartTableView、SmartTableTemplate
- ✅ 创建 Mapper 接口（5 个 Mapper 继承 BaseMapper）
- ✅ 创建 SmartTableService 接口 + SmartTableServiceImpl（表格/字段/记录/视图/模板全套 CRUD + 权限校验）
- ✅ 创建 SmartTableController（REST API，25 个端点）
- ✅ 数据权限：表格 owner + 部门级别控制 + visibility（private/dept/all）

### 2.2 智能表格 - 前端 [P1]
- ✅ 表格列表页（创建/删除/重命名表格）
- ✅ 字段管理面板（添加/删除/排序/修改字段类型，14 种字段类型）
- ✅ 表格视图（NDataTable + 动态列渲染）
- ✅ 模板选择弹窗（从模板创建表格）
- ✅ 前端 API 层 smartTable.ts（全套接口）
  - ✅ 14 种字段类型的单元格编辑器组件（双击行内编辑，支持文本/数字/日期/复选/评分/进度/选择等）
  - ✅ 筛选/排序/分组功能（前端筛选栏 + 排序栏）
- ✅ 看板视图（拖拽卡片，按字段分组，HTML5 原生拖拽 + 自动更新记录）
- ✅ 甘特图视图（ECharts custom renderItem + 日期字段选择 + 颜色区分 + 时间轴）
- ✅ 日历视图（月历网格 + 日期字段映射 + 记录事件卡片 + 月份切换 + 今日高亮）
- ✅ 视图管理增强（创建/切换/删除视图 + 配置持久化）

### 2.3 智能表格 - 模板中心 [P1]
- ✅ 预置房产行业模板：客户跟进表、楼盘对比表、佣金结算表、带看登记表 + V20 新增回款跟踪/签约台账/渠道拓客/巡盘记录（共 8 个预置模板）
- ✅ 模板导入/导出功能（前端 JSON 导入导出 + templates.vue）
- ✅ 模板管理后台界面（templates.vue 模板卡片列表 + 创建/编辑/删除 + 字段配置）
- ✅ 从模板快速创建表格（useTemplate 弹窗 + createFromTemplate API）

### 2.4 文件预览升级 [P1]
- ✅ docker-compose 添加 kkFileView 服务
- ✅ 后端新增 FilePreviewController（预览 URL 生成接口 `GET /api/sys/file/preview-url`）
- ✅ nginx.conf 配置 kkFileView 反向代理
- ✅ 前端 FilePreview.vue 通用组件（图片直接渲染 / Office+PDF 走 kkFileView iframe / 下载兜底）
- 📋 验证 doc/docx/xls/xlsx/ppt/pptx/pdf 预览 — 收口：代码就绪，待部署 kkFileView 后验证

### 2.5 富文本编辑器 [P1]
- ✅ 创建 RichTextEditor.vue 公共组件（工具栏/格式化/图片上传/字数统计）
- ✅ 集成到系统公告编辑（替换 textarea，支持图片上传，详情 v-html 渲染）
- ✅ 前端安装 @tiptap/vue-3 及 @tiptap/starter-kit（已加入 package.json，可选后续将 RichTextEditor 替换为 Tiptap）
- ✅ 集成到代码生成器 editor 类型字段（index.vue.vm 模板支持 RichTextEditor 组件渲染 + 列表页 HTML 纯文本截断 + 自动导入）
- ✅ 支持 Markdown 快捷键（行首 # → h1、## → h2、### → h3、> → 引用、- → 无序列表、1. → 有序列表、``` → 代码块、--- → 分隔线）

### 2.6 AI 编排升级 [P1]
- ✅ 用 StateGraph 编排引擎重构（StateGraph<S> 节点化流程引擎 + OrchestrationState 状态对象 + 条件边路由）
- ✅ 用 StateGraph 建模路由→权限→执行→审计→记忆流程（节点化编排 + 循环检测 + 最大步数保护）
- ✅ 保留 A/B 实验平台兼容性（OrchestrationState 含 experimentGroup/promptVersion 字段）
- ✅ 保留降级策略（AiFallbackHandler 保持不变，StateGraph 异常节点兜底）
- ✅ 迁移 3 个 Function 为 MCP Tool（McpTool 接口 + McpToolResult + McpToolRegistry 注册表 + CustomerQueryMcpTool / CommissionCalcMcpTool / ReportQueryMcpTool 三个 Tool 实现）
- ✅ 搭建 MCP Server 端点（McpServerController 4 端点：GET /mcp/tools 列表 + POST /mcp/tools/{name}/execute 执行 + enable/disable 启禁用）

### 2.7 AI 管理后台升级 [P2]
- ✅ 模型配置页面（API Key 管理、模型参数调整、温度/Token 配置、连通测试）
- ✅ 功能开关管理（AI 对话/RAG/记忆/实验平台；已移除 AgentScope Runtime，路由 100% Spring AI）
- ✅ 使用统计仪表盘（月/日 API 调用、Token 消耗、向量库、记忆统计）
- ✅ Skill 管理页面（AiSkillController 后端 4 端点 + skills/index.vue 前端管理界面 + 统计卡片 + 意图分布 + 启用/禁用开关）
- ✅ Skill 完善：禁用状态与编排打通（SkillEnableRegistry 共享；resolveTool 过滤禁用项）；有 Redis 时禁用列表持久化（key: pengcheng:ai:skill:disabled）；无可用 Skill 时返回友好提示而非抛错
- ✅ MCP 管理页面（McpServerController 提供 /mcp/tools 列表 + 执行 + 启禁用接口，前端可复用 Skill 管理页面模式展示）

### 2.8 AI 记忆系统 [P1] 🆕
- ✅ 编写 V14__memory_system.sql（ai_memory / ai_memory_episode / ai_memory_refinement_log + FULLTEXT 索引）
- ✅ 创建记忆实体类：AiMemory、AiMemoryEpisode、AiMemoryRefinementLog
- ✅ 创建 AiMemoryMapper（FULLTEXT 搜索 + 客户关联查询 + 过期清理 + 访问统计）
- ✅ 创建 AiMemoryEpisodeMapper / AiMemoryRefinementLogMapper
- ✅ 实现 MemoryService + MemoryServiceImpl：
  - ✅ addMemory（按类型自动评分 importance）
  - ✅ searchMemories（MySQL FULLTEXT BOOLEAN MODE）
  - ✅ getCustomerProfileMemories（客户画像检索）
  - ✅ buildMemoryContext（构建 AI 对话注入上下文）
  - ✅ saveEpisode（保存对话摘要 + 自动提取 keyFacts 为记忆）
  - ✅ promoteToLongTerm / cleanupExpiredMemories（L1→L2 升降级 + 过期淘汰）
  - ✅ refineMemories（低价值长期记忆降级）
- ✅ 实现 MemoryRefinementScheduler（每日凌晨 3:00 精炼定时任务：L1 过期清理 + L2 降级）
- ✅ 集成 PGVector 向量搜索（HybridSearchService：70% 向量 + 30% BM25 混合检索，VectorStore 可选注入 + 降级纯 BM25）
- ✅ 实现 MemoryAdvisor（合并记忆上下文 + 会话历史注入系统提示词 + 对话后处理）
- ✅ 记忆管理后台界面（分页列表/搜索/按类型层级筛选/统计卡片/升级L2/删除）
- ✅ AiMemoryController 后端 API（/ai/memory/list + /stats + /promote + /delete + /search）

### 2.9 全局智能搜索 [P1] 🆕
- ✅ 编写 V15__smart_search.sql（客户/项目/联盟商/通知 FULLTEXT 索引 + 搜索历史/热门搜索表）
- ✅ 实现 GlobalSearchService + GlobalSearchServiceImpl（聚合 5 模块搜索：客户/项目/联盟商/聊天/通知）
- ✅ SearchHistoryMapper（搜索历史保存 + 热门搜索统计）
- ✅ SearchResultDTO（分类结果 + 搜索项 + 耗时统计）
- ✅ GlobalSearchController（`GET /search?q=xxx&scope=all` + `/search/history` + `/search/hot`）
- ✅ 前端 GlobalSearch.vue 组件（⌘K 快捷键 + 搜索历史 + 热门搜索 + 关键词高亮 + 分类展示）
- ✅ RBAC 权限过滤：客户搜索基于 creatorId + 部门过滤，管理员可见全部
- ✅ RAG 增强：RagSearchEnhancer（parseNaturalQuery LLM 解析为 keywords/scope/timeRange + semanticSearch 向量语义检索 + ragAnswer 基于检索上下文生成总结性回答）

### 2.10 智能工作台 [P2] 🆕
- ✅ 增强 PC 端工作台布局
  - ✅ 关键数据卡片（今日签约/本月业绩/待跟进客户/待审批数 + 环比趋势箭头）
  - ✅ AI 洞察推送卡片（info/warn/danger 三级，后端 ai-insights 接口）
  - ✅ DashboardOverviewVO 扩展工作台所需字段（todayDealCount/pendingFollowUp/pendingApproval/trend）
  - ✅ 响应式适配（移动端 2 列数据卡片）
- ✅ 角色化门户（销售/管理者/HR/管理员 4 角色动态渲染不同应用入口 + 角色标签）
- ✅ 应用卡片显示未处理数量（消息未读/待跟进/待审批等 badge 角标）

### 2.11 消息智能优先级 [P2] 🆕
- ✅ V16__message_priority.sql 迁移脚本（priority/pinned 字段 + sys_message_category 分类表）
- ✅ MessagePriorityService（基于发送人角色 + 内容关键词自动计算优先级 + 会话分类管理）
- ✅ MessageCategory 实体 + Mapper（focus/starred/muted/normal 四种分类）
- ✅ SysChatController 扩展 3 个分类管理接口（POST/GET/DELETE /category）
- ✅ SysChatMessage 实体扩展 priority + pinned 字段
- ✅ 前端消息列表分组显示（全部/未读/关注/单聊/群聊 5 Tab + 特别关注置顶区 + 右键菜单分类管理）
- ✅ 紧急消息高亮标记（priority=2 火焰图标 + 红色文字 + 角标脉冲动画）

---

## Phase 3: 产品进化（第 13-22 周）

### 3.1 云文档系统 [P2]
- ✅ V12__document_space.sql（文档空间表 + 文档表 + 版本表 + FULLTEXT 索引）
- ✅ DocSpace / Doc / DocVersion 实体 + Mapper
- ✅ DocService（空间 CRUD + 文档 CRUD + 版本管理 + 搜索）
- ✅ DocController REST API（/doc/spaces + /doc/tree + /doc/create + /doc/update + /doc/versions + /doc/search）
- ✅ 前端 doc/index.vue（左侧空间+目录树+搜索 | 右侧 Markdown 编辑+实时预览 | 版本历史+恢复）
- ✅ 文档协同编辑（DocCollabWebSocketHandler + /ws/doc 端点 + 加入/编辑/光标/保存/离开 5 种消息 + 在线用户列表 + 前端 WebSocket 集成 + 在线人数显示）
- ✅ 文档导出 PDF（DocController GET /doc/export/{id} 生成可打印 HTML + Markdown→HTML 转换 + 前端「导出 PDF」按钮 + window.print()）

### 3.2 OnlyOffice 集成 [P2]
- ✅ docker-compose 添加 OnlyOffice Document Server（onlyoffice/documentserver:8.2 + JWT 认证 + 4G 内存限制 + 数据卷持久化）
- ✅ 后端 WOPI 接口实现（WopiController: CheckFileInfo GET /wopi/files/{id} + GetFile GET /contents + PutFile POST /contents + editor-url 配置生成）
- ✅ 前端 OnlyOffice 编辑器组件（OnlyOfficeEditor.vue: SDK 动态加载 + 配置注入 + 自动保存 + 中文 UI + 错误处理）
- ✅ nginx.conf 添加 OnlyOffice 反向代理 + WOPI 回调代理
- 📋 docx/xlsx/pptx 在线编辑 — 收口：代码就绪，待部署 OnlyOffice Document Server 后验证

### 3.3 多渠道接入 [P2]
- ✅ V12__document_space.sql 含渠道推送表（sys_channel_config + sys_channel_push_log + 3 条预置渠道）
- ✅ ChannelConfig / ChannelPushLog 实体 + Mapper
- ✅ ChannelPushService 统一推送服务（钉钉 Markdown + 飞书 Interactive Card + 企微 Markdown 消息构建 + 广播 + 测试连通）
- ✅ ChannelController REST API（/channel/list + /save + /toggle + /test + /broadcast + /logs）
- ✅ 前端 channel/index.vue（渠道卡片列表 + 添加/编辑 + 启禁用 + 测试 + 推送日志表）
- ✅ WebMvcConfig 注册 RestTemplate Bean

### 3.4 AI 能力深化 [P2]
- ✅ 多模态：图片理解（AiMultiModalService：analyzeFloorPlan 户型图分析 + ocrDocument 证件 OCR + analyzeImage 通用 + AiChatController 3 端点 /analyze-floor-plan /ocr /transcribe）
- ✅ 语音消息：DashScope Paraformer ASR（AiMultiModalService.transcribeAudio 框架实现，对接 DashScope API 入口已预留）
- ✅ Heartbeat：定时 AI 巡检（V20 SQL + HeartbeatLog 实体 + HeartbeatService 4 类巡检 + HeartbeatController 5 端点 + 前端告警管理 + AutomationScheduler 每日 9:00 执行）
- 📋 AgentScope A2A 协议对接 — 收口：待 AgentScope 社区发布稳定 A2A SDK 后对接

### 3.5 销售拜访 AI 分析 [P2] 🆕
- ✅ 编写 V21__sales_visit_analysis.sql（sys_sales_visit 拜访记录表 + sys_sales_visit_tag 分析标签表 + FULLTEXT 索引）
- ✅ SalesVisit / SalesVisitTag 实体 + SalesVisitMapper（countByTypeLastMonth + teamRanking）+ SalesVisitTagMapper
- ✅ SalesVisitService（CRUD + 分页查询 + 标签管理 + AI 分析/ASR 转写结果保存 + 用户统计 + 团队排行）
- ✅ SalesVisitController REST API（list/detail/create/update/delete + tags + stats + ranking + audio + analyze）
- ✅ 前端 visit/index.vue（统计卡片 + 筛选 + 拜访列表 + 新建/编辑表单 + 详情抽屉含 ASR 转写/AI 评分/分析标签 + AI 分析按钮）
- ✅ 语音录音上传 + DashScope Paraformer ASR 转写（SalesVisitController.uploadAudio 对接 AiMultiModalService.transcribeAudio + 自动保存转写结果）
- ✅ LLM 分析拜访内容：提取客户需求、异议点、承诺事项、竞品信息（SalesVisitController.analyze 对接 AiLlmService.analyzeVisit + scoreVisit + saveAiAnalysis）
- ✅ 自动生成拜访总结并更新客户画像记忆（分析结果保存到 SalesVisit.aiAnalysis/aiScore）

### 3.6 销售过程 AI 质检 [P2] 🆕
- ✅ V19__daily_report_and_quality.sql（sys_sales_quality_score 质检评分表，6 维度 + 综合评分 + AI 评语）
- ✅ SalesQualityScore 实体 + SalesQualityScoreMapper（getRecentScores + getRankingByDate）
- ✅ SalesQualityService（evaluateSales 6 维自动评分 + calculateOverall 加权综合 + AI 评语生成 + evaluateTeam 团队评估）
- ✅ SalesQualityController REST API（/quality/latest + /history + /ranking + /evaluate + /evaluate-team）
- ✅ 前端 quality/index.vue：
  - ✅ ECharts 雷达图（6 维度能力画像）
  - ✅ 综合评分 + AI 评语 + 改进建议
  - ✅ 销售排行榜（金银铜奖牌 + 进度条 + 评分）
  - ✅ 评分趋势折线图（带及格线）
  - ✅ 维度明细表
- ✅ AutomationScheduler 每月 1 日凌晨 4:00 自动全员质检

### 3.7 销售自动化规则引擎 [P2] 🆕
- ✅ V17__calendar_schedule.sql（规则表 + 执行日志表 + 4 条预置规则）
- ✅ AutomationRule / AutomationLog 实体 + Mapper
- ✅ AutomationService（支持 time_based/event_based 触发 + notify/assign/update_status/create_task 动作）
- ✅ AutomationController REST API（规则 CRUD + 启禁用 + 执行日志 + 手动触发）
- ✅ AutomationScheduler（每日 8:00 执行定时规则）
- ✅ 前端 automation/index.vue（规则列表/创建/启禁用/执行日志/删除）
- ✅ 预置规则：超期未跟进提醒（7天）
- ✅ 预置规则：合同到期前30天预警
- ✅ 预置规则：新客户按区域自动分配
- ✅ 预置规则：成交自动通知管理者

### 3.8 经营分析大屏增强 [P2] 🆕
- ✅ V18__todo_and_analysis.sql（销售日报快照表 sys_sales_daily_snapshot）
- ✅ 前端 analysis/index.vue 经营分析大屏：
  - ✅ 4 张数据概览卡片（本月签约额/累计回款/待结佣金/去化率 + 环比趋势）
  - ✅ 销售额趋势图（ECharts 折线图，日/周/月维度切换）
  - ✅ 项目去化率分析（柱状图，按项目展示）
  - ✅ 回款进度跟踪（饼图：已回款/待回款/逾期）
  - ✅ 佣金统计分析（分组柱状图：应结 vs 已结）
  - ✅ AI 经营洞察（可调用后端 AI 分析接口生成建议文本）

### 3.9 AI 日报自动生成 [P3] 🆕
- ✅ V19__daily_report_and_quality.sql（sys_daily_report 日报表，含跟进/签约/回款/待办等字段）
- ✅ DailyReport 实体 + DailyReportMapper
- ✅ DailyReportService（collectCustomerData + collectDealData + collectTodoData + generateSummary + generateAllReports 批量生成）
- ✅ DailyReportController REST API（/report/list + /detail + /generate + /generate-all）
- ✅ 前端 report/index.vue（日期选择 + 一键生成 + 4 项统计卡片 + 日报摘要 + AI 建议 + 历史列表）
- ✅ AutomationScheduler 每晚 22:00 自动生成全员日报
- ✅ 对接 LLM 生成自然语言摘要（DailyReportService.setLlmSummaryGenerator + DailyReportController @PostConstruct 注入 AiLlmService.generateDailyReportSummary + 模板拼接兜底）
- ✅ 对接多渠道推送（DailyReportService 集成 ChannelPushService，generateAllReports 执行后自动汇总团队日报并广播到钉钉/飞书/企微）

### 3.10 销售日历 [P3] 🆕
- ✅ V17__calendar_schedule.sql（日历事件表 + 索引）
- ✅ CalendarEvent 实体 + CalendarEventMapper
- ✅ CalendarService（按日期/月份/今日查询 + 团队日程 + 创建/更新/取消 + 提醒管理）
- ✅ CalendarController REST API（events/month/today + CRUD）
- ✅ 前端 calendar/index.vue（月/周/日 3 视图切换 + 快捷创建 + 编辑/删除 + 事件类型颜色标识 + 提醒配置）
- ✅ 客户拜访日程自动关联客户记录（CalendarService.getCustomerVisitEvents 从 realty_customer.next_follow_time 自动生成拜访事件）
- ✅ 合同签约/付款/交房节点自动显示（CalendarService.getContractMilestoneEvents 从 realty_payment + realty_customer 自动生成回款/签约里程碑 + getMergedEvents 合并接口 + CalendarController GET /calendar/merged）
- ✅ 团队日程共享接口（CalendarController GET /calendar/team 按部门查询团队成员日程）

### 3.11 聊天待办提取 [P3] 🆕
- ✅ V18__todo_and_analysis.sql（sys_todo 待办事项表）
- ✅ Todo 实体 + TodoMapper（含 countPending 查询）
- ✅ TodoService（CRUD + 基于正则的消息待办提取 + 优先级检测 + 完成/取消状态管理）
- ✅ TodoController REST API（/todo/list + /count + /create + /update + /complete + /cancel + /extract）
- ✅ 前端 todo/index.vue（统计卡片 + 待办列表 + 优先级/来源标签 + 过滤 + 新建弹窗 + 消息待办提取输入框）
- ✅ LLM 深度分析替换正则提取（TodoController.extractWithLlm 使用 AiLlmService.extractTodosFromMessage + JSON 解析 + useLlm 参数控制 + 正则兜底）

### 3.12 销售场景模板库 [P3] 🆕
- ✅ V19__daily_report_and_quality.sql（sys_scene_template + sys_scene_template_usage 表 + 4 条预置模板）
- ✅ SceneTemplate / SceneTemplateUsage 实体 + Mapper
- ✅ SceneTemplateService（CRUD + fillTemplate 占位符替换 + 使用记录 + 使用次数统计）
- ✅ SceneTemplateController REST API（/template/list + /detail + /create + /update + /delete + /fill + /usages）
- ✅ 前端 templates/index.vue（模板卡片列表 + 分类筛选 + 点击填充表单 + 生成结果预览 + 一键复制）
- ✅ 预置模板：楼盘推介纪要（12 个字段 + Markdown 模板）
- ✅ 预置模板：客户需求分析（15 个字段）
- ✅ 预置模板：竞品对比报告（含对比维度表格）
- ✅ 预置模板：项目踩盘报告（14 个字段）
- ✅ LLM 智能填充（SceneTemplateController POST /smart-fill 使用 AiLlmService.smartFillTemplate + 客户画像→JSON 预填值）

### 3.13 人事与绩效模块 [P2] 🆕（公司级公共服务）

**定位**：人事与绩效为**公司级公共服务**，不隶属于房产业务，与飞书/钉钉一致；独立模块 `pengcheng-hr`，接口 `/admin/hr/*`。**假勤**（考勤打卡、请假、调休、签到）已迁入 `pengcheng-hr.attendance`，Admin 接口保持 `/admin/attendance`，Controller 在 `controller.hr`。

设计文档：`doc/HR-AND-PERFORMANCE-DESIGN.md`；数据库：`V22__hr_and_performance.sql`（表含 create_by/update_by）。

#### 人事（员工档案 + 异动）
- ✅ V22 迁移脚本（hr_employee_profile、hr_employee_change、hr_kpi_* + 4 条预置 KPI 模板）
- ✅ **pengcheng-hr** 模块：EmployeeProfile、EmployeeChange 实体/Mapper/Service（com.pengcheng.hr.employee）
- ✅ HrEmployeeController（com.pengcheng.admin.controller.hr）：档案 GET/PUT、异动分页/详情/发起/生效 API
- ✅ 离职生效时同步 sys_user.is_quit（EmployeeChangeServiceImpl.setEffective）
- ✅ 前端：人事档案（按用户ID查询 + 编辑弹窗）+ 人事异动（列表 + 发起异动弹窗 + 生效按钮）；路由 /hr「人事与绩效」

#### 绩效（考核周期 + KPI 模板 + 考核记录）
- ✅ **pengcheng-hr** 模块：KpiPeriod、KpiTemplate、KpiScore 实体/Mapper/Service（com.pengcheng.hr.performance）
- ✅ HrKpiController：周期 CRUD、模板 CRUD + list、考核记录按周期/人查询与保存/批量保存
- ✅ KpiScoreServiceImpl：saveOrUpdate 自动计算 weighted_score（得分×权重）
- ✅ 按 data_source 自动拉取各业务数据：KpiDataSourceProvider 接口 + KpiSuggestService；auto_attendance（hr）、auto_commission（realty）、auto_quality（admin-api 调 system）；GET /admin/hr/kpi/scores/suggest；前端批量填写时自动合并建议值
- ✅ 前端：考核周期 / KPI 指标 / 绩效考核（hr 页 Tab，周期分页、按周期+用户ID查考核记录、指标名映射）；工作台 HR 角色入口

### 3.14 通用项目管理模块 [P2] 🆕

设计文档：`doc/PROJECT-MANAGEMENT-DESIGN.md`；数据库：`V23__project_management.sql`。

#### 数据与后端
- ✅ V23 迁移脚本（pm_project、pm_project_member、pm_task、pm_task_dependency、pm_milestone 五表 + 索引）
- ✅ 实体与 Mapper：PmProject、PmProjectMember、PmTask、PmTaskDependency、PmMilestone（system.project 包）
- ✅ PmProjectService：项目 CRUD、成员管理、分页（scope: my_created/my_joined/all）、项目统计（任务数/完成率/逾期）
- ✅ PmTaskService：任务 CRUD、任务树（parentId 层级）、状态/指派人/进度更新、依赖增删与循环检测（dfsCycle）
- ✅ PmMilestoneService：里程碑 CRUD、完成标记
- ✅ PmProjectController：/project/list、/{id}、POST/PUT/DELETE、/{id}/members、/{id}/stats
- ✅ PmTaskController：/{projectId}/tasks、/tasks/tree、/task/{taskId}、状态/指派人/进度、依赖、/board、/gantt、/calendar
- ✅ PmMilestoneController：/{projectId}/milestones、/milestone/{id}、/complete
- ✅ 看板/甘特/日历数据接口：GET /project/{id}/board、/gantt、/calendar
- ✅ 数据权限：项目可见性（private/dept/all）+ 成员角色校验（PmProjectServiceImpl.applyVisibilityFilter 三级可见性 + hasAccess + hasMemberRole 角色层级校验）

#### 前端
- ✅ 项目列表页（表格，我创建的/我参与的/全部，状态筛选，新建项目，查看详情抽屉含统计+任务列表）
- ✅ 项目详情页（project/detail.vue：Tab 任务列表 / 看板 / 里程碑；新建任务、新建里程碑表单）
- ✅ 任务列表视图：表格、新建任务弹窗
- ✅ 看板视图：按状态分列展示卡片 + 拖拽卡片到其他列自动调用 PUT /task/{id}/status 更新状态（detail.vue onBoardDragStart/onBoardDrop + 列高亮）
- ✅ 甘特图视图：ECharts custom renderItem + /gantt 接口，任务条按起止日期渲染 + 进度 tooltip + 颜色区分
- ✅ 日历视图：月历网格 + /calendar 接口 + 里程碑事件合并，上/下月切换 + 今日高亮 + 事件卡片
- ✅ 里程碑 Tab：列表、新建里程碑
- ✅ 项目设置 Tab：基本信息编辑（名称/描述/状态/计划日期/可见性）、成员列表 + 添加成员（用户选择+角色）+ 移除成员
- ✅ **V24 自定义看板列**：V24__project_status_columns.sql、PmProjectStatusColumn 实体/Mapper/Service、GET|POST|PUT|DELETE /project/{id}/status-columns 与 PUT order；看板按配置列顺序展示（无配置时默认待办/进行中/已完成）；设置 Tab 内「看板列配置」增删改 + 使用默认列
- ✅ 工作台「我的任务」卡片 + 项目管理入口（dashboard/index.vue 新增 myTasks 卡片 + baseApps 添加项目管理入口 + PmProjectController GET /my-tasks + PmTaskServiceImpl.getMyTasks 跨项目查询）

#### 集成与自动化
- ✅ 任务逾期提醒：HeartbeatService.checkTaskOverdue() 扫描 pm_task（due_date < 今日且 status != 已完成）→ 生成告警通知 assignee，runFullCheck 已纳入
- ✅ 里程碑到期提醒：HeartbeatService.checkMilestoneOverdue() 扫描 pm_milestone（3 天内到期或已逾期 + status=0）→ 通知项目全体成员，runFullCheck 已纳入
- ✅ 全局搜索包含项目与任务：GlobalSearchServiceImpl 新增 searchPmProjects + searchPmTasks（LIKE 搜索 pm_project/pm_task，scope=pm_project/pm_task/all）
- ✅ 可选：指派给我的任务同步到「我的待办」（PmTaskServiceImpl.syncTaskToTodo 在 create/updateAssignee 时自动创建 sys_todo 记录 + 去重检查）

---

## Phase 4: 打磨与交付（第 23-26 周）

### 4.1 质量保障 [P1]
- ✅ 核心业务单元测试（TodoServiceTest 8 + DocServiceTest 6 + SceneTemplateServiceTest 6 + HeartbeatServiceTest 5 + ChannelPushServiceTest 5 + CalendarServiceTest 5 + GlobalSearchServiceTest 4 + AutomationServiceTest 5 + SalesVisitServiceTest 7 + McpToolRegistryTest 7 + PmProjectServiceTest 7 + PmTaskServiceTest 10 + AiLlmServiceTest 9 + ChatModuleE2ETest 8 = 共 92 用例）
- ✅ AI 模块集成测试（AiLlmServiceTest 9 用例覆盖 generate/analyzeVisit/dailyReport/extractTodos/scoreVisit/smartFill 及容错）
- ✅ 聊天模块端到端测试（ChatModuleE2ETest 8 用例覆盖消息持久化/撤回窗口/引用/离线同步/优先级/提及/@/搜索/ACK）
- 📋 性能压测 — 收口：k6 脚本已就绪（`scripts/perf/api-smoke.js`），需在运行环境中执行，可选 TOKEN 认证
- ✅ 安全审计（SECURITY-AUDIT.md：认证授权/输入验证/敏感数据/通信安全/依赖安全/AI 安全/性能 7 类 30+ 检查项）

### 4.2 DevOps [P2]
- ✅ GitHub Actions CI 配置（ci.yml：后端 Maven 构建+测试 + 前端 npm lint+type-check+build，MySQL/Redis 服务容器）
- ✅ GitHub Actions CD 配置（cd.yml：前端构建+后端打包+Docker 镜像构建推送 GHCR，tag 触发+手动触发）
- ✅ 生产环境部署文档（DEPLOYMENT.md：系统要求/Docker Compose 快速部署/手动部署/Nginx 配置/数据库备份/Flyway/监控/FAQ/升级）

### 4.3 文档交付 [P1]
- ✅ API 接口文档（API-REFERENCE.md：18 个模块 100+ 端点，含认证/客户/项目/佣金/聊天/AI/表格/搜索/文档/日历/自动化/待办/日报/质检/模板/渠道/统计/系统管理）
- ✅ 部署运维手册（DEPLOYMENT.md）
- ✅ 编写用户使用手册（USER-MANUAL.md：15 章覆盖工作台/客户/项目/佣金/回款/即时通讯/智能表格/云文档/销售工具箱/AI 助手/系统管理/移动端/FAQ）
- ✅ 更新技术架构方案至最终版（ARCHITECTURE.md：9 章含系统总览/后端架构/前端架构/AI 架构/部署架构/性能设计/安全架构/监控运维/扩展性设计）

---

## 依赖关系图

```
Phase 1 (基础加固, 第1-4周)
├── 1.1 安全配置 ──────────────────────────────────────────────┐
├── 1.2 Spring AI 升级 ─────┬── 1.3 RAG 激活                   │
│                           └── 1.4 会话记忆升级                 │
├── 1.5 聊天后端 ──── 1.6 聊天前端                               │
└── 1.7 基础设施 ──────────────────────────────────────────────┘
                              │
Phase 2 (功能构建, 第5-12周)   ↓
├── 2.1 智能表格后端 ─┬── 2.2 智能表格前端
│                     └── 2.3 模板中心
├── 2.4 文件预览 ──────────────────────────────────────────┐
├── 2.5 富文本编辑器                                        │
├── 2.6 AI 编排升级 ──── 2.7 AI 管理后台                    │
├── 2.8 AI 记忆系统 ──── 2.9 全局智能搜索                    │
├── 2.10 智能工作台                                         │
└── 2.11 消息智能优先级                                      │
                              │                             │
Phase 3 (产品进化, 第13-22周)  ↓                             ↓
├── 3.1 云文档 ──── 3.2 OnlyOffice ──── 3.12 场景模板库
├── 3.3 多渠道接入
├── 3.4 AI 能力深化（多模态/语音/Heartbeat/A2A）
├── 3.5 销售拜访AI分析 ──── 3.6 销售过程AI质检
├── 3.7 销售自动化规则引擎
├── 3.8 经营分析大屏
├── 3.9 AI 日报生成
├── 3.10 销售日历
├── 3.11 聊天待办提取
├── 3.13 人事与绩效（档案/异动/KPI 周期/模板/考核记录）
└── 3.14 通用项目管理（项目空间/任务/依赖/里程碑/看板/甘特/日历）
                              │
Phase 4 (打磨交付, 第23-26周)  ↓
├── 4.1 质量保障
├── 4.2 DevOps
└── 4.3 文档交付
```

---

## 编译环境说明

- **推荐 JDK**：17（与 `java.version` 一致）。
- 若出现 `com.sun.tools.javac.code.TypeTag :: UNKNOWN` 或 `sun.misc.Unsafe` 相关错误，多为 **Lombok 与高版本 JDK（如 21/24）不兼容**。处理方式：
  1. 使用 **JDK 17** 编译：`JAVA_HOME=<jdk17> mvn compile`；
  2. 或升级 Lombok 至更新版本（根 POM 中 `lombok.version` 当前为 1.18.36）。

---

## 收口说明（V3.0 交付前）

- **📋 标记项**：表示功能代码或脚本已完成，需在目标运行/部署环境中完成验证后即可视为闭环。
- **环境验证清单**：见 `doc/RELEASE-CHECKLIST-V3.0.md`，包含编译、测试、部署、AI/RAG/文件预览/OnlyOffice、性能压测等检查项。
- **发布建议**：完成 RELEASE-CHECKLIST 中「必选」项后即可发布 V3.0；「可选」项可在上线后按需验证。
