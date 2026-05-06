# MasterLife APP 打包指南

本文档说明如何将 UniApp 项目打包为 iOS 和 Android 原生 APP。

## 一、准备工作

### 1.1 环境要求

| 平台 | 工具 | 版本要求 |
|------|------|----------|
| 通用 | HBuilderX | 3.9+ |
| 通用 | Node.js | 18+ |
| Android | Android Studio | 2022+ (可选，用于自定义基座) |
| Android | JDK | 11+ |
| iOS | Xcode | 14+ (Mac only) |
| iOS | CocoaPods | 1.12+ |

### 1.2 下载 HBuilderX

1. 访问官网：https://www.dcloud.io/hbuilderx.html
2. 下载并安装 HBuilderX
3. 启动 HBuilderX

---

## 二、配置 manifest.json

### 2.1 基本配置

打开 `manifest.json`，配置以下信息：

```json
{
    "name": "MasterLife",
    "appid": "__UNI__xxxxxxxx",  // HBuilderX 自动分配
    "description": "MasterLife - 高效沟通，智慧办公",
    "versionName": "1.0.0",
    "versionCode": "100"
}
```

### 2.2 Android 配置

```json
"app-plus": {
    "distribute": {
        "android": {
            "permissions": [
                "<uses-permission android:name=\"android.permission.INTERNET\"/>",
                "<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\"/>",
                "<uses-permission android:name=\"android.permission.ACCESS_WIFI_STATE\"/>",
                "<uses-permission android:name=\"android.permission.CAMERA\"/>",
                "<uses-permission android:name=\"android.permission.READ_EXTERNAL_STORAGE\"/>",
                "<uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\"/>",
                "<uses-permission android:name=\"android.permission.READ_PHONE_STATE\"/>",
                "<uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\"/>",
                "<uses-permission android:name=\"android.permission.ACCESS_COARSE_LOCATION\"/>",
                "<uses-permission android:name=\"android.permission.VIBRATE\"/>"
            ],
            "package": "com.pengcheng.masterlife",
            "icon": {
                "hdpi": "static/app-icon/android/icon-72.png",
                "xhdpi": "static/app-icon/android/icon-96.png",
                "xxhdpi": "static/app-icon/android/icon-144.png",
                "xxxhdpi": "static/app-icon/android/icon-192.png"
            },
            "schemes": "masterlife://"
        }
    }
}
```

### 2.3 iOS 配置

```json
"app-plus": {
    "distribute": {
        "ios": {
            "bundleid": "com.pengcheng.masterlife",
            "appstore": true,
            "dSYMs": false,
            "icon": {
                "appstore": "static/app-icon/ios/icon-1024.png",
                "ipad": {
                    "app": "static/app-icon/ios/icon-76.png",
                    "app@2x": "static/app-icon/ios/icon-152.png",
                    "notification": "static/app-icon/ios/icon-20.png",
                    "notification@2x": "static/app-icon/ios/icon-40.png",
                    "proapp@2x": "static/app-icon/ios/icon-167.png",
                    "settings": "static/app-icon/ios/icon-29.png",
                    "settings@2x": "static/app-icon/ios/icon-58.png",
                    "spotlight": "static/app-icon/ios/icon-40.png",
                    "spotlight@2x": "static/app-icon/ios/icon-80.png"
                },
                "iphone": {
                    "app@2x": "static/app-icon/ios/icon-120.png",
                    "app@3x": "static/app-icon/ios/icon-180.png",
                    "notification@2x": "static/app-icon/ios/icon-40.png",
                    "notification@3x": "static/app-icon/ios/icon-60.png",
                    "settings@2x": "static/app-icon/ios/icon-58.png",
                    "settings@3x": "static/app-icon/ios/icon-87.png",
                    "spotlight@2x": "static/app-icon/ios/icon-80.png",
                    "spotlight@3x": "static/app-icon/ios/icon-120.png"
                }
            },
            "schemes": "masterlife://"
        }
    }
}
```

### 2.4 API 地址配置

修改 `utils/config.js`：

```javascript
export default {
    // 生产环境 API 地址（云端部署地址）
    API_BASE_URL: 'https://your-domain.com',
    
    // WebSocket 地址
    WS_URL: 'wss://your-domain.com/ws',
    
    // 是否启用加密
    ENABLE_CRYPTO: true
}
```

---

## 三、打包流程

### 3.1 云打包（推荐）

1. **在 HBuilderX 中打开项目**
   - 文件 → 打开目录 → 选择 `pengcheng-uniapp`

2. **登录 DCloud 账号**
   - 工具栏 → 工具 → 登录 → 使用 DCloud 账号登录

3. **选择打包类型**
   - 工具栏 → 发行 → 原生 App-云打包

4. **配置打包参数**

   | 参数 | 说明 | 推荐值 |
   |------|------|--------|
   | 证书 | Android 签名证书 | 新建或选择已有证书 |
   | 渠道包 | 是否需要多渠道 | 根据需要选择 |
   | SDK 版本 | 选择离线 SDK | 默认即可 |

5. **开始打包**
   - 点击"打包"按钮
   - 等待打包完成（约 5-10 分钟）
   - 下载生成的 `.apk` 或 `.ipa` 文件

### 3.2 离线打包（高级）

#### Android 离线打包

1. **下载离线 SDK**
   - https://nativesupport.dcloud.net.cn/AppDocs/download/android

2. **导入 Android Studio**
   - 解压 SDK
   - 用 Android Studio 打开

3. **配置签名证书**

   在 `app/build.gradle` 中配置：

   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file('masterlife.keystore')
               storePassword 'your_password'
               keyAlias 'masterlife'
               keyPassword 'your_password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
           }
       }
   }
   ```

4. **编译打包**
   ```bash
   ./gradlew assembleRelease
   ```

#### iOS 离线打包

1. **下载离线 SDK**
   - https://nativesupport.dcloud.net.cn/AppDocs/download/ios

2. **配置证书和描述文件**
   - 需要 Apple Developer 账号
   - 创建 App ID
   - 创建证书和描述文件

3. **使用 Xcode 打包**
   - 打开项目
   - 选择 Product → Archive
   - 导出 .ipa

---

## 四、证书申请

### 4.1 Android 证书

**新建证书（推荐）：**

1. HBuilderX → 工具 → 打包设置 → 证书管理
2. 点击"新建证书"
3. 填写信息：
   - 别名：`masterlife`
   - 密码：`你的密码（记住！）`
   - 有效期：`25 年`
   - 姓名：`公司名`
   - 组织：`公司名`
   - 城市：`城市名`
   - 省份：`省份`
   - 国家：`CN`

**或使用已有证书：**
```bash
keytool -genkey -v -keystore masterlife.keystore -alias masterlife -keyalg RSA -keysize 2048 -validity 10000
```

### 4.2 iOS 证书

1. **注册 Apple Developer 账号**
   - https://developer.apple.com
   - 年费：$99

2. **创建 App ID**
   - Bundle ID: `com.pengcheng.masterlife`

3. **创建证书**
   - Certificates → + → App Store and Ad Hoc

4. **创建描述文件**
   - Provisioning Profiles → + → App Store

---

## 五、上架准备

### 5.1 Android 应用商店

| 商店 | 要求 |
|------|------|
| 华为应用市场 | 软著 + 隐私政策 + ICP 备案 |
| 小米应用商店 | 软著 + 隐私政策 |
| OPPO 软件商店 | 软著 + 隐私政策 |
| vivo 应用商店 | 软著 + 隐私政策 |
| 应用宝 | 软著 + 隐私政策 + ICP 备案 |

### 5.2 App Store

1. **准备材料**
   - Apple Developer 账号
   - 软著（中国开发者）
   - 隐私政策 URL
   - 支持 URL

2. **创建 App**
   - 登录 App Store Connect
   - 创建新 App
   - 填写元数据（截图、描述、关键词）

3. **提交审核**
   - 上传构建版本
   - 填写审核信息
   - 提交审核（通常 1-3 天）

---

## 六、常见问题

### Q1: 打包失败，提示证书错误
**解决：** 检查证书密码是否正确，证书是否过期

### Q2: APP 无法连接服务器
**解决：** 
1. 检查 `utils/config.js` 中的 API 地址
2. 确保服务器已配置 HTTPS
3. 检查服务器 CORS 配置

### Q3: iOS 打包提示 Bundle ID 冲突
**解决：** 检查 App Store Connect 中是否已有相同 Bundle ID 的 App

### Q4: Android 安装提示"未知来源"
**解决：** 这是正常提示，用户需要在设置中允许安装未知来源应用

---

## 七、APP 功能清单

当前 UniApp 项目已实现的功能模块：

| 模块 | 功能 | 状态 |
|------|------|------|
| 工作台 | 快捷入口、待办事项 | ✅ |
| 消息 | 即时通讯、群聊 | ✅ |
| 通讯录 | 联系人列表 | ✅ |
| 登录 | 密码/短信登录 | ✅ |
| 个人中心 | 个人信息、修改密码 | ✅ |
| 客户管理 | 报备、列表、详情 | ✅ |
| 考勤打卡 | 打卡、日历、签到 | ✅ |
| 审批流程 | 请假/报销/垫佣等申请 | ✅ |
| AI 助手 | 智能问答 | ✅ |

---

## 八、技术支持

- **官方文档**: https://uniapp.dcloud.net.cn/
- **HBuilderX 下载**: https://www.dcloud.io/hbuilderx.html
- **DCloud 论坛**: https://ask.dcloud.net.cn/
- **项目问题**: 联系项目管理员
