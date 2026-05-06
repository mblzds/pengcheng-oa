# 微信小程序部署指南

本文档说明如何将 MasterLife 小程序从开发到上线的完整流程。

---

## 一、部署流程概览

```
开发环境 → 体验版 → 审核版 → 正式版（上线）
```

| 阶段 | 用途 | 用户范围 | 有效期 |
|------|------|----------|--------|
| **开发版** | 开发调试 | 开发者 | 永久 |
| **体验版** | 内部测试 | 体验成员 | 永久 |
| **审核版** | 提交审核 | 审核人员 | 审核期 |
| **正式版** | 线上发布 | 所有用户 | 永久 |

---

## 二、开发环境配置

### 2.1 安装微信开发者工具

1. **下载地址**: https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html
2. **选择版本**: 根据系统选择 Windows/macOS 版
3. **安装并启动**: 使用微信扫码登录

### 2.2 导入项目

1. 打开微信开发者工具
2. 点击"+"导入项目
3. 选择项目目录：`pengcheng-admin/pengcheng-uniapp`
4. 填写小程序 AppID（测试可选"测试号"）
5. 点击"导入"

### 2.3 配置项目

**project.config.json**（HBuilderX 自动生成）:

```json
{
  "appid": "wx_xxxxxxxxxxxxxxxx",
  "projectname": "masterlife",
  "description": "MasterLife - 高效沟通，智慧办公",
  "setting": {
    "urlCheck": false,
    "es6": true,
    "postcss": true,
    "minified": true
  }
}
```

### 2.4 配置服务器域名（开发环境）

开发阶段可临时关闭域名校验：

1. 微信开发者工具 → 右上角"详情"
2. "本地设置" → 勾选：
   - ✅ 不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书
   - ✅ 开启调试模式

**注意**: 真机预览和上线时必须配置合法域名

---

## 三、小程序配置

### 3.1 修改 manifest.json

打开 `pengcheng-uniapp/manifest.json`:

```json
{
  "name": "MasterLife",
  "appid": "__UNI__.xxxxxxxx",
  "description": "MasterLife - 高效沟通，智慧办公",
  "versionName": "1.0.0",
  "versionCode": "100",
  "mp-weixin": {
    "appid": "wx_xxxxxxxxxxxxxxxx",
    "setting": {
      "urlCheck": false,
      "es6": true,
      "postcss": true,
      "minified": true
    },
    "usingComponents": true,
    "permission": {
      "scope.userLocation": {
        "desc": "你的位置信息将用于考勤打卡"
      }
    },
    "requiredPrivateInfos": ["chooseLocation", "getLocation"]
  }
}
```

### 3.2 配置 API 地址

修改 `pengcheng-uniapp/utils/config.js`:

```javascript
// 开发环境
const DEFAULT_API_BASE_URL = 'http://localhost:8080'

// 生产环境（上线前修改）
// const DEFAULT_API_BASE_URL = 'https://api.pengcheng.com'
```

### 3.3 配置 WebSocket

修改 `pengcheng-uniapp/utils/websocket.js`:

```javascript
// 开发环境
const WS_URL = 'ws://localhost:8080/ws'

// 生产环境
// const WS_URL = 'wss://api.pengcheng.com/ws'
```

---

## 四、编译与预览

### 4.1 HBuilderX 编译到小程序

1. 打开 HBuilderX
2. 菜单：**发行** → **微信小程序**
3. 等待编译完成
4. 自动生成到 `unpackage/dist/mp-weixin/`

### 4.2 微信开发者工具预览

1. 打开微信开发者工具
2. 导入 `unpackage/dist/mp-weixin/` 目录
3. 编译运行
4. 查看预览效果

### 4.3 真机预览

1. 微信开发者工具 → 右上角"预览"
2. 使用微信扫码
3. 在手机上体验

---

## 五、上传代码

### 5.1 版本管理

在 `manifest.json` 中管理版本：

```json
{
  "versionName": "1.0.0",  // 版本号（用户可见）
  "versionCode": "100"     // 版本号（递增，用于更新）
}
```

**版本规则**:
- `versionName`: X.Y.Z（主版本。次版本。修订号）
- `versionCode`: 整数，每次上传必须递增

### 5.2 上传步骤

**方式一：HBuilderX 上传**

1. HBuilderX → **发行** → **微信小程序**
2. 勾选"上传代码"
3. 填写版本号和备注
4. 点击"上传"

**方式二：微信开发者工具上传**

1. 微信开发者工具导入项目
2. 点击右上角"上传"
3. 填写版本信息
4. 点击"上传"

### 5.3 版本备注模板

```
v1.0.0 - 首次发布
- 新增微信登录
- 新增客户管理
- 新增考勤打卡
- 新增审批流程
- 新增 AI 助手
```

---

## 六、体验版配置

### 6.1 设置体验成员

登录小程序后台：**成员管理** → **体验成员**:

1. 点击"添加"
2. 输入成员微信号
3. 设置权限
4. 保存

### 6.2 体验版二维码

1. 登录小程序后台
2. **版本管理** → 找到刚上传的版本
3. 点击"选为体验版"
4. 扫描二维码下载

### 6.3 体验版测试清单

- [ ] 微信登录功能
- [ ] 手机号登录
- [ ] 客户报备
- [ ] 考勤打卡
- [ ] 审批申请
- [ ] 消息聊天
- [ ] AI 助手
- [ ] 个人中心

---

## 七、提交审核

### 7.1 准备审核材料

| 材料 | 说明 | 要求 |
|------|------|------|
| **功能截图** | 小程序各功能页面截图 | 至少 5 张 |
| **隐私政策** | 用户隐私协议链接 | 必须有 |
| **用户协议** | 用户使用协议链接 | 必须有 |
| **测试账号** | 供审核使用的账号 | 建议提供 |
| **补充材料** | 行业资质等 | 根据类目 |

### 7.2 选择服务类目

登录小程序后台：**设置** → **基本设置** → **服务类目**:

**房地产中介**（推荐）:
- 需要提供营业执照
- 需要《房地产经纪机构备案证书》

**企业管理**（备选）:
- 需要提供营业执照
- 审核相对宽松

### 7.3 提交审核流程

1. 登录小程序后台
2. **版本管理** → 找到体验版
3. 点击"提交审核"
4. 填写审核信息：
   - 功能页面截图
   - 测试账号（账号：test，密码：test123）
   - 补充说明
5. 提交等待审核

### 7.4 审核时间

- **正常审核**: 1-7 个工作日
- **加急审核**: 当天（每月有次数限制）
- **审核结果**: 短信通知

### 7.5 常见审核不通过原因

| 原因 | 解决方案 |
|------|----------|
| 功能未完成 | 完善功能后重新提交 |
| 隐私政策缺失 | 添加隐私政策页面 |
| 测试账号无法登录 | 提供有效测试账号 |
| 类目选择错误 | 选择正确的服务类目 |
| 涉及虚拟支付 | 移除 iOS 端的支付功能 |

---

## 八、发布上线

### 8.1 发布流程

1. 审核通过后，登录小程序后台
2. **版本管理** → 找到审核版本
3. 点击"发布"
4. 扫码确认发布

### 8.2 发布后检查

- [ ] 搜索小程序名称是否能找到
- [ ] 扫码是否能打开
- [ ] 各功能是否正常
- [ ] 数据是否准确

### 8.3 生成小程序码

1. 登录小程序后台
2. **设置** → **基本设置**
3. "小程序码及线下物料下载"
4. 下载小程序码图片

---

## 九、运营与维护

### 9.1 数据统计

登录小程序后台：**统计**:

- **访问分析**: DAU、MAU、访问时长
- **用户画像**: 地域、年龄、性别
- **行为分析**: 页面访问、点击热图

### 9.2 版本更新

**自动更新**: 用户下次打开自动更新

**强制更新**（代码实现）:

```javascript
// 检查更新
const updateManager = uni.getUpdateManager()
updateManager.onCheckForUpdate((res) => {
  if (res.hasUpdate) {
    updateManager.onUpdateReady(() => {
      uni.showModal({
        title: '更新提示',
        content: '新版本已准备好，请重启应用',
        success: (res) => {
          if (res.confirm) {
            updateManager.applyUpdate()
          }
        }
      })
    })
  }
})
```

### 9.3 用户反馈

1. 小程序后台 → **反馈管理**
2. 查看用户反馈
3. 及时回复处理

### 9.4 性能监控

1. 小程序后台 → **性能监控**
2. 查看启动耗时、页面渲染耗时
3. 优化性能瓶颈

---

## 十、小程序页面清单

当前项目包含的页面：

| 页面路径 | 说明 | 状态 |
|----------|------|------|
| `pages/login/index` | 登录页 | ✅ |
| `pages/workbench/index` | 工作台 | ✅ |
| `pages/index/index` | 消息列表 | ✅ |
| `pages/contacts/index` | 通讯录 | ✅ |
| `pages/chat/index` | 聊天页 | ✅ |
| `pages/group-chat/index` | 群聊 | ✅ |
| `pages/profile/index` | 个人中心 | ✅ |
| `pages/profile/edit` | 编辑资料 | ✅ |
| `pages/profile/password` | 修改密码 | ✅ |
| `pages/customer/list` | 客户列表 | ✅ |
| `pages/customer/detail` | 客户详情 | ✅ |
| `pages/customer/report` | 客户报备 | ✅ |
| `pages/attendance/clock` | 考勤打卡 | ✅ |
| `pages/attendance/calendar` | 考勤日历 | ✅ |
| `pages/attendance/sign` | 扫码签到 | ✅ |
| `pages/apply/list` | 申请记录 | ✅ |
| `pages/apply/leave` | 请假申请 | ✅ |
| `pages/apply/compensate` | 调休申请 | ✅ |
| `pages/apply/expense` | 报销申请 | ✅ |
| `pages/apply/advance` | 垫佣申请 | ✅ |
| `pages/apply/prepay` | 预付佣申请 | ✅ |
| `pages/approval/list` | 审批中心 | ✅ |
| `pages/approval/detail` | 审批详情 | ✅ |
| `pages/ai/chat` | AI 助手 | ✅ |

---

## 十一、小程序 TabBar 配置

当前 TabBar 配置（`pages.json`）:

```json
"tabBar": {
  "color": "#999999",
  "selectedColor": "#07C160",
  "backgroundColor": "#ffffff",
  "list": [
    {
      "pagePath": "pages/workbench/index",
      "text": "工作台",
      "iconPath": "static/tabbar/workbench.png",
      "selectedIconPath": "static/tabbar/workbench-active.png"
    },
    {
      "pagePath": "pages/index/index",
      "text": "消息",
      "iconPath": "static/tabbar/chat.png",
      "selectedIconPath": "static/tabbar/chat-active.png"
    },
    {
      "pagePath": "pages/contacts/index",
      "text": "通讯录",
      "iconPath": "static/tabbar/contacts.png",
      "selectedIconPath": "static/tabbar/contacts-active.png"
    },
    {
      "pagePath": "pages/profile/index",
      "text": "我的",
      "iconPath": "static/tabbar/profile.png",
      "selectedIconPath": "static/tabbar/profile-active.png"
    }
  ]
}
```

---

## 十二、小程序权限配置

### 12.1 已配置的权限

| 权限 | 用途 | 配置 |
|------|------|------|
| `scope.userLocation` | 考勤打卡定位 | 已配置 |
| `getLocation` | 获取当前位置 | 已配置 |
| `chooseLocation` | 选择位置 | 已配置 |

### 12.2 权限使用说明

在 `manifest.json` 中配置：

```json
"mp-weixin": {
  "permission": {
    "scope.userLocation": {
      "desc": "你的位置信息将用于考勤打卡"
    }
  },
  "requiredPrivateInfos": ["chooseLocation", "getLocation"]
}
```

### 12.3 权限申请技巧

- **描述具体**: 说明权限的具体用途
- **场景匹配**: 在需要使用权限的页面申请
- **用户引导**: 提前告知用户为什么需要权限

---

## 十三、常见问题

### Q1: 小程序无法连接服务器

**解决**:
1. 检查服务器域名是否配置
2. 确认域名已备案
3. 检查 SSL 证书是否有效
4. 开发环境可临时关闭域名校验

### Q2: 上传代码失败

**解决**:
1. 检查 AppID 是否正确
2. 确认有上传权限
3. 检查网络是否畅通
4. 重启微信开发者工具

### Q3: 审核被拒绝

**解决**:
1. 查看拒绝原因
2. 根据要求修改
3. 重新提交审核
4. 可在"站内信"查看详细说明

### Q4: 小程序无法搜索到

**解决**:
1. 新发布小程序需要 24 小时后可搜索
2. 检查是否已发布（不是体验版）
3. 检查是否被下架

### Q5: 用户反馈无法登录

**解决**:
1. 检查服务器是否正常运行
2. 检查域名是否过期
3. 查看后端日志
4. 提供客服联系方式

---

## 十四、相关文档

- [微信小程序开发文档](https://developers.weixin.qq.com/miniprogram/dev/framework/)
- [微信小程序运营规范](https://developers.weixin.qq.com/miniprogram/product/brief.html)
- [微信登录配置](./WECHAT-LOGIN-PAY-CONFIG.md)
- [APP 打包指南](../pengcheng-uniapp/APP-BUILD-GUIDE.md)

---

**文档版本**: V1.0  
**最后更新**: 2026-03-12
