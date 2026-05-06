# APP 端 API 接口映射表

本文档说明 APP（小程序/原生应用）与后端 API 的完整映射关系。

---

## 一、基础信息

| 项目 | 值 |
|------|-----|
| **Base URL** | `/api` |
| **认证方式** | Sa-Token (Header: `satoken`) |
| **客户端类型** | `APP` |
| **WebSocket** | `/ws/message` |

---

## 二、API 接口分类

### 2.1 认证模块 (`/app/auth`)

| 接口 | Method | 路径 | 说明 | 前端页面 |
|------|--------|------|------|----------|
| 登录 | POST | `/app/auth/login` | 用户登录 | `pages/login/index` |
| 发送验证码 | POST | `/app/auth/sms-code` | 获取短信验证码 | `pages/login/index` |
| 获取个人信息 | GET | `/app/auth/profile` | 获取当前用户信息 | `pages/profile/index` |
| 更新个人信息 | PUT | `/app/auth/profile` | 修改昵称/头像/手机等 | `pages/profile/edit` |
| 修改密码 | POST | `/app/auth/password` | 修改登录密码 | `pages/profile/password` |
| 上传头像 | POST | `/app/auth/upload-avatar` | 上传头像图片 | `pages/profile/edit` |

**请求示例 - 登录：**
```json
POST /api/app/auth/login
Content-Type: application/json

{
  "loginType": "sms",
  "phone": "13800138000",
  "smsCode": "123456",
  "clientType": "APP"
}
```

**响应示例：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "token": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "user": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员",
      "avatar": "https://xxx.com/avatar.jpg"
    }
  }
}
```

---

### 2.2 工作台模块 (`/app/workbench`)

| 接口 | Method | 路径 | 说明 | 前端页面 |
|------|--------|------|------|----------|
| 获取工作台 | GET | `/app/workbench` | 获取统计数据、快捷入口、通知 | `pages/workbench/index` |

**响应结构：**
```json
{
  "code": 200,
  "data": {
    "roleCode": "field_agent",
    "statsCards": [
      {"label": "今日报备数", "value": 10, "icon": "report"}
    ],
    "quickEntries": [
      {"label": "客户报备", "icon": "customer-report", "path": "/pages/customer/report"}
    ],
    "recentNotices": [...],
    "pendingApprovalCount": 5
  }
}
```

---

### 2.3 客户管理模块 (`/app/customer`)

| 接口 | Method | 路径 | 说明 | 前端页面 |
|------|--------|------|------|----------|
| 客户列表 | GET | `/app/customer/list` | 分页查询客户 | `pages/customer/list` |
| 客户详情 | GET | `/app/customer/{id}` | 获取客户详情 | `pages/customer/detail` |
| 客户报备 | POST | `/app/customer/report` | 新增客户报备 | `pages/customer/report` |
| 更新客户 | PUT | `/app/customer` | 修改客户信息 | `pages/customer/detail` |
| 添加跟进 | POST | `/app/customer/follow` | 添加跟进记录 | `pages/customer/detail` |
| 客户判重 | POST | `/app/customer/duplicate-check` | 检查客户是否重复 | `pages/customer/report` |

**请求示例 - 客户报备：**
```json
POST /api/app/customer/report
Content-Type: application/json

{
  "name": "张三",
  "phone": "13800138000",
  "projectId": 1,
  "visitTime": "2026-03-15 10:00:00",
  "remark": "首次看房"
}
```

---

### 2.4 考勤打卡模块 (`/app/attendance`)

| 接口 | Method | 路径 | 说明 | 前端页面 |
|------|--------|------|------|----------|
| GPS 打卡 | POST | `/app/attendance/clock` | GPS 定位打卡 | `pages/attendance/clock` |
| 扫码签到 | POST | `/app/attendance/scan-sign` | 扫码签到 | `pages/attendance/sign` |
| 考勤日历 | GET | `/app/attendance/calendar` | 获取考勤日历 | `pages/attendance/calendar` |
| 打卡记录 | GET | `/app/attendance/records` | 查询打卡记录 | `pages/attendance/calendar` |

**请求示例 - GPS 打卡：**
```json
POST /api/app/attendance/clock
Content-Type: application/json

{
  "type": "clock_in",
  "latitude": 31.2304,
  "longitude": 121.4737,
  "address": "上海市浦东新区 xxx 路 xxx 号",
  "deviceInfo": "iPhone 14 Pro"
}
```

---

### 2.5 审批模块 (`/app/approval`)

| 接口 | Method | 路径 | 说明 | 前端页面 |
|------|--------|------|------|----------|
| 申请列表 | GET | `/app/approval/list` | 查询申请记录 | `pages/apply/list` |
| 申请详情 | GET | `/app/approval/{id}` | 获取申请详情 | `pages/approval/detail` |
| 提交请假 | POST | `/app/approval/leave` | 提交请假申请 | `pages/apply/leave` |
| 提交调休 | POST | `/app/approval/compensate` | 提交调休申请 | `pages/apply/compensate` |
| 提交报销 | POST | `/app/approval/expense` | 提交报销申请 | `pages/apply/expense` |
| 提交垫佣 | POST | `/app/approval/advance` | 提交垫佣申请 | `pages/apply/advance` |
| 提交预付佣 | POST | `/app/approval/prepay` | 提交预付佣申请 | `pages/apply/prepay` |
| 审批通过 | POST | `/app/approval/{id}/approve` | 审批通过 | `pages/approval/detail` |
| 审批驳回 | POST | `/app/approval/{id}/reject` | 审批驳回 | `pages/approval/detail` |

---

### 2.6 消息聊天模块

#### REST API (`/sys/chat`)

| 接口 | Method | 路径 | 说明 | 前端页面 |
|------|--------|------|------|----------|
| 联系人列表 | GET | `/sys/chat/users` | 获取聊天联系人 | `pages/index/index` |
| 消息历史 | GET | `/sys/chat/messages` | 获取聊天记录 | `pages/chat/index` |
| 发送消息 | POST | `/sys/chat/send` | 发送消息 | `pages/chat/index` |
| 撤回消息 | POST | `/sys/chat/recall/{msgId}` | 撤回消息 | `pages/chat/index` |
| 消息确认 | POST | `/sys/chat/ack/{msgId}` | 消息已读确认 | - |
| 离线消息 | GET | `/sys/chat/offline` | 获取离线消息 | - |
| 消息搜索 | GET | `/sys/chat/search` | 搜索消息 | - |

#### WebSocket

| 事件 | 方向 | 说明 |
|------|------|------|
| `connect` | Client→Server | 建立连接 |
| `message` | Server→Client | 接收消息 |
| `notice` | Server→Client | 系统通知 |
| `unreadCount` | Server→Client | 未读数更新 |
| `dataChange` | Server→Client | 数据变更通知 |

**WebSocket 连接示例：**
```javascript
const ws = new WebSocket('wss://your-domain.com/ws/message?token=xxx')
ws.onmessage = (event) => {
  const data = JSON.parse(event.data)
  // data.type: 'message' | 'notice' | 'unreadCount'
}
```

---

### 2.7 AI 助手模块 (`/app/ai`)

| 接口 | Method | 路径 | 说明 | 前端页面 |
|------|--------|------|------|----------|
| AI 对话 | POST | `/app/ai/chat` | 智能问答 | `pages/ai/chat` |
| 文案生成 | POST | `/app/ai/content/generate` | 营销文案生成 | - |
| 客户分析 | POST | `/app/ai/customer/analyze` | AI 客户分析 | `pages/customer/detail` |

**请求示例 - AI 对话：**
```json
POST /api/app/ai/chat
Content-Type: application/json

{
  "message": "如何跟进意向客户？",
  "conversationId": "xxx-xxx-xxx"
}
```

---

### 2.8 通讯录模块 (`/app/contacts`)

| 接口 | Method | 路径 | 说明 | 前端页面 |
|------|--------|------|------|----------|
| 联系人列表 | GET | `/app/contacts/list` | 获取通讯录列表 | `pages/contacts/index` |
| 联系人详情 | GET | `/app/contacts/{id}` | 获取联系人详情 | - |
| 搜索联系人 | GET | `/app/contacts/search` | 搜索联系人 | `pages/contacts/index` |

---

### 2.9 个人中心模块

| 接口 | Method | 路径 | 说明 | 前端页面 |
|------|--------|------|------|----------|
| 个人信息 | GET | `/app/auth/profile` | 获取个人信息 | `pages/profile/index` |
| 更新资料 | PUT | `/app/auth/profile` | 修改个人信息 | `pages/profile/edit` |
| 修改密码 | POST | `/app/auth/password` | 修改密码 | `pages/profile/password` |
| 黑名单 | GET | `/app/user/blacklist` | 获取黑名单列表 | `pages/profile/blacklist` |
| 添加黑名单 | POST | `/app/user/blacklist` | 拉黑用户 | - |
| 移除黑名单 | DELETE | `/app/user/blacklist/{id}` | 移除黑名单 | `pages/profile/blacklist` |

---

## 三、UniApp 请求封装

项目已封装统一的请求工具，位于 `utils/request.js`：

```javascript
import request from '@/utils/request'

// GET 请求
export function getCustomerList(params) {
  return request({
    url: '/app/customer/list',
    method: 'get',
    params
  })
}

// POST 请求
export function submitReport(data) {
  return request({
    url: '/app/customer/report',
    method: 'post',
    data
  })
}
```

**请求拦截器自动处理：**
- 添加 `satoken` 认证头
- 统一错误处理
- Token 过期自动跳转登录

---

## 四、API 配置

### 4.1 环境配置 (`utils/config.js`)

```javascript
export default {
  // 开发环境
  API_BASE_URL: 'http://localhost:8080/api',
  WS_URL: 'ws://localhost:8080/ws',
  
  // 生产环境（云端部署后修改）
  // API_BASE_URL: 'https://your-domain.com/api',
  // WS_URL: 'wss://your-domain.com/ws',
  
  ENABLE_CRYPTO: true  // 是否启用接口加密
}
```

### 4.2 生产环境部署

1. 云端部署后获取域名（如 `https://api.pengcheng.com`）
2. 修改 `utils/config.js` 中的 `API_BASE_URL`
3. 重新打包 APP

---

## 五、错误码说明

| Code | 说明 | 处理建议 |
|------|------|----------|
| 200 | 成功 | - |
| 401 | 未认证 | 跳转登录页 |
| 403 | 无权限 | 提示"无权限访问" |
| 404 | 资源不存在 | 提示"资源不存在" |
| 500 | 服务器错误 | 提示"服务器繁忙" |
| 1001 | 账号被禁用 | 提示联系客服 |
| 1002 | 账号被列入黑名单 | 提示"账号异常" |

---

## 六、接口安全

### 6.1 认证机制

- 所有接口（除登录/注册外）都需要携带 `satoken`
- Token 存储在本地：`uni.setStorageSync('token', token)`
- Token 过期时间：默认 7 天

### 6.2 接口加密（可选）

敏感接口支持 RSA+AES 双重加密：

1. 客户端生成随机 AES Key
2. 使用 AES Key 加密请求体
3. 使用服务端公钥加密 AES Key
4. 一起发送到服务端

---

## 七、API 版本管理

当前版本：**v1.0**

版本升级策略：
- 向后兼容的变更：保持原路径
- 破坏性变更：`/api/v2/...`

---

## 八、开发调试

### 8.1 使用 Postman 调试

1. 导入 Postman 集合（见 `doc/postman/APP-API.postman_collection.json`）
2. 配置环境变量 `baseUrl = http://localhost:8080/api`
3. 登录后保存 token 到环境变量
4. 在 Authorization 中添加 `satoken: {{token}}`

### 8.2 使用 HBuilderX 调试

1. HBuilderX → 运行到浏览器
2. 打开开发者工具查看 Network
3. 或使用 `uni.request` 的 success/fail 回调调试

---

## 九、相关文档

- [APP 打包指南](../pengcheng-uniapp/APP-BUILD-GUIDE.md)
- [部署运维手册](DEPLOYMENT.md)
- [用户手册](USER-MANUAL.md)
- [系统架构文档](ARCHITECTURE.md)
