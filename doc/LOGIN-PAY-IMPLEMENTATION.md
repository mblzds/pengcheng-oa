# 微信/支付宝登录支付实现完成报告

**完成日期**: 2026-03-12  
**版本**: V1.0

---

## 一、实现内容总览

### 1.1 已完成功能

| 功能模块 | 状态 | 文件位置 |
|----------|------|----------|
| 微信支付回调验签 | ✅ 完成 | `pengcheng-pay/WechatPayVerifyService.java` |
| 微信支付回调控制器 | ✅ 完成 | `WechatPayNotifyController.java` |
| 支付宝支付回调验签 | ✅ 完成 | `pengcheng-pay/AlipayVerifyService.java` |
| 支付宝支付回调控制器 | ✅ 完成 | `AlipayNotifyController.java` |
| 微信公众号登录 | ✅ 完成 | `pengcheng-social/WechatMpLogin.java` |
| 微信公众号登录控制器 | ✅ 完成 | `WechatMpAuthController.java` |
| 支付宝登录 | ✅ 完成 | `pengcheng-social/AlipayLogin.java` |
| 手机号获取费用提示 | ✅ 完成 | `SystemConfigHelper.java` |

### 1.2 新增文件清单

```
pengcheng-admin/
├── pengcheng-infra/
│   ├── pengcheng-pay/
│   │   ├── WechatPayVerifyService.java        # 微信支付验签服务
│   │   └── AlipayVerifyService.java           # 支付宝验签服务
│   └── pengcheng-social/
│       ├── WechatMpLogin.java                 # 微信公众号登录服务
│       └── AlipayLogin.java                   # 支付宝登录服务
├── pengcheng-api/
│   └── pengcheng-admin-api/
│       └── controller/
│           ├── WechatPayNotifyController.java # 微信支付回调
│           ├── AlipayNotifyController.java    # 支付宝支付回调
│           └── WechatMpAuthController.java    # 微信公众号登录
└── doc/
    ├── LOGIN-PAY-OFFICIAL-COMPLIANCE.md       # 官方文档合规性分析
    └── LOGIN-PAY-IMPLEMENTATION.md            # 本文档
```

---

## 二、功能详细说明

### 2.1 微信支付回调验签

**文件**: `WechatPayVerifyService.java`

**功能**:
- ✅ 验证微信支付回调签名（SHA256withRSA）
- ✅ 验证时间戳（防止重放攻击，5 分钟有效期）
- ✅ 解密回调数据（AES-256-GCM）
- ✅ 解析回调内容（订单号、交易状态、金额等）

**使用示例**:
```java
// 控制器自动处理
@PostMapping("/api/pay/wechat/notify")
public String handleNotify(
        @RequestBody String body,
        @RequestHeader("Wechatpay-Signature") String signature,
        @RequestHeader("Wechatpay-Nonce") String nonce,
        @RequestHeader("Wechatpay-Timestamp") String timestamp,
        @RequestHeader("Wechatpay-Serial") String serialNo) {
    
    // 1. 验证时间戳
    if (!verifyService.verifyTimestamp(timestamp)) {
        return "{\"code\":\"FAIL\",\"message\":\"时间戳验证失败\"}";
    }
    
    // 2. 验证签名
    if (!verifyService.verifySignature(body, signature, nonce, timestamp, serialNo)) {
        return "{\"code\":\"FAIL\",\"message\":\"签名验证失败\"}";
    }
    
    // 3. 处理支付结果
    WechatPayNotifyResource resource = verifyService.parseNotifyBody(body);
    paymentService.updatePayStatus(resource.getOutTradeNo(), ...);
    
    return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
}
```

**配置要求**:
```json
// 系统配置 → 支付配置 → 微信支付
{
  "enabled": true,
  "mchId": "1234567890",
  "apiV3Key": "32 位密钥",
  "privateKey": "商户私钥",
  "certSerialNo": "证书序列号",
  "notifyUrl": "https://your-domain.com/api/pay/wechat/notify"
}
```

---

### 2.2 支付宝支付回调验签

**文件**: `AlipayVerifyService.java`

**功能**:
- ✅ 验证支付宝回调签名（RSA2）
- ✅ 验证参数完整性
- ✅ 解析回调结果
- ✅ 交易状态验证
- ✅ 幂等性检查（防止重复处理）

**使用示例**:
```java
@PostMapping("/api/pay/alipay/notify")
public String handleNotify(@RequestParam Map<String, String> params) {
    // 1. 验证参数完整性
    if (!verifyService.verifyParams(params)) {
        return "failure";
    }
    
    // 2. 验证签名
    if (!verifyService.verifySignature(params)) {
        return "failure";
    }
    
    // 3. 解析回调结果
    AlipayNotifyResult result = verifyService.parseNotifyResult(params);
    
    // 4. 处理支付结果
    if (verifyService.isValidTradeStatus(result.getTradeStatus())) {
        paymentService.updatePayStatus(result.getOutTradeNo(), ...);
    }
    
    return "success";
}
```

**配置要求**:
```json
// 系统配置 → 支付配置 → 支付宝
{
  "enabled": true,
  "appId": "支付宝 AppID",
  "privateKey": "应用私钥",
  "publicKey": "支付宝公钥",
  "notifyUrl": "https://your-domain.com/api/pay/alipay/notify"
}
```

---

### 2.3 微信公众号登录

**文件**: `WechatMpLogin.java` + `WechatMpAuthController.java`

**功能**:
- ✅ 获取授权 URL（支持 snsapi_base 和 snsapi_userinfo）
- ✅ Code 换取 Access Token
- ✅ 获取用户信息（昵称、头像、性别等）
- ✅ Token 刷新
- ✅ 自动注册/登录

**使用示例**:

**前端跳转授权**:
```javascript
// 1. 获取授权 URL
const { data } = await get('/api/auth/wechat-mp/authorize-url', {
  redirectUri: 'https://your-domain.com/callback',
  scope: 'snsapi_userinfo'
})

// 2. 跳转到微信授权页面
window.location.href = data.authorizeUrl
```

**后端回调处理**:
```java
@GetMapping("/api/auth/wechat-mp/callback")
public Result<LoginResult> handleCallback(
        @RequestParam String code,
        @RequestParam String state) {
    
    // 1. 使用 code 换取用户信息
    SocialUserInfo userInfo = wechatMpLogin.getUserInfo(code);
    
    // 2. 查找或创建用户
    SysUser user = userService.getByOpenId(userInfo.getOpenId());
    if (user == null) {
        user = autoRegister(userInfo);
    }
    
    // 3. 执行登录
    LoginResult result = loginHelper.doLogin(user);
    return Result.ok(result);
}
```

**配置要求**:
```json
// 系统配置 → 微信公众号
{
  "enabled": true,
  "appId": "公众号 AppID",
  "appSecret": "公众号 AppSecret",
  "oauthRedirectUrl": "https://your-domain.com/api/auth/wechat-mp/callback"
}
```

---

### 2.4 支付宝登录

**文件**: `AlipayLogin.java`

**功能**:
- ✅ 获取授权 URL
- ✅ 授权码换取 Access Token
- ✅ 获取用户信息
- ✅ Token 刷新
- ✅ 性别解析（F/M 转 0/1/2）

**使用示例**:
```java
// 1. 获取授权 URL
String authorizeUrl = alipayLogin.getAuthorizeUrl(
    "https://your-domain.com/callback",
    "STATE"
);

// 2. 回调处理
@GetMapping("/auth/alipay/callback")
public Result<LoginResult> callback(@RequestParam String authCode) {
    SocialUserInfo userInfo = alipayLogin.getUserInfo(authCode);
    // 处理登录...
}
```

**配置要求**:
```json
// 系统配置 → 支付宝配置
{
  "enabled": true,
  "appId": "支付宝 AppID",
  "privateKey": "应用私钥",
  "publicKey": "支付宝公钥"
}
```

---

### 2.5 手机号获取费用提示

**文件**: `SystemConfigHelper.java` + `MiniProgramLoginStrategy.java`

**功能**:
- ✅ 检测是否已确认为付费服务
- ✅ 输出费用提示日志
- ✅ 可配置费用提示文案

**配置**:
```json
// 系统配置 → 微信小程序
{
  "enabled": true,
  "appId": "wx_xxxxxx",
  "appSecret": "xxxxxx",
  "phoneVerifyPaid": false,  // 是否已确认为付费服务
  "phoneVerifyFeeNotice": "温馨提示：根据微信官方政策，获取手机号服务为付费服务（0.03 元/次）"
}
```

**日志输出**:
```
WARN  获取手机号失败：温馨提示：根据微信官方政策，获取手机号服务为付费服务（0.03 元/次），请确认已开通并了解相关费用。
```

---

## 三、API 接口清单

### 3.1 微信支付接口

| 接口 | Method | 路径 | 说明 |
|------|--------|------|------|
| 支付回调 | POST | `/api/pay/wechat/notify` | 微信支付结果通知 |
| 查询订单 | GET | `/api/pay/wechat/order/{orderNo}` | 查询订单状态 |
| 申请退款 | POST | `/api/pay/wechat/refund` | 申请退款 |

### 3.2 支付宝支付接口

| 接口 | Method | 路径 | 说明 |
|------|--------|------|------|
| 支付回调 | POST | `/api/pay/alipay/notify` | 支付宝支付结果通知 |
| 查询订单 | GET | `/api/pay/alipay/order/{orderNo}` | 查询订单状态 |
| 申请退款 | POST | `/api/pay/alipay/refund` | 申请退款 |
| 验签测试 | POST | `/api/pay/alipay/verify` | 手动验证签名（调试用） |

### 3.3 微信登录接口

| 接口 | Method | 路径 | 说明 |
|------|--------|------|------|
| 获取授权 URL | GET | `/api/auth/wechat-mp/authorize-url` | 获取微信授权 URL |
| 授权回调 | GET | `/api/auth/wechat-mp/callback` | 微信授权回调处理 |
| 检查登录状态 | GET | `/api/auth/wechat-mp/check` | 检查登录状态 |
| 获取当前用户 | GET | `/api/auth/wechat-mp/user` | 获取当前用户信息 |

---

## 四、配置说明

### 4.1 微信小程序配置

登录管理后台，进入 **系统配置 → 微信小程序**:

```json
{
  "enabled": true,
  "appId": "wx_xxxxxxxxxxxxxxxx",
  "appSecret": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "phoneVerifyPaid": false
}
```

### 4.2 微信公众号配置

登录管理后台，进入 **系统配置 → 微信公众号**:

```json
{
  "enabled": true,
  "appId": "xxxxxxxxxxxxxx",
  "appSecret": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "oauthRedirectUrl": "https://your-domain.com/api/auth/wechat-mp/callback"
}
```

### 4.3 微信支付配置

登录管理后台，进入 **系统配置 → 支付配置 → 微信支付**:

```json
{
  "enabled": true,
  "mchId": "1234567890",
  "appId": "wx_xxxxxxxxxxxxxxxx",
  "apiV3Key": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "privateKey": "-----BEGIN PRIVATE KEY-----\n...",
  "certSerialNo": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "notifyUrl": "https://your-domain.com/api/pay/wechat/notify"
}
```

### 4.4 支付宝配置

登录管理后台，进入 **系统配置 → 支付配置 → 支付宝**:

```json
{
  "enabled": true,
  "appId": "xxxxxxxxxxxxxx",
  "privateKey": "-----BEGIN RSA PRIVATE KEY-----\n...",
  "publicKey": "-----BEGIN PUBLIC KEY-----\n...",
  "notifyUrl": "https://your-domain.com/api/pay/alipay/notify"
}
```

---

## 五、测试指南

### 5.1 微信支付测试

```bash
# 1. 创建测试订单
curl -X POST http://localhost:8080/api/pay/wechat/create-test-order \
  -H "Authorization: Bearer <token>"

# 2. 使用微信扫码支付

# 3. 查看回调日志
tail -f logs/app.log | grep "微信支付回调"
```

### 5.2 支付宝支付测试

```bash
# 1. 创建测试订单
curl -X POST http://localhost:8080/api/pay/alipay/create-test-order \
  -H "Authorization: Bearer <token>"

# 2. 使用支付宝扫码支付

# 3. 查看回调日志
tail -f logs/app.log | grep "支付宝回调"
```

### 5.3 微信登录测试

```bash
# 1. 获取授权 URL
curl "http://localhost:8080/api/auth/wechat-mp/authorize-url?redirect_uri=http://localhost:3000/callback"

# 2. 在微信中打开授权 URL

# 3. 授权后跳转回调地址，携带 code 参数
```

---

## 六、安全注意事项

### 6.1 密钥管理

- ✅ 所有密钥存储在系统配置中，不硬编码
- ✅ 支持从环境变量读取敏感配置
- ⚠️ 建议生产环境使用配置中心（如 Nacos、Apollo）

### 6.2 回调安全

- ✅ 微信支付回调验签（SHA256withRSA）
- ✅ 支付宝回调验签（RSA2）
- ✅ 时间戳验证（防止重放攻击）
- ✅ 幂等性检查（防止重复处理）

### 6.3 日志脱敏

- ⚠️ 待完善：支付回调日志中的敏感信息脱敏
- ⚠️ 待完善：用户信息日志脱敏

---

## 七、常见问题

### Q1: 微信支付回调验签失败

**可能原因**:
1. 证书序列号配置错误
2. APIv3 密钥配置错误
3. 商户私钥格式不正确

**解决方案**:
```bash
# 检查证书序列号（去掉冒号和空格）
openssl x509 -in apiclient_cert.pem -noout -serial

# 检查私钥格式（应包含 BEGIN 和 END）
cat apiclient_key.pem
```

### Q2: 支付宝回调验签失败

**可能原因**:
1. 支付宝公钥配置错误
2. 签名类型配置错误（应为 RSA2）

**解决方案**:
1. 登录支付宝开放平台重新生成密钥
2. 确保配置的是**支付宝公钥**，不是应用公钥

### Q3: 微信公众号登录获取不到用户信息

**可能原因**:
1. 使用了 snsapi_base 静默授权（只返回 openid）
2. 公众号未认证（未认证公众号无法获取用户信息）

**解决方案**:
1. 使用 snsapi_userinfo 授权模式
2. 完成公众号认证

---

## 八、后续优化建议

| 优先级 | 优化项 | 说明 |
|--------|--------|------|
| P1 | 添加支付结果通知 | 支付成功后推送消息通知用户 |
| P1 | 完善退款流程 | 退款审批流程、退款回调处理 |
| P2 | 日志脱敏处理 | 敏感信息（手机号、身份证）脱敏 |
| P2 | 添加对账功能 | 每日自动对账，发现异常订单 |
| P3 | 支付风控 | 大额支付二次验证、异常交易检测 |
| P3 | 多渠道支付 | 支持 QQ 支付、银联支付等 |

---

## 九、相关文档

- [官方文档合规性分析](./LOGIN-PAY-OFFICIAL-COMPLIANCE.md)
- [微信登录支付配置](./WECHAT-LOGIN-PAY-CONFIG.md)
- [小程序部署指南](./MINIPROGRAM-DEPLOY.md)
- [配置模板大全](./DEPLOY-CONFIG-TEMPLATES.md)

---

**完成时间**: 2026-03-12  
**负责人**: 开发团队  
**审核状态**: 待审核
