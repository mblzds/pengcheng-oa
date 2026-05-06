package com.pengcheng.sms;

/**
 * 短信服务接口
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否发送成功
     */
    boolean sendCode(String phone, String code);

    /**
     * 获取服务商名称
     */
    String getProviderName();
}
