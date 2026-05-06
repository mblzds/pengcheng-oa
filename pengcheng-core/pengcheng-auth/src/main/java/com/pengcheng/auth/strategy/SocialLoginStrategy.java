package com.pengcheng.auth.strategy;

import com.pengcheng.auth.LoginHelper;
import com.pengcheng.auth.LoginRequest;
import com.pengcheng.auth.LoginResult;
import com.pengcheng.auth.LoginStrategy;
import com.pengcheng.auth.enums.ClientType;
import com.pengcheng.auth.enums.LoginType;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.social.SocialLoginFactory;
import com.pengcheng.social.SocialLoginService;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 三方授权登录策略（App/Web 通用）
 * 通过三方平台授权码换取用户信息，自动绑定或注册
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLoginStrategy implements LoginStrategy {

    private final SocialLoginFactory socialLoginFactory;
    private final SysUserService userService;
    private final LoginHelper loginHelper;

    @Override
    public LoginType getType() {
        return LoginType.SOCIAL;
    }

    @Override
    public ClientType[] supportedClients() {
        return new ClientType[]{ClientType.APP, ClientType.WEB};
    }

    @Override
    public LoginResult login(LoginRequest request) {
        String platform = request.getPlatform();
        String authCode = request.getAuthCode();

        if (platform == null || platform.isEmpty()) {
            throw new BusinessException("请指定三方登录平台");
        }
        if (authCode == null || authCode.isEmpty()) {
            throw new BusinessException("授权码不能为空");
        }

        // 1. 获取三方用户信息
        SocialLoginService socialService = socialLoginFactory.getService(platform);
        SocialLoginService.SocialUserInfo socialUser = socialService.getUserInfo(authCode);

        // 2. 通过 openId 查找已绑定用户
        SysUser user = userService.getByOpenId(socialUser.getOpenId());

        // 根据客户端类型确定用户类型
        String userType = request.getClientType() == ClientType.APP ? "app" : "pc";

        if (user == null) {
            // 自动注册
            String socialUsername = "social_" + platform + "_" + socialUser.getOpenId();
            user = new SysUser();
            user.setUsername(socialUsername);
            user.setNickname(socialUser.getNickname() != null ? socialUser.getNickname() : platform + "用户");
            user.setAvatar(socialUser.getAvatar());
            user.setPhone(socialUser.getPhone());
            user.setEmail(socialUser.getEmail());
            user.setGender(socialUser.getGender() != null ? socialUser.getGender() : 0);
            user.setPassword(""); // 三方登录无密码
            user.setStatus(1);
            user.setOpenId(socialUser.getOpenId());
            user.setUserType(userType);
            userService.save(user);
            log.info("三方登录自动注册: platform={}, openId={}, userType={}", platform, socialUser.getOpenId(), userType);
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("用户已被禁用");
        }

        return loginHelper.doLogin(user);
    }
}
