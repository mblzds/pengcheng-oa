package com.pengcheng.sms;

import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 阿里云短信服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunSmsService implements SmsService {

    private final SystemConfigHelper configHelper;

    @Override
    public boolean sendCode(String phone, String code) {
        String accessKeyId = configHelper.getSmsAliyunAccessKeyId();
        String accessKeySecret = configHelper.getSmsAliyunAccessKeySecret();
        String signName = configHelper.getSmsAliyunSignName();
        String templateCode = configHelper.getSmsAliyunTemplateCode();
        String maskedPhone = SmsLogSanitizer.maskPhone(phone);

        if (accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
            log.error("阿里云短信配置不完整，拒绝发送: phone={}, signName={}, templateCode={}",
                    maskedPhone, signName, templateCode);
            return false;
        }

        try {
            log.error("阿里云短信发送未实现，拒绝假成功: phone={}, signName={}, templateCode={}, code={}",
                    maskedPhone, signName, templateCode, SmsLogSanitizer.maskCode(code));
            return false;
        } catch (Exception e) {
            log.error("阿里云短信发送异常: phone={}", maskedPhone, e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "aliyun";
    }
}
