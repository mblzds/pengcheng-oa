package com.pengcheng.auth.strategy;

import cn.hutool.crypto.digest.BCrypt;
import com.pengcheng.auth.LoginHelper;
import com.pengcheng.auth.LoginRequest;
import com.pengcheng.auth.LoginResult;
import com.pengcheng.auth.LoginStrategy;
import com.pengcheng.auth.enums.ClientType;
import com.pengcheng.auth.enums.LoginType;
import com.pengcheng.common.feature.FeatureFlags;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.helper.SystemConfigHelper;
import com.pengcheng.system.service.SysUserService;
import com.pengcheng.wechat.WechatMiniProgramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = FeatureFlags.WECHAT_MINI_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")
public class MiniProgramLoginStrategy implements LoginStrategy {

    private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();

    private final WechatMiniProgramService wechatMiniProgramService;
    private final SysUserService userService;
    private final LoginHelper loginHelper;
    private final SystemConfigHelper configHelper;

    @Override
    public LoginType getType() {
        return LoginType.MINIPROGRAM;
    }

    @Override
    public ClientType[] supportedClients() {
        return new ClientType[]{ClientType.APP};
    }

    @Override
    public LoginResult login(LoginRequest request) {
        if (request.getWxCode() == null || request.getWxCode().isEmpty()) {
            throw new BusinessException("微信授权码不能为空");
        }

        // 1. code 换取 openId
        WechatMiniProgramService.MiniProgramLoginResult wxResult = wechatMiniProgramService.login(request.getWxCode());
        String openId = wxResult.getOpenId();

        // 2. 从 sys_user 查找用户，不存在则自动注册
        SysUser user = userService.getByOpenId(openId);
        if (user == null) {
            user = autoRegister(openId);
            log.info("小程序新用户注册: openId={}", openId);
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        // 3. 获取手机号（如果有 phoneCode）
        if (request.getPhoneCode() != null && !request.getPhoneCode().isEmpty()) {
            // 检查是否已确认为付费服务
            if (!configHelper.isPhoneVerifyPaid()) {
                log.warn(configHelper.getPhoneVerifyFeeNotice());
            }
            try {
                String phoneNumber = wechatMiniProgramService.getPhoneNumber(request.getPhoneCode());
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    user.setPhone(phoneNumber);
                    userService.updateById(user);
                    log.info("获取用户手机号成功：{}", phoneNumber);
                }
            } catch (Exception e) {
                log.warn("获取手机号失败：{}", e.getMessage());
            }
        }

        // 4. 执行登录
        return loginHelper.doLogin(user);
    }

    /**
     * 小程序用户自动注册到 sys_user
     */
    private SysUser autoRegister(String openId) {
        SysUser user = new SysUser();
        user.setUsername("wx_" + openId.substring(0, Math.min(openId.length(), 10)));
        user.setNickname("微信用户");
        user.setPassword(hashRandomPassword());
        user.setOpenId(openId);
        user.setStatus(1);
        user.setGender(0);
        user.setUserType("app");
        userService.save(user);
        return user;
    }

    private String hashRandomPassword() {
        byte[] rawPassword = new byte[24];
        PASSWORD_RANDOM.nextBytes(rawPassword);
        return BCrypt.hashpw(Base64.getUrlEncoder().withoutPadding().encodeToString(rawPassword));
    }
}
