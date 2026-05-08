# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码仓库中工作时提供指引。

## 项目概述

`pengcheng-uniapp` 是 MasterLife OA 平台的移动端客户端（UniApp / Vue 3 + Vite），目标平台为微信小程序与原生 App（通过 HBuilderX 云打包），同时支持 H5 模式用于浏览器调试。后端服务位于同级目录的 `pengcheng-oa` 多模块项目（Spring Boot）。

## 常用命令

```bash
# 安装依赖
npm install

# H5 开发服务器（CLI 唯一支持的调试目标）
npm run dev:h5        # 别名：npm run dev:mobile
# 监听 0.0.0.0，局域网内真机可直接访问

# 校验页面/接口一致性
npm run verify        # 执行以下两个脚本
npm run verify:miniapp   # 检查 pages.json 与 pages/ 目录是否匹配
npm run verify:api       # 检查 api.js 接口覆盖情况

# 首次运行前检查环境
node scripts/check-env.js
```

**微信小程序与 App 打包**必须通过 HBuilderX 完成（无 CLI 路径），详见 `QUICK-START.md`。

## 架构

### 目录结构

```
utils/
  config.js       # API 基础地址（运行时可配置，默认局域网 IP）
  request.js      # 统一 HTTP 客户端（注入 Token、错误处理、离线缓存）
  auth.js         # Token / userInfo 工具函数（基于 uni.getStorageSync）
  api.js          # 所有后端接口定义，页面统一从此导入
  websocket.js    # 单例 WS 客户端（心跳 + 指数退避重连）
  crypto.js       # AES-GCM 响应解密（基于 crypto-js）
  notice.js       # 通知工具函数
pages/            # 按功能模块组织，每个目录含 index.vue 或命名视图
static/           # 图片、tabBar 图标
scripts/          # 开发工具脚本（环境配置、一致性校验）
```

### 核心模式

**API 调用** — 所有接口定义在 `utils/api.js`，调用 `utils/request.js` 中的 `get`/`post`/`put`/`del`。页面内禁止直接调用 `uni.request`。

**HTTP 客户端** — `utils/request.js` 自动注入 `Authorization` Token，解包后端统一响应格式 `{ code, msg, data }`，自动解密 AES-GCM 响应，遇到 401 时重定向至 `/pages/login/index`。网络失败时，关键表单提交会缓存至 `pending_submissions` 本地存储以供恢复。

**认证** — Token 以键名 `'token'`、用户信息以 `'userInfo'` 存储于 `uni.getStorageSync`。统一使用 `utils/auth.js` 中的工具函数：`getToken`、`setToken`、`clearAuth`、`checkLogin`。

**API 基础地址** — 配置于 `utils/config.js`，默认为 `http://192.168.31.135:8080`（本地局域网地址）。生产打包前需修改 `DEFAULT_API_BASE_URL`。运行时也可通过 `setApiBaseUrl()` 动态覆盖并持久化到存储。

**WebSocket** — 从 `utils/websocket.js` 导出的单例，连接至 `ws(s)://<base>/ws/message?token=<token>`，发出类型化事件：`chat`、`groupChat`、`notice`、`dataChange`、`unreadCount`。`App.vue` 在登录后注册全局监听器，并更新 tabBar 消息角标。

**响应加密** — 后端可能对 `data` 字段进行 AES-GCM 加密，`request.js` 会自动调用 `isAesEncryptedData()` / `decryptResponseData()` 完成解密；加密配置从 `/api/crypto/config` 获取后缓存。

**UI 组件** — uview-plus 3.x，通过 `easycom` 自动导入，所有 `u-*` 组件全局可用，无需手动 import。全局通用样式（`.card`、`.btn-primary`、`.avatar` 等）在 `App.vue` 中声明。

### 后端 API 前缀

| 前缀 | 用途 |
|---|---|
| `/api/app/auth/...` | 移动端登录、个人资料、头像 |
| `/api/app/...` | 移动端专属：工作台、客户、考勤、请假、付款、审批、AI |
| `/api/sys/chat/...` | 私聊（记录、联系人、已读、黑名单） |
| `/api/chat/group/...` | 群聊管理 |
| `/api/auth/...` | 通用认证（用户信息、修改密码、退出） |
| `/api/sys/notice/...` | 系统通知 |

### 导航与权限

4 个 tabBar 页（工作台 / 消息 / 通讯录 / 我的）固定配置于 `pages.json`。工作台快捷入口与我的页功能列表均**硬编码**在各自的 `index.vue` 中，不受后端菜单/RBAC 数据驱动，所有已实现页面对任意登录用户可见。

`App.vue → onLaunch` 为 App 入口：依次检查登录状态、连接 WebSocket、注册全局事件监听，并检查是否存在待重提交的缓存表单数据。
