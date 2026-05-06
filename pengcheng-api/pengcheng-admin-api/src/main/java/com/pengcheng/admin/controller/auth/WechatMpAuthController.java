package com.pengcheng.admin.controller.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.auth.LoginHelper;
import com.pengcheng.auth.LoginResult;
import com.pengcheng.common.feature.FeatureFlags;
import com.pengcheng.common.result.Result;
import com.pengcheng.social.SocialLoginService;
import com.pengcheng.social.impl.WechatMpLogin;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信公众号授权登录控制器（迁自 temp-disabled）。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/wechat-mp")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = FeatureFlags.WECHAT_MP_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")
public class WechatMpAuthController {

    private final WechatMpLogin wechatMpLogin;
    private final SysUserService userService;
    private final LoginHelper loginHelper;

    @GetMapping("/authorize-url")
    public Result<Map<String, String>> getAuthorizeUrl(
            @RequestParam String redirectUri,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, defaultValue = "snsapi_userinfo") String scope) {
        Map<String, String> result = new HashMap<>();
        result.put("authorizeUrl", wechatMpLogin.getAuthorizeUrl(redirectUri, state, scope));
        return Result.ok(result);
    }

    @GetMapping("/callback")
    public Result<LoginResult> handleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        log.info("[微信MP] 授权回调 code={}, state={}", code, state);
        try {
            SocialLoginService.SocialUserInfo userInfo = wechatMpLogin.getUserInfo(code);
            SysUser user = userService.getByOpenId(userInfo.getOpenId());
            if (user == null) {
                user = autoRegister(userInfo);
                log.info("[微信MP] 新用户自动注册 openId={}", userInfo.getOpenId());
            }
            if (user.getStatus() != 1) {
                return Result.fail("账号已被禁用");
            }
            updateUserFromWechat(user, userInfo);
            return Result.ok(loginHelper.doLogin(user));
        } catch (Exception e) {
            log.error("[微信MP] 登录失败", e);
            return Result.fail("登录失败：" + e.getMessage());
        }
    }

    private SysUser autoRegister(SocialLoginService.SocialUserInfo userInfo) {
        SysUser user = new SysUser();
        String openId = userInfo.getOpenId();
        user.setUsername("mp_" + openId.substring(0, Math.min(openId.length(), 10)));
        user.setNickname(userInfo.getNickname() != null ? userInfo.getNickname() : "微信用户");
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("123456"));
        user.setOpenId(openId);
        // unionId 字段已从 SysUser 中移除；如需支持多端统一标识请另建 sys_user_social 关联表
        user.setStatus(1);
        user.setGender(userInfo.getGender() != null ? userInfo.getGender() : 0);
        user.setUserType("mp");
        user.setAvatar(userInfo.getAvatar());
        userService.save(user);
        return user;
    }

    private void updateUserFromWechat(SysUser user, SocialLoginService.SocialUserInfo userInfo) {
        boolean changed = false;
        if (userInfo.getNickname() != null && !userInfo.getNickname().equals(user.getNickname())) {
            user.setNickname(userInfo.getNickname());
            changed = true;
        }
        if (userInfo.getAvatar() != null && !userInfo.getAvatar().equals(user.getAvatar())) {
            user.setAvatar(userInfo.getAvatar());
            changed = true;
        }
        if (userInfo.getGender() != null && !userInfo.getGender().equals(user.getGender())) {
            user.setGender(userInfo.getGender());
            changed = true;
        }
        if (changed) {
            userService.updateById(user);
        }
    }

    @GetMapping("/check")
    public Result<Map<String, Object>> checkLoginStatus() {
        Map<String, Object> result = new HashMap<>();
        if (StpUtil.isLogin()) {
            result.put("isLogin", true);
            result.put("userId", StpUtil.getLoginId());
        } else {
            result.put("isLogin", false);
        }
        return Result.ok(result);
    }

    @GetMapping("/user")
    public Result<SysUser> getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setPassword(null);
        return Result.ok(user);
    }
}
