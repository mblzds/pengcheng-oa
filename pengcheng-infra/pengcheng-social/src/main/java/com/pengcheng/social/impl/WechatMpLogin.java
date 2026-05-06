package com.pengcheng.social.impl;

import com.pengcheng.common.feature.FeatureFlags;
import com.pengcheng.social.SocialLoginService;
import com.pengcheng.system.helper.SystemConfigHelper;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信公众号 OAuth2.0 网页授权登录
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = FeatureFlags.WECHAT_MP_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")
public class WechatMpLogin implements SocialLoginService {

    private final SystemConfigHelper configHelper;

    private static final String AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo";
    private static final String REFRESH_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/refresh_token";

    @Override
    public String getPlatform() {
        return "wechat_mp";
    }

    /**
     * 获取微信授权 URL
     * 
     * @param redirectUri 回调地址（需urlencode）
     * @param state 状态参数，用于防止 CSRF
     * @param scope 授权作用域（snsapi_base 静默授权 / snsapi_userinfo 需用户确认）
     * @return 授权 URL
     */
    @Override
    public String getAuthorizeUrl(String redirectUri, String state, String scope) {
        String appId = configHelper.getWechatMpAppId();
        
        if (appId == null || appId.isEmpty()) {
            throw new RuntimeException("微信公众号 AppID 未配置");
        }

        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String useScope = scope != null ? scope : "snsapi_userinfo";

        // 构建授权 URL
        return String.format(
            "%s?appid=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s#wechat_redirect",
            AUTHORIZE_URL,
            appId,
            encodedRedirectUri,
            useScope,
            state != null ? state : "STATE"
        );
    }

    /**
     * 兼容无 scope 参数的调用
     */
    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        return getAuthorizeUrl(redirectUri, state, "snsapi_userinfo");
    }

    /**
     * 使用 code 换取用户信息
     * 
     * @param code 授权码
     * @return 用户信息
     */
    @Override
    public SocialUserInfo getUserInfo(String code) {
        String appId = configHelper.getWechatMpAppId();
        String appSecret = configHelper.getWechatMpAppSecret();

        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            throw new RuntimeException("微信公众号配置未完成");
        }

        // 1. code 换取 access_token
        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", appSecret);
        params.put("code", code);
        params.put("grant_type", "authorization_code");

        String tokenResponse = HttpUtil.get(ACCESS_TOKEN_URL, params);
        JSONObject tokenJson = JSONUtil.parseObj(tokenResponse);

        if (tokenJson.containsKey("errcode") && tokenJson.getInt("errcode") != 0) {
            log.error("微信授权码换取 access_token 失败：{}", tokenResponse);
            throw new RuntimeException("微信登录失败：" + tokenJson.getStr("errmsg"));
        }

        String accessToken = tokenJson.getStr("access_token");
        String openId = tokenJson.getStr("openid");
        int expiresIn = tokenJson.getInt("expires_in");

        log.info("微信 access_token 换取成功，openId={}, expiresIn={}s", openId, expiresIn);

        // 2. 获取用户信息（snsapi_userinfo 模式）
        SocialUserInfo info = new SocialUserInfo();
        info.setPlatform(getPlatform());
        info.setOpenId(openId);
        info.setRawJson(tokenResponse);

        // 拉取用户信息（需要用户授权）
        Map<String, Object> userInfoParams = new HashMap<>();
        userInfoParams.put("access_token", accessToken);
        userInfoParams.put("openid", openId);
        userInfoParams.put("lang", "zh_CN");

        String userInfoResponse = HttpUtil.get(USER_INFO_URL, userInfoParams);
        JSONObject userInfoJson = JSONUtil.parseObj(userInfoResponse);

        if (userInfoJson.containsKey("errcode") && userInfoJson.getInt("errcode") != 0) {
            log.warn("微信获取用户信息失败：{}，可能为静默授权模式", userInfoResponse);
            // 静默授权（snsapi_base）只返回 openid，不需要获取用户信息
            return info;
        }

        // 解析用户信息
        info.setNickname(userInfoJson.getStr("nickname"));
        info.setGender(userInfoJson.getInt("sex", 0));
        info.setProvince(userInfoJson.getStr("province"));
        info.setCity(userInfoJson.getStr("city"));
        info.setCountry(userInfoJson.getStr("country"));
        info.setAvatar(userInfoJson.getStr("headimgurl"));
        info.setPrivilege(userInfoJson.getJSONArray("privilege"));
        info.setUnionId(userInfoJson.getStr("unionid"));

        log.info("微信用户信息获取成功：nickname={}", info.getNickname());

        return info;
    }

    /**
     * 刷新 access_token
     * 
     * @param refreshToken 刷新令牌
     * @return 新的 access_token
     */
    public String refreshAccessToken(String refreshToken) {
        String appId = configHelper.getWechatMpAppId();

        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);

        String response = HttpUtil.get(REFRESH_TOKEN_URL, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            log.error("刷新 access_token 失败：{}", response);
            throw new RuntimeException("刷新 token 失败：" + json.getStr("errmsg"));
        }

        return json.getStr("access_token");
    }

    /**
     * 检验授权凭证（access_token）是否有效
     */
    public boolean checkAccessToken(String accessToken, String openId) {
        Map<String, Object> params = new HashMap<>();
        params.put("access_token", accessToken);
        params.put("openid", openId);

        String response = HttpUtil.get(
            "https://api.weixin.qq.com/sns/auth", params
        );
        JSONObject json = JSONUtil.parseObj(response);

        return json.containsKey("errcode") && json.getInt("errcode") == 0;
    }
}
