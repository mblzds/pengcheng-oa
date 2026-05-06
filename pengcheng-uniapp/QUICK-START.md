# APP 打包快速开始

本文档提供 APP 打包的快速入门指南，5 分钟完成环境配置。

---

## 一、快速配置（3 分钟）

### 方式一：自动配置（推荐）

#### Windows
```bash
cd pengcheng-admin/pengcheng-uniapp
scripts\setup-env.bat
```

#### macOS / Linux
```bash
cd pengcheng-admin/pengcheng-uniapp
chmod +x scripts/setup-env.sh
./scripts/setup-env.sh
```

### 方式二：手动配置

```bash
# 1. 安装依赖
npm install

# 2. 检查环境
node scripts/check-env.js
```

---

## 二、HBuilderX 配置（2 分钟）

### 1. 下载 HBuilderX

访问：https://www.dcloud.io/hbuilderx.html

下载并安装 HBuilderX（选择正式版）

### 2. 打开项目

1. 启动 HBuilderX
2. 文件 → 打开目录
3. 选择 `pengcheng-uniapp` 目录

### 3. 登录 DCloud 账号

1. 工具栏 → 工具 → 登录
2. 使用 DCloud 账号登录（没有则注册）

### 4. 配置 manifest.json

在 HBuilderX 中打开 `manifest.json`，切换到"App 其他配置"：

| 配置项 | 值 |
|--------|-----|
| 应用名称 | MasterLife |
| 应用包名 | `com.pengcheng.masterlife` |
| 版本号 | 1.0.0 |
| 版本号 (Code) | 100 |

### 5. 配置证书

**Android 证书（首次打包需要）：**

1. 发行 → 原生 App-云打包
2. 点击"证书管理"
3. 新建证书：
   - 别名：`masterlife`
   - 密码：`MasterLife@2026`（记住这个密码）
   - 有效期：25 年
   - 其他信息按实填写

---

## 三、开始打包

### 1. 云打包（推荐）

1. 菜单：发行 → 原生 App-云打包
2. 选择"正式打包"
3. 选择刚才创建的证书
4. 点击"打包"
5. 等待 5-10 分钟
6. 打包完成后下载 APK

### 2. 获取安装包

打包完成后，HBuilderX 会自动打开输出目录：

```
unpackage/dist/apk/
└── MasterLife_1.0.0.apk
```

---

## 四、安装测试

### Android 安装

1. 将 APK 传输到手机
2. 允许安装未知来源应用
3. 点击安装
4. 打开应用

### 测试登录

使用以下账号测试：

| 账号 | 密码 |
|------|------|
| admin | admin123 |

---

## 五、配置生产环境

### 修改 API 地址

打包前修改 `utils/config.js`：

```javascript
export default {
  // 修改为云端部署地址
  API_BASE_URL: 'https://your-domain.com/api',
  WS_URL: 'wss://your-domain.com/ws',
  ENABLE_CRYPTO: true
}
```

### 重新打包

修改配置后需要重新打包：

1. 保存文件
2. 发行 → 原生 App-云打包
3. 选择正式打包

---

## 六、常见问题

### Q: 打包失败，提示证书错误

**解决：**
1. 检查证书密码是否正确
2. 删除旧证书重新创建
3. 确保证书别名不重复

### Q: APP 无法连接服务器

**解决：**
1. 检查 `utils/config.js` 中的 API 地址
2. 确保服务器已配置 HTTPS
3. 检查服务器防火墙设置

### Q: 安装包太大

**解决：**
1. 压缩静态资源图片
2. 选择"自定义基座"（减小包体积）
3. 使用云打包而非本地打包

### Q: 如何更新 APP

**解决：**
1. 修改 `manifest.json` 中的版本号
2. 重新打包
3. 分发新版本 APK 给用户

---

## 七、下一步

打包完成后，参考以下文档：

- [APP 打包完整指南](./APP-BUILD-GUIDE.md) - 详细说明所有配置项
- [图标和启动图配置](./APP-ICON-GUIDE.md) - 制作 APP 图标
- [API 接口映射](../doc/APP-API-MAPPING.md) - 了解接口文档
- [安装包生成指南](../doc/安装包生成指南.md) - 更多打包方案

---

## 八、技术支持

遇到问题？

- 查看文档：`pengcheng-admin/doc/`
- 联系管理员：support@pengchengkeji.com
- DCloud 论坛：https://ask.dcloud.net.cn/
