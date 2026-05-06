# 鹏程房产销售管理系统 - 技术架构方案

**版本**: V3.0 Final  
**更新日期**: 2026-03-02

---

## 1. 系统总览

### 1.1 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.2.2 |
| **持久层** | MyBatis Plus | 3.5.5 |
| **认证授权** | Sa-Token | 1.37.0 |
| **AI 框架** | Spring AI Alibaba | 1.0.0-M6.1 |
| **数据库** | MySQL | 8.0+ |
| **缓存** | Redis | 7.0+ |
| **向量数据库** | PostgreSQL + PGVector | 16+ |
| **对象存储** | MinIO | Latest |
| **文件预览** | kkFileView | 4.x |
| **前端框架** | Vue 3 + TypeScript | 3.4+ |
| **UI 组件库** | Naive UI | 2.38+ |
| **图表** | ECharts | 5.x |
| **构建工具** | Vite | 5.x |
| **容器化** | Docker + Docker Compose | Latest |
| **CI/CD** | GitHub Actions | - |
| **反向代理** | Nginx | Latest |

### 1.2 架构风格

采用 **单体模块化架构**（Modular Monolith），在保持部署简单性的同时实现良好的模块化：

```
pengcheng-admin/
├── pengcheng-starter/          # 启动模块（主入口 + Flyway 迁移）
├── pengcheng-common/           # 公共模块（工具类/异常/响应体）
├── pengcheng-core/             # 核心业务模块
│   ├── pengcheng-system/       # 系统管理 + 通用业务
│   ├── pengcheng-realty/       # 房产业务核心
│   ├── pengcheng-ai/           # AI 能力层
│   └── pengcheng-message/      # 消息/通知
├── pengcheng-api/              # API 接口层
│   ├── pengcheng-admin-api/    # 管理端 API
│   └── pengcheng-app-api/      # 移动端 API
└── pengcheng-ui/               # Vue 3 前端
```

---

## 2. 后端架构

### 2.1 分层架构

```
┌─────────────────────────────────────────┐
│              API 层 (Controller)         │
│  pengcheng-admin-api / pengcheng-app-api │
├─────────────────────────────────────────┤
│              业务层 (Service)             │
│  pengcheng-system / realty / ai / message│
├─────────────────────────────────────────┤
│              数据层 (Mapper/DAO)          │
│         MyBatis Plus BaseMapper          │
├─────────────────────────────────────────┤
│              基础层 (Common)             │
│        工具类 / 异常 / 响应体 / 配置      │
└─────────────────────────────────────────┘
```

### 2.2 核心模块说明

#### pengcheng-system（系统模块）

负责基础功能和通用业务：

| 子包 | 功能 |
|------|------|
| `system/` | 用户、角色、菜单、部门、岗位、字典、配置 |
| `auth/` | 认证、加密 |
| `file/` | 文件上传/下载/预览 |
| `gen/` | 代码生成器 |
| `smarttable/` | 智能表格（表格/字段/记录/视图/模板） |
| `doc/` | 云文档（空间/文档/版本） |
| `channel/` | 多渠道推送（钉钉/飞书/企微） |
| `todo/` | 待办事项管理 |
| `calendar/` | 日历事件 |
| `automation/` | 自动化规则引擎 |
| `template/` | 销售场景模板 |
| `report/` | AI 日报 |
| `quality/` | 销售质检评分 |
| `heartbeat/` | AI 巡检告警 |
| `search/` | 全局智能搜索 |

#### pengcheng-realty（房产业务模块）

| 子包 | 功能 |
|------|------|
| `customer/` | 客户管理、跟进、判客 |
| `project/` | 项目/楼盘管理 |
| `commission/` | 佣金管理 |
| `payment/` | 回款管理 |
| `alliance/` | 联盟商管理（渠道签到统计引用 pengcheng-hr 签到表） |
| `dashboard/` | 数据仪表盘 |

#### pengcheng-hr（人事与绩效公共服务模块，公司级）

人事与绩效作为**公司级公共服务**，不隶属于房产业务或单一业务线，与飞书/钉钉的「人事」「绩效」定位一致，服务全公司各部门（销售、支持、职能等）。

| 子包 | 功能 |
|------|------|
| `employee/` | 员工档案扩展、人事异动（入职/离职/调岗/调薪） |
| `attendance/` | **假勤**：考勤打卡、请假、调休、签到（公司级） |
| `performance/` | 考核周期、KPI 指标模板、考核记录 |

- 接口路径：`/admin/hr/employee`、`/admin/attendance`（假勤，兼容原路径）、`/admin/hr/kpi`（见 doc/HR-AND-PERFORMANCE-DESIGN.md）
- KPI 数据来源可插拔：手工录入，或由各业务模块（如房产业务的佣金/考勤/质检）对接提供实际值

#### pengcheng-ai（AI 模块）

| 子包 | 功能 |
|------|------|
| `orchestration/` | AI 编排引擎（路由/权限/执行/审计） |
| `chat/` | AI 对话服务 |
| `content/` | 文案生成 |
| `analysis/` | 报表分析 |
| `knowledge/` | RAG 知识库 |
| `memory/` | AI 记忆系统（L1/L2） |
| `experiment/` | A/B 实验平台 |
| `agent/` | Agent 工具集 |

#### pengcheng-message（消息模块）

| 子包 | 功能 |
|------|------|
| `chat/` | 即时聊天（单聊/群聊） |
| `notice/` | 系统通知 |
| `priority/` | 消息优先级服务 |

### 2.3 认证与安全

采用 **Sa-Token** 框架：

- Token 生成与验证
- 角色权限校验（`@SaCheckRole` / `@SaCheckPermission`）
- 多端登录控制
- 接口防重放
- RSA 前端加密传输

数据安全措施：

- 敏感配置使用环境变量注入
- 数据库密码、API Key 不入库
- 手机号脱敏存储
- 操作日志全记录

### 2.4 WebSocket

实时通信架构：

```
客户端 ←→ WebSocket ←→ MessageWebSocketHandler
                           │
                     ┌─────┴─────┐
                     │   Redis   │  (消息中间件 + 在线状态)
                     └───────────┘
```

支持的 WebSocket 事件：

- `message` - 新消息
- `recall` - 消息撤回
- `ack` - 送达确认
- `typing` - 正在输入
- `data-change` - 数据变更推送

### 2.5 数据库设计

#### 数据库迁移管理

使用 **Flyway** 管理数据库版本：

| 版本 | 内容 |
|------|------|
| V1~V9 | 基线（原始表结构） |
| V10 | 聊天增强（撤回/引用/ACK/搜索） |
| V11 | 智能表格（5 表 + 4 模板） |
| V12 | 云文档 + 多渠道推送 |
| V14 | AI 记忆系统 |
| V15 | 全局搜索 + 搜索历史 |
| V16 | 消息优先级 |
| V17 | 日历 + 自动化规则 |
| V18 | 待办 + 经营分析 |
| V19 | 日报 + 质检 + 场景模板 |
| V20 | 扩展模板 + AI 巡检 |
| V21 | 销售拜访分析（拜访记录 + AI 标签） |
| V22 | 人事与绩效（员工档案/异动/KPI 周期/模板/考核记录） |
#### 索引策略

- 业务查询字段建立 B+Tree 索引
- 全文搜索字段建立 FULLTEXT 索引（MySQL InnoDB）
- AI 向量检索使用 PGVector HNSW 索引

### 2.6 定时任务

| 任务 | 频率 | 功能 |
|------|------|------|
| 自动化规则 | 每天 8:00 | 执行时间触发类规则 |
| AI 巡检 | 每天 9:00 | 客户/佣金/合同/回款检查 |
| 日历提醒 | 每 5 分钟 | 检查待发送的日历提醒 |
| AI 日报 | 每天 22:00 | 生成全员工作日报 |
| 销售质检 | 每月 1 日 4:00 | 全员销售能力评估 |
| 记忆精炼 | 每天 3:00 | L1/L2 记忆升降级 |

---

## 3. 前端架构

### 3.1 目录结构

```
pengcheng-ui/
├── src/
│   ├── api/              # API 接口层（按模块拆分）
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   │   ├── ChatSidebar.vue
│   │   ├── MessageList.vue
│   │   ├── ChatInput.vue
│   │   ├── MessageBubble.vue
│   │   ├── GroupPanel.vue
│   │   ├── GlobalSearch.vue
│   │   ├── FilePreview.vue
│   │   └── RichTextEditor.vue
│   ├── composables/      # 组合函数
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia 状态管理
│   ├── utils/            # 工具函数
│   │   ├── request.ts    # Axios 封装
│   │   ├── notification.ts
│   │   └── message.ts
│   └── views/            # 页面视图
│       ├── chat/         # 聊天
│       ├── dashboard/    # 工作台
│       ├── doc/          # 云文档
│       ├── monitor/      # 监控
│       ├── realty/       # 房产业务
│       ├── smarttable/   # 智能表格
│       ├── system/       # 系统管理
│       └── todo/         # 待办
```

### 3.2 关键技术决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 状态管理 | Pinia | Vue 3 官方推荐，TypeScript 友好 |
| HTTP 客户端 | Axios | 拦截器/重试/Token 刷新 |
| 图表 | ECharts 5 | 功能丰富，雷达图/甘特图/日历等 |
| 拖拽 | HTML5 DnD API | 原生支持，无额外依赖 |
| 富文本 | contenteditable | 轻量级，后续可升级 Tiptap |
| Markdown | 自定义解析 | 轻量级文档编辑 |

### 3.3 路由与菜单架构

侧边栏菜单由后端 `sys_menu` 表驱动，按"业务优先"原则组织为 9 个一级目录：

```
/login                              # 登录
/                                   # 主布局
├── /dashboard                      # 首页（固定前置）
│
├── 房产业务 (sort=1)
│   ├── /realty/customer            # 客户管理
│   ├── /realty/customer-pool       # 客户公海池
│   ├── /realty/alliance            # 联盟商管理
│   ├── /realty/project             # 项目楼盘
│   ├── /realty/commission          # 成交佣金
│   ├── /realty/payment             # 付款申请
│   ├── /realty/visit               # 拜访记录
│   ├── /realty/calendar            # 销售日历
│   ├── /realty/report              # AI 日报
│   ├── /realty/quality             # 销售质检
│   ├── /realty/templates           # 场景模板
│   ├── /realty/analysis            # 经营分析
│   └── /realty/stats               # 数据统计
│
├── 智能助手 (sort=2)
│   ├── /ai/chat                    # AI 助手
│   ├── /ai/knowledge               # 知识库管理
│   ├── /ai/experiment              # AI 实验
│   ├── /ai/config                  # 模型与技能
│   ├── /ai/memory                  # AI 记忆
│   ├── /ai/skills                  # Skill 管理
│   └── /ai/mcp                    # MCP 工具
│
├── 协作办公 (sort=3)
│   ├── /message/chat               # 即时聊天
│   ├── /contacts                   # 通讯录
│   ├── /message/notice             # 系统通知
│   ├── /meeting/calendar           # 会议日历
│   ├── /doc                        # 云文档
│   ├── /smart-table                # 智能表格
│   ├── /smart-table/template-mgmt  # 表格模板管理
│   ├── /todo                       # 待办事项
│   └── /project                    # 项目管理
│
├── 人事管理 (sort=4)
│   ├── /hr                         # 人事档案
│   ├── /realty/attendance          # 考勤打卡
│   ├── /hr/performance             # 绩效考核
│   └── /hr/review-360             # 360度评估
│
├── 组织管理 (sort=5)
│   ├── /org/dept                   # 部门管理
│   └── /org/post                   # 岗位管理
│
├── 系统管理 (sort=6)
│   ├── /system/user                # 用户管理
│   ├── /system/role                # 角色管理
│   ├── /system/menu                # 菜单管理
│   ├── /system/dict                # 字典管理
│   ├── /system/config              # 系统配置
│   ├── /system/file                # 文件列表
│   ├── /system/file-config         # 文件配置
│   ├── /system/channel             # 渠道推送
│   └── /system/automation          # 自动化规则
│
├── 系统监控 (sort=7)
│   ├── /monitor/online             # 在线用户
│   ├── /monitor/job                # 定时任务
│   ├── /monitor/cache              # 缓存监控
│   ├── /monitor/server             # 服务监控
│   ├── /monitor/server-manager     # 服务器管理
│   └── /monitor/heartbeat          # AI 巡检
│
├── 系统日志 (sort=8)
│   ├── /log/operlog                # 操作日志
│   └── /log/loginlog               # 登录日志
│
└── 开发工具 (sort=99)
    └── /tool/gen                   # 代码生成
```

---

## 4. AI 架构

### 4.1 AI 编排引擎

```
用户请求
    │
    ▼
┌──────────┐    ┌──────────┐    ┌──────────┐
│ AI Router │──▶│ 权限守卫  │──▶│ 工具执行  │
│ (意图路由) │    │(Permission│    │(Function │
│           │    │  Guard)   │    │  Call)   │
└──────────┘    └──────────┘    └──────────┘
    │                               │
    ▼                               ▼
┌──────────┐               ┌──────────┐
│ 记忆注入  │               │ 审计日志  │
│(Memory   │               │(Audit    │
│ Advisor) │               │  Log)    │
└──────────┘               └──────────┘
```

### 4.2 记忆系统

双层记忆架构：

| 层级 | 存储 | 生命周期 | 用途 |
|------|------|----------|------|
| L1 短期 | MySQL | 7 天 | 近期对话上下文 |
| L2 长期 | MySQL + FULLTEXT | 永久 | 客户画像、行业知识 |
| 向量索引 | PGVector（规划中） | 永久 | 语义检索 |

精炼流程：
- 每日凌晨 3:00 执行精炼任务
- L1 超 7 天且低重要度的记忆清理
- L2 低访问频率的记忆降级

### 4.3 A/B 实验平台

支持 AI 策略的在线实验和效果评估：

- 流量分配（按用户 ID Hash）
- 实验配置管理
- 效果指标追踪
- 实验报告生成

---

## 5. 部署架构

### 5.1 Docker Compose 架构

```
┌──────────────────────────────────────────────────┐
│                    Nginx (80/443)                  │
│           反向代理 + SSL + 静态资源                  │
└───────────┬──────────┬──────────┬─────────────────┘
            │          │          │
     ┌──────▼──┐  ┌───▼───┐  ┌──▼─────────┐
     │  App    │  │  Vue  │  │ kkFileView │
     │ (8080) │  │ (静态) │  │  (8012)    │
     └────┬───┘  └───────┘  └────────────┘
          │
   ┌──────┼──────┬──────────┐
   │      │      │          │
┌──▼──┐ ┌─▼──┐ ┌▼───────┐ ┌▼─────┐
│MySQL│ │Redis│ │PGVector│ │MinIO │
│3306 │ │6379 │ │ 5432   │ │ 9000 │
└─────┘ └────┘ └────────┘ └──────┘
```

### 5.2 CI/CD 流水线

```
代码推送 (push/PR)
      │
      ▼
┌─ CI Pipeline (ci.yml) ─────────────────┐
│  ┌──────────────┐  ┌────────────────┐  │
│  │ Backend Build │  │ Frontend Build │  │
│  │  Maven + Test │  │ npm lint+build │  │
│  │  MySQL/Redis  │  │ type-check     │  │
│  └──────────────┘  └────────────────┘  │
└────────────────────────────────────────┘
      │ (tag v*)
      ▼
┌─ CD Pipeline (cd.yml) ─────────────────┐
│  Frontend Build → Backend Package       │
│  → Docker Build → Push to GHCR          │
│  → Auto tag (semver/sha/latest)         │
└────────────────────────────────────────┘
```

### 5.3 环境配置

| 环境 | 配置文件 | 用途 |
|------|----------|------|
| 开发 | application-dev.yml | 本地开发 |
| 生产 | application-prod.yml | 线上部署 |

敏感信息通过环境变量注入，参考 `.env.example` 中定义的 30+ 配置项。

---

## 6. 性能设计

### 6.1 缓存策略

| 场景 | 缓存方式 | TTL |
|------|----------|-----|
| 用户会话 | Redis String | 30min |
| 数据字典 | Redis Hash | 24h |
| AI 对话历史 | Redis List | 7d |
| 系统配置 | 本地内存 | 启动加载 |

### 6.2 数据库优化

- 合理使用 FULLTEXT 索引替代 LIKE 查询
- 分页查询使用 MyBatis Plus Page 插件
- 大数据量表预设覆盖索引
- SQL 慢查询监控和优化

### 6.3 JVM 调优

Dockerfile 中配置的 JVM 参数：

```
-Xms512m -Xmx1024m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

---

## 7. 安全架构

### 7.1 认证流程

```
登录请求 → RSA 解密密码 → 验证凭证 → 生成 Token → 返回客户端
    │
    ▼
请求拦截 → 校验 Token → 注入用户上下文 → 权限校验 → 业务处理
```

### 7.2 安全措施

| 层面 | 措施 |
|------|------|
| 传输安全 | HTTPS + RSA 前端加密 |
| 认证安全 | Sa-Token + Token 过期 + 多端互踢 |
| 授权安全 | RBAC 角色权限 + 数据权限（部门/个人） |
| 数据安全 | 敏感字段脱敏 + 环境变量管理密钥 |
| 操作安全 | 操作日志 + 演示模式拦截器 |
| 接口安全 | API 前缀隔离 + 接口限流 |

---

## 8. 监控与运维

### 8.1 系统监控

- 服务器状态监控（CPU/内存/磁盘/网络）
- 在线用户管理
- 定时任务监控
- 缓存状态监控

### 8.2 日志体系

| 日志类型 | 存储 | 用途 |
|----------|------|------|
| 操作日志 | MySQL | 用户操作审计 |
| 登录日志 | MySQL | 登录行为分析 |
| AI 审计日志 | MySQL | AI 调用追踪 |
| 应用日志 | 文件 | 系统运行状态 |
| AI 巡检日志 | MySQL | 业务风险告警 |

### 8.3 告警通知

通过多渠道推送服务，可将系统告警发送到：

- 钉钉群机器人
- 飞书群机器人
- 企业微信群机器人

---

## 9. 扩展性设计

### 9.1 模块扩展

新增业务模块只需：

1. 在 `pengcheng-core` 下创建新子模块
2. 添加实体 + Mapper + Service
3. 在 `pengcheng-api` 中添加 Controller
4. 编写 Flyway 迁移脚本
5. 前端添加路由和页面

### 9.2 AI 能力扩展

- 新增 Agent Tool：实现 `Function` 接口并注册
- 新增路由分支：在 `AgentRouterService` 中添加意图类别
- 新增记忆类型：在 `MemoryService` 中扩展

### 9.3 未来演进方向

| 方向 | 计划 |
|------|------|
| 微服务拆分 | 按模块拆分为独立服务（如需水平扩展） |
| 消息队列 | 引入 RocketMQ 处理异步任务 |
| 分布式缓存 | Redis Cluster 高可用 |
| 全链路追踪 | SkyWalking / Jaeger |
| 多租户 | 数据库级别隔离 |

---

**文档维护**: 本文档随代码同步更新，最新版本请查看代码仓库 `doc/ARCHITECTURE.md`。
