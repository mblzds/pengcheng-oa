package com.pengcheng.auth.strategy;

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

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = FeatureFlags.WECHAT_MINI_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")
public class MiniProgramLoginStrategy implements LoginStrategy {

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

        // 2. 从 sys_user 查找用户，不存在则提示联系管理员
        SysUser user = userService.getByOpenId(openId);
        if (user == null) {
            log.warn("小程序 openId 未绑定: {}", openId);
            throw new BusinessException("账号未注册，请联系管理员开通");
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
}
