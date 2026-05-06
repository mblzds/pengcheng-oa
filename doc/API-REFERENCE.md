# 鹏程地产系统 — API 接口文档

**Base URL**: `/api`  
**认证方式**: Sa-Token (Header: `satoken`)

---

## 1. 认证模块

| Method | Path | 说明 |
|--------|------|------|
| POST | `/auth/login` | 用户登录（返回 token） |
| POST | `/auth/register` | 用户注册 |
| POST | `/auth/logout` | 退出登录 |
| GET | `/auth/info` | 获取当前用户信息 |
| GET | `/auth/menus` | 获取菜单权限 |

## 2. 客户管理

| Method | Path | 说明 |
|--------|------|------|
| GET | `/realty/customer/list` | 客户列表（分页） |
| GET | `/realty/customer/{id}` | 客户详情 |
| POST | `/realty/customer` | 新增客户 |
| PUT | `/realty/customer` | 更新客户 |
| DELETE | `/realty/customer/{id}` | 删除客户 |
| POST | `/realty/customer/advance` | 推进客户状态 |
| POST | `/realty/customer/follow` | 添加跟进记录 |

## 3. 项目管理

### 3.1 楼盘项目（房产业务）

| Method | Path | 说明 |
|--------|------|------|
| GET | `/realty/project/list` | 楼盘列表 |
| GET | `/realty/project/{id}` | 楼盘详情 |
| POST | `/realty/project` | 新增楼盘 |
| PUT | `/realty/project` | 更新楼盘 |

### 3.2 通用项目管理（任务/看板/甘特/里程碑）

| Method | Path | 说明 |
|--------|------|------|
| GET | `/project/list` | 项目列表（分页，scope=my_created/my_joined/all，status 筛选） |
| GET | `/project/{id}` | 项目详情 |
| POST | `/project` | 创建项目 |
| PUT | `/project/{id}` | 更新项目 |
| DELETE | `/project/{id}` | 删除/归档项目 |
| GET | `/project/{id}/members` | 项目成员列表 |
| POST | `/project/{id}/members` | 添加成员（userId, role） |
| PUT | `/project/{id}/members/{userId}` | 修改成员角色 |
| DELETE | `/project/{id}/members/{userId}` | 移除成员 |
| GET | `/project/{id}/stats` | 项目统计（任务数/完成率/逾期数等） |
| GET | `/project/my-tasks` | 当前用户被指派的任务（工作台用，limit 默认 5） |
| GET | `/project/{projectId}/tasks` | 任务列表（分页，parentId/assigneeId/status/priority） |
| GET | `/project/{projectId}/tasks/tree` | 任务树（含子任务层级） |
| GET | `/project/task/{taskId}` | 任务详情 |
| POST | `/project/{projectId}/tasks` | 创建任务 |
| PUT | `/project/task/{taskId}` | 更新任务 |
| DELETE | `/project/task/{taskId}` | 删除任务 |
| PUT | `/project/task/{taskId}/status` | 更新任务状态（看板拖拽） |
| PUT | `/project/task/{taskId}/assignee` | 变更执行人 |
| PUT | `/project/task/{taskId}/progress` | 更新进度 |
| GET | `/project/task/{taskId}/dependencies` | 任务依赖列表 |
| POST | `/project/task/{taskId}/dependencies` | 添加依赖（dependsOnTaskId, type=fs/ff/ss/sf） |
| DELETE | `/project/task/{taskId}/dependencies/{depId}` | 删除依赖 |
| GET | `/project/{projectId}/board` | 看板数据（按状态分列） |
| GET | `/project/{projectId}/gantt` | 甘特图数据（任务+起止日期+依赖） |
| GET | `/project/{projectId}/calendar` | 日历数据（任务+里程碑按日期） |
| GET | `/project/{projectId}/milestones` | 里程碑列表 |
| POST | `/project/{projectId}/milestones` | 创建里程碑 |
| PUT | `/project/milestone/{id}` | 更新里程碑 |
| DELETE | `/project/milestone/{id}` | 删除里程碑 |
| PUT | `/project/milestone/{id}/complete` | 标记里程碑完成/未完成 |

## 4. 佣金管理

| Method | Path | 说明 |
|--------|------|------|
| GET | `/realty/commission/list` | 佣金列表 |
| POST | `/realty/commission` | 创建佣金记录 |
| PUT | `/realty/commission` | 更新佣金 |

## 5. 即时聊天

| Method | Path | 说明 |
|--------|------|------|
| GET | `/sys/chat/users` | 聊天联系人列表 |
| GET | `/sys/chat/messages` | 消息历史 |
| POST | `/sys/chat/send` | 发送消息 |
| POST | `/sys/chat/recall/{msgId}` | 撤回消息 |
| POST | `/sys/chat/ack/{msgId}` | 消息确认 |
| GET | `/sys/chat/offline` | 离线消息补偿 |
| GET | `/sys/chat/search` | 消息搜索 |
| POST | `/sys/chat/category` | 设置会话分类 |
| GET | `/sys/chat/categories` | 获取会话分类 |
| DELETE | `/sys/chat/category` | 移除分类 |
| WS | `/ws/message` | WebSocket 实时消息 |

## 6. AI 模块

| Method | Path | 说明 |
|--------|------|------|
| POST | `/ai/chat` | AI 对话 |
| POST | `/ai/content/generate` | 文案生成 |
| POST | `/ai/analysis/report` | 报表问答 |
| POST | `/ai/analysis/insight` | AI 洞察生成 |
| GET | `/ai/knowledge/list` | 知识库列表 |
| POST | `/ai/knowledge/upload` | 上传知识文档 |

### 6.1 AI 记忆

| Method | Path | 说明 |
|--------|------|------|
| GET | `/ai/memory/list` | 记忆列表（分页） |
| GET | `/ai/memory/stats` | 记忆统计 |
| POST | `/ai/memory/{id}/promote` | 升级为 L2 长期记忆 |
| DELETE | `/ai/memory/{id}` | 删除记忆 |

## 7. 智能表格

| Method | Path | 说明 |
|--------|------|------|
| GET | `/smart-table/tables` | 表格列表 |
| POST | `/smart-table/tables` | 创建表格 |
| GET | `/smart-table/tables/{id}` | 表格详情 |
| DELETE | `/smart-table/tables/{id}` | 删除表格 |
| GET | `/smart-table/tables/{id}/fields` | 字段列表 |
| POST | `/smart-table/tables/{id}/fields` | 添加字段 |
| GET | `/smart-table/tables/{id}/records` | 记录列表 |
| POST | `/smart-table/tables/{id}/records` | 添加记录 |
| PUT | `/smart-table/records/{id}` | 更新记录 |
| GET | `/smart-table/templates` | 模板列表 |

## 8. 全局搜索

| Method | Path | 说明 |
|--------|------|------|
| GET | `/search?q=xxx&scope=all` | 全局搜索 |
| GET | `/search/history` | 搜索历史 |
| GET | `/search/hot` | 热门搜索 |

## 9. 云文档

| Method | Path | 说明 |
|--------|------|------|
| GET | `/doc/spaces` | 文档空间列表 |
| POST | `/doc/space` | 创建空间 |
| GET | `/doc/tree/{spaceId}` | 文档目录树 |
| GET | `/doc/{id}` | 文档内容 |
| POST | `/doc/create` | 创建文档 |
| PUT | `/doc/update` | 更新文档 |
| DELETE | `/doc/{id}` | 删除文档 |
| GET | `/doc/versions/{docId}` | 版本历史 |
| POST | `/doc/versions/restore` | 恢复版本 |
| GET | `/doc/search` | 文档搜索 |

## 10. 销售日历

| Method | Path | 说明 |
|--------|------|------|
| GET | `/calendar/events` | 按日期范围查事件 |
| GET | `/calendar/month` | 按月查事件 |
| GET | `/calendar/today` | 今日事件 |
| GET | `/calendar/team` | 团队日程 |
| POST | `/calendar/event` | 创建事件 |
| PUT | `/calendar/event` | 更新事件 |
| DELETE | `/calendar/event/{id}` | 取消事件 |

## 11. 自动化规则

| Method | Path | 说明 |
|--------|------|------|
| GET | `/automation/rules` | 规则列表 |
| POST | `/automation/rule` | 创建规则 |
| PUT | `/automation/rule` | 更新规则 |
| POST | `/automation/rule/{id}/toggle` | 启/禁用规则 |
| DELETE | `/automation/rule/{id}` | 删除规则 |
| GET | `/automation/rule/{id}/logs` | 执行日志 |

## 12. 待办事项

| Method | Path | 说明 |
|--------|------|------|
| GET | `/todo/list` | 待办列表 |
| GET | `/todo/count` | 待办计数 |
| POST | `/todo/create` | 创建待办 |
| PUT | `/todo/update` | 更新待办 |
| POST | `/todo/complete/{id}` | 完成待办 |
| POST | `/todo/cancel/{id}` | 取消待办 |
| POST | `/todo/extract` | 从消息提取待办 |

## 13. AI 日报

| Method | Path | 说明 |
|--------|------|------|
| GET | `/report/list` | 日报列表 |
| GET | `/report/detail` | 日报详情 |
| POST | `/report/generate` | 生成日报 |
| POST | `/report/generate-all` | 批量生成全员日报 |

## 14. 销售质检

| Method | Path | 说明 |
|--------|------|------|
| GET | `/quality/latest` | 最新评分 |
| GET | `/quality/history` | 评分历史 |
| GET | `/quality/ranking` | 排行榜 |
| POST | `/quality/evaluate` | 执行评估 |
| POST | `/quality/evaluate-team` | 团队评估 |

## 15. 场景模板

| Method | Path | 说明 |
|--------|------|------|
| GET | `/template/list` | 模板列表 |
| GET | `/template/{id}` | 模板详情 |
| POST | `/template/create` | 创建模板 |
| PUT | `/template/update` | 更新模板 |
| DELETE | `/template/{id}` | 删除模板 |
| POST | `/template/fill` | 填充模板 |
| GET | `/template/usages` | 使用记录 |

## 16. 多渠道推送

| Method | Path | 说明 |
|--------|------|------|
| GET | `/channel/list` | 渠道列表 |
| POST | `/channel/save` | 保存渠道 |
| POST | `/channel/toggle/{id}` | 启/禁用渠道 |
| POST | `/channel/test/{id}` | 测试渠道 |
| POST | `/channel/broadcast` | 广播消息 |
| GET | `/channel/logs` | 推送日志 |

## 17. 数据统计

| Method | Path | 说明 |
|--------|------|------|
| GET | `/admin/dashboard/overview` | 核心指标概览 |
| GET | `/admin/dashboard/funnel` | 转化漏斗 |
| GET | `/admin/dashboard/ranking` | 业绩排行 |
| GET | `/admin/dashboard/ai-insights` | AI 洞察 |

## 18. 系统管理

| Method | Path | 说明 |
|--------|------|------|
| GET/POST/PUT/DELETE | `/sys/user/*` | 用户管理 |
| GET/POST/PUT/DELETE | `/sys/role/*` | 角色管理 |
| GET/POST/PUT/DELETE | `/sys/menu/*` | 菜单管理 |
| GET/POST/PUT/DELETE | `/sys/dept/*` | 部门管理 |
| GET/POST/PUT/DELETE | `/sys/dict/*` | 字典管理 |
| GET/POST/PUT/DELETE | `/sys/config/*` | 系统配置 |
| GET | `/monitor/server` | 服务器监控 |
| GET | `/monitor/cache` | 缓存监控 |

---

## 通用响应格式

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { ... }
}
```

## 错误码

| Code | 说明 |
|------|------|
| 200 | 成功 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
