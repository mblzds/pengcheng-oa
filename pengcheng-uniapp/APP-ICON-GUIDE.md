# MasterLife APP 图标和启动图配置指南

本文档说明如何配置 APP 的图标和启动图。

---

## 一、图标要求

### 1.1 Android 图标

| 密度 | 尺寸 (px) | 文件路径 |
|------|-----------|----------|
| hdpi | 72x72 | `static/app-icon/android/icon-72.png` |
| xhdpi | 96x96 | `static/app-icon/android/icon-96.png` |
| xxhdpi | 144x144 | `static/app-icon/android/icon-144.png` |
| xxxhdpi | 192x192 | `static/app-icon/android/icon-192.png` |

**要求：**
- 格式：PNG（带透明通道）
- 形状：圆角矩形（建议圆角半径 15%）
- 背景：纯色或渐变，避免复杂图案

### 1.2 iOS 图标

| 用途 | 尺寸 (px) | 文件路径 |
|------|-----------|----------|
| App Store | 1024x1024 | `static/app-icon/ios/icon-1024.png` |
| iPhone App (@2x) | 120x120 | `static/app-icon/ios/icon-120.png` |
| iPhone App (@3x) | 180x180 | `static/app-icon/ios/icon-180.png` |
| iPad App | 76x76 | `static/app-icon/ios/icon-76.png` |
| iPad App (@2x) | 152x152 | `static/app-icon/ios/icon-152.png` |
| Settings (@2x) | 58x58 | `static/app-icon/ios/icon-58.png` |
| Settings (@3x) | 87x87 | `static/app-icon/ios/icon-87.png` |
| Spotlight (@2x) | 80x80 | `static/app-icon/ios/icon-80.png` |
| Spotlight (@3x) | 120x120 | `static/app-icon/ios/icon-120.png` |

**要求：**
- 格式：PNG（带透明通道）
- 形状：正方形（系统自动添加圆角）
- 背景：不建议透明，使用实心背景

### 1.3 小程序图标

| 平台 | 尺寸 (px) | 说明 |
|------|-----------|------|
| 微信 | 144x144 | 小程序 Logo |
| 支付宝 | 192x192 | 小程序 Logo |

---

## 二、启动图要求

### 2.1 Android 启动图

| 密度 | 尺寸 (px) | 文件路径 |
|------|-----------|----------|
| hdpi | 480x800 | `static/app-splash/android/splash-hdpi.png` |
| xhdpi | 720x1280 | `static/app-splash/android/splash-xhdpi.png` |
| xxhdpi | 1080x1920 | `static/app-splash/android/splash-xxhdpi.png` |
| xxxhdpi | 1440x2560 | `static/app-splash/android/splash-xxxhdpi.png` |

**横屏启动图（可选）：**
| 密度 | 尺寸 (px) | 文件路径 |
|------|-----------|----------|
| hdpi | 800x480 | `static/app-splash/android/splash-hdpi-land.png` |
| xhdpi | 1280x720 | `static/app-splash/android/splash-xhdpi-land.png` |

**要求：**
- 格式：PNG
- 内容：居中放置 Logo + 应用名称
- 背景：纯色或与品牌一致的渐变色

### 2.2 iOS 启动图

iOS 使用 LaunchScreen.storyboard，建议尺寸：

| 设备 | 尺寸 (px) | 文件路径 |
|------|-----------|----------|
| iPhone 6/7/8 Plus | 1242x2208 | `static/app-splash/ios/launch-1242x2208.png` |
| iPhone X/XS | 1125x2436 | `static/app-splash/ios/launch-1125x2436.png` |
| iPhone XR/XS Max | 828x1792 | `static/app-splash/ios/launch-828x1792.png` |
| iPhone 12/12 Pro | 1170x2532 | `static/app-splash/ios/launch-1170x2532.png` |
| iPhone 12 Pro Max | 1284x2778 | `static/app-splash/ios/launch-1284x2778.png` |
| iPad Pro | 2048x2732 | `static/app-splash/ios/launch-2048x2732.png` |

**要求：**
- 格式：PNG
- 内容：简洁，避免过多文字
- 背景：与 App 主题色一致

---

## 三、制作工具推荐

### 3.1 在线工具

| 工具 | 网址 | 功能 |
|------|------|------|
| App Icon Generator | https://appicon.co/ | 一键生成多尺寸图标 |
| Launcher Icon Generator | https://romannurik.github.io/AndroidAssetStudio/ | Android 图标生成 |
| Canva | https://www.canva.com/ | 在线设计工具 |
| Figma | https://www.figma.com/ | 专业 UI 设计工具 |

### 3.2 本地工具

| 工具 | 平台 | 说明 |
|------|------|------|
| Photoshop | Win/Mac | 专业图像处理 |
| Sketch | Mac | UI 设计工具 |
| GIMP | Win/Mac/Linux | 免费开源替代 |
| IconKit | Mac | 图标生成工具 |

---

## 四、配置 manifest.json

### 4.1 Android 配置

```json
"app-plus": {
  "distribute": {
    "android": {
      "icon": {
        "hdpi": "static/app-icon/android/icon-72.png",
        "xhdpi": "static/app-icon/android/icon-96.png",
        "xxhdpi": "static/app-icon/android/icon-144.png",
        "xxxhdpi": "static/app-icon/android/icon-192.png"
      },
      "splashscreen": {
        "hdpi": "static/app-splash/android/splash-hdpi.png",
        "xhdpi": "static/app-splash/android/splash-xhdpi.png",
        "xxhdpi": "static/app-splash/android/splash-xxhdpi.png",
        "xxxhdpi": "static/app-splash/android/splash-xxxhdpi.png"
      }
    }
  }
}
```

### 4.2 iOS 配置

```json
"app-plus": {
  "distribute": {
    "ios": {
      "icon": {
        "appstore": "static/app-icon/ios/icon-1024.png",
        "iphone": {
          "app@2x": "static/app-icon/ios/icon-120.png",
          "app@3x": "static/app-icon/ios/icon-180.png"
        },
        "ipad": {
          "app": "static/app-icon/ios/icon-76.png",
          "app@2x": "static/app-icon/ios/icon-152.png"
        }
      },
      "splashscreen": {
        "iphone": {
          "1125x2436": "static/app-splash/ios/launch-1125x2436.png",
          "1242x2208": "static/app-splash/ios/launch-1242x2208.png"
        }
      }
    }
  }
}
```

---

## 五、一键生成脚本

创建 `scripts/generate-icons.js`：

```javascript
const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const SOURCE = 'static/logo-1024.png'; // 源文件

const androidSizes = [72, 96, 144, 192];
const iosSizes = [
  { size: 1024, name: 'icon-1024.png' },
  { size: 120, name: 'icon-120.png' },
  { size: 180, name: 'icon-180.png' },
  { size: 76, name: 'icon-76.png' },
  { size: 152, name: 'icon-152.png' },
  { size: 58, name: 'icon-58.png' },
  { size: 87, name: 'icon-87.png' },
  { size: 80, name: 'icon-80.png' }
];

async function generateAndroidIcons() {
  console.log('正在生成 Android 图标...');
  
  for (const size of androidSizes) {
    const outputPath = `static/app-icon/android/icon-${size}.png`;
    await sharp(SOURCE)
      .resize(size, size)
      .toFile(outputPath);
    console.log(`✓ 生成：${outputPath}`);
  }
}

async function generateIosIcons() {
  console.log('正在生成 iOS 图标...');
  
  for (const { size, name } of iosSizes) {
    const outputPath = `static/app-icon/ios/${name}`;
    await sharp(SOURCE)
      .resize(size, size)
      .toFile(outputPath);
    console.log(`✓ 生成：${outputPath}`);
  }
}

async function main() {
  try {
    // 创建目录
    fs.mkdirSync('static/app-icon/android', { recursive: true });
    fs.mkdirSync('static/app-icon/ios', { recursive: true });
    
    await generateAndroidIcons();
    await generateIosIcons();
    
    console.log('\n✓ 图标生成完成！');
  } catch (error) {
    console.error('✗ 生成失败:', error.message);
  }
}

main();
```

**使用：**
```bash
npm install sharp
node scripts/generate-icons.js
```

---

## 六、品牌设计规范

### 6.1 主色调

| 颜色 | HEX | 用途 |
|------|-----|------|
| 主色 | #07C160 | 按钮、图标、强调 |
| 背景 | #EDEDED | 页面背景 |
| 文字 | #1A1A1A | 主要文字 |
| 次要文字 | #999999 | 辅助说明 |

### 6.2 Logo 使用规范

- **最小尺寸**: 32x32px
- **安全边距**: Logo 周围保留至少 1/4 高度的空白
- **背景**: 优先使用白色或品牌绿色背景
- **禁用**: 拉伸变形、添加效果、更改颜色

---

## 七、检查清单

打包前检查：

- [ ] Android 图标（4 种尺寸）
- [ ] iOS 图标（8 种尺寸）
- [ ] Android 启动图（4 种尺寸）
- [ ] iOS 启动图（6 种尺寸）
- [ ] manifest.json 中配置了正确的路径
- [ ] 图标文件为 PNG 格式
- [ ] 图标清晰无锯齿
- [ ] 启动图内容简洁美观

---

## 八、相关文档

- [APP 打包指南](./APP-BUILD-GUIDE.md)
- [安装包生成指南](../doc/安装包生成指南.md)
- [API 接口映射](../doc/APP-API-MAPPING.md)
