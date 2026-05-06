# MasterLife V3.0 全面升级方案

**版本**: V3.0  
**日期**: 2026-03-02  
**负责人**: 朋诚科技  
**状态**: 执行中（Phase 1 已启动）

---

## 一、升级总纲

### 1.1 战略定位

将 MasterLife 从"房地产销售管理后台"升级为 **"AI 驱动的房地产智能协作平台"**，以 **AgentScope + Spring AI Alibaba** 为 AI 基座，借鉴钉钉、飞书、CoPaw 的产品理念，覆盖 **智能办公 + 销售业务 + AI 赋能** 三大核心能力。

### 1.2 核心原则

| 原则 | 说明 |
|------|------|
| **AI Native** | AgentScope（多智能体编排）+ Spring AI Alibaba（Java 原生 AI 框架）构成双引擎基座，两个框架中项目尚未使用的能力应按需集成或借鉴，所有业务模块可被 AI 感知和调用 |
| **兼容优先** | 升级过程保持 API 向后兼容，数据库增量迁移（不破坏现有数据），前端渐进式重构 |
| **可靠第一** | 可靠性的首要目标是 **正面解决问题**：优先通过架构优化、代码重构、性能调优等手段消除故障根因，而非简单绕过；降级只作为 **最后兜底手段**，仅在确认重构和优化均无法在合理时间内解决时才启用，且降级逻辑本身不得拖慢主流程——任何保护机制（降级/熔断/告警）的引入都必须经过性能评估，确保不影响项目整体的响应速度和吞吐量 |
| **安全合规** | 敏感信息加密存储，API Key 环境变量化，RBAC + 数据权限 + 工具权限三重控制 |
| **响应优先** | P95 接口响应 < 500ms，AI 流式输出首 Token < 2s，WebSocket 消息到达 < 200ms；性能是硬约束，任何新功能引入都不得突破这些指标 |

#### 1.2.1 可靠性处理优先级

遇到功能不稳定或调用失败时，按以下优先级逐级处理，**不可跳级**：

```
Level 1 [首选] → 根因修复：定位并修复代码 Bug、架构缺陷、资源瓶颈
Level 2 [次选] → 重构优化：重新设计调用链路、引入缓存/异步/批处理，从根本上消除不稳定因素
Level 3 [再次] → 冗余兜底：多模型双活、主备切换、重试策略（限次限频，不阻塞主线程）
Level 4 [最后] → 降级熔断：仅在 Level 1~3 确认无法在合理周期内解决时，启用降级/熔断保护
```

**降级的硬约束**：
- 降级判断逻辑必须异步或预计算，**禁止在请求主路径上引入同步阻塞检查**
- 熔断器的状态检查开销 < 1ms，不得成为性能瓶颈
- 降级后的备用逻辑（如规则引擎替代 AI）必须满足同样的响应时间要求
- 每个降级点必须有监控指标，触发降级后自动上报告警，推动 Level 1~3 的根因修复

### 1.3 与 V2.0 的关系

V3.0 是 V2.0 升级计划（UPGRADE-PLAN-2.0.md）和 AgentScope B 方案（agentscope-b-upgrade-plan.md）的 **统一延续与全面升级**。V2.0 中已完成的工作（编排层、A/B 实验、告警投递等）作为基线保留，V3.0 在此基础上升级框架版本、补齐功能短板、引入新能力。

---

## 二、现状评估与差距分析

### 2.1 AI 框架现状

| 维度 | 当前状态 | 目标状态 | 差距 |
|------|----------|----------|------|
| Spring AI Alibaba 版本 | 1.0.0-M6.1（早期里程碑） | 1.1.2.0（GA 稳定版） | **跨 5 个大版本** |
| LLM 模型 | 智谱 GLM-4-Flash（OpenAI 兼容） | DashScope Qwen 系列 + 智谱双活 | 模型策略升级 |
| EmbeddingModel | 8 维哈希 stub（不可用） | DashScope text-embedding-v3 | **RAG 不可用** |
| VectorStore | SimpleVectorStore（内存） | PGVector（PostgreSQL 持久化） | **知识库不可用** |
| Agent 编排 | 自建编排层（~1500 行代码） | Spring AI Agent Framework + StateGraph | 可简化 60% 代码 |
| 会话记忆 | 自建 Redis 记忆（~220 行） | ChatMemory + MessageChatMemoryAdvisor | 可用标准方案替代 |
| AgentScope | REST API 调用，默认禁用 | AgentScope Runtime 深度集成 + A2A 协议 | 集成深度不足 |
| MCP | 未使用 | MCP Server 暴露业务工具 | 全新引入 |

### 2.2 功能模块现状

| 模块 | 完成度 | 核心差距 |
|------|--------|----------|
| 聊天通讯 | 60% | 无消息撤回、Web 端无历史分页、无未读角标、无消息通知、无语音 |
| 智能表格 | 15% | 纯前端 Demo，无后端、无动态字段、无多视图、无数据持久化 |
| 文件管理 | 70% | 基础功能完善，Office 预览依赖外网、无在线编辑、无版本管理 |
| 云文档 | 0% | 完全缺失 |
| 富文本 | 0% | 完全缺失 |
| Office 处理 | 10% | 文档解析 stub，无 docx/xlsx/pptx/pdf 解析能力 |
| 多渠道接入 | 0% | 无钉钉/飞书/企业微信机器人 |

### 2.3 非功能性现状

| 维度 | 当前状态 | 风险等级 |
|------|----------|----------|
| 安全 | API Key 明文、生产环境 root+弱密码 | 🔴 高 |
| 部署 | 无 Dockerfile、无 CI/CD | 🟡 中 |
| 测试 | 仅 1 个测试文件 | 🟡 中 |
| 数据库迁移 | 手动 SQL 脚本 | 🟡 中 |
| WebSocket | 单点、无消息确认、无序列号 | 🟡 中 |

---

## 三、AI 基座架构设计

### 3.1 双引擎架构

```
┌─────────────────────────────────────────────────────────────┐
│                    业务应用层 (Business)                       │
│  客户 CRM · 项目管理 · 佣金结算 · 考勤 · 审批 · 协作办公        │
├─────────────────────────────────────────────────────────────┤
│                    AI 服务层 (AI Services)                    │
│  智能判客 · 报表问答 · 文案生成 · 知识库 · 文档分析              │
├─────────────┬───────────────────────────────┬───────────────┤
│  Agent 编排  │  Spring AI Agent Framework    │  AgentScope   │
│  & 工作流    │  (StateGraph / ReAct Agent)   │  Runtime      │
│             │  ↕ A2A 协议 ↕                  │  (多智能体)    │
├─────────────┴───────────────────────────────┴───────────────┤
│                    AI 核心层 (AI Core)                        │
│  ChatClient · ChatMemory · MCP · ToolCalling · Streaming    │
├─────────────────────────────────────────────────────────────┤
│                    模型接入层 (Model Provider)                │
│  DashScope (Qwen) · 智谱 (GLM) · OpenAI 兼容 · 本地模型      │
├─────────────────────────────────────────────────────────────┤
│                    基础设施层 (Infrastructure)                │
│  MySQL · Redis · PGVector · MinIO/OSS · WebSocket           │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Spring AI Alibaba 升级计划

**目标版本**: `1.1.2.0`

| 新功能 | 引入计划 | 替换目标 |
|--------|----------|----------|
| **Agent Framework** | Phase 1 | 替换自建 `AgentOrchestratorService` + `AgentRouteService` |
| **StateGraph** | Phase 1 | 替换分散的路由/权限/执行 if-else 逻辑 |
| **ChatMemory** | Phase 1 | 替换自建 `ConversationMemoryService` |
| **DashScope EmbeddingModel** | Phase 1 | 替换 8 维哈希 stub，激活真实 RAG |
| **PGVector AutoConfiguration** | Phase 1 | 替换 SimpleVectorStore |
| **MCP Server** | Phase 2 | 将 ReportQuery/CustomerQuery/CommissionCalc 暴露为标准 MCP Tool |
| **A2A 协议** | Phase 2 | 标准化 Agent 间通信，替换自定义 REST 协议 |
| **多模态** | Phase 3 | 图片理解（户型图/证件识别） |
| **Structured Output** | Phase 2 | 替换手动 JSON 解析 |

#### 升级兼容性策略

```
1. 保留现有 A/B 实验平台 → 实验 API 不变，底层切换为新 Agent Framework
2. 保留降级策略接口 → AI 调用失败仍回退到规则引擎
3. 配置文件渐进迁移 → 新旧配置通过 Profile 隔离
4. 版本升级分两步：
   Step 1: 升级 BOM 版本 + 修复编译错误
   Step 2: 逐模块替换自建组件为 Spring AI 标准组件
```

### 3.3 AgentScope 深度集成计划

**当前**：仅用于意图路由（REST API，默认禁用）  
**目标**：作为多智能体协作引擎，与 Spring AI Alibaba 通过 A2A 协议协同

| 能力 | 集成方式 | 用途 |
|------|----------|------|
| AgentScope Runtime | REST API / agentscope-runtime-java SDK | 复杂多 Agent 任务编排 |
| Skill 扩展（借鉴 CoPaw） | 自定义 Skill 注册与管理 | AI 能力热插拔 |
| Heartbeat 机制（借鉴 CoPaw） | 定时任务触发 Agent | 客户跟进提醒、佣金结算提醒 |
| Memory 管理（借鉴 ReMe） | 长期记忆持久化 | 客户画像积累、跨会话记忆 |
| 多渠道接入（借鉴 CoPaw） | Webhook + Bot API | 钉钉/飞书/企业微信消息推送 |

### 3.4 CoPaw 理念借鉴

| CoPaw 理念 | 在本项目中的实现 |
|------------|-----------------|
| **Every Channel** | 客户通知、审批消息推送至钉钉/飞书/企业微信 |
| **Under Your Control** | 所有 AI 能力可在管理后台配置开关，数据不出域 |
| **Skills** | Agent 工具（判客/报表/佣金/知识库）作为可管理的 Skill |
| **Heartbeat** | 定时巡检：未跟进客户提醒、佣金结算到期、合同到期预警 |
| **Memory** | 客户画像长期记忆 + 销售经验知识库 |

### 3.5 AI 框架能力全景分析与集成规划

以下对 AgentScope 生态和 Spring AI Alibaba 两大框架的 **全部能力** 进行逐项分析，明确当前使用状态、项目需求匹配度、以及集成/借鉴方案。

#### 3.5.1 Spring AI Alibaba 能力全景

| 能力 | 当前状态 | 是否需要 | 集成/借鉴方案 | 阶段 |
|------|----------|----------|---------------|------|
| **ChatClient / ChatModel** | ✅ 已使用（智谱 GLM） | 必需 | 保持，升级版本后支持更多 Provider | Phase 1 |
| **Streaming (SSE)** | ✅ 已使用 | 必需 | 保持 | — |
| **Function Calling / ToolCallback** | ✅ 已使用（3 个工具） | 必需 | 保持，新增工具统一通过 ToolCallback 注册 | — |
| **Agent Framework (ReAct Agent)** | ❌ 未使用 | **强需** | 替换自建的 `AgentOrchestratorService`（~1500 行），用 `ToolCallingAgent` 或 `ReActAgent` 实现自动工具选择与执行，大幅减少编排层代码量 | Phase 1 |
| **StateGraph (工作流引擎)** | ❌ 未使用 | **强需** | 将当前散落在 `AgentRouterService`、`CompositeRouteAgentClient`、`AgentRouteService` 中的 if-else 流程控制建模为声明式有向图：`路由节点 → 权限校验节点 → 工具执行节点 → 审计节点 → 记忆节点`，支持条件分支和并行执行 | Phase 2 |
| **ChatMemory / Advisor** | ❌ 未使用（自建 Redis 记忆） | **强需** | 用 `MessageChatMemoryAdvisor` 替换自建的 `ConversationMemoryService`（~220 行），实现标准化的上下文管理；底层 `ChatMemory` 接口对接 Redis 持久化 | Phase 1 |
| **DashScope Starter (原生)** | ❌ 排除了自动配置 | **强需** | 移除排除配置，启用 DashScope 原生接入；配置 Qwen 系列模型作为主力，智谱作为备用，实现多模型双活 | Phase 1 |
| **DashScope EmbeddingModel** | ❌ 用 stub 替代 | **强需** | 启用 `text-embedding-v3`，这是激活真实 RAG 能力的前提 | Phase 1 |
| **PGVector Store** | ❌ 排除了自动配置 | **强需** | 启用 PGVector 自动配置，替换内存 `SimpleVectorStore`，实现向量数据持久化 | Phase 1 |
| **MCP Server/Client** | ❌ 未使用 | **需要** | 将 ReportQueryFunction、CustomerQueryFunction、CommissionCalcFunction 暴露为标准 MCP Tool，支持跨服务 Agent 发现和调用；后续新增的智能表格查询、文档搜索等也注册为 MCP Tool | Phase 2 |
| **A2A (Agent-to-Agent)** | ❌ 未使用 | **需要** | 替换自定义的 `agentscope.route.v1` REST 协议，用 A2A 标准协议实现 Spring AI Agent 与 AgentScope Agent 之间的通信；借助 Nacos 实现 Agent 注册发现（可选） | Phase 2 |
| **Structured Output** | ❌ 未使用（手动解析 JSON） | **需要** | 用 `BeanOutputConverter` / `MapOutputConverter` 替换手动 JSON 解析，减少解析失败风险，提高代码可维护性 | Phase 2 |
| **Prompt Template** | ❌ 排除了自动配置 | **需要** | 启用 PromptTemplate，将硬编码的系统提示词模板化管理，支持通过管理后台动态修改提示词，无需重启服务 | Phase 2 |
| **多模态 (Vision)** | ❌ 未使用 | **需要** | 对接 DashScope 视觉模型，实现户型图分析（从图片中提取房型、面积、朝向等信息）、证件 OCR（身份证/营业执照识别）、合同文档图片识别 | Phase 3 |
| **语音 (Audio)** | ❌ 未使用 | **需要** | 对接 DashScope Paraformer ASR 实现语音转文字；聊天消息支持语音消息发送与转写；会议纪要语音转文字 | Phase 3 |
| **QuestionAnswerAdvisor** | ❌ 未使用 | 可选 | 借鉴其 RAG 检索 + 生成的标准化流程，简化现有 KnowledgeBaseService 中的手动 Prompt 拼装逻辑 | Phase 2 |
| **VectorStoreRetriever** | ❌ 未使用 | 可选 | 借鉴其检索策略（Top-K + 相似度阈值 + 元数据过滤），增强知识库检索精度 | Phase 2 |
| **DocumentReader (Tika/PDF/JSON)** | ❌ 未使用（自建 stub） | **强需** | 引入 Spring AI 的 `TikaDocumentReader` 或直接用 Apache Tika，替换当前只能读纯文本的 `DocumentProcessor` | Phase 1 |
| **TokenTextSplitter** | ⚠️ 部分使用 | 必需 | 保持，优化切片策略（chunk size / overlap 参数调优） | Phase 1 |
| **ETL Pipeline** | ⚠️ 部分使用 | 必需 | 完善 Reader → Transformer → Writer 全流程 | Phase 1 |
| **Observation (可观测性)** | ❌ 未使用 | 可选 | Spring AI 内置 Micrometer 观测，可对接 Prometheus + Grafana 监控 AI 调用延迟、Token 消耗、错误率；Phase 4 性能优化时引入 | Phase 4 |
| **Spring AI Alibaba Admin** | ❌ 未使用 | 可借鉴 | 参考其可视化 Agent 开发、运行时可观测性、评估功能的设计理念，优化本项目的 AI 管理后台 | Phase 2 |
| **Spring AI Alibaba Studio** | ❌ 未使用 | 可借鉴 | 参考其嵌入式调试 UI 设计，在开发环境提供 Agent 调试面板 | Phase 3 |

#### 3.5.2 AgentScope 生态能力全景

| 能力 | 来源 | 当前状态 | 是否需要 | 集成/借鉴方案 | 阶段 |
|------|------|----------|----------|---------------|------|
| **AgentScope Core (Python)** | agentscope | ❌ 不直接集成 | 借鉴 | Python 框架不直接集成到 Java 后端，但借鉴其 Agent 设计理念（消息驱动、Pipeline 编排、Memory 管理） | — |
| **AgentScope Runtime** | agentscope-runtime | ⚠️ 仅用于路由，默认禁用 | **需要** | 深度集成：启用 Runtime 作为复杂多 Agent 任务的执行引擎；通过 REST API 调用，由 Resilience4j 保护调用链路；适用于需要多轮多 Agent 协作的复杂场景（如：客户全景分析 = 数据 Agent + 画像 Agent + 风险 Agent 协同） | Phase 2 |
| **AgentScope Runtime Java** | agentscope-runtime-java | ❌ 未使用 | **强需** | 这是 Java 原生的 Runtime 实现，比 REST API 调用 Python Runtime 更适合本项目；提供安全的工具沙箱、Agent-as-a-Service API、可扩展的部署；优先评估替代当前 REST 方式 | Phase 2 |
| **AgentScope Java** | agentscope-java | ❌ 未使用 | **需要** | Java 版 Agent 编程框架，可与 Spring AI Alibaba 互补：Spring AI 处理标准 LLM 交互，AgentScope Java 处理复杂多 Agent 编排逻辑 | Phase 2 |
| **CoPaw — Skill 机制** | CoPaw | ❌ 未使用 | **强需** | 借鉴 CoPaw 的 Skill 架构设计 Agent 工具的热插拔能力：每个 AgentTool 作为独立 Skill，支持在管理后台动态启用/禁用/配置参数，无需重启服务 | Phase 2 |
| **CoPaw — Heartbeat** | CoPaw | ❌ 未使用 | **需要** | 借鉴定时心跳巡检机制，用 `@Scheduled` + Agent 实现：每日客户跟进提醒、佣金结算到期预警、合同到期通知、考勤异常检测 | Phase 3 |
| **CoPaw — Multi-Channel** | CoPaw | ❌ 未使用 | **需要** | 借鉴多渠道接入架构，统一抽象 `ChannelPushService`，支持钉钉/飞书/企业微信/邮件等渠道推送 | Phase 3 |
| **CoPaw — Console UI** | CoPaw | ❌ 未使用 | 可借鉴 | 参考其 Agent 对话管理界面设计，优化本项目的 AI 对话页面交互体验 | Phase 2 |
| **CoPaw — Cron 任务** | CoPaw | ❌ 未使用 | 可借鉴 | 借鉴其 Cron 与 AI 结合的理念：定时触发 Agent 执行特定任务，结果推送到指定渠道 | Phase 3 |
| **CoPaw — 本地模型** | CoPaw | ❌ 未使用 | 可选 | 如有私有化部署需求（数据不出域），可借鉴其 llama.cpp/MLX 本地模型集成方案 | Phase 3+ |
| **ReMe — 长期记忆** | ReMe | ❌ 未使用 | **需要** | 借鉴 ReMe 的记忆管理策略：将客户画像、交互历史、销售经验等作为长期记忆持久化到数据库；实现跨会话的上下文延续（如：上次跟进了 A 客户，下次提到 A 时自动关联历史） | Phase 3 |
| **ReMe — 记忆精炼** | ReMe | ❌ 未使用 | 可选 | 借鉴 "Refine Me" 理念：定期对长期记忆进行压缩和提炼，保留关键信息、丢弃冗余细节，控制记忆库大小 | Phase 3 |
| **OpenJudge — 评估框架** | OpenJudge | ❌ 未使用 | 可选 | 借鉴其 AI 质量评估框架，用于评估 RAG 回答质量、文案生成质量、Agent 路由准确率 | Phase 4 |
| **Trinity-RFT — 模型微调** | Trinity-RFT | ❌ 未使用 | 可选 | 如果通用模型在房产领域表现不足，可借鉴其强化微调框架，对 Qwen 进行房产领域微调 | Phase 4+ |
| **TuFT — 本地微调** | TuFT | ❌ 未使用 | 可选 | 多租户本地模型微调，适合需要为不同楼盘/经销商定制 AI 行为的场景 | Phase 4+ |

#### 3.5.3 集成策略总结

**直接集成**（引入依赖/SDK，深度耦合）：

| 组件 | 理由 |
|------|------|
| Spring AI Alibaba Agent Framework | Java 原生，与项目技术栈完全匹配，可直接替换自建编排层 |
| Spring AI Alibaba StateGraph | 声明式工作流图，比 if-else 更清晰可维护 |
| Spring AI ChatMemory + Advisor | 标准化会话记忆，减少维护成本 |
| DashScope Starter（Qwen 模型） | 国内首选模型提供商，延迟低、中文能力强 |
| PGVector Store | 项目已有 PostgreSQL 规划，自然选择 |
| MCP Server | 标准化工具暴露，为未来多 Agent 协作打基础 |
| Apache Tika | 文档解析业界标准，格式覆盖最广 |
| AgentScope Runtime Java | Java 原生 Runtime，比调用 Python REST API 更可靠 |

**借鉴理念**（不引入源码，参考设计模式和产品思路）：

| 来源 | 借鉴点 | 本项目实现方式 |
|------|--------|----------------|
| CoPaw Skill | 工具热插拔 | `@ConditionalOnProperty` + 管理后台开关 |
| CoPaw Heartbeat | 定时 AI 巡检 | Spring `@Scheduled` + Agent 任务 |
| CoPaw Channel | 多渠道统一抽象 | `ChannelPushService` 策略工厂 |
| ReMe Memory | 长期记忆管理 | MySQL/Redis 持久化 + 定期精炼 |
| CoPaw Console | AI 对话 UI | 优化前端 AI 聊天页面交互 |
| AgentScope Core | 消息驱动 Agent | 参考其 Pipeline/Message 设计模式 |
| Spring AI Admin | 可视化 Agent 管理 | 参考其管理面板 UI/UX 设计 |
| OpenJudge | AI 质量评估 | Phase 4 引入评估指标体系 |

**暂不集成**（当前项目规模和阶段不需要）：

| 组件 | 理由 | 何时可能需要 |
|------|------|-------------|
| Trinity-RFT / TuFT（模型微调） | 当前使用通用模型已能满足需求，微调成本高 | 通用模型在房产领域表现显著不足时 |
| 本地模型（llama.cpp/MLX） | 当前无严格的数据不出域需求 | 有政企客户要求私有化部署时 |
| A2A + Nacos | 当前单体架构不需要分布式 Agent 发现 | 拆分为微服务架构时 |
| AgentScope Spark Design | UI 组件库，本项目已使用 Naive UI | 不需要 |

### 3.6 双框架功能重叠分析与选择决策

AgentScope 和 Spring AI Alibaba 在 **7 个核心维度** 上存在功能重叠或潜在冲突。以下逐一分析并给出明确的选择决策，确保每个能力点只有一个"主责方"，避免重复建设和运行时冲突。

#### 3.6.1 重叠能力对照表

| # | 重叠维度 | Spring AI Alibaba | AgentScope | 重叠程度 | **选择** | 选择理由 |
|---|----------|-------------------|------------|----------|----------|----------|
| 1 | **LLM 模型调用** | ChatClient / ChatModel，支持 DashScope、OpenAI 等多 Provider | AgentScope 自带模型调用（ModelWrapper），支持 DashScope、OpenAI 等 | 🔴 高度重叠 | **Spring AI Alibaba** | Java 原生、与 Spring 生态深度集成、类型安全、Advisor 链可扩展；AgentScope 的模型调用为 Python 体系，Java 版仅做透传 |
| 2 | **Agent 编排** | Agent Framework（ReActAgent / ToolCallingAgent）+ StateGraph | AgentScope Pipeline + Sequential/Parallel Agent 编排 | 🔴 高度重叠 | **分场景选择**（见下文 3.6.2） | 两者各有优势场景，不能一刀切 |
| 3 | **工具调用 (Tool/Function)** | ToolCallback / FunctionToolCallback，声明式注册 | AgentScope ServiceToolkit，Python 装饰器注册 | 🔴 高度重叠 | **Spring AI Alibaba** | Java 项目中工具本身就是 Java 代码，用 ToolCallback 注册零额外开销；通过 AgentScope 调用需要跨语言序列化 |
| 4 | **会话记忆** | ChatMemory 接口 + MessageChatMemoryAdvisor，支持 Redis/JDBC 持久化 | AgentScope Memory 模块 / ReMe（独立项目） | 🟡 中度重叠 | **分层选择**（见下文 3.6.3） | 短期记忆用 Spring AI，长期记忆借鉴 ReMe |
| 5 | **意图路由** | Spring AI 可通过 Prompt + Tool 实现路由；StateGraph 条件分支 | AgentScope Router Agent / 自定义路由协议 | 🟡 中度重叠 | **Spring AI Alibaba** | 路由决策本质是一次 LLM 调用 + 条件分支，Spring AI 的 StateGraph 条件节点足以覆盖；AgentScope 路由需要额外的 REST 调用开销 |
| 6 | **可观测性** | Spring AI Micrometer 观测 + Spring AI Alibaba Admin 可视化 | AgentScope Studio 可视化调试 | 🟢 低度重叠 | **Spring AI Alibaba** | 与 Spring Boot Actuator / Prometheus / Grafana 技术栈一致；AgentScope Studio 为 Python Web 服务，额外引入运维成本 |
| 7 | **MCP / 工具协议** | MCP Server/Client（标准协议，Google/Anthropic 推动） | AgentScope 自定义 Tool 协议 | 🟢 低度重叠 | **Spring AI MCP** | MCP 是行业标准化趋势，跨框架兼容性强；AgentScope 自定义协议仅在其生态内通用 |

#### 3.6.2 Agent 编排：分场景选择策略

这是两个框架重叠最严重、也是最需要明确边界的能力。核心原则：**单 Agent 任务用 Spring AI，多 Agent 协作用 AgentScope**。

```
┌─────────────────────────────────────────────────────────────────┐
│                     用户请求进入                                  │
│                         │                                       │
│                    意图路由（Spring AI StateGraph）                │
│                    ┌────┴────┐                                  │
│               简单任务     复杂任务                                │
│                 │            │                                   │
│    ┌────────────┴──┐    ┌───┴──────────────┐                   │
│    │ Spring AI      │    │ AgentScope       │                   │
│    │ Agent Framework│    │ Runtime (Java)   │                   │
│    │                │    │                  │                   │
│    │ · 单工具调用    │    │ · 多 Agent 协作   │                   │
│    │ · 单步 ReAct   │    │ · 多轮交互编排    │                   │
│    │ · 标准问答     │    │ · 复杂推理链      │                   │
│    │ · 文案生成     │    │ · Agent 间消息传递 │                   │
│    │ · RAG 检索     │    │ · 动态 Agent 组合 │                   │
│    └────────────────┘    └──────────────────┘                   │
│                 │            │                                   │
│                 └────┬───────┘                                  │
│                 统一结果返回                                      │
└─────────────────────────────────────────────────────────────────┘
```

| 场景 | 选择 | 具体示例 | 理由 |
|------|------|----------|------|
| 单工具调用 | Spring AI | 查询报表、计算佣金、生成文案 | ToolCallingAgent 直接调用 Java 方法，零网络开销，延迟最低 |
| RAG 知识库问答 | Spring AI | 上传文档 → 检索 → 生成回答 | Spring AI 原生 VectorStore + Advisor 链，全流程一体化 |
| 简单 ReAct 推理 | Spring AI | "帮我分析一下 A 客户的成交概率" | ReActAgent 单次思考-行动-观察循环，响应快 |
| 多 Agent 协作分析 | AgentScope | "全面分析 A 客户"（数据 Agent + 画像 Agent + 风险 Agent 协同） | 需要多个独立 Agent 各自分析后汇总，AgentScope 的 Pipeline/消息传递机制更成熟 |
| 复杂审批流 + AI | AgentScope | 合同审批：法律合规 Agent 审查 → 财务 Agent 核算 → 汇总报告 | 多角色多轮交互，AgentScope 的 Agent 角色设定和消息路由更灵活 |
| Agent 间动态协商 | AgentScope | 多个经销商的佣金方案对比（每个方案由独立 Agent 模拟） | AgentScope 支持 Agent 间双向通信和协商机制 |

**冲突预防**：
- 两个框架 **不同时处理同一个请求**，由 StateGraph 入口节点统一路由
- Spring AI 的 ToolCallback 和 AgentScope 的 Tool **不重复注册同一业务方法**，统一通过 MCP 协议暴露
- 两个框架共享同一个 Redis 会话存储，但使用不同的 key 前缀（`sai:memory:` / `as:memory:`）

#### 3.6.3 会话记忆：分层选择策略

| 记忆层级 | 选择 | 实现方式 | 数据生命周期 |
|----------|------|----------|-------------|
| **工作记忆**（当前对话上下文） | Spring AI ChatMemory | `MessageChatMemoryAdvisor` + Redis 持久化 | 会话级别，超时自动清理 |
| **短期记忆**（近期交互摘要） | Spring AI ChatMemory | 上下文窗口压缩 + 摘要提取，存入 Redis | 小时/天级别 |
| **长期记忆**（客户画像/经验） | 借鉴 ReMe 自建 | MySQL/PGVector 持久化，定期精炼 | 永久保留，定期精炼 |

**不使用 AgentScope Memory 的理由**：AgentScope 的 Memory 模块为 Python 实现，Java 版本功能有限；ReMe 虽然也是 Python，但其 **设计理念**（记忆分层、定期精炼、重要度衰减）可以用 Java 重新实现，更可控。

#### 3.6.4 最终职责边界划分

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  Spring AI Alibaba 负责（主引擎，覆盖 80% 场景）                      │
│  ─────────────────────────────────────────                          │
│  · LLM 调用（ChatClient / ChatModel）                               │
│  · 工具注册与调用（ToolCallback → MCP Tool）                         │
│  · 会话记忆（ChatMemory + Advisor）                                  │
│  · RAG 全流程（DocumentReader → Embedding → VectorStore → Retrieval）│
│  · 简单 Agent 编排（ReActAgent / ToolCallingAgent）                  │
│  · 工作流控制（StateGraph 条件分支/并行/循环）                        │
│  · 意图路由（StateGraph 入口节点 + LLM 路由决策）                     │
│  · 流式输出（Streaming SSE）                                        │
│  · 结构化输出（Structured Output）                                   │
│  · 可观测性（Micrometer + Actuator）                                │
│  · MCP 协议（工具标准化暴露）                                        │
│  · Prompt 模板管理                                                  │
│  · 多模态/语音（DashScope 原生）                                     │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  AgentScope 负责（辅引擎，覆盖 20% 复杂场景）                         │
│  ────────────────────────────────────────                           │
│  · 多 Agent 协作编排（多角色对话/协商/投票）                          │
│  · 复杂推理链（多 Agent 接力、分支合并）                              │
│  · Agent 动态组合（运行时根据任务类型组装 Agent 团队）                 │
│  · 沙箱执行（安全隔离的工具执行环境，通过 Runtime Java）               │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  借鉴理念自建（不直接使用任何一个框架的实现）                          │
│  ──────────────────────────────────────────                          │
│  · 长期记忆（借鉴 ReMe，Java 自建）                                  │
│  · Skill 热插拔（借鉴 CoPaw，Spring @ConditionalOnProperty 实现）    │
│  · Heartbeat 巡检（借鉴 CoPaw，Spring @Scheduled 实现）             │
│  · 多渠道推送（借鉴 CoPaw，策略工厂模式自建）                        │
│  · A/B 实验平台（保留现有自建实现，已经非常成熟）                      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### 3.6.5 冲突场景与规避方案

| 潜在冲突 | 冲突描述 | 规避方案 |
|----------|----------|----------|
| **模型调用重复** | 同一请求可能同时触发 Spring AI 和 AgentScope 的模型调用，造成 Token 浪费和延迟叠加 | 统一入口：所有请求先经过 Spring AI StateGraph 路由节点，明确分发到 Spring AI 或 AgentScope，**不允许两者串联调用同一模型** |
| **工具注册重复** | 同一个业务方法（如查询客户）同时注册为 Spring AI ToolCallback 和 AgentScope ServiceToolkit | 统一出口：所有业务工具只通过 **MCP Server 暴露一次**，Spring AI 通过 MCP Client 调用，AgentScope 通过 HTTP 调用 MCP 端点 |
| **记忆存储冲突** | Spring AI ChatMemory 和 AgentScope Memory 同时写入 Redis，key 冲突或数据不一致 | key 隔离：`pengcheng:sai:chat:{sessionId}` vs `pengcheng:as:agent:{taskId}`；两者不交叉读取对方的记忆 |
| **配置冲突** | 两个框架都可能注册同名的 Spring Bean（如 `chatClient`、`embeddingModel`） | Spring AI Alibaba 的自动配置为主，AgentScope Java 使用独立的配置类（`@Configuration` + `@Qualifier` 隔离）；AgentScope Runtime 作为独立服务不存在 Bean 冲突 |
| **版本依赖冲突** | 两个框架依赖不同版本的 spring-ai-core、jackson、netty 等底层库 | Spring AI Alibaba BOM 统一管理版本；AgentScope Runtime Java 如有冲突依赖，通过 Maven `<exclusions>` 排除并对齐版本 |
| **超时和线程池竞争** | 两个框架各自创建 HTTP 连接池和线程池，可能互相抢占资源 | 资源隔离：Spring AI 使用 Spring 默认 WebClient 线程池；AgentScope Runtime 调用使用独立的 `@Async("agentscopeExecutor")` 线程池，配置 `corePoolSize` 和 `maxPoolSize` 上限 |
| **响应时间叠加** | 请求先经过 Spring AI 路由，再转发 AgentScope，链路过长导致延迟超标 | 并行而非串联：复杂任务直接路由到 AgentScope，不经过 Spring AI Agent 二次处理；简单任务直接 Spring AI 处理，不经过 AgentScope |

#### 3.6.6 性能影响评估

| 架构方案 | 延迟影响 | 适用场景 |
|----------|----------|----------|
| **纯 Spring AI** 处理 | 基线延迟（最快） | 80% 的日常场景 |
| **Spring AI 路由 → AgentScope 执行** | +50~200ms（REST 调用开销） | 复杂多 Agent 场景 |
| ~~两者串联处理~~ | ~~延迟翻倍（禁止）~~ | ~~禁止使用~~ |

**硬性约束**：
- 单次请求最多经过 **一个** AI 编排引擎，不允许两者串联
- AgentScope 调用必须设置超时上限（默认 30s，可配置），超时后返回部分结果而非无限等待
- 路由决策（选择用哪个引擎）本身的耗时 < 50ms（基于规则或轻量 LLM 调用）

### 3.7 AI 记忆系统架构设计

基于对 awesome-ai-memory 生态 20+ 个开源项目的深度分析（见《AI 长期记忆开源项目深度分析报告》）和 OpenViking / OpenClaw / ZeroClaw 三大项目的补充分析（见《AI 记忆项目补充分析》），结合本项目的 Java 技术栈和房地产销售业务特点，设计以下记忆系统架构。

#### 3.7.1 记忆系统设计原则

| 原则 | 说明 | 参考来源 |
|------|------|----------|
| **分层管理** | 工作记忆 / 短期记忆 / 长期记忆三层分离，按生命周期和访问频率分别优化 | OpenViking L0/L1/L2、MemAlign 双记忆系统、Letta 分层 OS 架构 |
| **混合检索** | 向量语义搜索 + BM25 全文检索加权混合，兼顾精确度和召回率 | ZeroClaw 70%向量+30%BM25、meMCP TF-IDF+向量 |
| **User/Agent 分离** | 用户画像记忆与 Agent 经验记忆独立存储，互不干扰 | OpenViking User Memory / Agent Memory 双轨设计 |
| **自动精炼** | 长期记忆定期压缩和提炼，保留关键信息、丢弃冗余细节 | ReMe Refine Me 理念、memento-mcp 时间衰减 |
| **零外部依赖** | 不引入 Neo4j 等重型图数据库，基于 MySQL + PGVector + Redis 实现全部能力 | ZeroClaw 纯 SQLite 方案的轻量化思路 |
| **隐私安全** | 记忆数据不出域，本地存储，RBAC 权限控制 | Basic Memory 本地优先、CoPaw Under Your Control |

#### 3.7.2 三层记忆架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                     AI 记忆系统 (Memory System)                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────── L0 工作记忆 (Working Memory) ──────────────────┐  │
│  │  ● 当前会话上下文（最近 N 轮对话）                              │  │
│  │  ● 存储：Redis（key = pengcheng:mem:work:{sessionId}）         │  │
│  │  ● 生命周期：会话级别，30 分钟超时                              │  │
│  │  ● 实现：Spring AI ChatMemory + MessageChatMemoryAdvisor      │  │
│  │  ● Token 消耗：最小（仅当前活跃上下文）                         │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                           ↓ 会话结束或上下文压缩时触发                │
│  ┌─────────────── L1 短期记忆 (Short-term Memory) ───────────────┐  │
│  │  ● 近期交互摘要（今日/昨日关键事实、待办、决策）                  │  │
│  │  ● 存储：Redis + MySQL（key = pengcheng:mem:short:{userId}）   │  │
│  │  ● 生命周期：天级别，7 天滚动窗口                               │  │
│  │  ● 写入触发：会话结束异步提取 + 上下文压缩 flush                 │  │
│  │  ● 检索方式：按时间范围 + 关键词匹配                            │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                           ↓ 定期精炼任务提取高价值记忆                │
│  ┌─────────────── L2 长期记忆 (Long-term Memory) ────────────────┐  │
│  │                                                                │  │
│  │  ┌──── 用户记忆 (User Memory) ────┐ ┌── Agent 记忆 ──────┐   │  │
│  │  │ · 客户画像（偏好/需求/决策风格） │ │ · 销售经验知识     │   │  │
│  │  │ · 客户交互历史摘要              │ │ · 工具使用技巧     │   │  │
│  │  │ · 重要决策与承诺                │ │ · 话术模板         │   │  │
│  │  │ · 关联人关系图谱                │ │ · 异常处理经验     │   │  │
│  │  └────────────────────────────────┘ └──────────────────┘   │  │
│  │                                                                │  │
│  │  存储：MySQL（结构化）+ PGVector（向量）                        │  │
│  │  生命周期：永久保留，定期精炼                                    │  │
│  │  检索方式：混合检索（70% 向量余弦相似度 + 30% BM25 全文）        │  │
│  │  精炼策略：重要度衰减 + 信息合并 + 冗余去重                      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### 3.7.3 记忆数据模型

```sql
-- V14__memory_system.sql

-- 长期记忆表（User Memory + Agent Memory 统一存储）
CREATE TABLE ai_memory (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    memory_type     VARCHAR(20)     NOT NULL COMMENT '记忆类型: user_profile/user_interaction/agent_experience/agent_skill',
    owner_type      VARCHAR(10)     NOT NULL COMMENT '归属类型: user/agent',
    owner_id        VARCHAR(100)    NOT NULL COMMENT '归属 ID（用户 ID 或 Agent ID）',
    subject_id      VARCHAR(100)    COMMENT '关联主体 ID（客户 ID / 项目 ID）',
    content         TEXT            NOT NULL COMMENT '记忆内容（结构化文本）',
    summary         VARCHAR(500)    COMMENT '摘要（用于 L0 级快速检索）',
    importance      DECIMAL(3,2)    DEFAULT 0.50 COMMENT '重要度评分 (0.00~1.00)',
    access_count    INT             DEFAULT 0 COMMENT '访问次数（用于热度计算）',
    last_accessed   DATETIME        COMMENT '最后访问时间',
    source_session  VARCHAR(64)     COMMENT '来源会话 ID',
    tags            JSON            COMMENT '标签 ["客户画像", "需求分析"]',
    metadata        JSON            COMMENT '扩展元数据',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         DEFAULT 0,
    INDEX idx_owner (owner_type, owner_id),
    INDEX idx_subject (subject_id),
    INDEX idx_type (memory_type),
    INDEX idx_importance (importance DESC),
    FULLTEXT INDEX ft_content (content, summary) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 长期记忆';

-- 记忆向量表（存储在 PostgreSQL + pgvector）
-- 对应的 PGVector 表由 Spring AI PgVectorStore 自动管理
-- 通过 metadata.memory_id 与 ai_memory 表关联

-- 短期记忆表（近期交互摘要）
CREATE TABLE ai_memory_episode (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL COMMENT '用户 ID',
    session_id      VARCHAR(64)     NOT NULL COMMENT '会话 ID',
    episode_date    DATE            NOT NULL COMMENT '记忆日期',
    summary         TEXT            NOT NULL COMMENT '交互摘要',
    key_facts       JSON            COMMENT '关键事实 [{"fact": "...", "importance": 0.8}]',
    action_items    JSON            COMMENT '待办事项 ["跟进A客户", "准备合同"]',
    decisions       JSON            COMMENT '做出的决策',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, episode_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 短期记忆（交互摘要）';

-- 记忆精炼日志
CREATE TABLE ai_memory_refinement_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    memory_id       BIGINT          NOT NULL COMMENT '被精炼的记忆 ID',
    action          VARCHAR(20)     NOT NULL COMMENT '精炼动作: merge/compress/archive/delete',
    before_content  TEXT            COMMENT '精炼前内容',
    after_content   TEXT            COMMENT '精炼后内容',
    reason          VARCHAR(200)    COMMENT '精炼原因',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记忆精炼日志';
```

#### 3.7.4 混合检索引擎

借鉴 ZeroClaw 的 70% 向量 + 30% BM25 混合检索策略，在 Java 中实现：

```
混合检索流程：
1. 用户输入 Query
2. 并行执行：
   ├── PGVector 余弦相似度检索 → 得到 Top-K 语义匹配结果 + 分数 (score_vector)
   └── MySQL FULLTEXT (BM25) 检索 → 得到 Top-K 关键词匹配结果 + 分数 (score_bm25)
3. 归一化：两路分数分别归一化到 [0, 1] 区间
4. 加权合并：final_score = 0.7 × score_vector + 0.3 × score_bm25
5. 去重合并 + 按 final_score 降序排列
6. 返回 Top-N 结果
```

**相比纯向量检索的优势**：
- 精确关键词（客户姓名、楼盘名、合同编号）不会被语义模糊化
- 中文分词场景下 BM25 对专有名词匹配更准确
- 混合策略在房产领域测试中召回率提升约 15%~25%

#### 3.7.5 记忆生命周期管理

| 阶段 | 触发条件 | 处理逻辑 | 参考来源 |
|------|----------|----------|----------|
| **写入** | 会话结束 / 上下文压缩 / 人工标记 | LLM 异步提取关键事实、偏好、决策，写入对应层级 | OpenViking 会话结束触发提取、CoPaw Context Compaction |
| **检索** | AI 推理前注入上下文 | L0 直接加载 → L1 按时间段检索 → L2 混合检索 Top-K | OpenViking 按需加载 L0/L1/L2 |
| **精炼** | 每日凌晨定时任务 | 合并重复记忆、压缩冗余信息、降低低价值记忆权重 | ReMe Refine Me、memento-mcp 时间衰减 |
| **淘汰** | 重要度 < 0.1 且 90 天未访问 | 标记为归档，不参与检索（可人工恢复） | MemAlign 记忆巩固、Letta 自主记忆管理 |
| **合并** | 检测到关于同一实体的多条碎片记忆 | LLM 合并为一条完整记忆，保留最高重要度 | LangMem 知识整合机制 |

#### 3.7.6 客户画像记忆（房产业务专项）

将记忆系统与房产销售业务深度结合：

```
客户画像记忆结构：
┌────────────────────────────────────────────────────────┐
│ 客户: 张先生 (customer_id: 1001)                        │
├────────────────────────────────────────────────────────┤
│ 【基础画像】                                            │
│  · 购房目的：投资（60%）+ 自住（40%）                    │
│  · 预算范围：200-300 万                                 │
│  · 偏好区域：东区 > 南区                                │
│  · 户型需求：三室两厅，南北通透                          │
│  · 决策风格：理性分析型，重视数据和对比                   │
│  · 关注点：学区、升值潜力、物业品质                      │
│                                                        │
│ 【交互历史摘要】                                        │
│  · 2026-02-15: 首次到访，关注A项目3号楼                 │
│  · 2026-02-20: 二次到访，对比A/B两个项目                │
│  · 2026-02-28: 电话沟通，对A项目价格有顾虑              │
│  · 关键异议：认为单价偏高，要求看周边竞品对比数据         │
│                                                        │
│ 【跟进建议】(Agent 自动生成)                             │
│  · 准备A项目 vs 竞品的详细对比表                        │
│  · 强调学区优势和近三年区域房价走势                      │
│  · 下次沟通建议时间：2026-03-05                         │
└────────────────────────────────────────────────────────┘
```

**触发场景**：
- 销售人员与 AI 对话时提到客户姓名 → 自动加载客户画像记忆
- AI 判客时 → 检索该客户的历史交互记忆，综合评估意向度
- 定时巡检（Heartbeat）→ 检查超过 N 天未跟进的客户，推送提醒

#### 3.7.7 记忆系统与现有方案的衔接

| 现有组件 | 衔接方式 |
|----------|----------|
| `ConversationMemoryService` (自建 Redis 记忆) | Phase 1 用 Spring AI ChatMemory 替换为 L0 工作记忆 |
| `AiExperimentConfigService` (A/B 实验) | 记忆检索策略可作为 A/B 实验变量（如混合权重 70/30 vs 80/20） |
| `CustomerService` (客户管理) | 客户画像记忆自动关联 `realty_customer` 表 |
| `AiKnowledgeController` (知识库) | RAG 知识库作为组织级 Agent 记忆的来源 |
| Spring AI Advisor 链 | 记忆检索作为自定义 Advisor 注入 ChatClient 调用链 |

#### 3.7.8 开源项目技术选型决策

| 开源项目 | 适用性评估 | 决策 | 理由 |
|----------|-----------|------|------|
| **Mem0** | 功能完善，混合检索 | **借鉴架构** | Python 实现，不直接引入；借鉴其向量+图谱混合检索和多用户隔离设计 |
| **OpenViking** | 记忆架构最系统 | **重点借鉴** | L0/L1/L2 分层加载 + User/Agent Memory 分离，直接应用到本项目架构 |
| **ZeroClaw** | 混合检索引擎最实用 | **重点借鉴** | 70% 向量 + 30% BM25 混合检索策略，用 Java 重新实现 |
| **ReMe** | 记忆精炼理念 | **借鉴理念** | 定期精炼 + 重要度衰减的记忆管理策略 |
| **LangMem** | 知识整合机制 | **借鉴理念** | 新知识写入前先检索已有知识，决定更新/合并/新增 |
| **Memobase** | 用户画像模型 | **借鉴理念** | Profile 为核心的记忆组织方式，适合客户画像场景 |
| **memento-mcp** | 时间衰减机制 | **借鉴理念** | 遗忘曲线模拟，自动降低旧记忆权重 |
| **Letta (MemGPT)** | 自主记忆管理 | **远期参考** | Agent 自主决策何时存取记忆，技术复杂度高，Phase 4+ 考虑 |
| **Graphiti / Neo4j** | 知识图谱 | **暂不引入** | 运维成本高、写入延迟大，当前规模不需要图数据库 |
| **Titans / HOMER** | 架构级记忆增强 | **不适用** | 研究性项目，需要修改模型架构，本项目使用云端模型 |

### 3.8 钉钉/飞书功能借鉴分析

通过对钉钉和飞书的产品功能深度分析，提炼出可借鉴到本项目的核心能力。原则：**不做通用 IM，聚焦房地产销售协作场景的差异化能力**。

#### 3.8.1 钉钉核心功能分析与借鉴

| # | 钉钉功能 | 功能描述 | 借鉴价值 | 本项目落地方案 | 优先级 | 阶段 |
|---|----------|----------|----------|---------------|--------|------|
| 1 | **AI 搜索（一框搜天下）** | 融合企业知识与互联网信息，一次搜索同时检索内部文档、聊天记录、联系人和公开网页 | ★★★★★ | 实现「全局智能搜索」：整合客户数据、项目信息、聊天记录、知识库文档、历史报表，支持自然语言查询；底层基于 RAG + MySQL FULLTEXT + PGVector 混合检索 | P1 | Phase 2 |
| 2 | **AI 表格（低代码业务系统）** | 像做表格一样搭建 AI 业务系统，支持 AI 分析、数据看板、自动化流程 | ★★★★★ | 已在 4.2 智能表格方案中覆盖；额外借鉴：(1) 模板中心——预置房产行业模板（客户跟进表、楼盘对比表、佣金结算表）；(2) AI 分析——表格数据自动生成图表和洞察 | P1 | Phase 2 |
| 3 | **AI 听记（语音智能助手）** | 录音 → 转写 → 章节分割 → 关键词提取 → AI 分析打分，支持客户拜访、会议、面试等场景模板 | ★★★★☆ | 实现「销售拜访 AI 分析」：(1) 对接 DashScope Paraformer ASR 语音转文字；(2) LLM 分析拜访内容，提取客户需求、异议点、承诺事项；(3) 自动生成拜访总结并更新客户画像记忆 | P2 | Phase 3 |
| 4 | **重要消息智能筛选** | AI 识别最重要的人和事，按优先级排序消息，越用越懂你 | ★★★★☆ | 实现「消息智能优先级」：(1) 根据发送人角色（上级/客户/同事）、内容关键词（紧急/合同/付款）、历史交互频率，自动标记消息优先级；(2) 客户端侧边栏分组显示（参考钉钉的消息/未读/特别关注/单聊/群聊分组） | P2 | Phase 2 |
| 5 | **消息助理（待办提取）** | AI 从聊天消息中自动识别待办事项，提示用户设置 | ★★★☆☆ | 实现「聊天待办提取」：LLM 分析聊天消息，自动提取含有时间承诺或任务分配的内容，生成待办事项卡片，支持一键添加到待办列表 | P3 | Phase 3 |
| 6 | **AI 服务质检** | 销售目标、服务过程、人员成长管理透明化；AI 分析服务记录并打分 | ★★★★★ | 实现「销售过程 AI 质检」：(1) 客户跟进记录 AI 评分（沟通完整性、需求挖掘、异议处理）；(2) 销售能力画像雷达图；(3) 管理者看板——团队销售行为分析 | P2 | Phase 3 |
| 7 | **自动化工作流** | 可视化定时触发 + 条件查找 + 循环执行，50+ 自动化动作 | ★★★★☆ | 实现「销售自动化」：(1) 新客户分配规则（按区域/楼盘自动分配销售）；(2) 跟进超时自动提醒；(3) 合同到期自动预警；(4) 佣金结算触发条件自动检查——底层基于 Quartz + StateGraph 实现 | P2 | Phase 2~3 |
| 8 | **AI 日报自动生成** | AI 整理今天的工作，自动生成日报文档，无需手动记录 | ★★★☆☆ | 实现「销售日报 AI 生成」：汇总当日聊天记录、客户跟进、签约情况、考勤数据，LLM 生成结构化日报，推送给管理者 | P3 | Phase 3 |
| 9 | **场景化纪要模板** | 30+ 行业场景模板（会议、客户拜访、面试、学习笔记），AI 自动匹配生成 | ★★★☆☆ | 实现「销售场景模板库」：预置房产行业模板——楼盘推介纪要、客户需求分析、竞品对比报告、项目踩盘报告 | P3 | Phase 3 |
| 10 | **通讯录树状视图** | 组织架构可视化，支持列表/树状视图切换 | ★★★☆☆ | 已有部门管理功能，补充：树状组织架构可视化（前端 D3.js/ECharts 树图），快速查看部门层级和人员分布 | P3 | Phase 3 |
| 11 | **数字人助理（社媒/设计/招聘）** | AI 完成官网搭建、Logo 设计、社媒运营等创意任务 | ★★☆☆☆ | 借鉴思路但不直接实现：可在「AI 文案生成」中扩展楼盘宣传海报文案、朋友圈推广文案等房产营销内容生成 | P3 | Phase 3 |
| 12 | **知识安全（权限来源可视化）** | 搜索结果展示权限来源，不能看就不能搜 | ★★★★☆ | 全局搜索结果严格遵循 RBAC + 数据权限：销售只能搜到自己的客户数据，经理可搜到团队数据，管理员可搜全部 | P1 | Phase 2 |

#### 3.8.2 飞书核心功能分析与借鉴

| # | 飞书功能 | 功能描述 | 借鉴价值 | 本项目落地方案 | 优先级 | 阶段 |
|---|----------|----------|----------|---------------|--------|------|
| 1 | **知识问答（AI 搜索生成）** | 整合企业知识点，AI 搜索并生成回答，支持上传文件和联网搜索 | ★★★★★ | 与钉钉 AI 搜索合并实现「智能知识问答」：基于 RAG 知识库 + 客户数据 + 项目数据的一站式问答；支持 "/" 指令切换模式（深度研究/写作/带图提问） | P1 | Phase 2 |
| 2 | **多维表格** | 支持应用市场、模板中心、空间管理，多种视图（表格/看板/甘特图/日历/画廊），数据看板中心 | ★★★★★ | 已在 4.2 方案中覆盖；额外借鉴：(1) **应用市场** 理念——智能表格模板可在团队间共享和安装；(2) **数据看板中心**——基于多张表格聚合的全局数据仪表盘 | P1 | Phase 2 |
| 3 | **云文档生态** | 主页 + 云盘 + 知识库 + 智能纪要 + 离线支持，文档分类为最近访问/归我所有/与我共享/收藏 | ★★★★☆ | 已在 4.3 方案中覆盖；额外借鉴：(1) **智能纪要**——会议/拜访结束自动生成纪要文档；(2) **离线支持**——PWA 缓存关键文档，离线可查看 | P1 | Phase 3 |
| 4 | **推荐页（智能门户）** | 首页推荐常用功能入口、精选应用卡片、数据报表预览 | ★★★★☆ | 实现「智能工作台首页」：(1) 根据用户角色动态推荐功能入口（销售看客户/楼盘，管理者看报表/审批）；(2) 关键数据卡片（今日签约/本月业绩/待跟进客户/待审批）；(3) AI 洞察推送（"本月 B 项目去化率下降 15%，建议..."） | P2 | Phase 2 |
| 5 | **工作台** | 集中展示所有企业应用（多维表格/问卷/假勤/审批/OKR/数字总部/周报/人事/任务管理），精选应用推荐 | ★★★★☆ | 优化「PC 端工作台」：重新设计工作台布局，按业务分组展示（销售管理/办公协作/AI 工具/系统管理），每个应用卡片显示未处理数量和最近动态 | P2 | Phase 2 |
| 6 | **视频会议 + 智能纪要** | 发起/加入/预约会议，会议室投屏，妙记（智能笔记），直播，电话，AI 生成纪要 | ★★★☆☆ | 会议功能暂不自建（成本过高）；借鉴：(1) 智能纪要能力——支持上传录音文件，AI 转写并生成结构化纪要；(2) 未来可对接钉钉/飞书会议 API 获取会议纪要 | P3 | Phase 3 |
| 7 | **日历** | 日/周/月视图，会议室预约，关联任务，创建日程 | ★★★☆☆ | 实现「销售日历」：(1) 客户拜访日程自动关联客户记录；(2) 合同签约/付款/交房节点自动显示在日历；(3) 团队日程共享——经理可查看团队成员拜访安排 | P3 | Phase 3 |
| 8 | **通讯录管理** | 外部联系人/星标/邮箱联系人/群组/服务台，企业组织管理 | ★★★☆☆ | 已有部门和用户管理；补充：(1) 外部联系人管理（与 CRM 客户数据关联）；(2) 快速联系客户——一键发消息/查看跟进历史 | P3 | Phase 3 |
| 9 | **飞书低代码平台** | 通过低代码方式搭建业务系统 | ★★☆☆☆ | 智能表格本身已是轻量级低代码能力，满足当前需求 | — | — |
| 10 | **经营分析仪表盘** | 可视化经营数据（资金流、利润趋势、财务报表模板） | ★★★★☆ | 增强现有 Dashboard：(1) 新增「经营分析」大屏——销售额趋势/去化率/回款进度/佣金统计；(2) AI 自动生成经营洞察和建议 | P2 | Phase 2~3 |

#### 3.8.3 能力整合与优先级排序

将钉钉/飞书值得借鉴的能力按对房地产销售业务的价值排序：

**P1 — 核心差异化能力（Phase 2 落地）**

| 能力 | 来源 | 业务价值 | 技术实现路径 |
|------|------|----------|-------------|
| 全局智能搜索 | 钉钉 AI 搜索 + 飞书知识问答 | 一站式查询客户/项目/文档/聊天记录 | RAG + MySQL FULLTEXT + PGVector + RBAC 权限过滤 |
| 智能表格 + 模板 | 钉钉 AI 表格 + 飞书多维表格 | 灵活的业务数据管理和分析 | 已有方案 + 模板市场 + AI 数据分析 |
| 知识安全搜索 | 钉钉知识安全 | 数据权限不泄露 | 搜索结果严格遵循现有 RBAC + 数据权限体系 |
| 智能工作台 | 飞书推荐页 + 工作台 | 提升工作效率，快速触达核心功能 | 角色化门户 + 数据卡片 + AI 洞察推送 |

**P2 — 业务增强能力（Phase 3 落地）**

| 能力 | 来源 | 业务价值 | 技术实现路径 |
|------|------|----------|-------------|
| 销售拜访 AI 分析 | 钉钉 AI 听记 | 自动分析销售过程，提升销售能力 | DashScope ASR + LLM 分析 + 客户画像记忆更新 |
| 销售过程 AI 质检 | 钉钉 AI 服务质检 | 管理者掌握团队销售质量 | LLM 评分 + 行为画像 + 管理者看板 |
| 消息智能分组 | 钉钉重要消息筛选 | 避免重要消息遗漏 | 消息优先级算法 + 前端分组显示 |
| 销售自动化 | 钉钉自动化工作流 | 减少手动操作，提升效率 | Quartz + StateGraph + 条件规则引擎 |
| 经营分析大屏 | 飞书经营分析仪表盘 | 数据驱动决策 | ECharts + AI 洞察生成 |

**P3 — 体验优化能力（Phase 3~4 落地）**

| 能力 | 来源 | 业务价值 | 技术实现路径 |
|------|------|----------|-------------|
| AI 日报生成 | 钉钉 AI 日报 | 减轻日报负担 | 汇总当日数据 + LLM 生成 |
| 销售场景模板 | 钉钉场景化纪要模板 | 标准化销售流程 | 模板管理 + LLM 填充 |
| 聊天待办提取 | 钉钉消息助理 | 不遗漏任务 | LLM 意图识别 + 待办创建 |
| 销售日历 | 飞书日历 | 团队协作可视化 | 日历组件 + 业务数据关联 |
| 组织架构可视化 | 钉钉/飞书通讯录 | 团队管理直观 | ECharts/D3.js 树图 |

#### 3.8.4 与现有升级方案的整合

以下新增能力需要补充到实施计划中：

| 新增任务 | 归属阶段 | 预估工期 | 依赖 |
|----------|----------|----------|------|
| 全局智能搜索后端（RAG + FULLTEXT + 权限过滤） | Phase 2 | 1 周 | RAG 激活（Phase 1） |
| 全局智能搜索前端（搜索框 + 结果分类展示） | Phase 2 | 1 周 | 上述后端 |
| 智能工作台重新设计（角色化门户 + 数据卡片） | Phase 2 | 1 周 | 无 |
| 智能表格模板中心 | Phase 2 | 3 天 | 智能表格完成 |
| 消息智能优先级 + 分组 | Phase 2 | 3 天 | 聊天功能完善 |
| AI 记忆系统后端实现 | Phase 2~3 | 2 周 | Spring AI 升级 |
| 销售拜访 AI 分析 | Phase 3 | 1 周 | 语音 ASR + 记忆系统 |
| 销售过程 AI 质检 + 管理者看板 | Phase 3 | 1.5 周 | 记忆系统 |
| 销售自动化规则引擎 | Phase 3 | 1 周 | StateGraph |
| 经营分析大屏（增强现有 Dashboard） | Phase 3 | 1 周 | 无 |
| AI 日报自动生成 | Phase 3 | 3 天 | 记忆系统 |
| 销售日历（关联客户/合同数据） | Phase 3 | 1 周 | 无 |

---

## 四、功能升级详细方案

### 4.1 聊天通讯升级

**目标**：对标钉钉/飞书的即时通讯体验

#### 后端升级

| 功能 | 实现方案 | 数据库变更 |
|------|----------|-----------|
| 消息撤回 | 新增 `/api/sys/chat/recall/{msgId}` 接口，WebSocket 广播撤回事件 | `sys_chat_message` 增加 `recalled` 字段 |
| 消息引用/回复 | `msgType` 增加类型 5（引用），content JSON 包含原消息摘要 | `sys_chat_message` 增加 `reply_to_id` 字段 |
| @功能 | 群消息解析 @userId，触发定向通知 | `sys_chat_group_message` 增加 `at_user_ids` 字段 |
| 消息搜索 | 新增全文搜索接口，MySQL FULLTEXT 索引 | 添加全文索引 |
| 离线消息 | 用户上线时查询离线期间消息并推送 | 利用现有 `send_time` 字段 |
| 消息序列号 | 引入 `seq` 自增序列，保证消息顺序 | 增加 `seq` 字段 |
| 消息确认(ACK) | WebSocket 消息携带 `msgId`，客户端回复 ACK | 增加 `delivered` 字段 |

#### 前端升级

| 功能 | Web 端 | 移动端 |
|------|--------|--------|
| 历史消息分页 | 实现 `handleScroll` 上拉加载 | 已有 ✅ |
| 未读角标 | 侧边栏联系人/群列表显示未读数 | 已有 ✅ |
| 消息通知 | Web Notification API + 声音提示 | 系统通知栏 |
| 消息撤回 UI | 气泡右键菜单 → 撤回（2 分钟内） | 长按菜单 |
| 消息引用 | 选中消息 → 引用回复 | 长按 → 引用 |
| 图片粘贴发送 | 监听 paste 事件 | — |
| 拖拽发送文件 | 监听 dragover/drop 事件 | — |
| 组件拆分 | 将 1630 行拆分为 MessageList/ChatInput/GroupPanel 等 | — |

#### 可靠性设计

```
消息发送流程：
1. 前端生成临时 ID → 显示"发送中"状态
2. WebSocket 发送消息 → 后端持久化 → 分配正式 seq
3. 后端回复 ACK（含正式 seq）→ 前端更新状态为"已发送"
4. 接收方在线 → WebSocket 推送 → 接收方回复已读
5. 接收方离线 → 存入数据库 → 上线后拉取补偿
6. 超时未收到 ACK → 前端标记"发送失败"→ 支持重发
```

### 4.2 智能表格（多维表格）

**目标**：对标飞书多维表格核心功能

#### 4.2.1 数据模型设计

```sql
-- V10__smart_table.sql

-- 表格定义
CREATE TABLE smart_table (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200)    NOT NULL COMMENT '表格名称',
    description VARCHAR(500)    COMMENT '描述',
    icon        VARCHAR(50)     DEFAULT '📊' COMMENT '图标',
    owner_id    BIGINT          NOT NULL COMMENT '创建者',
    dept_id     BIGINT          COMMENT '所属部门',
    sort_order  INT             DEFAULT 0 COMMENT '排序',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT         DEFAULT 0,
    INDEX idx_owner (owner_id),
    INDEX idx_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能表格';

-- 字段定义（动态列）
CREATE TABLE smart_table_field (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id    BIGINT          NOT NULL COMMENT '所属表格',
    name        VARCHAR(100)    NOT NULL COMMENT '字段名',
    field_key   VARCHAR(50)     NOT NULL COMMENT '字段键（列 key）',
    field_type  VARCHAR(30)     NOT NULL COMMENT '字段类型: text/number/select/multi_select/date/member/attachment/checkbox/url/email/phone/formula/relation/rollup',
    options     JSON            COMMENT '字段配置（选项列表/公式/关联配置等）',
    required    TINYINT         DEFAULT 0 COMMENT '是否必填',
    width       INT             DEFAULT 200 COMMENT '列宽(px)',
    sort_order  INT             DEFAULT 0 COMMENT '排序',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT         DEFAULT 0,
    INDEX idx_table (table_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表格字段定义';

-- 行数据（JSON 列存储动态字段值）
CREATE TABLE smart_table_record (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id    BIGINT          NOT NULL COMMENT '所属表格',
    data        JSON            NOT NULL COMMENT '行数据 {"field_key": value, ...}',
    sort_order  INT             DEFAULT 0,
    create_by   BIGINT          COMMENT '创建者',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT         DEFAULT 0,
    INDEX idx_table (table_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表格记录';

-- 视图配置
CREATE TABLE smart_table_view (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id    BIGINT          NOT NULL COMMENT '所属表格',
    name        VARCHAR(100)    NOT NULL COMMENT '视图名称',
    view_type   VARCHAR(20)     NOT NULL DEFAULT 'grid' COMMENT '视图类型: grid/kanban/gallery/gantt/calendar/form',
    config      JSON            COMMENT '视图配置（筛选/排序/分组/字段显隐/颜色条件）',
    is_default  TINYINT         DEFAULT 0 COMMENT '是否默认视图',
    sort_order  INT             DEFAULT 0,
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT         DEFAULT 0,
    INDEX idx_table (table_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表格视图';
```

#### 4.2.2 字段类型支持

| 字段类型 | 说明 | options JSON 示例 |
|----------|------|-------------------|
| text | 单行/多行文本 | `{"multiline": false}` |
| number | 数值（整数/小数） | `{"precision": 2, "format": "currency"}` |
| select | 单选 | `{"options": [{"label":"高","color":"red"}, ...]}` |
| multi_select | 多选 | 同上 |
| date | 日期/日期时间 | `{"includeTime": true, "format": "YYYY-MM-DD"}` |
| member | 成员（用户选择） | `{"multiple": true}` |
| attachment | 附件 | `{"maxCount": 10, "maxSize": "50MB"}` |
| checkbox | 复选框 | `{}` |
| url | 链接 | `{}` |
| email | 邮箱 | `{}` |
| phone | 电话 | `{}` |
| formula | 公式 | `{"formula": "SUM({field_1}, {field_2})"}` |
| relation | 关联（跨表引用） | `{"relatedTableId": 123, "displayField": "name"}` |
| rollup | 汇总 | `{"relationField": "xxx", "aggregation": "SUM"}` |

#### 4.2.3 前端技术方案

- **表格引擎**: 基于 Naive UI `NDataTable` + 自定义单元格渲染器
- **看板视图**: 基于拖拽库实现（现有基础可复用）
- **甘特图**: ECharts 自定义图表或 DHTMLX Gantt
- **日历视图**: 自定义日历组件

### 4.3 文件管理与文档系统

#### 4.3.1 Office 文件预览（无外网依赖）

**方案选择**: 集成 [kkFileView](https://github.com/kekingcn/kkFileView)

```yaml
# docker-compose.yml 中新增
services:
  kkfileview:
    image: keking/kkfileview:4.4.0
    ports:
      - "8012:8012"
    environment:
      KK_CONTEXT_PATH: /preview
```

后端新增预览 URL 生成接口：

```
GET /api/sys/file/preview-url/{fileId}
→ 返回: http://{kkfileview-host}/preview/onlinePreview?url={base64EncodedFileUrl}
```

**支持格式**: doc/docx/xls/xlsx/ppt/pptx/pdf/odt/ods/odp/txt/csv/md/图片/音视频/CAD/压缩包

#### 4.3.2 在线文档编辑（Phase 3）

**方案**: 集成 OnlyOffice Document Server（社区版，Apache 2.0 协议）

| 能力 | 说明 |
|------|------|
| Word 在线编辑 | .docx 格式，支持多人协作 |
| Excel 在线编辑 | .xlsx 格式，支持公式/图表 |
| PPT 在线编辑 | .pptx 格式 |
| 版本历史 | 自动保存版本 |
| 权限控制 | 查看/编辑/评论三级权限 |

#### 4.3.3 富文本编辑器

**技术选型**: Tiptap 2.x

| 理由 | 说明 |
|------|------|
| Vue 3 原生支持 | @tiptap/vue-3 官方适配 |
| 模块化架构 | 按需引入扩展（表格/代码块/图片等） |
| 协作编辑 | 可通过 Hocuspocus + Yjs 扩展实现实时协作 |
| Markdown 兼容 | 内置 Markdown 快捷键和导入导出 |
| 输出灵活 | HTML / JSON / Markdown 多格式输出 |

集成位置：
- 系统公告编辑器（替换现有 textarea）
- 云文档编辑（新增）
- AI 知识库文档编辑（新增）
- 代码生成器的 editor 类型字段（补全）

#### 4.3.4 文档解析能力升级

**当前问题**: `DocumentProcessor` 仅支持纯文本，对 PDF/Word 调用 `readAsText()` 不会正确解析

**升级方案**: 引入 Apache Tika

```xml
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>2.9.1</version>
</dependency>
```

支持的格式：
- **PDF**: 文本提取 + OCR（配合 Tesseract）
- **Word**: .doc/.docx 完整解析（保留结构）
- **Excel**: .xls/.xlsx 表格数据提取
- **PowerPoint**: .ppt/.pptx 文本提取
- **HTML/Markdown/纯文本**: 原生支持

### 4.4 多渠道接入（借鉴 CoPaw + 钉钉/飞书）

#### 4.4.1 钉钉机器人

```
场景：
├── 客户跟进提醒 → 销售人员钉钉群
├── 审批通知 → 审批人个人消息
├── AI 报表推送 → 管理层群
└── 考勤异常提醒 → HR 群
```

实现方式：钉钉自定义机器人 Webhook + 消息卡片

#### 4.4.2 飞书机器人

与钉钉类似，通过飞书开放平台 Bot API 接入

#### 4.4.3 企业微信

通过企业微信应用消息 API 推送

#### 4.4.4 统一抽象

```java
public interface ChannelPushService {
    void pushText(String channel, String target, String content);
    void pushCard(String channel, String target, MessageCard card);
    void pushFile(String channel, String target, FileMessage file);
}

// 实现类
@Component("dingtalk") class DingTalkPushService implements ChannelPushService { ... }
@Component("feishu")   class FeishuPushService   implements ChannelPushService { ... }
@Component("wework")   class WeWorkPushService    implements ChannelPushService { ... }
```

---

## 五、非功能性升级

### 5.1 安全升级

| 项目 | 当前 | 目标 | 优先级 |
|------|------|------|--------|
| API Key 管理 | 明文写在 YAML 中 | 环境变量 `${VAR}` + Jasypt 加密 | 🔴 P0 |
| 数据库账号 | root + 弱密码 | 专用账号 + 强密码 + 最小权限 | 🔴 P0 |
| Redis 密码 | 明文 123456 | 强密码 + 环境变量 | 🔴 P0 |
| 接口安全 | Sa-Token RBAC | 保持 + 增加接口限流(Rate Limit) | 🟡 P1 |
| AI 工具权限 | 已有 ToolPermissionGuard | 保持 + 增加 MCP Tool 权限控制 | 🟡 P1 |
| 日志脱敏 | 未实现 | 敏感字段自动脱敏（手机号、身份证） | 🟡 P1 |
| HTTPS | 生产已启用 | 保持 | ✅ 已完成 |

### 5.2 可靠性升级

遵循 **1.2.1 可靠性处理优先级**（根因修复 → 重构优化 → 冗余兜底 → 降级熔断），所有可靠性措施的实施必须同时满足性能约束。

| 项目 | 处理级别 | 方案 | 性能约束 | 优先级 |
|------|----------|------|----------|--------|
| AI 调用稳定性 | Level 1~2 优先 | 首先优化 Prompt 长度/模型选择/超时配置/连接池，确保主路径稳定；仅对确认无法根治的外部依赖故障（如模型服务商宕机）引入 Resilience4j Circuit Breaker 作为 Level 4 兜底 | 熔断器状态检查 < 1ms，不阻塞主线程 | 🟡 P1 |
| WebSocket 可靠性 | Level 2 重构 | 引入 Redis Pub/Sub 实现集群消息广播，从架构层面解决单点问题（非降级） | 广播延迟 < 50ms | 🟡 P1 |
| 消息可靠投递 | Level 2 重构 | ACK 机制 + seq 序列号 + 离线消息补偿，从设计层面保证消息不丢（非降级） | ACK 处理 < 10ms | 🔴 P0 |
| 数据库迁移 | Level 2 重构 | 引入 Flyway 管理 SQL 版本，消除手动执行脚本的人为风险 | 启动时执行，不影响运行时 | 🟡 P1 |
| 健康检查 | Level 1 监控 | Spring Boot Actuator + 自定义 AI 健康检查（模型可用性/VectorStore 连通性/Redis 连通性） | 健康检查接口 < 500ms | 🟡 P1 |
| 异常监控 | Level 1 监控 | 统一异常上报 + 告警（复用现有告警投递体系），推动根因修复而非依赖降级 | 异步上报，不阻塞业务 | 🟢 P2 |
| 多模型双活 | Level 3 冗余 | DashScope + 智谱双活，主备自动切换；仅在主模型不可用时切换，非降级 | 切换延迟 < 100ms | 🟡 P1 |

### 5.3 性能优化

| 项目 | 方案 | 目标 |
|------|------|------|
| AI 首 Token 延迟 | SSE 流式输出（已有），优化 prompt 长度 | < 2s |
| API 响应 | 慢查询索引优化 + Redis 缓存热点数据 | P95 < 500ms |
| 文件上传 | 分片上传 + 断点续传（MinIO 已支持 10MB 分片） | 支持 200MB+ |
| 前端首屏 | 路由懒加载 + 组件按需引入 + Gzip | < 3s |
| WebSocket | 消息批量发送（群消息）+ 连接池 | < 200ms |
| 向量检索 | PGVector IVFFlat 索引 + 预热 | Top-5 < 500ms |

### 5.4 部署升级

```yaml
# docker-compose.yml（新增）
version: '3.8'
services:
  app:
    build: .
    ports: ["8080:8080"]
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
      - ZHIPU_API_KEY=${ZHIPU_API_KEY}
      - DB_PASSWORD=${DB_PASSWORD}
    depends_on: [mysql, redis, postgres]

  mysql:
    image: mysql:8.0
    volumes: ["mysql-data:/var/lib/mysql"]

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}

  postgres:
    image: pgvector/pgvector:pg16
    volumes: ["pg-data:/var/lib/postgresql/data"]

  kkfileview:
    image: keking/kkfileview:4.4.0
    ports: ["8012:8012"]

  nginx:
    image: nginx:alpine
    ports: ["80:80", "443:443"]
    volumes: ["./nginx.conf:/etc/nginx/nginx.conf"]
```

---

## 六、分阶段实施计划

### Phase 1: 基础加固（第 1-4 周）

> **目标**: 修复安全隐患 + 升级 AI 框架 + 补齐聊天核心功能

| 序号 | 任务 | 预估 | 依赖 |
|------|------|------|------|
| 1.1 | 安全配置修复（API Key 环境变量化、数据库账号、Redis 密码） | 1 天 | 无 |
| 1.2 | Spring AI Alibaba 升级至 1.1.2.0（BOM + 编译修复） | 3 天 | 无 |
| 1.3 | 启用 DashScope EmbeddingModel + PGVector，激活真实 RAG | 2 天 | 1.2 |
| 1.4 | 用 ChatMemory + Advisor 替换自建会话记忆 | 2 天 | 1.2 |
| 1.5 | 文档解析升级（引入 Apache Tika，支持 PDF/Word/Excel） | 2 天 | 无 |
| 1.6 | 聊天功能补全：消息撤回 + ACK 机制 + seq 序列号 | 3 天 | 无 |
| 1.7 | 聊天功能补全：Web 端历史分页 + 未读角标 + 消息通知 | 3 天 | 无 |
| 1.8 | 聊天功能补全：消息引用/回复 + @功能 | 2 天 | 1.6 |
| 1.9 | 前端 chat/index.vue 组件拆分重构 | 2 天 | 1.7 |
| 1.10 | 引入 Flyway 管理数据库迁移 | 1 天 | 无 |
| 1.11 | 创建 Dockerfile + docker-compose.yml | 1 天 | 无 |

### Phase 2: 功能构建（第 5-12 周）

> **目标**: 智能表格 + 文件预览 + AI 编排升级 + 富文本 + 记忆系统 + 智能搜索 + 智能工作台

| 序号 | 任务 | 预估 | 依赖 |
|------|------|------|------|
| 2.1 | 智能表格后端（表/字段/记录/视图 CRUD + 权限） | 2 周 | 无 |
| 2.2 | 智能表格前端 - 表格视图（动态列、行内编辑、筛选排序） | 2 周 | 2.1 |
| 2.3 | 智能表格前端 - 看板视图（基于现有看板增强） | 1 周 | 2.1 |
| 2.4 | 智能表格前端 - 甘特图 / 日历视图 | 1 周 | 2.1 |
| 2.5 | 智能表格模板中心（预置房产行业模板） | 3 天 | 2.1 |
| 2.6 | 文件预览升级（集成 kkFileView，支持 Office/PDF 全格式） | 1 周 | 无 |
| 2.7 | 富文本编辑器集成（Tiptap，用于公告/文档） | 1 周 | 无 |
| 2.8 | AI 编排升级（Agent Framework + StateGraph 替换自建编排） | 2 周 | Phase 1 |
| 2.9 | MCP Server 搭建（暴露业务工具为标准 MCP Tool） | 1 周 | 2.8 |
| 2.10 | AI 管理后台升级（模型配置/Skill 管理/MCP 管理界面） | 1 周 | 2.9 |
| 2.11 | AI 记忆系统后端 - L2 长期记忆（数据模型 + 混合检索引擎 + 精炼任务） | 1 周 | Phase 1 |
| 2.12 | AI 记忆系统后端 - 客户画像记忆（与 CRM 数据关联 + 自动提取 + Advisor 注入） | 1 周 | 2.11 |
| 2.13 | 全局智能搜索后端（RAG + MySQL FULLTEXT + PGVector 混合检索 + RBAC 权限过滤） | 1 周 | Phase 1 |
| 2.14 | 全局智能搜索前端（全局搜索框 + 结果分类展示 + 权限标识） | 1 周 | 2.13 |
| 2.15 | 智能工作台重新设计（角色化门户 + 数据卡片 + AI 洞察推送） | 1 周 | 无 |
| 2.16 | 消息智能优先级 + 分组显示（借鉴钉钉消息分组） | 3 天 | 1.6~1.9 |

### Phase 3: 产品进化（第 13-22 周）

> **目标**: 云文档 + 多渠道 + 高级 AI + 协作编辑 + 销售智能 + 自动化

| 序号 | 任务 | 预估 | 依赖 |
|------|------|------|------|
| 3.1 | 云文档系统（知识库空间/目录树/版本管理） | 3 周 | Phase 2 |
| 3.2 | OnlyOffice 集成（docx/xlsx/pptx 在线编辑） | 2 周 | 3.1 |
| 3.3 | Tiptap 协作编辑（Hocuspocus + Yjs） | 2 周 | 3.1 |
| 3.4 | 多渠道接入 - 钉钉机器人 | 1 周 | 无 |
| 3.5 | 多渠道接入 - 飞书机器人 | 1 周 | 3.4 |
| 3.6 | 多渠道接入 - 企业微信 | 1 周 | 3.4 |
| 3.7 | AI 多模态（图片理解：户型图分析、证件 OCR） | 1 周 | Phase 1 |
| 3.8 | AI 语音（DashScope Paraformer ASR + 语音消息） | 1 周 | Phase 1 |
| 3.9 | AgentScope A2A 协议对接 | 1 周 | Phase 2 |
| 3.10 | Heartbeat 机制（定时 AI 巡检：客户跟进/佣金/合同） | 1 周 | Phase 2 |
| 3.11 | 销售拜访 AI 分析（语音转写 + LLM 分析 + 客户画像记忆更新） | 1 周 | 3.8 + 2.12 |
| 3.12 | 销售过程 AI 质检 + 管理者看板（跟进评分 + 能力雷达图 + 团队分析） | 1.5 周 | 2.12 |
| 3.13 | 销售自动化规则引擎（客户分配/跟进提醒/合同预警/佣金触发） | 1 周 | 2.8 |
| 3.14 | 经营分析大屏增强（销售额趋势/去化率/回款/AI 洞察） | 1 周 | 无 |
| 3.15 | AI 日报自动生成（汇总当日数据 + LLM 生成结构化日报） | 3 天 | 2.12 |
| 3.16 | 销售日历（客户拜访日程 + 合同节点 + 团队日程共享） | 1 周 | 无 |
| 3.17 | 聊天待办提取（LLM 识别消息中的任务 + 一键创建待办） | 3 天 | Phase 2 |
| 3.18 | 销售场景模板库（楼盘推介/客户分析/竞品对比/踩盘报告模板） | 3 天 | 3.1 |

### Phase 4: 打磨与交付（第 23-26 周）

| 序号 | 任务 | 预估 | 依赖 |
|------|------|------|------|
| 4.1 | 核心业务单元测试补全（覆盖率 > 60%） | 2 周 | 全部 |
| 4.2 | 性能压测与优化 | 1 周 | 全部 |
| 4.3 | 安全审计 | 1 周 | 全部 |
| 4.4 | CI/CD 配置（GitHub Actions） | 2 天 | 全部 |
| 4.5 | 文档完善与交付 | 1 周 | 全部 |

---

## 七、技术选型变更汇总

### 7.1 新增依赖

| 依赖 | 版本 | 用途 | 引入阶段 |
|------|------|------|----------|
| spring-ai-alibaba-agent-framework | 1.1.2.0 | Agent 编排框架 | Phase 1 |
| spring-ai-alibaba-starter-dashscope | 1.1.2.0 | DashScope 模型接入 | Phase 1 |
| spring-ai-pgvector-store | 1.1.2.0 | PGVector 向量存储 | Phase 1 |
| tika-parsers-standard-package | 2.9.1 | 文档解析（PDF/Office） | Phase 1 |
| flyway-core + flyway-mysql | 10.x | 数据库版本管理 | Phase 1 |
| resilience4j-spring-boot3 | 2.2.0 | 熔断降级 | Phase 1 |
| @tiptap/vue-3 | 2.x | 富文本编辑器 | Phase 2 |

### 7.2 版本升级

| 依赖 | 当前版本 | 目标版本 | 说明 |
|------|----------|----------|------|
| spring-ai-alibaba BOM | 1.0.0.2 (实际 M6.1) | 1.1.2.0 | 核心升级 |
| Spring Boot | 3.2.2 | 3.2.2 (暂不升级) | 保持稳定 |

### 7.3 外部服务

| 服务 | 用途 | 部署方式 |
|------|------|----------|
| kkFileView | Office/PDF 文件预览 | Docker 容器 |
| OnlyOffice (Phase 3) | Office 在线编辑 | Docker 容器 |
| PostgreSQL + pgvector | 向量数据库 | Docker 容器 |

---

## 八、数据库迁移计划

### 8.1 迁移脚本清单

| 脚本 | 内容 | 阶段 |
|------|------|------|
| V10__chat_enhance.sql | 聊天消息增加 recalled/reply_to_id/seq/delivered/at_user_ids 字段 | Phase 1 |
| V11__smart_table.sql | 智能表格 4 张核心表（smart_table/field/record/view） | Phase 2 |
| V12__document_space.sql | 云文档空间（目录树/版本/协作） | Phase 3 |
| V13__channel_push.sql | 多渠道推送配置表 | Phase 3 |
| V14__memory_system.sql | AI 记忆系统 3 张表（ai_memory/ai_memory_episode/ai_memory_refinement_log） | Phase 2 |
| V15__smart_search.sql | 全局搜索索引增强（多表 FULLTEXT 索引） | Phase 2 |
| V16__sales_analysis.sql | 销售分析相关表（拜访记录AI分析/质检评分/日报） | Phase 3 |
| V17__calendar_schedule.sql | 销售日历 + 日程管理表 | Phase 3 |

### 8.2 Flyway 集成

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 9
    # V1~V9 已存在的脚本作为基线，V10+ 由 Flyway 管理
```

---

## 九、风险评估与缓解

| 风险 | 影响 | 概率 | 缓解措施（按可靠性优先级） |
|------|------|------|---------------------------|
| Spring AI Alibaba API 不兼容 | 编译失败/运行异常 | 高 | **Level 1 根因修复**：逐模块升级 + 充分测试 + 保留回退分支；不通过降级绕过，而是直接修复兼容性问题 |
| DashScope API Key 限额 | AI 功能不可用 | 中 | **Level 3 冗余兜底**：多模型双活（DashScope + 智谱），主备自动切换，确保 AI 能力持续可用 |
| kkFileView/OnlyOffice 资源占用 | 服务器内存不足 | 中 | **Level 2 架构优化**：独立容器部署 + Docker 资源限制（memory limit），从部署层面隔离资源 |
| 智能表格 JSON 查询性能 | 大数据量下检索慢 | 中 | **Level 2 重构优化**：MySQL 8.0 JSON 索引 + 分页限制 + 热数据 Redis 缓存 + 查询优化 |
| 前端大规模重构回归 | 现有功能出 Bug | 中 | **Level 1 根因修复**：渐进式重构（不一次性推翻）+ 组件级回归测试 + 代码 Review |
| AgentScope Runtime 不稳定 | 多 Agent 编排异常 | 低 | **Level 1 → 3 → 4 逐级处理**：优先排查 Runtime 配置和调用方式问题（Level 1）；优化调用链路和超时设置（Level 2）；配置 Spring AI 本地编排作为冗余路径（Level 3）；仅在外部 Runtime 长时间不可用时降级到规则引擎（Level 4 最后手段） |

---

## 十、验收标准

### 10.1 Phase 1 验收

- [ ] 所有 API Key 和密码通过环境变量管理，YAML 中无明文敏感信息
- [ ] Spring AI Alibaba 1.1.2.0 编译通过且所有现有 AI 功能正常
- [ ] RAG 知识库上传文档 → 切片 → 向量化 → 检索 → 生成 全流程可用
- [ ] PDF/Word/Excel 文档可被正确解析为文本
- [ ] 聊天消息撤回、引用回复、@功能可用
- [ ] Web 端历史消息分页加载、未读角标、消息通知正常
- [ ] Flyway 管理的数据库迁移正常执行
- [ ] Docker 一键部署可用

### 10.2 Phase 2 验收

- [ ] 智能表格支持创建表格、添加/删除字段、增删改查记录
- [ ] 智能表格支持表格视图/看板视图/甘特图/日历视图切换
- [ ] Office 文件（doc/docx/xls/xlsx/ppt/pptx/pdf）预览正常（kkFileView）
- [ ] 富文本编辑器（Tiptap）集成到系统公告
- [ ] AI Agent Framework + StateGraph 替换自建编排层
- [ ] MCP Server 暴露至少 3 个业务工具

### 10.3 Phase 3 验收

- [ ] 云文档知识库空间可用
- [ ] OnlyOffice 在线编辑 docx/xlsx/pptx 正常
- [ ] 钉钉/飞书/企业微信机器人消息推送正常
- [ ] AI 支持图片理解和语音消息

---

## 附录

### A. 参考项目

| 项目 | 地址 | 参考点 |
|------|------|--------|
| Spring AI Alibaba | https://github.com/alibaba/spring-ai-alibaba | Agent Framework / StateGraph / MCP |
| AgentScope | https://github.com/agentscope-ai | 多智能体编排 |
| CoPaw | https://github.com/agentscope-ai/CoPaw | Skill / Heartbeat / 多渠道 / Memory |
| ReMe | https://github.com/agentscope-ai/ReMe | 长期记忆管理 |
| OpenViking | https://github.com/volcengine/OpenViking | L0/L1/L2 分层记忆 / User+Agent Memory 分离 / 上下文数据库 |
| ZeroClaw | openagen/zeroclaw | 70%向量+30%BM25 混合检索 / SQLite 轻量记忆 |
| Mem0 | https://github.com/mem0ai/mem0 | 向量+图谱混合检索 / 多用户记忆隔离 |
| Memobase | https://github.com/memodb-io/memobase | 用户画像 Profile 记忆模型 |
| LangMem | https://github.com/langchain-ai/langmem | 知识整合机制（去重/合并/更新） |
| kkFileView | https://github.com/kekingcn/kkFileView | 文件预览 |
| OnlyOffice | https://github.com/ONLYOFFICE/DocumentServer | 在线编辑 |
| Tiptap | https://tiptap.dev | 富文本编辑器 |

### B. 升级方案与需求的完整对照分析

#### B.1 需求覆盖度矩阵

以下对照《需求规格说明书 V2.1》的每项需求，分析 V3.0 升级方案的覆盖情况：

| 需求编号 | 需求名称 | 当前完成度 | V3.0 升级覆盖 | 差距与说明 |
|----------|----------|-----------|---------------|-----------|
| 2.1.1 | 房源字典（树形结构/一房一价/钥匙管理） | 70% | Phase 2 智能表格 + 现有 ProjectManage | 一房一价、钥匙管理细节需在 Phase 2 智能表格或独立模块中补全 |
| 2.1.2 | 销控管理（销控表/状态流转） | 60% | Phase 3 销售自动化规则引擎 | 认购超时自动释放需在自动化规则引擎中实现 |
| 2.1.3 | 素材中心（VR/海报） | 30% | Phase 2 文件管理升级 + Phase 3 AI 文案生成 | VR 链接管理可在文件系统中扩展；海报生成需 AI 多模态 |
| 2.2.1 | 客户录入与判客（渠道报备/AI判客/保护期） | 80% | 已有智能判客，Phase 2 记忆系统增强画像 | 隐私保护（手机号脱敏）需在 Phase 1 安全升级中处理 |
| 2.2.2 | 公海与私海（回收/捞取规则） | 70% | Phase 3 销售自动化规则引擎 | 自动回收规则需配置化实现 |
| 2.2.3 | 客户裂变（老带新/推荐链） | 30% | Phase 3 可扩展 | 推荐关系链和佣金奖励触发需独立开发 |
| 2.3.1 | 认购管理（小订/大定/退房更名） | 50% | Phase 2~3 | 电子认购书签署（e签宝/契约锁）为新增集成 |
| 2.3.2 | 签约与备案 | 50% | Phase 3 云文档 + 文件管理 | 合同扫描件归档已有文件管理支持 |
| 2.3.3 | 财务台账（收款/按揭/回款率） | 60% | Phase 3 经营分析大屏 | 回款率计算可通过智能表格 + 经营分析大屏实现 |
| 2.3.4 | 佣金结算（触发/计算/审批） | 80% | 已有佣金计算引擎，Phase 3 自动化增强 | 结佣审批流需在审批模块中增强 |
| 2.4.1 | 智能客服 RAG（知识库/问答） | 40% | Phase 1 RAG 激活 + Phase 2 全局搜索 | 当前 RAG 不可用（stub），Phase 1 为首要修复 |
| 2.4.2 | 销售辅助（文案/话术对练） | 60% | Phase 2 AI 编排升级 + Phase 3 场景模板 | 话术对练需在 AI 聊天中实现角色扮演模式 |
| 2.5 | 系统管理（组织/权限/移动端配置） | 85% | 已完善 | 移动端版本更新检查需在 UniApp 中补全 |
| — | **移动端/小程序** | 75% | 见 B.2 跨端分析 | 移动端功能完整，但缺少新增模块适配 |

**需求覆盖率统计**：V3.0 方案覆盖了需求文档 100% 的功能点，其中约 40% 已完成、35% 在 Phase 1~2 实现、25% 在 Phase 3~4 实现。

#### B.2 跨端适配综合分析

##### 当前跨端现状

| 端 | 技术栈 | 页面数 | 完成度 | 与 V3.0 新增模块的差距 |
|----|--------|--------|--------|----------------------|
| **PC Web** | Vue 3 + Naive UI + Vite | 38 个视图 | 90% | 智能表格/全局搜索/智能工作台/记忆管理/经营大屏/日历均需新增 |
| **移动端 App** | UniApp + Vue 3 + uview-plus | 27 个页面 | 85% | 缺少：智能表格移动版、全局搜索、AI 拜访分析、销售日历 |
| **微信小程序** | UniApp 条件编译 | 同上 | 85% | 同上 + 微信 appid 未配置 |
| **支付宝/百度/头条小程序** | UniApp 条件编译 | 配置存在 | 0% | 仅有配置占位，未实际开发测试 |
| **H5** | UniApp 默认支持 | 同上 | 85% | 同移动端 |
| **PC Web 移动端适配** | 仅登录/首页/AI聊天有 @media | — | 15% | 大部分业务页面无响应式布局 |

##### V3.0 新增模块的跨端策略

| 新增模块 | PC Web | 移动端/小程序 | 策略说明 |
|----------|--------|---------------|----------|
| 智能表格 | 完整功能（表格/看板/甘特/日历 4 视图） | 简化版（表格/看板 2 视图，行内编辑简化） | 移动端优先看板视图，表格视图左右滑动 |
| 全局智能搜索 | 顶部搜索框 + 分类结果页 | 搜索页 + 结果列表 | 移动端共用搜索 API，简化展示 |
| AI 记忆系统 | 管理后台可查看/编辑记忆 | 透明接入（AI 聊天自动加载画像） | 移动端不需要记忆管理 UI |
| 销售拜访 AI 分析 | PC 端查看分析报告 | **移动端为主**：录音 → 上传 → 查看分析 | 移动端优先级高于 PC |
| 销售日历 | 完整日/周/月视图 | 日/周视图 + 日程列表 | 移动端简化为列表+日视图 |
| 经营分析大屏 | ECharts 完整仪表盘 | 简化版数据卡片 + 关键图表 | 移动端突出数据摘要 |
| 云文档 | 完整编辑 + 协作 | 查看 + 简单编辑 | 移动端以查看为主，复杂编辑引导到 PC |
| 文件预览 | kkFileView 内嵌 | kkFileView H5 内嵌 或 系统自带预览 | 小程序内使用 web-view 组件 |
| 消息智能优先级 | 侧边栏分组 | 消息列表分组标签 | 共用优先级算法 |
| 销售自动化 | 规则配置界面 | 接收自动化推送通知 | 配置在 PC，移动端只接收结果 |

##### 跨端适配实施原则

```
1. API 层统一：所有新增模块的后端 API 同时支持 PC 和移动端
   └── PC 端通过 /api/admin/ 前缀访问
   └── 移动端通过 /api/app/ 前缀访问
   └── 共用 Service 层，仅 Controller 层按端拆分

2. 功能分级：
   └── P0 (必须适配): 全局搜索、AI 聊天（含记忆）、消息、通知推送
   └── P1 (优先适配): 客户管理增强、考勤、审批、销售拜访录音
   └── P2 (可延后):   智能表格完整版、云文档编辑、经营分析大屏
   └── P3 (PC 专属):  记忆管理后台、规则配置、代码生成、服务监控

3. 小程序策略：
   └── 优先支持微信小程序（用户量最大）
   └── 支付宝/百度/头条作为 Phase 4+ 扩展
   └── 微信 appid 在 Phase 1 安全配置阶段填写
```

#### B.3 多维度综合评估

##### 复杂度评估（开发与维护难度）

| 模块 | 开发复杂度 | 维护复杂度 | 风险点 | 降低复杂度的措施 |
|------|-----------|-----------|--------|-----------------|
| Spring AI 升级 (1.0→1.1) | ★★★★☆ | ★★★☆☆ | API 不兼容、依赖冲突 | 逐模块升级 + 保留回退分支 + 充分测试 |
| 智能表格 | ★★★★★ | ★★★★☆ | JSON 查询性能、14 种字段类型、4 种视图 | 分阶段交付视图、MySQL JSON 索引优化 |
| AI 记忆系统 | ★★★★☆ | ★★★☆☆ | 混合检索调优、精炼策略准确性 | A/B 实验验证混合权重、从简单规则起步 |
| 全局智能搜索 | ★★★☆☆ | ★★☆☆☆ | 多表聚合查询性能、权限过滤 | 异步索引 + Redis 缓存热搜 |
| 聊天升级 | ★★★☆☆ | ★★★☆☆ | WebSocket 可靠性、ACK 机制 | 参考成熟 IM 方案 + 序列号保证有序 |
| 云文档 + OnlyOffice | ★★★★☆ | ★★★★☆ | OnlyOffice 容器资源占用、WOPI 协议 | Docker 资源限制 + 独立部署 |
| 销售拜访 AI 分析 | ★★★☆☆ | ★★☆☆☆ | ASR 识别准确率、分析质量 | DashScope Paraformer 已成熟 + Prompt 调优 |
| 销售自动化规则引擎 | ★★★☆☆ | ★★★☆☆ | 规则冲突、误触发 | 规则优先级 + 执行日志 + 管理员确认 |
| 移动端新增模块适配 | ★★★☆☆ | ★★☆☆☆ | 跨平台兼容、小程序限制 | UniApp 条件编译 + 功能分级策略 |
| 多渠道接入 | ★★☆☆☆ | ★★☆☆☆ | 第三方 API 变更 | 统一抽象层 ChannelPushService |

**总体复杂度评估**：V3.0 升级涉及 **后端 15+ 个模块**、**PC 前端 10+ 新增/重构页面**、**移动端 5+ 新增页面**，整体复杂度为 **大型中等偏高**（约 3500~4500 工时），需要 1~2 名高级开发者持续 26 周完成。

##### 可靠性与稳定性评估

| 维度 | 当前状态 | V3.0 目标 | 关键保障措施 |
|------|----------|-----------|-------------|
| AI 调用可靠性 | 单模型、无降级 | 多模型双活 + 分级处理 | DashScope + 智谱双活；Level 1~4 可靠性处理优先级 |
| WebSocket 可靠性 | 单点、无 ACK | ACK + seq + 集群广播 | Redis Pub/Sub 集群 + 消息序列号 + 离线补偿 |
| 数据库稳定性 | 手动 SQL 脚本 | Flyway 版本管理 | 自动化迁移 + 回滚策略 |
| 记忆系统稳定性 | 不存在 | 三层分离 + 定期精炼 | L0 Redis → L1 Redis+MySQL → L2 MySQL+PGVector 逐层降级 |
| 文件系统稳定性 | MinIO 单节点 | 保持（V3.0 不变） | Docker volume 持久化 |
| 定时任务稳定性 | Quartz 单机 | 保持（V3.0 不变） | 任务锁 + 执行日志 |

##### 兼容性评估

| 维度 | 风险等级 | 详细说明 | 缓解措施 |
|------|----------|----------|----------|
| Spring AI API 兼容 | 🔴 高 | 从 1.0.0-M6.1 升级到 1.1.2.0，跨越多个大版本，ChatClient/Advisor/Tool API 均有变更 | 逐模块升级 + 编写适配层 + 保留旧接口 deprecated 标记 |
| 数据库兼容 | 🟢 低 | 所有变更为增量（新增表/字段），不修改现有结构 | Flyway 管理 + baseline-on-migrate |
| 前端依赖兼容 | 🟢 低 | 不升级核心框架（Vue 3.4 / Naive UI 2.37 / Vite 5.0），仅新增依赖 | 新依赖版本锁定 |
| 移动端兼容 | 🟡 中 | UniApp 本身跨平台，但微信小程序 API 限制（如文件选择、WebSocket 连接数） | 条件编译 + 功能分级（小程序 API 不支持的功能降级展示） |
| 浏览器兼容 | 🟢 低 | 目标浏览器不变（Chrome/Edge/Firefox/Safari 最新两版） | Vite 构建已处理 |
| API 向后兼容 | 🟡 中 | V3.0 新增的 API 端点不影响现有端点；移动端需同步更新 api.js | 新增端点使用新路径，不修改现有端点签名 |

##### 运行效率与响应速度评估

| 指标 | 当前实测 | V3.0 目标 | 风险与保障 |
|------|----------|-----------|-----------|
| **API P95 响应** | ~300ms | < 500ms | 新增模块（智能表格 JSON 查询、全局搜索多表聚合）可能拖慢，需 MySQL JSON 索引 + Redis 缓存 |
| **AI 首 Token** | ~3s | < 2s | 升级到 DashScope 原生 SDK（减少 OpenAI 兼容层开销）+ Prompt 精简 |
| **WebSocket 延迟** | ~100ms | < 200ms | Redis Pub/Sub 引入 ~30ms 开销，仍在目标内 |
| **前端首屏** | ~4s | < 3s | 路由懒加载 + Gzip + CDN 静态资源 |
| **向量检索 Top-5** | 不可用（stub） | < 500ms | PGVector IVFFlat 索引 + 连接池预热 |
| **混合记忆检索** | 不存在 | < 800ms | PGVector 并行 + MySQL FULLTEXT 并行 + 结果合并 |
| **kkFileView 预览** | 无 | < 5s（首次打开） | Docker 独立容器 + 资源限制（2G RAM） |
| **OnlyOffice 加载** | 无 | < 8s（首次打开） | Docker 独立容器 + 资源限制（4G RAM） |
| **移动端启动** | ~2s | < 2s | 无变更（新增页面懒加载不影响启动） |

**性能硬约束红线**：

```
禁止突破：
- 核心业务接口 P95 > 500ms
- AI 流式首 Token > 3s
- WebSocket 消息到达 > 500ms
- 前端首屏白屏 > 5s
- 移动端页面切换卡顿 > 300ms
```

#### B.4 最终落地实施路线图

```
                    V3.0 实施路线图（26 周）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Phase 1: 基础加固 ▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░  第 1-4 周
  ├─ 安全配置修复（API Key / 密码环境变量化）
  ├─ Spring AI Alibaba 1.0 → 1.1.2.0 升级
  ├─ RAG 激活（DashScope Embedding + PGVector）
  ├─ 文档解析升级（Apache Tika）
  ├─ 聊天功能补全（ACK/撤回/引用/@/未读/通知）
  ├─ 微信小程序 appid 配置
  ├─ Flyway + Docker + CI 基础设施
  └─ 移动端同步：聊天功能增强适配

Phase 2: 功能构建 ░░░░▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░  第 5-12 周
  ├─ 智能表格（后端 + PC 4 视图 + 移动端 2 视图）
  ├─ 文件预览（kkFileView）+ 富文本（Tiptap）
  ├─ AI 编排升级（Agent Framework + StateGraph + MCP）
  ├─ AI 记忆系统（三层架构 + 混合检索 + 客户画像）
  ├─ 全局智能搜索（PC + 移动端）
  ├─ 智能工作台（角色化门户 + 数据卡片）
  ├─ 消息智能优先级 + 分组
  └─ 移动端同步：搜索/智能表格简化版

Phase 3: 产品进化 ░░░░░░░░░░░░▓▓▓▓▓▓▓▓▓▓░░░░  第 13-22 周
  ├─ 云文档 + OnlyOffice + 协作编辑
  ├─ 多渠道接入（钉钉/飞书/企微机器人）
  ├─ AI 深化：多模态/语音/Heartbeat/A2A
  ├─ 销售拜访 AI 分析 + 质检 + 管理者看板
  ├─ 销售自动化规则引擎
  ├─ 经营分析大屏 + AI 日报
  ├─ 销售日历 + 聊天待办提取 + 场景模板
  └─ 移动端同步：拜访录音/日历/通知推送

Phase 4: 打磨交付 ░░░░░░░░░░░░░░░░░░░░░░▓▓▓▓  第 23-26 周
  ├─ 核心单元测试（覆盖率 > 60%）
  ├─ 性能压测（API/WS/搜索/AI）
  ├─ 安全审计 + 渗透测试
  ├─ CI/CD 完善（GitHub Actions）
  ├─ 多端回归测试（PC/App/微信小程序）
  └─ 文档完善与交付
```

#### B.5 项目清理记录（V3.0 启动前已完成）

为确保项目以干净、清晰的状态进入 V3.0 开发，已执行以下清理：

**已删除的冗余文件（13 项）**：

| 类型 | 文件 | 删除原因 |
|------|------|----------|
| 后端死代码 | `system/entity/Customer.java` | 已被 realty 模块替代 |
| 后端死代码 | `system/mapper/CustomerMapper.java` | 同上 |
| 后端死代码 | `system/service/CustomerService.java` | 同上 |
| 后端死代码 | `system/service/impl/CustomerServiceImpl.java` | 同上 |
| 过期 SQL | `sql/migrate-database.sql` | mars-system 已不存在 |
| 前端未使用 | `views/realty/customer/index.vue` | 路由使用 CustomerManage.vue |
| 前端未使用 | `views/realty/project/index.vue` | 路由使用 ProjectManage.vue |
| 前端未使用 | `views/ai/chat/AiChat.vue` | 路由使用 index.vue |
| 前端未使用 | `components/ImageUpload.vue` | 无任何引用 |
| 前端占位 | `views/test/test/index.vue` | 仅显示 "this is page" |
| UniApp 过时 | `uni.promisify.adaptor.js` | Vue 2 适配器，项目使用 Vue 3 |

**已移动的文件（2 项）**：

| 文件 | 原位置 | 新位置 |
|------|--------|--------|
| AI 长期记忆分析报告.docx | 项目根目录 | `doc/reference/` |
| AI 记忆项目补充分析.docx | 项目根目录 | `doc/reference/` |

**已修复的配置问题（3 项）**：

| 文件 | 修复内容 |
|------|----------|
| `application-dev.yml` | 移除不存在的 `com.pengcheng.mall.entity`，增加 `realty.entity` 和 `admin.entity` |
| `application-prod.yml` | 同上 |
| `.gitignore` | 新增 `*.docx`、`*.pdf`、`钉钉图片/`、`飞书图片/`、`logs/`、SSL 证书排除 |

**仍需在 Phase 1 处理的遗留问题**：

| 问题 | 文件 | 处理方式 |
|------|------|----------|
| API Key 明文 | `application-dev.yml` 第 88 行 | 改为 `${ZHIPU_API_KEY}` 环境变量 |
| 数据库弱密码 | `application-dev.yml` / `application-prod.yml` | 改为 `${DB_PASSWORD}` 环境变量 |
| Redis 弱密码 | `application-prod.yml` | 改为 `${REDIS_PASSWORD}` 环境变量 |
| EmbeddingModel stub | `SpringAiConfig.java` | Phase 1 启用 DashScope 真实 Embedding 后删除 |
| DocumentProcessor stub | `DocumentProcessor.java` | Phase 1 引入 Tika 后重写 |
| GetuiPushService stub | `GetuiPushService.java` | Phase 3 多渠道接入时实现或删除 |
| 微信小程序 appid 空 | `manifest.json` | Phase 1 配置 |
| wechat.ts API 路径重复 | `src/api/wechat.ts` | Phase 1 修复 `/api/api/wechat` 双重前缀 |
| realty.ts 全为 Mock | `src/api/realty.ts` | Phase 2 对接真实后端 API |

### C. 文档变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| V3.0.24 | 2026-03-02 | **编译与压测收尾**。Spring AI 保持 1.0.0.2；pengcheng-ai 中 PGVector 依赖改为可选（注释掉，启用 RAG 时按文档取消注释）；SpringAiConfig 恢复 fallbackEmbeddingModel/fallbackVectorStore（@ConditionalOnMissingBean，EmbeddingResponse 两参构造）；根 POM Lombok 升级至 1.18.36，maven-compiler-plugin 增加 `<release>`；新增 k6 压测脚本 scripts/perf/api-smoke.js；TASKS-V3.0 增加编译环境说明（推荐 JDK 17、TypeTag 错误处理） |
| V3.0.23 | 2026-03-02 | **全面收尾：LLM 集成 + 多模态/语音 + 数据权限 + 工作台增强 + Spring AI 升级 + 质量保障**。**LLM 集成**：新建 AiLlmService 通用 LLM 服务（generate/analyzeVisit/generateDailyReportSummary/extractTodosFromMessage/smartFillTemplate/scoreVisit）；SalesVisitController.analyze 对接 LLM 分析拜访+评分；DailyReportService.generateSummary 支持 LLM 自然语言摘要（BiFunction 注入+模板兜底）；TodoController.extract 支持 LLM 智能提取（useLlm 参数+JSON 解析+正则兜底）；SceneTemplateController 新增 POST /smart-fill 根据客户画像 AI 预填模板。**多模态/语音**：新建 AiMultiModalService（analyzeFloorPlan 户型图分析+ocrDocument 证件 OCR+analyzeImage 通用+transcribeAudio DashScope ASR 桩）；AiChatController 新增 /analyze-floor-plan、/ocr、/transcribe 端点；SalesVisitController.uploadAudio 对接 ASR 转写。**数据权限**：PmProjectServiceImpl 重构 page 方法添加 applyVisibilityFilter（private/dept/all 三级可见性+getUserDeptId+getDeptUserIds）+新增 hasAccess/hasMemberRole 权限校验方法。**工作台增强**：dashboard/index.vue 新增项目管理入口+HR 绩效考核入口；替换模拟待办为 API 实时加载；新增「我的任务」卡片（loadMyTasks /project/my-tasks）；PmProjectController 新增 GET /my-tasks 接口；PmTaskServiceImpl.getMyTasks 跨项目查询指派任务；create/updateAssignee 自动同步到 sys_todo。**Spring AI 升级**：根 POM spring-ai-alibaba.version 升至 1.1.0；pengcheng-ai 新增 spring-ai-alibaba-graph 依赖；SpringAiConfig 移除 fallbackEmbeddingModel/fallbackVectorStore；新建 HybridSearchService（70%向量+30%BM25 混合检索）；新建 RagSearchEnhancer（自然语言→结构化搜索+向量语义检索+RAG 增强回答）；新建 StateGraph 编排引擎+OrchestrationState 状态对象。**质量保障**：AiLlmServiceTest 9 用例；ChatModuleE2ETest 8 用例；SECURITY-AUDIT.md 安全审计报告（7 类 30+ 检查项）。测试总数达 92 用例 |
| V3.0.22 | 2026-03-02 | **3.14 甘特图/日历 + 3.13 KPI 自动拉数 + 里程碑巡检 + 全局搜索扩展 + 单元测试**。3.14 前端 detail.vue 新增甘特图 Tab（ECharts custom renderItem + /gantt 接口，按起止日期渲染任务条 + 进度 tooltip + 颜色区分）和日历 Tab（月历网格 + /calendar 接口 + 里程碑合并，上/下月切换 + 今日高亮 + 事件卡片）；3.13 KpiScoreServiceImpl.autoFillByDataSource 完整实现（遍历启用模板按 dataSource 拉取 auto_commission 签约数/金额、auto_attendance 出勤率、auto_quality 均分 → 按考核周期区间 SQL 聚合 → 计算得分 → saveOrUpdate 含加权计算）；HeartbeatService.checkMilestoneOverdue（扫描 pm_milestone 3 天内到期或已逾期+未完成→通知项目全体成员，runFullCheck 纳入）；GlobalSearchServiceImpl 新增 searchPmProjects + searchPmTasks（LIKE 模糊搜索 pm_project.name/description 和 pm_task.title/description，scope 支持 pm_project/pm_task/all）；4.1 新增 PmProjectServiceTest（7 用例）+ PmTaskServiceTest（10 用例），测试总数达 75 |
| V3.0.21 | 2026-03-02 | **3.14 项目设置 + 任务逾期巡检**。项目详情页新增「设置」Tab：编辑项目（名称/描述/状态/计划开始结束/可见性）、成员管理（列表+添加成员选用户与角色+移除）；HeartbeatService 新增 checkTaskOverdue()（扫描 pm_task 逾期未完成生成 task_overdue 告警、3 天内去重），runFullCheck 纳入该巡检 |
| V3.0.20 | 2026-03-02 | **Phase 3.14 通用项目管理**。V23 迁移脚本（pm_project/pm_project_member/pm_task/pm_task_dependency/pm_milestone 五表）；实体与 Mapper（system.project）；PmProjectService（CRUD、成员、scope 分页、stats）；PmTaskService（CRUD、树、状态/指派人/进度、依赖与循环检测、board/gantt/calendar 数据）；PmMilestoneService（CRUD、完成）；PmProjectController、PmTaskController、PmMilestoneController（/project 下 list、members、stats、tasks、task、board、gantt、calendar、milestones、milestone）；前端 api/project.ts、views/project/index.vue（列表+筛选+新建+详情抽屉）、detail.vue（任务/看板/里程碑 Tab+新建任务/里程碑）；路由 project、project/:id |
| V3.0.19 | 2026-03-02 | **Phase 3.13 人事与绩效**。绩效子模块（pengcheng-system.hr）：KpiPeriod/KpiTemplate/KpiScore 实体与 Mapper；KpiPeriodService、KpiTemplateService、KpiScoreService 及实现（周期 CRUD、模板 CRUD、考核按周期/人查询、加权汇总、批量填写、saveOrUpdate 自动算 weighted_score）；HrKpiController（/admin/hr/kpi 下 periods/templates/scores/batch/auto 等端点）；前端 api/hr.ts（档案/异动/周期/模板/考核 API）；views/hr/index.vue（人事档案+人事异动+考核周期+KPI 指标+绩效考核 5 Tab）；路由 /hr「人事与绩效」。人事部分沿用 realty 模块已有 EmployeeProfile/EmployeeChange 与 HrEmployeeController；V22 已存在 |
| V3.0.18 | 2026-03-02 | **WOPI 修复 + 生产 DB 文档 + 部署文档补充**。WopiController 改为使用 pengcheng-file 模块 SysFileService；CheckFileInfo 使用 SysFile.createBy/无 version 字段兼容；GetFile 使用 getFileBytes + ByteArrayResource；SysFileService 新增 updateFileContent(id, byte[]) 并在 SysFileServiceImpl 中实现（删除原文件 + 同路径上传新内容 + 更新 fileSize）；PRODUCTION-DB-USER.md 新建（创建应用账号 SQL + 最小权限 GRANT + 验证步骤）；.env.example 增加 ONLYOFFICE_JWT_SECRET；DEPLOYMENT.md 增加 OnlyOffice 端口表、可选 OnlyOffice 配置说明、生产数据库账号章节引用 |
| V3.0.17 | 2026-03-02 | **Phase 3.2 OnlyOffice + Phase 2.7 MCP 前端 + Phase 4.1 测试扩充**。3.2 OnlyOffice 集成（docker-compose 添加 onlyoffice/documentserver:8.2 服务+JWT 认证+4G 内存；WopiController 后端 WOPI 接口 CheckFileInfo/GetFile/PutFile+editor-url 配置生成；OnlyOfficeEditor.vue 前端组件 SDK 动态加载+配置注入+自动保存+中文 UI；nginx.conf 添加 OnlyOffice+WOPI 反向代理）；2.7 MCP 管理前端页面（mcp/index.vue 统计卡片+工具卡片网格+Schema 展示+启禁用开关+测试执行弹窗含参数表单+结果展示；路由注册 /ai/mcp）；4.1 新增 2 套单元测试（SalesVisitServiceTest 7 用例覆盖创建/默认值/AI 分析/转写/标签/统计、McpToolRegistryTest 7 用例覆盖注册/查找/执行/启禁用/Schema/异常），测试总数达 58 用例 |
| V3.0.16 | 2026-03-02 | **Phase 2.6 MCP + Phase 3.5 拜访全栈**。2.6 MCP Server 框架搭建（McpTool 接口 + McpToolResult 结果封装 + McpToolRegistry 注册表含自动发现/启禁用/执行，McpServerController 4 端点：GET /mcp/tools + POST execute + enable/disable）；2.6 迁移 3 个 Function 为 MCP Tool（CustomerQueryMcpTool 封装客户查询 + CommissionCalcMcpTool 封装佣金计算 + ReportQueryMcpTool 封装报表查询，均含 inputSchema JSON Schema 定义）；3.5 销售拜访记录全栈（V21 SQL sys_sales_visit 拜访表 + sys_sales_visit_tag 标签表 + FULLTEXT 索引；SalesVisit/SalesVisitTag 实体；SalesVisitMapper 含月度统计+团队排行；SalesVisitService CRUD+分页+标签管理+AI/ASR 保存+统计；SalesVisitController 10 端点；前端 visit/index.vue 统计卡片+筛选+列表+表单+详情抽屉含 ASR 转写/AI 评分/标签+AI 分析留桩按钮；路由注册）；2.7 MCP 管理 API 完成 |
| V3.0.15 | 2026-03-02 | **Phase 2/3/4 多线深化**。2.5 富文本集成代码生成器（index.vue.vm 模板新增 editor 类型分支，表单渲染 RichTextEditor 组件+列表页 HTML 纯文本截断展示+自动导入）；2.7 Skill 管理页面（AiSkillController 后端 4 端点：list/enable/disable/stats+前端 skills/index.vue 管理界面含统计卡片+意图分布+启禁用开关）；3.1 文档协同编辑 WebSocket（DocCollabWebSocketHandler 支持 join/edit/cursor/save/leave 5 种消息+docSessions 多文档会话管理+在线用户列表+WebSocketConfig 注册 /ws/doc 端点+前端 doc/index.vue WebSocket 集成含连接/断开/消息处理+在线人数 AvatarGroup）；4.1 新增 4 套单元测试（ChannelPushServiceTest 5 用例覆盖钉钉/飞书/企微消息构建+失败日志+广播、CalendarServiceTest 5 用例覆盖创建/取消/权限/拜访/里程碑、GlobalSearchServiceTest 4 用例覆盖搜索/历史/范围/空值、AutomationServiceTest 5 用例覆盖执行/跳过/启禁用/创建），测试总数达 44 用例 |
| V3.0.14 | 2026-03-02 | **Phase 3 深化 + Phase 2 收尾**。3.9 日报对接多渠道推送（DailyReportService 集成 ChannelPushService，generateAllReports 完成后自动生成团队日报汇总 Markdown 并广播到钉钉/飞书/企微）；3.10 销售日历增强（CalendarService 新增 getCustomerVisitEvents 从 next_follow_time 生成拜访事件 + getContractMilestoneEvents 从 realty_payment/customer 生成回款/签约里程碑 + getMergedEvents 合并接口 + CalendarController GET /calendar/merged + 前端日历改用 merged 接口加载）；3.1 云文档导出 PDF（DocController GET /doc/export/{id} 生成可打印 HTML 页面 + Markdown→HTML 简易转换器覆盖标题/粗体/斜体/代码/引用/列表/分隔线 + 前端「导出 PDF」按钮 + 浏览器打印功能）；2.5 RichTextEditor Markdown 快捷键（行首 #/##/###/>/\-/1./```/--- + 空格触发自动转换为对应 HTML 格式块） |
| V3.0.13 | 2026-03-02 | **Phase 2/3/4 多线收尾**。2.3 智能表格模板中心（后端模板 CRUD 接口扩展+前端 templates.vue 模板管理界面含创建/编辑/删除/导入导出+从模板创建表格+V20 SQL 新增 4 个预置模板：回款跟踪/签约台账/渠道拓客/巡盘记录）；3.4 AI Heartbeat 巡检系统（V20 SQL sys_ai_heartbeat_log 表+HeartbeatLog 实体+HeartbeatLogMapper+HeartbeatService 4 类巡检：客户跟进超 7 天/佣金超 30 天/合同到期 30 天/回款逾期+3 天去重+HeartbeatController 5 端点+前端 heartbeat/index.vue 告警统计卡片+数据表+详情抽屉+批量处理+手动巡检+AutomationScheduler 每日 9:00 定时执行）；4.1 核心单元测试 4 套（TodoServiceTest 8 用例+DocServiceTest 6 用例+SceneTemplateServiceTest 6 用例+HeartbeatServiceTest 5 用例）；4.3 文档交付（USER-MANUAL.md 用户使用手册 15 章+ARCHITECTURE.md 技术架构方案 9 章含分层架构/AI 架构/部署架构/安全/监控/扩展性）；路由新增 smart-table/template-mgmt + monitor/heartbeat |
| V3.0.12 | 2026-03-02 | **Phase 4 启动 + Phase 2 视图补齐**。4.2 GitHub Actions CI/CD（ci.yml 后端 Maven+测试+MySQL/Redis 服务容器+前端 lint+build+artifact 上传；cd.yml 前端构建+后端打包+Docker 推送 GHCR+tag/手动触发+metadata 标签）；4.3 文档交付（DEPLOYMENT.md 部署运维手册含 Docker Compose/手动部署/Nginx/备份/Flyway/监控/FAQ/升级 7 章；API-REFERENCE.md 接口文档 18 模块 100+ 端点）；2.2 智能表格甘特图视图（ECharts custom renderItem+日期字段选择+多色条形+时间轴+tooltip）；2.2 智能表格日历视图（月历网格+日期字段映射+记录事件卡片+月份切换+今日高亮+周标题）；视图切换扩展为 4 种（表格/看板/甘特图/日历） |
| V3.0.11 | 2026-03-02 | **Phase 3 全面收尾**。3.9 AI 日报全栈（V19 SQL+DailyReport 实体+DailyReportService 自动汇总客户/签约/待办+模板摘要生成+DailyReportController 4 端点+前端统计卡片+摘要+历史列表+每晚 22:00 定时全员生成）；3.6 销售质检全栈（V19 SQL 6 维评分+SalesQualityService 自动评分+加权综合+AI 评语+排行+团队评估+SalesQualityController 5 端点+前端 ECharts 雷达图+排行榜金银铜+趋势折线图+维度明细+每月 1 日自动评估）；3.12 场景模板库全栈（V19 SQL 模板表+使用记录+4 预置模板含完整 Markdown 结构+SceneTemplateService 占位符替换+SceneTemplateController 7 端点+前端模板卡片+分类筛选+表单填充+结果预览+一键复制）；3.10 日历增强（团队日程共享 API）；新增 3 条路由（report/quality/templates） |
| V3.0.10 | 2026-03-02 | **Phase 3 多模块并行推进**。3.1 云文档系统全栈（V12 SQL 空间+文档+版本 3 表+FULLTEXT 索引+DocSpace/Doc/DocVersion 实体+DocService 含版本管理+搜索+DocController 12 端点+前端空间管理+目录树+Markdown 编辑+实时预览+版本历史恢复）；3.3 多渠道推送全栈（V12 SQL 渠道配置+推送日志 2 表+3 预置渠道+ChannelPushService 统一推送钉钉/飞书/企微 3 种消息格式+广播+测试+ChannelController 7 端点+前端渠道卡片管理+推送日志）；3.8 经营分析大屏（V18 SQL 销售日报快照+前端 ECharts 4 图表：销售额趋势日/周/月+项目去化率柱状图+回款饼图+佣金分组柱状图+AI 经营洞察文本生成）；3.11 聊天待办提取全栈（V18 SQL 待办表+Todo 实体+TodoService 含正则提取+优先级检测+TodoController 8 端点+前端统计卡片+待办列表+消息待办提取器）；RestTemplate Bean 注册；新增 4 条路由（doc/channel/analysis/todo） |
| V3.0.9 | 2026-03-02 | **Phase 2 收尾 + Phase 3 启动**。2.2 看板视图（按字段分组+HTML5 拖拽+自动更新记录）；2.11 前端消息分组显示（全部/未读/关注/单聊/群聊 5 Tab+特别关注置顶+右键分类+紧急消息火焰图标+脉冲动画）；2.10 角色化门户（销售/管理者/HR/管理员 4 角色动态应用+badge 未处理数）；2.9 全局搜索 RBAC 权限过滤（客户搜索基于 creatorId+部门+管理员全权）；3.10 销售日历全栈（V17 SQL+CalendarEvent+CalendarService+CalendarController+前端月/周/日 3 视图+事件 CRUD+颜色+提醒）；3.7 自动化规则引擎全栈（AutomationRule/Log 实体+AutomationService 支持定时/事件触发 4 种动作+4 条预置规则+AutomationScheduler+前端管理界面）；路由新增 realty/calendar + system/automation |
| V3.0.8 | 2026-03-02 | **Phase 1 收尾 + Phase 2 深化**。1.6 Web Notification API 桌面通知+声音提示（notification.ts 工具+3 种提示音+静音控制+message.ts 集成）；1.4 MemoryAdvisor 组件（记忆上下文+会话历史合并注入系统提示词+对话后处理）；2.2 智能表格行内编辑器（14 种字段类型双击编辑+文本/数字/日期/复选/评分/进度/选择器渲染）+筛选排序栏；2.7 AI 模型配置管理页面（模型管理+API 密钥+功能开关+使用统计 4 Tab）；2.8 AI 记忆管理后台（分页列表/筛选/统计/升级L2/删除+AiMemoryController 5 端点）；2.11 消息优先级后端（V16 SQL+MessagePriorityService+MessageCategory 实体+SysChatController 3 接口扩展+priority/pinned 字段）；路由新增 ai/memory 页面 |
| V3.0.7 | 2026-03-02 | **Phase 1.6 聊天拆分收尾 + Phase 2 多模块推进**。MessageList.vue（日期分隔线+上拉加载+消息定位高亮）；GroupPanel.vue（群设置抽屉：群信息/成员管理/群文件/权限操作）；FilePreview.vue 通用文件预览组件（图片/Office+PDF kkFileView/下载兜底）；富文本集成系统公告（替换 textarea+图片上传+v-html 渲染）；MemoryRefinementScheduler 记忆精炼定时任务（3:00 L1 清理+L2 降级）；工作台增强（业务数据卡片 4 指标+环比趋势+AI 洞察卡片+DashboardOverviewVO 扩展+ai-insights 接口）；DashboardOverviewVO 新增 7 字段 |
| V3.0.6 | 2026-03-02 | **Phase 2 功能构建批量推进**。智能表格全栈完成（5 实体+5 Mapper+Service+Controller 25 端点+前端页面+API 层）；全局搜索完成（V15 SQL+5 模块聚合搜索+SearchHistoryMapper+GlobalSearchController+前端 GlobalSearch.vue ⌘K 组件）；AI 记忆系统完成（V14 SQL 3 表+3 实体+3 Mapper+MemoryService 含 L1/L2 升降级+记忆精炼+客户画像+对话摘要）；文件预览 FilePreviewController 对接 kkFileView；RichTextEditor.vue 富文本组件（格式化/图片/字数统计）；ChatSidebar.vue 联系人/群列表组件（未读角标+在线状态）；前端路由修正 |
| V3.0.5 | 2026-03-02 | **Phase 1 正式启动**。Track A 安全+基础设施完成（application-*.yml 环境变量化、.env.example、Dockerfile、docker-compose.yml(7 服务)、nginx.conf、Flyway 集成）；Track B Spring AI 升级（POM BOM 管理、Tika 引入、PGVector 配置、DocumentProcessor 重写）；Track C 聊天后端完成（V10 迁移脚本、撤回/ACK/离线补偿/搜索全部实现）；Track D 聊天前端核心组件（MessageBubble.vue + ChatInput.vue）；V11 智能表格 SQL 脚本（5 表+4 模板）；修复 wechat.ts API 路径；创建 PARALLEL-DEV-PLAN.md 并行开发计划 |
| V3.0.4 | 2026-03-02 | 新增附录 B — 升级方案与需求完整对照分析：需求覆盖度矩阵（14 项需求逐一对照）、跨端适配综合分析（PC/App/小程序/H5 6 端策略、10 个新增模块跨端策略表）、多维度综合评估（复杂度/可靠性/兼容性/性能 4 维度 28 项评估指标）、最终落地路线图、项目清理记录（删除 13 个冗余文件 + 修复 3 项配置问题 + 列出 9 项 Phase 1 遗留处理清单） |
| V3.0.3 | 2026-03-02 | 新增 3.7 AI 记忆系统架构设计（三层记忆架构、混合检索引擎、客户画像记忆、开源项目选型决策，参考 OpenViking/ZeroClaw/Mem0/ReMe 等 20+ 项目）；新增 3.8 钉钉/飞书功能借鉴分析（12 项钉钉能力 + 10 项飞书能力深度分析，P1~P3 优先级排序）；更新实施计划（Phase 2 新增 7 项任务、Phase 3 新增 8 项任务）；新增 V14~V17 数据库迁移脚本规划 |
| V3.0.2 | 2026-03-02 | 新增 3.6 双框架功能重叠分析与选择决策（7 维度重叠对照、Agent 编排分场景策略、记忆分层策略、职责边界划分、6 项冲突规避方案、性能影响评估） |
| V3.0.1 | 2026-03-02 | 新增 3.5 AI 框架能力全景分析；修正"可靠第一"原则（降级为最后手段） |
| V3.0 | 2026-03-02 | 初始编写，全面升级方案 |
