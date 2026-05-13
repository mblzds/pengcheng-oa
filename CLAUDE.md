# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码仓库中工作时提供指引。

## 构建与运行命令

### 后端（Maven 多模块，Java 17 + Spring Boot 3.3.5）
```bash
# 完整构建（跳过测试）
mvn clean package -DskipTests

# 构建并本地运行（dev 环境）
mvn clean package -DskipTests && java -jar pengcheng-starter/target/*.jar

# 运行关键模块的测试
mvn -pl pengcheng-common,pengcheng-infra/pengcheng-db,pengcheng-core/pengcheng-system,pengcheng-core/pengcheng-realty -am test

# 运行单个测试类
mvn -pl pengcheng-core/pengcheng-ai test -Dtest=ConversationMemoryServiceTest

# 运行并生成覆盖率报告（JaCoCo 默认关闭）
mvn test -Djacoco.skip=false
```

### 前端（Vue 3 + Vite + Naive UI，位于 `pengcheng-ui/`）
```bash
npm install
npm run dev          # 启动开发服务器
npm run build        # 生产构建（输出至 pengcheng-starter/src/main/resources/static）
npm run typecheck    # vue-tsc 类型检查
```

### Docker（本地全栈环境）
```bash
cp .env.example .env   # 填写必要的环境变量
docker compose up --build

# 若旧容器残留导致冲突：
docker ps -a --filter name=masterlife -q | xargs docker rm
docker compose up --build
```

app 服务必须在 docker-compose 中设置 `SERVER_PORT=8080` 和 `SERVER_SSL_ENABLED=false`（prod 配置默认为 HTTPS :8443；TLS 由 nginx 终止）。

### API 文档
启动后访问：`http://localhost:8080/doc.html`（Knife4j / OpenAPI 3.0）

## 架构

### 模块结构

```
pengcheng-common/          # 公共基础：Result<T>、BaseEntity、异常处理、工具类
pengcheng-infra/           # 基础设施适配：DB、Redis、OSS、SMS、支付、推送、WebSocket、微信、加解密、邮件
pengcheng-core/
  pengcheng-system/        # RBAC、SysUser/Role/Menu/Dict/Dept/Post + 智能表格、全局搜索、自动化规则
  pengcheng-auth/          # 登录策略（密码、短信、社会化、小程序），通过 LoginStrategyFactory 分发
  pengcheng-realty/        # 房地产 CRM：客户、项目、佣金、回款、渠道联盟
  pengcheng-ai/            # AI 层：编排、RAG、会话记忆、A/B 实验、MCP 工具
  pengcheng-file/          # 文件管理，可插拔存储策略（本地 / MinIO / OSS）
  pengcheng-message/       # IM 聊天 + 系统通知
  pengcheng-hr/            # HR 档案、KPI 绩效、考勤
  pengcheng-gen/           # 代码生成器
pengcheng-api/
  pengcheng-admin-api/     # 管理后台 REST 控制器
  pengcheng-app-api/       # 移动端 REST 控制器
  pengcheng-web-api/       # Web 前端 REST 控制器
pengcheng-starter/         # Spring Boot 启动入口、Flyway 迁移、resources/static（构建好的前端）
pengcheng-job/             # Quartz 定时任务
pengcheng-ui/              # Vue 3 前端（TypeScript、Naive UI、Pinia、vue-router）
pengcheng-uniapp/          # UniApp 微信小程序
```

### 核心模式

**统一响应格式** — 所有控制器返回 `pengcheng-common` 中的 `Result<T>`：
```java
return Result.ok(data);     // 200
return Result.fail("msg");  // 500
```

**基础实体** — 所有领域实体继承 `BaseEntity`（`pengcheng-common`），通过 MyBatis-Plus `FieldFill` 自动填充 `createTime`、`updateTime`、`createBy`、`updateBy`，通过 `@TableLogic deleted` 字段实现软删除。

**认证鉴权** — 使用 Sa-Token（非 Spring Security）。Token 为 UUID 风格，通过 HTTP Header 传递。权限校验使用 `@SaCheckPermission`。登录流程通过 `LoginStrategyFactory` 按 `LoginType` 枚举（PASSWORD、SMS_CODE、SOCIAL、MINI_PROGRAM）分发。

**数据库** — MyBatis-Plus，自增主键。Mapper 位于各模块 `mapper/` 子目录；XML 文件在 `resources/mapper/**/*.xml`。数据权限过滤通过 `pengcheng-infra/pengcheng-db` 中的 MyBatis-Plus 拦截器实现。Schema 版本由 Flyway 管理（`V0__` 至 `V9__` 迁移脚本位于 `pengcheng-starter/src/main/resources/db/migration/`）。

**AI 层**（`pengcheng-core/pengcheng-ai`）：
- Spring AI 1.1.2，DashScope（主模型）+ 智谱 GLM（备用模型）
- 自研编排层：`RouterService` 选择 Agent，`OrchestratorService` 协调多 Agent 执行
- 会话记忆：L1 Redis（热存储）+ L2 MySQL（持久化），最多 30 条消息，滑动 8 轮上下文窗口 + 压缩
- RAG：Apache Tika 解析文档 → DashScope 生成向量 → PGVector 存储（1536 维，IVFFlat 余弦距离）
- A/B 实验，失败率超阈值自动回滚
- 工具调用全程审计记录；高金额操作（>50,000）设有人工审批门控

**功能开关** — `application.yml` 中的 `pengcheng.feature.{alipay, wechat.mp, wechat.mini, wechat.pay}` 在启动时控制各集成的开启状态。

**文件存储** — 策略模式，实现位于 `pengcheng-file`。通过 `pengcheng.storage.type` 运行时配置（local / minio / aliyun-oss）。

### 环境配置差异

| 配置项 | dev | prod |
|---|---|---|
| 服务端口 | HTTP :8080 | HTTPS :8443（Docker 中需覆盖为 HTTP） |
| Redis 库 | 0 | 10 |
| 日志级别 | DEBUG（pengcheng.*） | INFO |
| 演示模式 | 关闭 | 开启（限制写操作） |
| PGVector / RAG | 默认关闭 | 开启 |
| AI 备用模型 | 单模型 | DashScope + 智谱兜底 |
| A/B 实验 | 关闭 | 开启 |

### Docker 服务

`docker-compose.yml` 启动：`masterlife-app`、`masterlife-mysql`（端口 3307）、`masterlife-redis`（6379）、`masterlife-postgres`（5432）、`masterlife-minio`（9000/9001）、`masterlife-kkfileview`（8012）、`masterlife-nginx`（80/443）。OnlyOffice 在 `profiles: optional` 下，需显式指定 `--profile optional` 才会启动。

必填 `.env` 变量：`DB_USERNAME`、`DB_PASSWORD`、`REDIS_PASSWORD`、`PG_PASSWORD`、`DASHSCOPE_API_KEY`。完整列表参见 `.env.example`。


