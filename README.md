<div align="center">

# MasterLife

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green?style=flat-square&logo=springboot)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.2-purple?style=flat-square)
![Spring AI Alibaba](https://img.shields.io/badge/Spring%20AI%20Alibaba-1.1.2.2-purple?style=flat-square)
![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen?style=flat-square&logo=vue.js)
![Naive UI](https://img.shields.io/badge/Naive%20UI-2.37-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

**AI 驱动的房地产智能协作平台 — 基于 Spring AI + Spring AI Alibaba，自研多 Agent 编排层**

[在线预览](#在线演示) | [开发文档](#开发指南) | [部署手册](doc/DEPLOYMENT.md) | [V3.0 发布清单](doc/RELEASE-CHECKLIST-V3.0.md) | [问题反馈](mailto:support@pengchengkeji.com)

</div>

## 在线演示

**演示地址：** 请联系项目管理员获取演示环境地址

| 账号 | 密码 | 说明 |
|------|------|------|
| admin | admin123 | 管理员账号（演示模式，部分操作受限） |

> 也可以自行注册账号体验完整功能

---

## 项目简介

MasterLife 是一个 **AI 驱动的房地产智能协作平台**，以 **Spring AI 1.1.2 + Spring AI Alibaba 1.1.2.2**（DashScope starter + PGVector starter）为 AI 基础设施，基于其上自研多 Agent 编排层（`pengcheng-ai/orchestration`），借鉴钉钉、飞书、CoPaw 的产品理念，采用前后端分离 + 模块化单体架构。

**核心能力**：
- **智能办公** — 即时通讯、多维表格、云文档、文件管理、审批流程
- **销售业务** — 房源管理、客户 CRM、渠道分销、佣金结算、考勤打卡
- **AI 赋能** — RAG 知识库、智能判客、营销文案、多 Agent 编排、MCP 工具服务、A/B 实验治理

**设计原则**：AI Native · 可靠优先 · 安全合规 · 渐进式升级

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.2 | 基础框架 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Sa-Token | 1.37.0 | 权限认证框架 |
| Redis | 7.0+ | 缓存/会话存储 |
| MySQL | 8.0+ | 数据库 |
| Quartz | 2.3.2 | 定时任务框架 |
| Spring AI | 1.1.2 | AI 框架基座 |
| Spring AI Alibaba | 1.1.2.2 | DashScope starter + PGVector starter |
| 自研 Orchestration | - | 多 Agent 编排（`pengcheng-ai/orchestration`，含 `RouterService`/`OrchestratorService`） |
| PostgreSQL + pgvector | 16+ | 知识库向量存储（RAG） |
| Apache Tika | 2.9.1 | 文档解析（PDF/Word/Excel/PPT） |
| Flyway | 10.x | 数据库版本管理 |
| Hutool | 5.8.25 | Java 工具类库 |
| MinIO | - | 对象存储（可选） |
| 阿里云 OSS | - | 对象存储（可选） |
| kkFileView | 4.4.0 | 文件预览服务（Office/PDF 全格式） |

### 前端（PC 管理后台）

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.15 | 前端框架 |
| Vite | 5.0.11 | 构建工具 |
| TypeScript | 5.3.3 | 类型安全 |
| Naive UI | 2.37.3 | UI 组件库 |
| Pinia | 2.1.7 | 状态管理 |
| Vue Router | 4.2.5 | 路由管理 |
| Axios | 1.6.5 | HTTP 客户端 |
| ECharts | 6.0.0 | 图表库 |
| xterm.js | 6.0.0 | 终端模拟器 |

### 移动端（小程序）

| 技术 | 版本 | 说明 |
|------|------|------|
| UniApp | - | 跨平台框架 |
| uView Plus | 3.3.36 | UI 组件库 |
| crypto-js | 4.2.0 | 加密工具 |

## 项目结构

```
pengcheng-admin
├── pengcheng-common              # 公共基础模块
│   ├── entity               # 基础实体类
│   ├── exception            # 全局异常处理
│   ├── result               # 统一响应封装
│   └── util                 # 工具类
│
├── pengcheng-infra               # 基础设施层
│   ├── pengcheng-db              # 数据库配置
│   ├── pengcheng-redis           # Redis 配置
│   ├── pengcheng-oss             # 文件存储（本地/MinIO/阿里云OSS）
│   ├── pengcheng-sms             # 短信服务（阿里云/腾讯云）
│   ├── pengcheng-pay             # 支付服务（微信/支付宝）
│   ├── pengcheng-push            # 推送服务（极光/友盟/个推）
│   ├── pengcheng-social          # 社交登录（微信/支付宝/苹果）
│   ├── pengcheng-wechat          # 微信公众号/小程序
│   ├── pengcheng-websocket       # WebSocket 支持
│   ├── pengcheng-crypto          # 加密解密
│   └── pengcheng-mail            # 邮件服务
│
├── pengcheng-core                # 业务核心层
│   ├── pengcheng-system          # 系统管理
│   │   ├── entity           # 系统实体（用户、角色、菜单等）
│   │   ├── mapper           # MyBatis Mapper
│   │   ├── service          # 服务层
│   │   ├── annotation       # 自定义注解
│   │   └── aspect           # AOP 切面
│   ├── pengcheng-auth            # 认证授权
│   │   ├── strategy         # 登录策略（密码/短信/社交/小程序）
│   │   └── enums            # 枚举定义
│   ├── pengcheng-realty           # 房产核心业务
│   │   ├── project          # 项目/楼盘管理
│   │   ├── customer         # 客户 CRM（公海/私海/报备/判客）
│   │   ├── commission       # 佣金结算
│   │   ├── alliance         # 渠道经销商管理
│   │   ├── attendance       # 考勤打卡
│   │   ├── payment          # 收款/财务台账
│   │   └── dashboard        # 业务数据看板
│   ├── pengcheng-ai              # AI 智能模块
│   │   ├── orchestration    # 多智能体编排层
│   │   ├── agent            # Agent 工具与路由
│   │   ├── service          # AI 服务（RAG/Chat/文案）
│   │   ├── experiment       # A/B 实验与评估
│   │   └── audit            # 工具调用审计
│   ├── pengcheng-file            # 文件管理
│   ├── pengcheng-gen             # 代码生成
│   └── pengcheng-message         # 消息中心（公告/聊天/群聊）
│
├── pengcheng-api                 # 接口层
│   ├── pengcheng-admin-api       # 后台管理接口
│   │   └── controller
│   │       ├── auth         # 认证接口
│   │       ├── system       # 系统管理接口
│   │       ├── monitor      # 系统监控接口
│   │       ├── realty        # 房产业务接口
│   │       ├── ai           # AI 智能接口
│   │       ├── message      # 消息接口
│   │       ├── file         # 文件接口
│   │       └── gen          # 代码生成接口
│   ├── pengcheng-app-api         # APP 接口
│   └── pengcheng-web-api         # 网页端接口
│
├── pengcheng-job                 # 定时任务模块
│   ├── entity               # 任务实体
│   ├── service              # 任务服务
│   └── util                 # Quartz 工具类
│
├── pengcheng-starter             # 启动入口
│   └── resources
│       ├── application.yml
│       ├── application-dev.yml
│       └── application-prod.yml
│
├── pengcheng-ui                  # 后台管理前端
│   ├── src
│   │   ├── api              # API 接口定义
│   │   ├── components       # 公共组件
│   │   ├── layout           # 布局组件
│   │   ├── router           # 路由配置
│   │   ├── stores           # Pinia 状态管理
│   │   ├── utils            # 工具函数
│   │   └── views            # 页面组件
│   │       ├── dashboard    # 控制台
│   │       ├── system       # 系统管理
│   │       ├── monitor      # 系统监控
│   │       ├── log          # 日志管理
│   │       ├── message      # 消息中心
│   │       ├── org          # 组织管理
│   │       └── tool         # 系统工具
│   └── package.json
│
├── pengcheng-uniapp              # 移动端小程序
│   ├── pages
│   │   ├── login            # 登录页
│   │   ├── index            # 首页
│   │   ├── chat             # 私聊
│   │   ├── group-chat       # 群聊
│   │   ├── contacts         # 联系人
│   │   ├── group            # 群组管理
│   │   └── profile          # 个人中心
│   └── utils                # 工具类
│
└── sql                      # 数据库脚本
    ├── pengcheng-system.sql     # 系统初始化脚本
    └── V1~V9__*.sql             # 增量迁移脚本（房产/AI 业务表）
```

## 功能特性

### 房产核心业务
- **项目管理** - 楼盘项目的创建、配置与管理
- **客户 CRM** - 客户全生命周期管理（报备、判客、公海/私海池、跟进记录）
- **渠道经销商** - 经销商入驻、分销管理、数据隔离
- **佣金结算** - 多策略佣金计算（固定金额/百分比/跳点）、结佣审批
- **收款管理** - 定金、首付、按揭等款项台账
- **考勤打卡** - 签到记录、考勤统计、补卡申请
- **业务看板** - 销售数据概览、回款率、业绩排行

### AI 智能能力（基于 Spring AI + Spring AI Alibaba，自研多 Agent 编排）
- **RAG 知识库** - 上传 PDF/Word/Excel/PPT，Apache Tika 解析 → PGVector 向量存储 → 智能问答
- **多 Agent 编排** - 自研 `pengcheng-ai/orchestration` 包（`RouterService / OrchestratorService`）基于 Spring AI Agent Framework 构建
- **MCP 工具服务** - 业务工具标准化暴露，支持跨服务 Agent 调用
- **智能判客** - AI 客户分析 + 规则引擎降级
- **营销文案生成** - 基于关键词生成朋友圈营销文案
- **AI 报表问答** - ChatClient + Function Calling + SSE 流式输出
- **会话记忆** - ChatMemory 上下文管理 + Redis 持久化
- **A/B 实验治理** - 路由/提示词实验 + 自动回滚 + 告警通知 + 健康巡检
- **多渠道接入** - 钉钉/飞书/企业微信机器人推送（规划中）

### 系统管理
- **用户管理** - 用户的增删改查、角色分配、状态管理、用户黑名单
- **角色管理** - 角色的权限配置、菜单分配、数据权限
- **菜单管理** - 菜单的增删改查、权限标识配置
- **部门管理** - 组织架构管理、树形结构展示
- **岗位管理** - 岗位的增删改查
- **字典管理** - 数据字典维护、字典项管理
- **系统配置** - 系统参数的动态配置（分组管理）

### 系统监控
- **在线用户** - 当前在线用户查看、强制下线
- **定时任务** - Quartz 任务调度、执行日志
- **服务监控** - 服务器 CPU、内存、JVM 信息
- **缓存监控** - Redis 缓存信息、键值管理

### 日志管理
- **登录日志** - 用户登录记录、登录地点
- **操作日志** - 用户操作记录、AOP 切面自动记录

### 协作办公
- **即时通讯** - WebSocket 实时消息、私聊/群聊、消息撤回/引用/@功能
- **系统公告** - 富文本公告发布、已读未读状态
- **智能表格** - 多维表格（动态字段、多视图：表格/看板/甘特图/日历）
- **云文档** - 知识库空间、版本管理、在线编辑（规划中）

### 文件管理
- **文件上传** - 支持本地/MinIO/阿里云OSS、分片上传
- **文件预览** - kkFileView 全格式预览（Office/PDF/CAD/图片/视频）
- **在线编辑** - OnlyOffice 文档协作编辑（规划中）
- **文档解析** - Apache Tika 解析 PDF/Word/Excel/PPT

### 代码生成
- **代码生成器** - 根据数据库表生成前后端代码

### 安全特性
- **验证码** - 图片验证码、滑块验证码、短信验证码
- **接口加密** - RSA 非对称加密传输
- **登录安全** - 登录失败限制、账号锁定
- **权限控制** - 基于 RBAC 的细粒度权限控制
- **多种登录方式** - 密码登录、短信登录、社交登录、小程序登录

### 扩展服务（策略工厂模式）
- **文件存储** - 本地存储 / MinIO / 阿里云 OSS
- **短信服务** - 控制台 / 阿里云 / 腾讯云
- **支付服务** - 微信支付 / 支付宝
- **推送服务** - 控制台 / 极光 / 友盟 / 个推
- **社交登录** - 微信公众号 / 微信小程序 / 支付宝 / 苹果

## 系统截图

### 登录页面
![登录](doc/登录.png)

### 控制台首页
![首页](doc/首页.png)

### 控制台首页暗黑首页
![首页暗黑](doc/暗黑首页.png)

### 用户管理
![用户管理](doc/用户管理.png)

### 系统配置
![系统配置](doc/系统配置.png)

### 即时聊天
![即时聊天](doc/即时聊天.png)

### 系统通知
![系统通知](doc/系统通知.png)

### 文件管理
![文件列表](doc/文件列表.png)

### 服务监控
![服务监控](doc/服务监控.png)

## 快速开始

### 环境准备

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+
- Node.js 18+

### 后端启动

1. **克隆项目**
```bash
git clone <your-repo-url>
cd pengcheng-admin
```

2. **初始化数据库**（生产环境建议使用专用账号，见 `doc/PRODUCTION-DB-USER.md`）
```sql
-- 创建数据库
CREATE DATABASE pengcheng-system DEFAULT CHARACTER SET utf8mb4;

-- 首次部署推荐使用 Flyway 自动执行迁移（见 doc/DEPLOYMENT.md）
-- 或手动导入：mysql -u root -p pengcheng-system < sql/pengcheng-system.sql
```

3. **修改配置**

修改 `pengcheng-starter/src/main/resources/application-dev.yml` 中的数据库和 Redis 配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pengcheng-system
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 10
```

4. **启动项目**
```bash
mvn clean install
cd pengcheng-starter
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。**一键部署**（含 MySQL/Redis/PostgreSQL/kkFileView 等）见 [部署运维手册](doc/DEPLOYMENT.md)。

### 前端启动

```bash
cd pengcheng-ui
npm install
npm run dev
```

前端默认运行在 `http://localhost:3000`

### 移动端启动

使用 HBuilderX 打开 `pengcheng-uniapp` 目录，运行到微信开发者工具即可。

### 默认账号

| 账号 | 密码 | 说明 |
|------|------|------|
| admin | admin123 | 超级管理员 |

> 支持自行注册账号体验

### 开箱即用 Demo 数据（V3.1）

Flyway 迁移 `V39__seed_demo.sql` 会自动装载**最小业务种子数据**，启动后可直接体验核心业务闭环：

| 业务 | 种子数据 |
|------|---------|
| 联盟商 | 3 家（钻石 / 金牌 / 普通） |
| 项目楼盘 | 3 个（望京壹号在售 / 中关村公馆在售 / 南城经典售罄） |
| 项目佣金规则 | 3 条（含跳点 / 开单奖 / 平台奖） |
| 客户 | 4 人（已成交 / 已到访 / 新报备 / 公海） |
| 到访 / 成交 | 3 条到访 + 1 条成交（望京壹号 A-3-1001，800 万） |
| **回款计划（演示 P0-2）** | 3 期：首付 300w ✅ 已结清 / 二期 400w ⏰ 2 天后到期 / 尾款 100w ❌ 已逾期 3 天 |

**体验路径**：
1. 登录 admin → 左菜单"房产业务 → 回款管理"
2. 应看到 3 条分期、1 条已结清、1 条即将到期、1 条已逾期
3. 启动应用后等至当天 08:30（或手动调 `POST /admin/receivable/check/overdue`），会自动触发逾期扫描、写入告警
4. "监控 → AI 巡检"页能看到新增的回款告警记录

> 回滚 Demo 数据：见 [V39__seed_demo.sql](pengcheng-starter/src/main/resources/db/migration/V39__seed_demo.sql) 文末 DELETE 模板。

## 配置说明

### 文件存储配置

系统支持多种文件存储方式，通过系统配置动态切换：

- **本地存储** - 默认方式，文件存储在服务器本地
- **MinIO** - 分布式对象存储
- **阿里云 OSS** - 阿里云对象存储服务

### 短信服务配置

- **控制台输出** - 开发测试使用，验证码打印到控制台
- **阿里云短信** - 阿里云短信服务
- **腾讯云短信** - 腾讯云短信服务

### 支付服务配置

- **微信支付** - 微信支付 API v3
- **支付宝** - 支付宝开放平台

### 推送服务配置

- **控制台输出** - 开发测试使用
- **极光推送** - JPush
- **友盟推送** - UMeng Push
- **个推** - GeTui

### 社交登录配置

- **微信公众号** - 微信公众平台网页授权
- **微信小程序** - 微信小程序登录
- **支付宝** - 支付宝账号登录
- **苹果登录** - Sign in with Apple

## 开发指南

### 添加新模块

**系统功能**：在 `pengcheng-core/pengcheng-system` 下开发

**房产业务**：在 `pengcheng-core/pengcheng-realty` 下按子域开发（如 customer、commission 等）

**AI 能力**：在 `pengcheng-core/pengcheng-ai` 下开发，新增 Agent 工具需纳入编排层和审计链路

通用步骤：
1. 在对应 core 模块下创建实体类、Mapper、Service
2. 在 `pengcheng-api/pengcheng-admin-api/controller` 下创建 Controller

### 添加操作日志

使用 `@Log` 注解自动记录操作日志：

```java
@Log(title = "用户管理", businessType = BusinessType.INSERT)
@PostMapping
public Result<Void> add(@RequestBody SysUser user) {
    // ...
}
```

### 添加权限控制

使用 Sa-Token 注解进行权限控制：

```java
@SaCheckPermission("system:user:add")
@PostMapping
public Result<Void> add(@RequestBody SysUser user) {
    // ...
}
```

### 添加新的登录方式

在 `pengcheng-core/pengcheng-auth/strategy` 下创建新的登录策略类，实现 `LoginStrategy` 接口：

```java
@Component
public class CustomLoginStrategy implements LoginStrategy {
    @Override
    public LoginResult login(LoginRequest request) {
        // 自定义登录逻辑
    }
}
```

## 更新日志

### V4.0（新产品规划 · 2026-04 起）
- **新产品定位**：AI 驱动的中小企业智能协作平台，按「完整业务闭环」组织产品
- 详细需求见 [`doc/PRD-V4.0-新产品需求文档.md`](doc/PRD-V4.0-新产品需求文档.md)
- 与 V3.2 [开发计划](doc/DEV-PLAN-V3.2.md) 并行推进，MVP 目标 6~8 周交付

### V3.2（当前迭代 · 2026-04-22 起）
- 见 [`doc/DEV-PLAN-V3.2.md`](doc/DEV-PLAN-V3.2.md)
- **Feature Flag 机制**：`pengcheng.feature.{alipay, wechat.mp, wechat.mini, wechat.pay}` 默认关闭，按需开启
- Sprint 5-7：参数校验/错误码收口、佣金闭环、密钥保险箱、部署安全、前端类型严格化、会议真 API、AI 容错与 RAG 降级、微信/支付宝加固、测试基线、可观测、CI/CD

### v3.0.0（收口阶段）
- 开发任务见 `doc/archive/v3.x-plans/TASKS-V3.0.md`（已归档），发布前检查见 `doc/RELEASE-CHECKLIST-V3.0.md`
- Spring AI Alibaba 升级至 1.1.2.2（DashScope starter + PGVector starter）
- 自研多 Agent 编排层（`pengcheng-ai/orchestration`）基于 Spring AI Agent Framework 构建
- RAG 知识库升级（DashScope Embedding + PGVector + Apache Tika）
- 聊天功能增强（消息撤回/引用/@/ACK/离线消息/未读角标/通知）
- 智能表格（多维表格，14 种字段类型，多视图）
- 文件预览增强（kkFileView 全格式预览）
- 富文本编辑器（Tiptap）
- Flyway 数据库版本管理
- Docker Compose 一键部署
- 安全加固（API Key 环境变量化、敏感信息加密）

### v2.0.0 (2026-03-01)
- 新增房产核心业务模块（项目管理、客户 CRM、佣金结算、渠道经销商、考勤、收款）
- 新增 AI 智能模块（RAG 知识库、文案生成、多智能体编排）
- 新增 AI 实验治理控制台（A/B 实验、自动回滚、告警通知、健康巡检）
- 新增邮件服务模块
- 新增业务数据看板
- 清理项目结构，移除无关的第三方库源码和旧版冗余代码

### v1.0.0 (2026-02-08)
- 重构项目结构为分层架构（common/infra/core/api）
- 新增 pengcheng-uniapp 移动端小程序（聊天办公）
- 新增群聊功能（群组创建、成员管理、群消息）
- 新增多种登录策略（密码/短信/社交/小程序）
- 新增社交登录（微信/支付宝/苹果）
- 新增代码生成器模块
- 新增文件存储策略工厂（本地/MinIO/OSS）
- 新增推送服务策略工厂（极光/友盟/个推）
- 新增短信服务策略工厂（阿里云/腾讯云）
- 新增支付服务策略工厂（微信/支付宝）
- 完善系统配置分组管理
- 优化登录页面（三种样式）
- 新增滑块验证码（弹窗拼图模式）

### v0.9.0 (2026-01-25)
- 完成字典管理和系统配置功能
- 实现部门和岗位管理
- 新增即时通讯功能（WebSocket）
- 优化前端界面和交互体验

### v0.8.0 (2026-01-20)
- 搭建项目基础框架
- 集成 Sa-Token 实现认证授权
- 完成前后端基础架构搭建
- 实现基础权限管理（用户、角色、菜单）

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 开源协议

本项目基于 [MIT License](LICENSE) 开源。

## 联系作者

- **微信**: 请联系项目管理员获取


### 学习交流群
![交流群](doc/交流群.jpg)


---

<div align="center">

**如果这个项目对你有帮助，请给一个 Star 支持一下！**

</div>
