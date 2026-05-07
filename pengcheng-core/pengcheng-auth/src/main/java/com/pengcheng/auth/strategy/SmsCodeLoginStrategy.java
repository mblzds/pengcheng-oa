package com.pengcheng.auth.strategy;

import com.pengcheng.auth.LoginHelper;
import com.pengcheng.auth.LoginRequest;
import com.pengcheng.auth.LoginResult;
import com.pengcheng.auth.LoginStrategy;
import com.pengcheng.auth.enums.ClientType;
import com.pengcheng.auth.enums.LoginType;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 手机验证码登录策略（App端）
 * 手机号+短信验证码，未注册需联系管理员开通
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeLoginStrategy implements LoginStrategy {

    private final SysUserService userService;
    private final StringRedisTemplate redisTemplate;
    private final LoginHelper loginHelper;

    private static final String SMS_CODE_KEY = "sms:login:";

    @Override
    public LoginType getType() {
        return LoginType.SMS;
    }

    @Override
    public ClientType[] supportedClients() {
        return new ClientType[]{ClientType.APP, ClientType.WEB};
    }

    @Override
    public LoginResult login(LoginRequest request) {
        String phone = request.getPhone();
        String smsCode = request.getSmsCode();

        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的手机号");
        }
        if (smsCode == null || smsCode.isEmpty()) {
            throw new BusinessException("请输入验证码");
        }

        // 校验验证码
        String cacheCode = redisTemplate.opsForValue().get(SMS_CODE_KEY + phone);
        if (cacheCode == null || !cacheCode.equals(smsCode)) {
            throw new BusinessException("验证码错误或已过期");
        }
        redisTemplate.delete(SMS_CODE_KEY + phone);

        SysUser user = userService.lambdaQuery()
                .eq(SysUser::getPhone, phone)
                .one();
        if (user == null) {
            log.warn("手机号未注册: {}", phone);
            throw new BusinessException("账号未注册，请联系管理员开通");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("用户已被禁用");
        }

        return loginHelper.doLogin(user);
    }
}
