package com.pengcheng.common.feature;

/**
 * 业务功能开关常量。
 */
public final class FeatureFlags {

    private FeatureFlags() {
    }

    public static final String ALIPAY_PREFIX = "pengcheng.feature.alipay";
    public static final String WECHAT_MP_PREFIX = "pengcheng.feature.wechat.mp";
    public static final String WECHAT_MINI_PREFIX = "pengcheng.feature.wechat.mini";
    public static final String WECHAT_PAY_PREFIX = "pengcheng.feature.wechat.pay";
    public static final String ENABLED = "enabled";
}
