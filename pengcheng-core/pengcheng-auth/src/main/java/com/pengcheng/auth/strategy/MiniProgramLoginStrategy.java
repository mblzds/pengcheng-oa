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
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;

    /** 与 SmsCodeLoginStrategy 保持一致的短信验证码 redis key 前缀 */
    private static final String SMS_CODE_KEY = "sms:login:";

    @Override
    public LoginType getType() {
        return LoginType.MINIPROGRAM;
    }

    @Override
    public ClientType[] supportedClients() {
        return new ClientType[]{ClientType.APP};
    }

    /** openId 未绑定且需要前端调起 getPhoneNumber 二次提交 */
    public static final int CODE_BIND_REQUIRED = 4001;
    /** 微信授权的手机号在 sys_user 里找不到对应账号 */
    public static final int CODE_PHONE_NOT_REGISTERED = 4002;

    @Override
    public LoginResult login(LoginRequest request) {
        if (request.getWxCode() == null || request.getWxCode().isEmpty()) {
            throw new BusinessException("微信授权码不能为空");
        }

        // 1. code 换取 openId
        WechatMiniProgramService.MiniProgramLoginResult wxResult = wechatMiniProgramService.login(request.getWxCode());
        String openId = wxResult.getOpenId();

        // 2. 从 sys_user 查找已绑定该 openId 的账号
        SysUser user = userService.getByOpenId(openId);

        // 3. 未绑定：拿手机号 → 按 phone 找现有员工 → 把 openId 绑上去
        //    手机号来源两种：
        //      a) phoneCode：微信认证手机号（仅企业小程序开通能力时可用）
        //      b) phone + smsCode：短信验证码兜底（所有小程序通用，与 SmsCodeLoginStrategy 共用 redis key）
        //    都没传时返回 4001，前端引导用户补齐
        if (user == null) {
            String boundPhone = null;
            if (request.getPhoneCode() != null && !request.getPhoneCode().isEmpty()) {
                try {
                    boundPhone = wechatMiniProgramService.getPhoneNumber(request.getPhoneCode());
                } catch (Exception e) {
                    log.warn("解析微信手机号失败: {}", e.getMessage());
                    throw new BusinessException("授权手机号失败，请重试");
                }
                if (boundPhone == null || boundPhone.isEmpty()) {
                    throw new BusinessException("未拿到微信手机号，请重新授权");
                }
            } else if (request.getPhone() != null && !request.getPhone().isEmpty()
                    && request.getSmsCode() != null && !request.getSmsCode().isEmpty()) {
                if (!request.getPhone().matches("^1[3-9]\\d{9}$")) {
                    throw new BusinessException("请输入正确的手机号");
                }
                String cacheCode = redisTemplate.opsForValue().get(SMS_CODE_KEY + request.getPhone());
                if (cacheCode == null || !cacheCode.equals(request.getSmsCode())) {
                    throw new BusinessException("验证码错误或已过期");
                }
                redisTemplate.delete(SMS_CODE_KEY + request.getPhone());
                boundPhone = request.getPhone();
            } else {
                log.warn("openId 未绑定且未提供 phoneCode/phone+smsCode: {}", openId);
                throw new BusinessException(CODE_BIND_REQUIRED, "未绑定，请用手机号验证码完成绑定");
            }

            SysUser byPhone = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, boundPhone).last("LIMIT 1"));
            if (byPhone == null) {
                log.warn("手机号 {} 未在系统中注册", boundPhone);
                throw new BusinessException(CODE_PHONE_NOT_REGISTERED, "手机号未注册：" + boundPhone + "，请联系管理员开通");
            }
            if (byPhone.getStatus() != null && byPhone.getStatus() != 1) {
                throw new BusinessException("账号已被禁用");
            }
            SysUser patch = new SysUser();
            patch.setId(byPhone.getId());
            patch.setOpenId(openId);
            userService.updateById(patch);
            log.info("已绑定 openId 到现有账号: userId={}, phone={}", byPhone.getId(), boundPhone);
            byPhone.setOpenId(openId);
            return loginHelper.doLogin(byPhone);
        }

        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        // 4. 已绑定的常规流程：顺手用 phoneCode 校正一下 phone
        if (request.getPhoneCode() != null && !request.getPhoneCode().isEmpty()) {
            if (!configHelper.isPhoneVerifyPaid()) {
                log.warn(configHelper.getPhoneVerifyFeeNotice());
            }
            try {
                String phoneNumber = wechatMiniProgramService.getPhoneNumber(request.getPhoneCode());
                if (phoneNumber != null && !phoneNumber.isEmpty() && !phoneNumber.equals(user.getPhone())) {
                    user.setPhone(phoneNumber);
                    userService.updateById(user);
                    log.info("更新用户手机号：{}", phoneNumber);
                }
            } catch (Exception e) {
                log.warn("获取手机号失败：{}", e.getMessage());
            }
        }

        return loginHelper.doLogin(user);
    }
}
