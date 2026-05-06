# 测试代码与参考文档说明

## 测试代码

### 测试文件分布

| 模块 | 测试类 | 类型 | 说明 |
|------|--------|------|------|
| pengcheng-ai | `AiExperimentConfigServiceTest.java` | 单元测试 | AI 实验配置服务测试 |
| pengcheng-ai | `DataChangeWebSocketBroadcastProperties.java` | 属性测试 | WebSocket 广播属性验证 |

### 运行测试

```bash
# 运行全部测试
mvn test

# 运行指定模块测试
mvn test -pl pengcheng-core/pengcheng-ai

# 运行指定测试类
mvn test -pl pengcheng-core/pengcheng-ai -Dtest=AiExperimentConfigServiceTest
```

### V3.0 测试计划

Phase 4 目标：核心业务单元测试覆盖率 > 60%，重点覆盖：
- AI 模块：RAG 全流程、Agent 编排、MCP 工具调用
- 房产业务：佣金计算、客户管理、审批流程
- 消息模块：消息发送/撤回/引用、WebSocket 连接
- 智能表格：字段验证、记录 CRUD、视图配置

---

## 参考文档

### 项目文档

| 文件 | 说明 |
|------|------|
| `doc/UPGRADE-PLAN-3.0.md` | **V3.0 全面升级方案**（当前版本） |
| `doc/TASKS-V3.0.md` | **V3.0 开发任务清单**（含进度跟踪） |
| `doc/UPGRADE-PLAN-2.0.md` | V2.0 升级计划（已完成，保留参考） |
| `doc/agentscope-b-upgrade-plan.md` | AgentScope B 方案路线（已完成 Phase 0-4） |
| `房地产销售系统-技术架构方案_V2.1.md` | 技术架构方案（已更新至 V3.0） |
| `房地产销售系统-需求规格说明书_V2.1.md` | 需求规格说明书 |

### 外部参考

| 项目 | 地址 | 参考点 |
|------|------|--------|
| Spring AI Alibaba | https://github.com/alibaba/spring-ai-alibaba | Agent Framework / StateGraph / MCP |
| AgentScope | https://github.com/agentscope-ai | 多智能体编排 |
| CoPaw | https://github.com/agentscope-ai/CoPaw | Skill / Heartbeat / 多渠道 / Memory |
| ReMe | https://github.com/agentscope-ai/ReMe | 长期记忆管理 |
| kkFileView | https://github.com/kekingcn/kkFileView | 文件预览 |
| OnlyOffice | https://github.com/ONLYOFFICE/DocumentServer | 在线编辑 |
| Tiptap | https://tiptap.dev | 富文本编辑器 |

---

## SQL 脚本

### 初始化脚本
- `pengcheng-system.sql` — 系统基础表（用户/角色/菜单/字典/文件/聊天/Quartz 等）

### 增量迁移脚本（V1 ~ V9 为 baseline，V10+ 由 Flyway 管理）

| 脚本 | 内容 | 状态 |
|------|------|------|
| V1__realty_init.sql | 房产业务表（项目/客户/联盟/佣金/考勤/付款） | ✅ baseline |
| V2__realty_job_init.sql | 房产定时任务 | ✅ baseline |
| V3__notification_compensate.sql | 通知 + 调休申请 | ✅ baseline |
| V4__ai_tool_audit.sql | AI 工具调用审计 | ✅ baseline |
| V5__ai_experiment_config_group.sql | AI 实验配置分组 | ✅ baseline |
| V6__ai_experiment_config_audit.sql | AI 实验配置审计 | ✅ baseline |
| V7__ai_experiment_alert_log.sql | AI 实验告警日志 | ✅ baseline |
| V8__ai_experiment_alert_delivery_log.sql | AI 实验告警投递日志 | ✅ baseline |
| V9__ai_experiment_alert_delivery_health_log.sql | AI 实验告警投递健康巡检 | ✅ baseline |
| V10__chat_enhance.sql | 聊天增强（撤回/引用/seq/ACK/@） | ⬜ Phase 1 |
| V11__smart_table.sql | 智能表格（4 张核心表） | ⬜ Phase 2 |
| V12__document_space.sql | 云文档空间 | ⬜ Phase 3 |
| V13__channel_push.sql | 多渠道推送配置 | ⬜ Phase 3 |

### 测试数据

- 初始化脚本包含超级管理员账号 `admin/admin123`
- 包含基础字典数据、菜单配置、角色权限
- V2 包含房产业务相关定时任务配置
- 可通过代码生成器快速生成测试 CRUD 页面
