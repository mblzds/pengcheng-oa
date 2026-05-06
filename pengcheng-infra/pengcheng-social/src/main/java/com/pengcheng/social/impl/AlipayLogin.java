package com.pengcheng.social.impl;

import com.pengcheng.common.feature.FeatureFlags;
import com.pengcheng.social.SocialLoginService;
import com.pengcheng.system.helper.SystemConfigHelper;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = FeatureFlags.ALIPAY_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")
public class AlipayLogin implements SocialLoginService {
    private final SystemConfigHelper configHelper;
    private static final String GATEWAY_URL = "https://openapi.alipay.com/gateway.do";
    private static final String AUTHORIZE_URL = "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm";

    @Override
    public String getPlatform() { return "alipay"; }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state, String scope) {
        String appId = configHelper.getAlipayAppId();
        if (appId == null || appId.isEmpty()) throw new RuntimeException("支付宝 AppID 未配置");
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return String.format("%s?app_id=%s&scope=%s&redirect_uri=%s&state=%s",
            AUTHORIZE_URL, appId, scope != null ? scope : "auth_user", encodedRedirectUri, state != null ? state : "STATE");
    }

    @Override
    public SocialUserInfo getUserInfo(String authCode) {
        try {
            AlipayClient client = createAlipayClient();
            AlipaySystemOauthTokenRequest tokenRequest = new AlipaySystemOauthTokenRequest();
            tokenRequest.setGrantType("authorization_code");
            tokenRequest.setCode(authCode);
            AlipaySystemOauthTokenResponse tokenResponse = client.execute(tokenRequest);
            if (!tokenResponse.isSuccess()) throw new RuntimeException("支付宝登录失败：" + tokenResponse.getSubMsg());
            
            SocialUserInfo info = new SocialUserInfo();
            info.setPlatform(getPlatform());
            info.setOpenId(tokenResponse.getUserId());
            
            AlipayUserInfoShareRequest shareRequest = new AlipayUserInfoShareRequest();
            AlipayUserInfoShareResponse shareResponse = client.execute(shareRequest, tokenResponse.getAccessToken());
            if (shareResponse.isSuccess()) {
                info.setNickname(shareResponse.getNickName());
                info.setAvatar(shareResponse.getAvatar());
                info.setGender(parseGender(shareResponse.getGender()));
            }
            return info;
        } catch (AlipayApiException e) {
            throw new RuntimeException("支付宝登录失败：" + e.getErrMsg());
        }
    }

    private AlipayClient createAlipayClient() {
        String appId = configHelper.getAlipayAppId();
        String privateKey = configHelper.getAlipayPrivateKey();
        String publicKey = configHelper.getAlipayPublicKey();
        if (appId == null || appId.isEmpty() || privateKey == null || privateKey.isEmpty())
            throw new RuntimeException("支付宝配置未完成");
        return new DefaultAlipayClient(GATEWAY_URL, appId, privateKey, "json", "UTF-8", publicKey, "RSA2");
    }

    private Integer parseGender(String gender) {
        if (gender == null) return 0;
        return "M".equals(gender) ? 1 : ("F".equals(gender) ? 2 : 0);
    }
}
