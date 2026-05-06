package com.pengcheng.sms;

import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 腾讯云短信服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentSmsService implements SmsService {

    private final SystemConfigHelper configHelper;

    @Override
    public boolean sendCode(String phone, String code) {
        String secretId = configHelper.getSmsTencentSecretId();
        String secretKey = configHelper.getSmsTencentSecretKey();
        String appId = configHelper.getSmsTencentAppId();
        String signName = configHelper.getSmsTencentSignName();
        String templateId = configHelper.getSmsTencentTemplateId();
        String maskedPhone = SmsLogSanitizer.maskPhone(phone);

        if (secretId.isEmpty() || secretKey.isEmpty()) {
            log.error("腾讯云短信配置不完整，拒绝发送: phone={}, appId={}, signName={}, templateId={}",
                    maskedPhone, appId, signName, templateId);
            return false;
        }

        try {
            log.error("腾讯云短信发送未实现，拒绝假成功: phone={}, appId={}, signName={}, templateId={}, code={}",
                    maskedPhone, appId, signName, templateId, SmsLogSanitizer.maskCode(code));
            return false;
        } catch (Exception e) {
            log.error("腾讯云短信发送异常: phone={}", maskedPhone, e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "tencent";
    }
}
