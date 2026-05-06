package com.pengcheng.social;

import cn.hutool.json.JSONArray;
import lombok.Data;

/**
 * 三方登录服务接口（策略模式）
 * 支持微信、支付宝、Apple、GitHub 等三方登录
 */
public interface SocialLoginService {

    /**
     * 获取三方平台名称
     */
    String getPlatform();

    /**
     * 获取授权 URL
     *
     * @param redirectUri 回调地址
     * @param state       防 CSRF 状态参数
     * @return 授权 URL
     */
    default String getAuthorizeUrl(String redirectUri, String state) {
        return getAuthorizeUrl(redirectUri, state, null);
    }

    /**
     * 获取授权 URL（带 scope 参数）
     *
     * @param redirectUri 回调地址
     * @param state       防 CSRF 状态参数
     * @param scope       授权作用域
     * @return 授权 URL
     */
    default String getAuthorizeUrl(String redirectUri, String state, String scope) {
        return null;
    }

    /**
     * 通过授权码获取用户信息
     *
     * @param code 授权码
     * @return 三方用户信息
     */
    SocialUserInfo getUserInfo(String code);

    /**
     * 三方用户信息
     */
    @Data
    class SocialUserInfo {
        /** 三方平台 */
        private String platform;
        /** 三方平台唯一ID（openId） */
        private String openId;
        /** 三方平台联合ID（unionId） */
        private String unionId;
        /** 昵称 */
        private String nickname;
        /** 头像 */
        private String avatar;
        /** 性别（0未知 1男 2女） */
        private Integer gender;
        /** 手机号（部分平台支持） */
        private String phone;
        /** 邮箱（部分平台支持） */
        private String email;
        /** 省份（部分平台支持） */
        private String province;
        /** 城市（部分平台支持） */
        private String city;
        /** 国家（部分平台支持） */
        private String country;
        /** 特权信息（部分平台支持） */
        private JSONArray privilege;
        /** 原始响应JSON */
        private String rawJson;
    }
}
