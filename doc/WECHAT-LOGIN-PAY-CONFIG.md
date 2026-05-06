# 微信小程序登录与支付配置指南

本文档详细说明如何配置微信小程序登录和支付功能。

---

## 一、前置准备

### 1.1 注册微信小程序

1. **访问官网**: https://mp.weixin.qq.com/
2. **注册账号**: 点击"立即注册" → 选择"小程序"
3. **填写信息**:
   - 邮箱（未注册过微信相关平台的）
   - 密码
   - 验证码
4. **主体信息**: 选择主体类型（企业/政府/其他组织）
5. **信息登记**: 填写统一社会信用代码、单位名称等
6. **管理员信息**: 填写管理员姓名、身份证号、手机号
7. **完成注册**: 缴纳认证费用（300 元/年）

### 1.2 获取必要凭证

登录小程序后台，获取以下信息：

| 凭证 | 获取路径 | 用途 |
|------|----------|------|
| **AppID** | 开发 → 开发管理 → 开发设置 | 小程序唯一标识 |
| **AppSecret** | 开发 → 开发管理 → 开发设置 | 接口调用凭证 |
| **服务器域名** | 开发 → 开发管理 → 开发设置 | 配置合法请求域名 |

---

## 二、微信支付商户号申请

### 2.1 申请流程

1. **访问商户平台**: https://pay.weixin.qq.com/
2. **注册商户号**: 点击"立即注册"
3. **选择账号类型**: 选择"微信小程序"
4. **填写基本信息**:
   - 商户简称（用于支付页面显示）
   - 营业执照信息
   - 法人身份证信息
   - 对公账户信息
5. **填写经营信息**:
   - 主营产品/服务
   - 经营场所
   - 年营业额
6. **结算信息**:
   - 结算银行账户
   - 结算周期（T+1/T+7）
7. **等待审核**: 1-5 个工作日
8. **签约**: 审核通过后在线签约

### 2.2 获取支付配置信息

登录微信支付商户平台，获取以下信息：

| 凭证 | 获取路径 | 说明 |
|------|----------|------|
| **商户号 (mchid)** | 账户中心 → 商户信息 | 10 位数字 |
| **API 密钥 (v2)** | 账户中心 → API 安全 → API 密钥 | 32 位字符串 |
| **API 密钥 (v3)** | 账户中心 → API 安全 → APIv3 密钥 | 32 位字符串 |
| **商户证书** | 账户中心 → API 安全 → 申请 API 证书 | 包含证书文件和私钥 |
| **证书序列号** | 证书详情页面 | 用于接口签名 |

### 2.3 配置支付目录和回调 URL

1. **产品中心** → **开发配置**
2. **支付目录**: 添加小程序的页面路径
3. **支付回调 URL**: 填写服务器接收支付结果的地址
   - 格式：`https://your-domain.com/api/pay/wechat/notify`
   - 必须是 HTTPS
   - 不能带参数

---

## 三、后端配置

### 3.1 配置微信小程序

登录管理后台，进入 **系统配置 → 微信小程序**:

```json
{
  "enabled": true,
  "appId": "wx_xxxxxxxxxxxxxxxx",
  "appSecret": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
}
```

**字段说明**:
- `enabled`: 是否启用小程序登录
- `appId`: 小程序 AppID（从微信后台获取）
- `appSecret`: 小程序 AppSecret（从微信后台获取）

### 3.2 配置微信支付

登录管理后台，进入 **系统配置 → 支付配置 → 微信支付**:

```json
{
  "enabled": true,
  "mchId": "1234567890",
  "appId": "wx_xxxxxxxxxxxxxxxx",
  "apiV3Key": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "privateKey": "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC...\n-----END PRIVATE KEY-----",
  "certSerialNo": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "notifyUrl": "https://your-domain.com/api/pay/wechat/notify"
}
```

**字段说明**:
- `enabled`: 是否启用微信支付
- `mchId`: 商户号（10 位数字）
- `appId`: 小程序 AppID（需与小程序一致）
- `apiV3Key`: APIv3 密钥（32 位字符串）
- `privateKey`: 商户私钥（从证书文件获取）
- `certSerialNo`: 证书序列号（从证书详情获取）
- `notifyUrl`: 支付结果回调地址

### 3.3 获取商户私钥

1. 登录微信支付商户平台
2. **账户中心** → **API 安全** → **申请 API 证书**
3. 下载证书工具
4. 运行工具生成证书和私钥
5. 打开 `apiclient_key.pem` 文件，复制全部内容（包含 BEGIN 和 END）

---

## 四、小程序端配置

### 4.1 修改 manifest.json

打开 `pengcheng-uniapp/manifest.json`，配置小程序信息：

```json
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
```

### 4.2 配置服务器域名

登录小程序后台，**开发** → **开发管理** → **开发设置** → **服务器域名**:

| 类型 | 配置 |
|------|------|
| **request 合法域名** | `https://your-domain.com` |
| **uploadFile 合法域名** | `https://your-domain.com` |
| **downloadFile 合法域名** | `https://your-domain.com` |
| **socket 合法域名** | `wss://your-domain.com` |

**注意**:
- 必须是 HTTPS/WSS
- 不能使用 IP 地址
- 每月可修改 5 次

### 4.3 修改 API 配置

打开 `pengcheng-uniapp/utils/config.js`:

```javascript
const DEFAULT_API_BASE_URL = 'https://your-domain.com'

// 或者在小程序中动态设置
export const setApiBaseUrl = (value) => {
  uni.setStorageSync('api_base_url', value)
}

// 初始化时设置
setApiBaseUrl('https://your-domain.com')
```

---

## 五、微信登录实现

### 5.1 前端登录流程

小程序端登录代码（已在 `pages/login/index.vue` 实现）：

```javascript
// 1. 调用微信登录获取 code
uni.login({
  provider: 'weixin',
  success: (loginRes) => {
    const code = loginRes.code
    
    // 2. 发送 code 到后端换取 token
    wxLogin({
      wxCode: code,
      loginType: 'MINIPROGRAM'
    }).then(res => {
      // 3. 保存 token
      uni.setStorageSync('token', res.data.token)
    })
  }
})
```

### 5.2 后端登录流程

后端自动处理流程（`MiniProgramLoginStrategy.java`）:

1. 接收小程序 code
2. 调用微信接口换取 openId
3. 查询用户是否存在
4. 不存在则自动注册
5. 返回 token

### 5.3 获取用户手机号（可选）

如需获取用户手机号，需额外步骤：

**前端代码**:
```html
<button open-type="getPhoneNumber" @getphonenumber="onGetPhone">
  手机号快捷登录
</button>
```

```javascript
onGetPhone(e) {
  if (e.detail.code) {
    // 发送 code 到后端
    wxLogin({
      wxCode: this.loginCode,
      phoneCode: e.detail.code,
      loginType: 'MINIPROGRAM'
    })
  }
}
```

**后端处理**:
```java
// MiniProgramLoginStrategy.java 已实现
if (request.getPhoneCode() != null) {
    String phoneNumber = wechatMiniProgramService.getPhoneNumber(
        request.getPhoneCode()
    );
    user.setPhone(phoneNumber);
    userService.updateById(user);
}
```

---

## 六、微信支付实现

### 6.1 前端支付流程

```javascript
// 1. 创建订单
const orderRes = await createPaymentOrder({
  amount: 0.01,  // 金额（元）
  description: '测试商品',
  payType: 'wechat'
})

// 2. 调用微信支付
uni.requestPayment({
  provider: 'wxpay',
  orderInfo: orderRes.data,  // 后端返回的支付参数
  success: (res) => {
    console.log('支付成功', res)
    // 3. 通知后端支付结果
    notifyPaymentSuccess(orderRes.data.orderNo)
  },
  fail: (err) => {
    console.error('支付失败', err)
  }
})
```

### 6.2 后端创建支付订单

```java
// WechatPayService.java 已实现
public Map<String, String> createMiniProgramOrder(
    String orderNo, 
    BigDecimal amount, 
    String description, 
    String openId
) {
    // 1. 构建微信支付请求
    // 2. 调用微信预支付接口
    // 3. 返回支付参数给前端
    return paymentParams;
}
```

### 6.3 支付回调处理

**配置回调地址**: `https://your-domain.com/api/pay/wechat/notify`

**后端处理**（需实现）:

```java
@RestController
@RequestMapping("/api/pay/wechat")
public class WechatPayNotifyController {
    
    @PostMapping("/notify")
    public String handleNotify(@RequestBody String notifyData,
                               @RequestHeader Map<String, String> headers) {
        // 1. 验证签名
        // 2. 解析支付结果
        // 3. 更新订单状态
        // 4. 返回成功响应
        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    }
}
```

---

## 七、测试验证

### 7.1 登录测试

1. **开发环境测试**:
   - 使用微信开发者工具
   - 编译运行小程序
   - 点击"微信授权登录"
   - 检查是否成功获取 token

2. **真机测试**:
   - 上传代码到微信后台
   - 设置为体验版
   - 扫码体验登录流程

### 7.2 支付测试

1. **沙箱环境**:
   - 微信支付提供沙箱环境
   - 使用测试商户号进行测试
   - 无需真实扣款

2. **真实支付测试**:
   - 创建 0.01 元测试订单
   - 完成支付
   - 检查回调是否成功
   - 验证订单状态更新

### 7.3 日志查看

后端日志位置：
```bash
# 查看登录日志
tail -f logs/app.log | grep "小程序登录"

# 查看支付日志
tail -f logs/app.log | grep "微信支付"
```

---

## 八、常见问题

### Q1: 小程序登录失败，提示"invalid appid"

**解决**:
1. 检查 AppID 是否正确
2. 检查 AppSecret 是否正确
3. 确认小程序已发布或设置为体验版

### Q2: 获取手机号失败

**解决**:
1. 确认小程序已认证（个人主体不支持）
2. 检查按钮是否添加 `open-type="getPhoneNumber"`
3. 确认用户已绑定手机号

### Q3: 支付失败，提示"签名错误"

**解决**:
1. 检查 APIv3 密钥是否正确
2. 检查商户私钥是否完整（包含 BEGIN 和 END）
3. 检查证书序列号是否正确

### Q4: 支付回调不触发

**解决**:
1. 检查回调 URL 是否配置正确
2. 确认服务器可外网访问
3. 检查防火墙设置
4. 查看微信支付商户平台的回调日志

### Q5: 域名未备案

**解决**:
1. 小程序要求服务器域名必须备案
2. 到工信部网站进行 ICP 备案
3. 备案完成后在微信后台配置

---

## 九、安全建议

### 9.1 密钥管理

- ✅ 将密钥存储在环境变量或配置中心
- ✅ 定期更换 API 密钥
- ✅ 不要将密钥提交到代码仓库
- ✅ 限制密钥使用范围

### 9.2 支付安全

- ✅ 验证回调签名
- ✅ 幂等处理回调（防止重复入账）
- ✅ 金额校验（防止篡改）
- ✅ 设置支付限额

### 9.3 登录安全

- ✅ Token 设置合理过期时间
- ✅ 敏感操作需二次验证
- ✅ 记录登录日志
- ✅ 异常登录检测

---

## 十、相关文档

- [小程序开发文档](https://developers.weixin.qq.com/miniprogram/dev/framework/)
- [微信支付文档](https://pay.weixin.qq.com/wiki/doc/apiv3/index.shtml)
- [APP-API 映射](./APP-API-MAPPING.md)
- [小程序部署指南](./MINIPROGRAM-DEPLOY.md)

---

**文档版本**: V1.0  
**最后更新**: 2026-03-12
