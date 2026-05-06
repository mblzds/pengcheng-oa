# MasterLife V3.0 并行开发计划

**创建日期**: 2026-03-02  
**状态**: 执行中

---

## 并行开发策略

Phase 1 的任务被分解为 4 条相互独立的开发线（Track），可同时推进：

```
                         Phase 1 并行开发甘特图（第 1-4 周）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                          Week 1      Week 2      Week 3      Week 4
Track A (安全+基础设施)   ▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
Track B (Spring AI+RAG)  ░░░░▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░
Track C (聊天后端)        ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░
Track D (聊天前端)        ░░░░░░░░░░░░▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░
                          ^^^^^^^^^^^^                      ^^^^^^^^
                          并行开发窗口                       集成测试
```

---

## Track A: 安全配置 + 基础设施（1.1 + 1.7）

**状态**: ✅ 已完成

| 任务 | 状态 | 产出物 |
|------|------|--------|
| API Key 环境变量化 | ✅ | `application-dev.yml` 修改 |
| 数据库密码环境变量化 | ✅ | `application-dev.yml` + `application-prod.yml` 修改 |
| Redis 密码环境变量化 | ✅ | `application-prod.yml` 修改 |
| 创建 `.env.example` | ✅ | `.env.example` |
| Flyway 引入 + 配置 | ✅ | `pom.xml` + `application-*.yml` |
| 创建 Dockerfile | ✅ | `Dockerfile` |
| 创建 docker-compose.yml | ✅ | `docker-compose.yml` (app+mysql+redis+postgres+minio+kkfileview+nginx) |
| 创建 nginx.conf | ✅ | `nginx.conf` |
| 修复 wechat.ts API 路径 | ✅ | `src/api/wechat.ts` |

---

## Track B: Spring AI 升级 + RAG 激活（1.2 + 1.3 + 1.4）

**状态**: 🔄 进行中

| 任务 | 状态 | 说明 |
|------|------|------|
| pengcheng-ai pom.xml 移除硬编码版本 | ✅ | 改用 BOM 管理 |
| 新增 Tika 依赖 | ✅ | tika-parsers-standard-package |
| 新增 pgvector-store-starter | ✅ | 替换旧版 spring-ai-pgvector-store |
| DashScope 配置 | ✅ | application-dev.yml 已配置 |
| PGVector 配置 | ✅ | application-dev.yml 已配置 |
| 移除 DashScope AutoConfiguration 排除 | ✅ | 已从 application-dev.yml 移除 |
| 根 POM spring-ai-alibaba.version 升级 | ⬜ | 需确认 1.1.2.0 与 Spring Boot 3.2.2 兼容性后执行 |
| 移除 fallback stub Bean | ⬜ | 依赖版本升级后执行 |
| 重写 DocumentProcessor (Tika) | ⬜ | 依赖 Tika 引入后实现 |
| ChatMemory 接口迁移 | ⬜ | 依赖版本升级后实现 |
| RAG 全流程验证 | ⬜ | 所有组件就位后端到端验证 |

---

## Track C: 聊天功能增强 - 后端（1.5）

**状态**: ✅ 已完成

| 任务 | 状态 | 产出物 |
|------|------|--------|
| V10__chat_enhance.sql 迁移脚本 | ✅ | 新增字段 + 序列号表 + 离线消息表 + FULLTEXT 索引 |
| SysChatMessage 实体增强 | ✅ | 新增 recalled/replyToId/seq/delivered/readFlag 字段 |
| ChatGroupMessage 实体增强 | ✅ | 新增 recalled/replyToId/seq/atUserIds 字段 |
| SysChatMessageService 接口扩展 | ✅ | 新增 recall/search/nextSeq/ack/offline/markDelivered 方法 |
| SysChatMessageServiceImpl 实现 | ✅ | 撤回(2分钟窗口) + FULLTEXT搜索 + 全局序列号 + ACK + 离线补偿 |
| SysChatController 接口新增 | ✅ | POST recall + GET search + POST ack + GET offline |
| V11__smart_table.sql 迁移脚本 | ✅ | 5 张表 + 4 个内置房产模板 |

---

## Track D: 聊天功能增强 - 前端（1.6）

**状态**: ⬜ 待开始

| 任务 | 状态 | 说明 |
|------|------|------|
| 拆分 chat/index.vue 为子组件 | ⬜ | ChatSidebar/MessageList/ChatInput/GroupPanel/MessageBubble |
| 上拉加载历史消息 | ⬜ | handleScroll 实现 |
| 未读角标 | ⬜ | 侧边栏联系人显示 |
| Web Notification 桌面通知 | ⬜ | Notification API + 声音提示 |
| 消息撤回 UI | ⬜ | 右键菜单，2 分钟内可撤回 |
| 消息引用/回复 UI | ⬜ | 引用消息预览 |
| @功能 UI | ⬜ | 群聊 @ 成员选择 |
| 图片粘贴/文件拖拽发送 | ⬜ | paste + drop 事件 |

---

## Phase 2 预研（可提前启动的部分）

以下 Phase 2 任务与 Phase 1 无强依赖关系，可在 Phase 1 期间提前启动：

| 任务 | 可提前启动时间 | 前置条件 |
|------|---------------|----------|
| 智能表格后端 (2.1) | 现在 | SQL 脚本已创建 ✅ |
| 文件预览 kkFileView (2.4) | 现在 | docker-compose 已配置 ✅ |
| 富文本 Tiptap (2.5) | 现在 | 无后端依赖 |
| 智能工作台 UI (2.10) | 现在 | 纯前端布局重构 |
| 消息优先级算法 (2.11) | Track C 完成后 | 依赖消息字段增强 |

---

## 里程碑检查点

| 里程碑 | 目标日期 | 验收标准 |
|--------|----------|----------|
| M1: 安全加固完成 | Week 1 | 无明文密码/Key，环境变量化 ✅ |
| M2: 聊天后端完成 | Week 2 | 撤回/搜索/ACK/离线补偿接口可调用 ✅ |
| M3: Spring AI 升级 | Week 2 | 项目编译通过，AI 功能正常 |
| M4: RAG 激活 | Week 3 | 上传文档→检索→生成 全流程可用 |
| M5: 聊天前端完成 | Week 3 | 撤回/引用/@/通知 UI 可交互 |
| M6: Phase 1 收尾 | Week 4 | Docker 一键部署成功，全部 Phase 1 功能验证通过 |
