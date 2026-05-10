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

        // 3. 未绑定：用 phoneCode 换微信认证手机号 → 找现有员工 → 把 openId 绑上去
        //    没传 phoneCode 时返回 4001，前端调起 <button open-type="getPhoneNumber"> 重试
        if (user == null) {
            if (request.getPhoneCode() == null || request.getPhoneCode().isEmpty()) {
                log.warn("openId 未绑定且未提供 phoneCode，请前端授权手机号: {}", openId);
                throw new BusinessException(CODE_BIND_REQUIRED, "未绑定，请授权手机号完成绑定");
            }
            String wxPhone;
            try {
                wxPhone = wechatMiniProgramService.getPhoneNumber(request.getPhoneCode());
            } catch (Exception e) {
                log.warn("解析微信手机号失败: {}", e.getMessage());
                throw new BusinessException("授权手机号失败，请重试");
            }
            if (wxPhone == null || wxPhone.isEmpty()) {
                throw new BusinessException("未拿到微信手机号，请重新授权");
            }
            SysUser byPhone = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, wxPhone).last("LIMIT 1"));
            if (byPhone == null) {
                log.warn("微信手机号 {} 未在系统中注册", wxPhone);
                throw new BusinessException(CODE_PHONE_NOT_REGISTERED, "手机号未注册：" + wxPhone + "，请联系管理员开通");
            }
            if (byPhone.getStatus() != null && byPhone.getStatus() != 1) {
                throw new BusinessException("账号已被禁用");
            }
            // 把 openId 写回该账号，下次微信登录就直接命中
            SysUser patch = new SysUser();
            patch.setId(byPhone.getId());
            patch.setOpenId(openId);
            userService.updateById(patch);
            log.info("已绑定 openId 到现有账号: userId={}, phone={}", byPhone.getId(), wxPhone);
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
