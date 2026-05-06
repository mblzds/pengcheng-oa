package com.pengcheng.system.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.entity.SysConfigGroup;
import com.pengcheng.system.service.SysConfigGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 系统配置帮助类
 * 方便从 sys_config_group 获取各种配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemConfigHelper implements com.pengcheng.crypto.CryptoConfigProvider, com.pengcheng.mail.MailConfigProvider {

    private final SysConfigGroupService configGroupService;
    private final ObjectMapper objectMapper;

    // 配置分组编码
    public static final String GROUP_SYSTEM = "system";
    public static final String GROUP_REGISTER = "register";
    public static final String GROUP_LOGIN = "login";
    public static final String GROUP_PASSWORD = "password";
    public static final String GROUP_EMAIL = "email";
    public static final String GROUP_SMS = "sms";
    public static final String GROUP_STORAGE = "storage";
    public static final String GROUP_PUSH = "push";
    public static final String GROUP_SECURITY = "security";
    public static final String GROUP_WECHAT_MINIPROGRAM = "wechatMiniProgram";
    public static final String GROUP_WECHAT_MP = "wechatMp";

    /**
     * 获取配置JSON
     */
    public JsonNode getConfig(String groupCode) {
        try {
            SysConfigGroup group = configGroupService.getByGroupCode(groupCode);
            if (group != null && group.getConfigValue() != null) {
                return objectMapper.readTree(group.getConfigValue());
            }
        } catch (Exception e) {
            log.error("读取配置失败: {}", groupCode, e);
        }
        return null;
    }

    /**
     * 获取字符串配置
     */
    public String getString(String groupCode, String key) {
        return getString(groupCode, key, null);
    }

    public String getString(String groupCode, String key, String defaultValue) {
        JsonNode config = getConfig(groupCode);
        if (config != null) {
            JsonNode node = config.get(key);
            if (node != null && !node.isNull()) {
                return node.asText();
            }
        }
        return defaultValue;
    }

    /**
     * 获取整数配置
     */
    public int getInt(String groupCode, String key, int defaultValue) {
        JsonNode config = getConfig(groupCode);
        if (config != null) {
            JsonNode node = config.get(key);
            if (node != null && !node.isNull()) {
                return node.asInt(defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * 获取长整数配置
     */
    public long getLong(String groupCode, String key, long defaultValue) {
        JsonNode config = getConfig(groupCode);
        if (config != null) {
            JsonNode node = config.get(key);
            if (node != null && !node.isNull()) {
                return node.asLong(defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * 获取布尔配置
     */
    public boolean getBoolean(String groupCode, String key) {
        return getBoolean(groupCode, key, false);
    }

    public boolean getBoolean(String groupCode, String key, boolean defaultValue) {
        JsonNode config = getConfig(groupCode);
        if (config != null) {
            JsonNode node = config.get(key);
            if (node != null && !node.isNull()) {
                return node.asBoolean(defaultValue);
            }
        }
        return defaultValue;
    }

    // ============ 注册配置 ============

    /**
     * 是否开放注册
     */
    public boolean isRegisterEnabled() {
        return getBoolean(GROUP_REGISTER, "enabled", true);
    }

    /**
     * 是否需要邮箱验证
     */
    public boolean isRegisterVerifyEmail() {
        return getBoolean(GROUP_REGISTER, "verifyEmail");
    }

    /**
     * 是否需要手机验证
     */
    public boolean isRegisterVerifyPhone() {
        return getBoolean(GROUP_REGISTER, "verifyPhone");
    }

    /**
     * 获取默认角色编码
     */
    public String getRegisterDefaultRole() {
        return getString(GROUP_REGISTER, "defaultRole", "user");
    }

    /**
     * 注册是否需要审核
     */
    public boolean isRegisterNeedAudit() {
        return getBoolean(GROUP_REGISTER, "needAudit");
    }

    // ============ 登录配置 ============

    /**
     * 是否启用验证码
     */
    public boolean isCaptchaEnabled() {
        return getBoolean(GROUP_LOGIN, "captchaEnabled");
    }

    /**
     * 是否启用记住我
     */
    public boolean isRememberMeEnabled() {
        return getBoolean(GROUP_LOGIN, "rememberMe", true);
    }

    /**
     * 获取验证码类型
     */
    public String getCaptchaType() {
        return getString(GROUP_LOGIN, "captchaType", "image");
    }

    /**
     * 获取最大登录重试次数
     */
    public int getMaxRetryCount() {
        return getInt(GROUP_LOGIN, "maxRetryCount", 5);
    }

    /**
     * 获取锁定时间（分钟）
     */
    public int getLockTime() {
        return getInt(GROUP_LOGIN, "lockTime", 30);
    }

    /**
     * 是否单点登录
     */
    public boolean isSingleLogin() {
        return getBoolean(GROUP_LOGIN, "singleLogin");
    }

    // ============ 密码配置 ============

    /**
     * 获取密码最小长度
     */
    public int getPasswordMinLength() {
        return getInt(GROUP_PASSWORD, "minLength", 6);
    }

    /**
     * 获取密码最大长度
     */
    public int getPasswordMaxLength() {
        return getInt(GROUP_PASSWORD, "maxLength", 20);
    }

    /**
     * 是否必须包含大写字母
     */
    public boolean isPasswordRequireUppercase() {
        return getBoolean(GROUP_PASSWORD, "requireUppercase");
    }

    /**
     * 是否必须包含小写字母
     */
    public boolean isPasswordRequireLowercase() {
        return getBoolean(GROUP_PASSWORD, "requireLowercase");
    }

    /**
     * 是否必须包含数字
     */
    public boolean isPasswordRequireNumber() {
        return getBoolean(GROUP_PASSWORD, "requireNumber");
    }

    /**
     * 是否必须包含特殊字符
     */
    public boolean isPasswordRequireSpecial() {
        return getBoolean(GROUP_PASSWORD, "requireSpecial");
    }

    /**
     * 验证密码规则
     */
    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        int minLen = getPasswordMinLength();
        int maxLen = getPasswordMaxLength();

        if (password.length() < minLen) {
            throw new RuntimeException("密码长度不能少于" + minLen + "位");
        }
        if (password.length() > maxLen) {
            throw new RuntimeException("密码长度不能超过" + maxLen + "位");
        }
        if (isPasswordRequireUppercase() && !password.matches(".*[A-Z].*")) {
            throw new RuntimeException("密码必须包含大写字母");
        }
        if (isPasswordRequireLowercase() && !password.matches(".*[a-z].*")) {
            throw new RuntimeException("密码必须包含小写字母");
        }
        if (isPasswordRequireNumber() && !password.matches(".*\\d.*")) {
            throw new RuntimeException("密码必须包含数字");
        }
        if (isPasswordRequireSpecial() && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new RuntimeException("密码必须包含特殊字符");
        }
    }

    // ============ 文件配置 ============

    /**
     * 获取存储方式
     */
    public String getStorageProvider() {
        return getString(GROUP_STORAGE, "provider", "local");
    }

    /**
     * 获取本地存储路径
     */
    public String getStorageLocalPath() {
        return getString(GROUP_STORAGE, "localPath", "./uploads");
    }

    /**
     * 获取最大文件大小(MB)
     */
    public int getStorageMaxSize() {
        return getInt(GROUP_STORAGE, "maxSize", 10);
    }

    /**
     * 获取允许的文件类型
     */
    public String getStorageAllowTypes() {
        // 支持常见的图片、文档、视频、音频、压缩包、代码等文件类型
        return getString(GROUP_STORAGE, "allowTypes",
            "jpg,jpeg,png,gif,webp,bmp,ico,svg," +  // 图片
            "pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,rtf,csv," +  // 文档
            "mp4,avi,mov,wmv,flv,mkv,webm,m4v," +  // 视频
            "mp3,wav,ogg,flac,aac,m4a," +  // 音频
            "zip,rar,7z,tar,gz,bz2," +  // 压缩包
            "json,xml,yaml,yml,ini,conf,cfg,properties," +  // 配置文件
            "js,ts,vue,jsx,tsx,css,scss,less,html,htm," +  // 前端
            "java,py,go,rs,c,cpp,h,hpp,cs,php,rb,swift,kt," +  // 后端
            "sql,sh,bat,ps1,log");  // 其他
    }

    /**
     * 验证文件类型
     */
    public void validateFileType(String fileName) {
        String allowTypes = getStorageAllowTypes();
        if (allowTypes == null || allowTypes.isEmpty()) {
            return;
        }

        String suffix = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            suffix = fileName.substring(lastDot + 1).toLowerCase();
        }

        String[] types = allowTypes.toLowerCase().split(",");
        for (String type : types) {
            if (type.trim().equals(suffix)) {
                return;
            }
        }
        throw new RuntimeException("不支持的文件类型: " + suffix + "，允许的类型: " + allowTypes);
    }

    /**
     * 验证文件大小
     */
    public void validateFileSize(long fileSize) {
        int maxSize = getStorageMaxSize();
        long maxBytes = maxSize * 1024L * 1024L;
        if (fileSize > maxBytes) {
            throw new RuntimeException("文件大小超过限制，最大允许: " + maxSize + "MB");
        }
    }

    // ============ Token 配置（存储在安全配置分组中） ============

    /**
     * 获取 Token 名称
     */
    public String getTokenName() {
        return getString(GROUP_SECURITY, "tokenName", "Authorization");
    }

    /**
     * 获取 Token 有效期（秒）
     */
    public long getTokenTimeout() {
        return getLong(GROUP_SECURITY, "tokenTimeout", 86400);
    }

    /**
     * 获取 Token 最低活跃频率（秒）
     */
    public long getTokenActiveTimeout() {
        return getLong(GROUP_SECURITY, "tokenActiveTimeout", 86400);
    }

    /**
     * 是否允许同一账号多地同时登录
     */
    public boolean isTokenConcurrent() {
        return getBoolean(GROUP_SECURITY, "tokenIsConcurrent", true);
    }

    /**
     * 多人登录同一账号时是否共用一个 Token
     */
    public boolean isTokenShare() {
        return getBoolean(GROUP_SECURITY, "tokenIsShare", true);
    }

    /**
     * 获取 Token 风格
     */
    public String getTokenStyle() {
        return getString(GROUP_SECURITY, "tokenStyle", "uuid");
    }

    /**
     * 是否输出 Sa-Token 操作日志
     */
    public boolean isTokenLog() {
        return getBoolean(GROUP_SECURITY, "tokenIsLog", false);
    }

    /**
     * 是否尝试从请求体里读取 Token
     */
    public boolean isTokenReadBody() {
        return getBoolean(GROUP_SECURITY, "tokenIsReadBody", false);
    }

    /**
     * 是否尝试从 Cookie 里读取 Token
     */
    public boolean isTokenReadCookie() {
        return getBoolean(GROUP_SECURITY, "tokenIsReadCookie", false);
    }

    /**
     * 是否尝试从 Header 里读取 Token
     */
    public boolean isTokenReadHeader() {
        return getBoolean(GROUP_SECURITY, "tokenIsReadHeader", true);
    }

    /**
     * 是否在初始化配置时打印版本字符画
     */
    public boolean isTokenPrint() {
        return getBoolean(GROUP_SECURITY, "tokenIsPrint", true);
    }

    /**
     * 是否在登录后将 Token 写入响应头
     */
    public boolean isTokenWriteHeader() {
        return getBoolean(GROUP_SECURITY, "tokenIsWriteHeader", false);
    }

    // ============ 安全配置 ============

    /**
     * 是否启用接口加密
     */
    public boolean isEncryptEnabled() {
        return getBoolean(GROUP_SECURITY, "encryptEnabled");
    }

    /**
     * 获取加密范围
     * @return "global" 全局加密, "partial" 部分加密(默认)
     */
    public String getEncryptScope() {
        return getString(GROUP_SECURITY, "encryptScope", "partial");
    }

    /**
     * 是否全局加密
     */
    public boolean isGlobalEncrypt() {
        return "global".equals(getEncryptScope());
    }

    /**
     * 是否启用XSS过滤
     */
    public boolean isXssFilterEnabled() {
        return getBoolean(GROUP_SECURITY, "xssFilter", true);
    }

    /**
     * 是否启用SQL注入防护
     */
    public boolean isSqlInjectEnabled() {
        return getBoolean(GROUP_SECURITY, "sqlInject", true);
    }

    // ============ 短信配置 ============

    /**
     * 获取短信服务商
     */
    public String getSmsProvider() {
        return getString(GROUP_SMS, "provider", "console");
    }

    /**
     * 获取阿里云短信AccessKeyId
     */
    public String getSmsAliyunAccessKeyId() {
        return getString(GROUP_SMS, "aliyunAccessKeyId", "");
    }

    /**
     * 获取阿里云短信AccessKeySecret
     */
    public String getSmsAliyunAccessKeySecret() {
        return getString(GROUP_SMS, "aliyunAccessKeySecret", "");
    }

    /**
     * 获取阿里云短信签名
     */
    public String getSmsAliyunSignName() {
        return getString(GROUP_SMS, "aliyunSignName", "");
    }

    /**
     * 获取阿里云短信模板ID
     */
    public String getSmsAliyunTemplateCode() {
        return getString(GROUP_SMS, "aliyunTemplateCode", "");
    }

    /**
     * 获取腾讯云短信SecretId
     */
    public String getSmsTencentSecretId() {
        return getString(GROUP_SMS, "tencentSecretId", "");
    }

    /**
     * 获取腾讯云短信SecretKey
     */
    public String getSmsTencentSecretKey() {
        return getString(GROUP_SMS, "tencentSecretKey", "");
    }

    /**
     * 获取腾讯云短信AppId
     */
    public String getSmsTencentAppId() {
        return getString(GROUP_SMS, "tencentAppId", "");
    }

    /**
     * 获取腾讯云短信签名
     */
    public String getSmsTencentSignName() {
        return getString(GROUP_SMS, "tencentSignName", "");
    }

    /**
     * 获取腾讯云短信模板ID
     */
    public String getSmsTencentTemplateId() {
        return getString(GROUP_SMS, "tencentTemplateId", "");
    }

    // ============ 邮件配置 ============

    /**
     * 是否启用邮件
     */
    public boolean isEmailEnabled() {
        return getBoolean(GROUP_EMAIL, "enabled");
    }

    /**
     * 获取SMTP服务器
     */
    public String getEmailHost() {
        return getString(GROUP_EMAIL, "host", "");
    }

    /**
     * 获取邮件端口
     */
    public int getEmailPort() {
        return getInt(GROUP_EMAIL, "port", 465);
    }

    /**
     * 获取邮件用户名
     */
    public String getEmailUsername() {
        return getString(GROUP_EMAIL, "username", "");
    }

    /**
     * 获取邮件密码/授权码
     */
    public String getEmailPassword() {
        return getString(GROUP_EMAIL, "password", "");
    }

    /**
     * 获取发件人名称
     */
    public String getEmailFromName() {
        return getString(GROUP_EMAIL, "fromName", "");
    }

    /**
     * 是否启用SSL
     */
    public boolean isEmailSsl() {
        return getBoolean(GROUP_EMAIL, "ssl", true);
    }

    // ============ 邮件模板配置 ============

    private static final String GROUP_EMAIL_TEMPLATE = "emailTemplate";

    /**
     * 获取验证码邮件模板
     */
    public String getEmailTemplateVerifyCode() {
        return getString(GROUP_EMAIL_TEMPLATE, "verifyCode", "您的验证码是：{code}，有效期{expire}分钟。");
    }

    /**
     * 获取重置密码邮件模板
     */
    public String getEmailTemplateResetPassword() {
        return getString(GROUP_EMAIL_TEMPLATE, "resetPassword", "您正在重置密码，验证码：{code}，有效期{expire}分钟。");
    }

    /**
     * 获取欢迎邮件模板
     */
    public String getEmailTemplateWelcome() {
        return getString(GROUP_EMAIL_TEMPLATE, "welcome", "欢迎注册{siteName}，您的账号已创建成功。");
    }

    // ============ 系统配置 ============

    /**
     * 获取站点名称
     */
    public String getSiteName() {
        return getString(GROUP_SYSTEM, "siteName", "MasterLife");
    }

    /**
     * 获取站点描述
     */
    public String getSiteDescription() {
        return getString(GROUP_SYSTEM, "siteDescription", "");
    }

    /**
     * 获取版权信息
     */
    public String getCopyright() {
        return getString(GROUP_SYSTEM, "copyright", "版权所有@朋诚科技");
    }

    /**
     * 获取站点Logo
     */
    public String getSiteLogo() {
        return getString(GROUP_SYSTEM, "siteLogo", "");
    }

    /**
     * 获取ICP备案号
     */
    public String getIcp() {
        return getString(GROUP_SYSTEM, "icp", "");
    }

    /**
     * 是否启用水印
     */
    public boolean isWatermarkEnabled() {
        return getBoolean(GROUP_SYSTEM, "watermarkEnabled", true);
    }

    /**
     * 获取水印类型（username/username_time/sitename/custom）
     */
    public String getWatermarkType() {
        return getString(GROUP_SYSTEM, "watermarkType", "username");
    }

    /**
     * 获取水印透明度
     */
    public double getWatermarkOpacity() {
        JsonNode config = getConfig(GROUP_SYSTEM);
        if (config != null) {
            JsonNode node = config.get("watermarkOpacity");
            if (node != null && !node.isNull()) {
                return node.asDouble(0.1);
            }
        }
        return 0.1;
    }

    // ============ 推送配置 ============

    /**
     * 是否启用推送
     */
    public boolean isPushEnabled() {
        return getBoolean(GROUP_PUSH, "enabled");
    }

    /**
     * 获取推送服务商
     */
    public String getPushProvider() {
        return getString(GROUP_PUSH, "provider", "console");
    }

    /**
     * 获取推送AppKey
     */
    public String getPushAppKey() {
        return getString(GROUP_PUSH, "appKey", "");
    }

    /**
     * 获取推送MasterSecret
     */
    public String getPushMasterSecret() {
        return getString(GROUP_PUSH, "masterSecret", "");
    }

    // ============ 文件存储扩展配置 ============

    /**
     * 获取存储域名
     */
    public String getStorageDomain() {
        return getString(GROUP_STORAGE, "domain", "http://localhost:8080");
    }

    /**
     * 获取MinIO端点
     */
    public String getStorageMinioEndpoint() {
        return getString(GROUP_STORAGE, "minioEndpoint", "");
    }

    /**
     * 获取MinIO AccessKey
     */
    public String getStorageMinioAccessKey() {
        return getString(GROUP_STORAGE, "minioAccessKey", "");
    }

    /**
     * 获取MinIO SecretKey
     */
    public String getStorageMinioSecretKey() {
        return getString(GROUP_STORAGE, "minioSecretKey", "");
    }

    /**
     * 获取MinIO存储桶
     */
    public String getStorageMinioBucket() {
        return getString(GROUP_STORAGE, "minioBucket", "");
    }

    /**
     * 获取阿里云OSS端点
     */
    public String getStorageAliyunEndpoint() {
        return getString(GROUP_STORAGE, "aliyunEndpoint", "");
    }

    /**
     * 获取阿里云OSS AccessKey
     */
    public String getStorageAliyunAccessKey() {
        return getString(GROUP_STORAGE, "aliyunAccessKey", "");
    }

    /**
     * 获取阿里云OSS SecretKey
     */
    public String getStorageAliyunSecretKey() {
        return getString(GROUP_STORAGE, "aliyunSecretKey", "");
    }

    /**
     * 获取阿里云OSS存储桶
     */
    public String getStorageAliyunBucket() {
        return getString(GROUP_STORAGE, "aliyunBucket", "");
    }

    /**
     * 获取腾讯云COS SecretId
     */
    public String getStorageTencentSecretId() {
        return getString(GROUP_STORAGE, "tencentSecretId", "");
    }

    /**
     * 获取腾讯云COS SecretKey
     */
    public String getStorageTencentSecretKey() {
        return getString(GROUP_STORAGE, "tencentSecretKey", "");
    }

    /**
     * 获取腾讯云COS存储桶
     */
    public String getStorageTencentBucket() {
        return getString(GROUP_STORAGE, "tencentBucket", "");
    }

    /**
     * 获取腾讯云COS地域
     */
    public String getStorageTencentRegion() {
        return getString(GROUP_STORAGE, "tencentRegion", "");
    }

    // ============ 微信小程序配置 ============

    /**
     * 是否启用小程序
     */
    public boolean isMiniProgramEnabled() {
        return getBoolean(GROUP_WECHAT_MINIPROGRAM, "enabled");
    }

    /**
     * 获取小程序AppID
     */
    public String getMiniProgramAppId() {
        return getString(GROUP_WECHAT_MINIPROGRAM, "appId", "");
    }

    /**
     * 获取小程序AppSecret
     */
    public String getMiniProgramAppSecret() {
        return getString(GROUP_WECHAT_MINIPROGRAM, "appSecret", "");
    }

    // ============ 微信公众号配置 ============

    /**
     * 是否启用公众号
     */
    public boolean isWechatMpEnabled() {
        return getBoolean(GROUP_WECHAT_MP, "enabled");
    }

    /**
     * 获取公众号AppID
     */
    public String getWechatMpAppId() {
        return getString(GROUP_WECHAT_MP, "appId", "");
    }

    /**
     * 获取公众号AppSecret
     */
    public String getWechatMpAppSecret() {
        return getString(GROUP_WECHAT_MP, "appSecret", "");
    }

    /**
     * 获取公众号Token
     */
    public String getWechatMpToken() {
        return getString(GROUP_WECHAT_MP, "token", "");
    }

    /**
     * 获取公众号AES Key
     */
    public String getWechatMpAesKey() {
        return getString(GROUP_WECHAT_MP, "aesKey", "");
    }

    /**
     * 获取公众号回调URL
     */
    public String getWechatMpCallbackUrl() {
        return getString(GROUP_WECHAT_MP, "callbackUrl", "");
    }

    /**
     * 获取公众号OAuth回调URL
     */
    public String getWechatMpOAuthRedirectUrl() {
        return getString(GROUP_WECHAT_MP, "oauthRedirectUrl", "");
    }

    /**
     * 获取公众号菜单配置
     */
    public String getWechatMpMenuConfig() {
        return getString(GROUP_WECHAT_MP, "menuConfig", "");
    }

    // ============ 支付宝配置 ============

    /**
     * 是否启用支付宝
     */
    public boolean isAlipayEnabled() {
        return getBoolean("alipay", "enabled");
    }

    /**
     * 获取支付宝 AppID
     */
    public String getAlipayAppId() {
        return getString("alipay", "appId", "");
    }

    /**
     * 获取支付宝应用私钥
     */
    public String getAlipayPrivateKey() {
        return getString("alipay", "privateKey", "");
    }

    /**
     * 获取支付宝公钥
     */
    public String getAlipayPublicKey() {
        return getString("alipay", "publicKey", "");
    }

    // ============ 微信小程序手机号获取配置（付费提示） ============

    /**
     * 是否已确认小程序手机号获取为付费服务
     * 2023 年 4 月 17 日起，微信小程序获取手机号改为 0.03 元/次
     */
    public boolean isPhoneVerifyPaid() {
        return getBoolean(GROUP_WECHAT_MINIPROGRAM, "phoneVerifyPaid", false);
    }

    /**
     * 获取手机号获取费用提示
     */
    public String getPhoneVerifyFeeNotice() {
        return getString(GROUP_WECHAT_MINIPROGRAM, "phoneVerifyFeeNotice", 
            "温馨提示：根据微信官方政策，获取手机号服务为付费服务（0.03 元/次），请确认已开通并了解相关费用。");
    }
}
