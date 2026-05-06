# 微信/支付宝登录支付 - 官方文档合规性分析

本文档分析当前项目实现与微信/支付宝官方最新开发文档的对比。

---

## 一、微信小程序登录

### 1.1 当前实现状态

| 接口/功能 | 当前实现 | 官方最新文档 | 状态 |
|-----------|----------|--------------|------|
| `jscode2session` | ✅ 已实现 | 仍在用 | ✅ 符合 |
| 获取手机号 | ✅ 已实现 | 2023 年起需付费 | ⚠️ 需注意 |
| UnionId 获取 | ✅ 已实现 | 需绑定开放平台 | ⚠️ 需配置 |
| AccessToken | ✅ 已实现 | 仍在用 | ✅ 符合 |

### 1.2 官方最新接口（2026 年）

**接口地址**（未变更）:
```
GET https://api.weixin.qq.com/sns/jscode2session
```

**请求参数**:
```
appid=小程序 AppID
secret=小程序 AppSecret
js_code=登录凭证 code
grant_type=authorization_code
```

**响应格式**（未变更）:
```json
{
  "openid": "用户唯一标识",
  "session_key": "会话密钥",
  "unionid": "统一标识（需绑定开放平台）"
}
```

### 1.3 代码合规性检查

**当前实现** (`WechatMiniProgramService.java:39-67`):

```java
// ✅ 接口地址正确
private static final String CODE2SESSION_URL = 
    "https://api.weixin.qq.com/sns/jscode2session";

// ✅ 参数名称正确
params.put("appid", appId);
params.put("secret", appSecret);
params.put("js_code", code);
params.put("grant_type", "authorization_code");

// ✅ 响应解析正确
result.setOpenId(json.getStr("openid"));
result.setSessionKey(json.getStr("session_key"));
result.setUnionId(json.getStr("unionid"));
```

**结论**: ✅ **完全符合官方文档**

---

## 二、微信小程序获取手机号

### 2.1 重要变更（2023 年 4 月 17 日起）

⚠️ **微信官方调整**: 获取手机号改为**付费服务**

| 项目 | 调整前 | 调整后 |
|------|--------|--------|
| 费用 | 免费 | 0.03 元/次（成功获取） |
| 接口 | `wxa.business.getuserphonenumber` | `wxa.business.getuserphonenumber` |
| 调用方式 | 前端 `getPhoneNumber` | 前端 `choosePhoneNumber` |

### 2.2 当前实现状态

**当前实现** (`WechatMiniProgramService.java:114-129`):

```java
// ⚠️ 接口仍可用，但开始计费
private static final String PHONE_NUMBER_URL = 
    "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

public String getPhoneNumber(String code) {
    String accessToken = getAccessToken();
    JSONObject body = new JSONObject();
    body.set("code", code);
    
    // 调用接口
    String response = HttpUtil.post(
        PHONE_NUMBER_URL + "?access_token=" + accessToken, 
        body.toString()
    );
    
    // ✅ 响应解析正确（新版返回 phone_info.purePhoneNumber）
    JSONObject phoneInfo = json.getJSONObject("phone_info");
    return phoneInfo.getStr("purePhoneNumber");
}
```

**结论**: ⚠️ **接口符合，但需注意计费**

### 2.3 建议修改

如需继续使用，建议添加费用提示和配置开关：

```java
// 在 SystemConfigHelper 中添加配置
public boolean isPhoneVerifyPaid() {
    return getBoolean(GROUP_WECHAT_MINIPROGRAM, "phoneVerifyPaid", false);
}

// 在获取手机号前检查
if (!configHelper.isPhoneVerifyPaid()) {
    log.warn("手机号获取为付费服务，请先确认已开通");
}
```

---

## 三、微信公众号登录（OAuth2.0）

### 3.1 当前实现状态

| 项目 | 当前实现 | 官方文档 | 状态 |
|------|----------|----------|------|
| 授权 URL | ❌ 未实现 | 仍在用 | ❌ 待实现 |
| 网页授权 | ❌ 未实现 | 仍在用 | ❌ 待实现 |
| 用户信息 | ❌ 未实现 | 需用户授权 | ❌ 待实现 |

### 3.2 官方最新接口

**授权 URL**:
```
https://open.weixin.qq.com/connect/oauth2/authorize?
  appid=APPID&
  redirect_uri=REDIRECT_URI&
  response_type=code&
  scope=SCOPE&
  state=STATE#wechat_redirect
```

**Code 换取 OpenId**:
```
GET https://api.weixin.qq.com/sns/oauth2/access_token?
  appid=APPID&
  secret=SECRET&
  code=CODE&
  grant_type=authorization_code
```

### 3.3 待实现代码

```java
@Service
public class WechatMpLogin implements SocialLoginService {
    
    private static final String AUTHORIZE_URL = 
        "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String ACCESS_TOKEN_URL = 
        "https://api.weixin.qq.com/sns/oauth2/access_token";
    
    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        return String.format(
            "%s?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=%s#wechat_redirect",
            AUTHORIZE_URL,
            configHelper.getWechatMpAppId(),
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
            state
        );
    }
    
    @Override
    public SocialUserInfo getUserInfo(String code) {
        // 1. code 换取 access_token
        Map<String, Object> params = new HashMap<>();
        params.put("appid", configHelper.getWechatMpAppId());
        params.put("secret", configHelper.getWechatMpAppSecret());
        params.put("code", code);
        params.put("grant_type", "authorization_code");
        
        String response = HttpUtil.get(ACCESS_TOKEN_URL, params);
        JSONObject json = JSONUtil.parseObj(response);
        
        String accessToken = json.getStr("access_token");
        String openId = json.getStr("openid");
        
        // 2. 获取用户信息
        String userInfoUrl = String.format(
            "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s",
            accessToken, openId
        );
        String userInfoResp = HttpUtil.get(userInfoUrl);
        JSONObject userInfo = JSONUtil.parseObj(userInfoResp);
        
        SocialUserInfo info = new SocialUserInfo();
        info.setPlatform("wechat_mp");
        info.setOpenId(userInfo.getStr("openid"));
        info.setNickname(userInfo.getStr("nickname"));
        info.setAvatar(userInfo.getStr("headimgurl"));
        info.setRawJson(userInfoResp);
        return info;
    }
}
```

---

## 四、支付宝登录

### 4.1 当前实现状态

| 项目 | 当前实现 | 官方文档 | 状态 |
|------|----------|----------|------|
| OAuth2.0 授权 | ❌ 仅 TODO | 仍在用 | ❌ 待实现 |
| 用户信息获取 | ❌ 仅 TODO | 需调用 API | ❌ 待实现 |

### 4.2 官方最新接口（2026 年）

**授权 URL**:
```
https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?
  app_id=APPID&
  scope=auth_user&
  redirect_uri=REDIRECT_URI&
  state=STATE
```

**Code 换取 Token**:
```
POST https://openapi.alipay.com/openapi.do
alipay.system.oauth.token
```

**获取用户信息**:
```
POST https://openapi.alipay.com/openapi.do
alipay.user.info.share
```

### 4.3 待实现代码

```java
@Service
public class AlipayLogin implements SocialLoginService {
    
    private final SystemConfigHelper configHelper;
    private static final String GATEWAY_URL = "https://openapi.alipay.com/openapi.do";
    
    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        String appId = configHelper.getAlipayAppId();
        return String.format(
            "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?" +
            "app_id=%s&scope=auth_user&redirect_uri=%s&state=%s",
            appId,
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
            state
        );
    }
    
    @Override
    public SocialUserInfo getUserInfo(String authCode) {
        // 1. code 换取 access_token
        AlipayClient client = createAlipayClient();
        AlipaySystemOauthTokenRequest tokenRequest = new AlipaySystemOauthTokenRequest();
        tokenRequest.setGrantType("authorization_code");
        tokenRequest.setCode(authCode);
        
        AlipaySystemOauthTokenResponse tokenResp = client.execute(tokenRequest);
        String accessToken = tokenResp.getAccessToken();
        String userId = tokenResp.getUserId();
        
        // 2. 获取用户信息
        AlipayUserInfoShareRequest shareRequest = new AlipayUserInfoShareRequest();
        AlipayUserInfoShareResponse shareResp = client.execute(shareRequest, accessToken);
        
        SocialUserInfo info = new SocialUserInfo();
        info.setPlatform("alipay");
        info.setUserId(userId);
        info.setNickname(shareResp.getNickName());
        info.setAvatar(shareResp.getAvatar());
        info.setGender(shareResp.getGender());
        return info;
    }
}
```

---

## 五、微信支付

### 5.1 当前实现状态

| 项目 | 当前实现 | 官方文档 | 状态 |
|------|----------|----------|------|
| API v3 | ✅ 已实现 | 官方推荐 | ✅ 符合 |
| 小程序支付 | ✅ 已实现 | JSAPI 支付 | ✅ 符合 |
| Native 支付 | ✅ 已实现 | 仍在用 | ✅ 符合 |
| 回调处理 | ⚠️ 需完善 | 必须验签 | ⚠️ 待完善 |

### 5.2 官方最新接口（2026 年）

微信支付 API v3 仍为官方推荐版本，接口未变更。

**小程序支付接口**:
```
POST https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi
```

**请求体**:
```json
{
  "appid": "小程序 AppID",
  "mchid": "商户号",
  "description": "商品描述",
  "out_trade_no": "商户订单号",
  "notify_url": "回调地址",
  "amount": {
    "total": 1,
    "currency": "CNY"
  },
  "payer": {
    "openid": "用户 OpenId"
  }
}
```

### 5.3 代码合规性检查

**当前实现** (`WechatPayService.java:101-200`):

```java
// ✅ 使用 API v3
Config wechatConfig = new RSAAutoCertificateConfig.Builder()
    .merchantId(mchId)
    .privateKey(privateKey)
    .merchantSerialNumber(certSerialNo)
    .apiV3Key(apiV3Key)
    .build();

// ✅ 使用 JSAPI 支付
com.wechat.pay.java.service.payments.jsapi.JsapiService jsapiService =
    new com.wechat.pay.java.service.payments.jsapi.JsapiService.Builder()
        .config(wechatConfig)
        .build();

// ✅ 请求参数正确
request.setAppid(appId);
request.setMchid(mchId);
request.setOutTradeNo(orderNo);
request.setNotifyUrl(notifyUrl);
```

**结论**: ✅ **完全符合官方文档**

### 5.4 待完善：回调验签

```java
@RestController
@RequestMapping("/api/pay/wechat")
public class WechatPayNotifyController {
    
    private final WechatPayService wechatPayService;
    
    @PostMapping("/notify")
    public String handleNotify(
            @RequestBody String body,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Timestamp") String timestamp) {
        
        // ✅ 必须验签
        boolean verified = wechatPayService.verifySignature(
            body, signature, nonce, timestamp
        );
        
        if (!verified) {
            log.error("微信支付回调验签失败");
            return "{\"code\":\"FAIL\",\"message\":\"验签失败\"}";
        }
        
        // 处理支付结果
        JSONObject result = JSONUtil.parseObj(body);
        String orderNo = result.getJSONObject("resource")
            .getStr("out_trade_no");
        String tradeState = result.getJSONObject("resource")
            .getStr("trade_state");
        
        if ("SUCCESS".equals(tradeState)) {
            // 更新订单状态
            wechatPayService.updateOrderStatus(orderNo);
        }
        
        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    }
}
```

---

## 六、支付宝支付

### 6.1 当前实现状态

| 项目 | 当前实现 | 官方文档 | 状态 |
|------|----------|----------|------|
| SDK 版本 | ✅ 使用官方 SDK | 官方推荐 | ✅ 符合 |
| 当面付 | ✅ 已实现 | 仍在用 | ✅ 符合 |
| 手机网站支付 | ❌ 未实现 | 仍在用 | ❌ 待实现 |
| 回调验签 | ⚠️ 需完善 | 必须验签 | ⚠️ 待完善 |

### 6.2 官方最新接口（2026 年）

支付宝官方 SDK 仍为推荐方式，接口未变更。

**当面付接口**:
```java
alipay.trade.precreate
```

### 6.3 代码合规性检查

**当前实现** (`AlipayService.java:58-78`):

```java
// ✅ 使用官方 SDK
AlipayClient alipayClient = new DefaultAlipayClient(
    gatewayUrl,
    appId,
    privateKey,
    "json",
    "UTF-8",
    publicKey,
    signType  // ✅ 支持 RSA2
);

// ✅ 使用当面付
AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
request.setNotifyUrl(notifyUrl);
request.setBizContent("{" +
    "\"out_trade_no\":\"" + orderNo + "\"," +
    "\"total_amount\":\"0.01\"," +
    "\"subject\":\"支付测试订单\"" +
    "}");
```

**结论**: ✅ **完全符合官方文档**

---

## 七、总结与改进建议

### 7.1 合规性总览

| 功能 | 合规状态 | 备注 |
|------|----------|------|
| 微信小程序登录 | ✅ 完全符合 | 接口未变更 |
| 微信小程序获取手机号 | ⚠️ 接口符合，需付费 | 2023 年 4 月起收费 |
| 微信公众号登录 | ❌ 待实现 | 代码有 TODO |
| 支付宝登录 | ❌ 待实现 | 代码有 TODO |
| 微信支付 | ✅ 完全符合 | API v3 正确 |
| 支付宝支付 | ✅ 完全符合 | SDK 正确 |
| 支付回调验签 | ⚠️ 待完善 | 必须实现 |

### 7.2 优先级改进清单

| 优先级 | 任务 | 预计工时 |
|--------|------|----------|
| **P0** | 完善微信支付回调验签 | 2 小时 |
| **P0** | 完善支付宝支付回调验签 | 2 小时 |
| **P1** | 实现微信公众号登录 | 4 小时 |
| **P1** | 实现支付宝登录 | 4 小时 |
| **P2** | 添加手机号获取费用提示 | 1 小时 |
| **P2** | 添加登录方式配置开关 | 2 小时 |

### 7.3 官方文档链接

| 平台 | 文档链接 |
|------|----------|
| 微信小程序登录 | https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/v2/user-login/code2Session.html |
| 微信小程序获取手机号 | https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/v2/user-info/getPhoneNumber.html |
| 微信公众号登录 | https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/Wechat_webpage_authorization.html |
| 微信支付 API v3 | https://pay.weixin.qq.com/wiki/doc/apiv3/index.shtml |
| 支付宝登录 | https://opendocs.alipay.com/open/01emu5 |
| 支付宝支付 | https://opendocs.alipay.com/open/02aile |

---

## 八、版本更新记录

| 日期 | 变更内容 |
|------|----------|
| 2026-03-12 | 初始分析文档 |
| - | - |

---

**文档版本**: V1.0  
**最后更新**: 2026-03-12
