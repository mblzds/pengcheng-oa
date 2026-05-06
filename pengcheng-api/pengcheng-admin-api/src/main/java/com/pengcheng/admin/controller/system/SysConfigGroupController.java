package com.pengcheng.admin.controller.system;

import com.pengcheng.common.result.Result;
import com.pengcheng.system.config.SaTokenConfigLoader;
import com.pengcheng.system.entity.SysConfigGroup;
import com.pengcheng.mail.EmailService;
import com.pengcheng.system.service.SysConfigGroupService;
import com.pengcheng.system.helper.SystemConfigHelper;
import com.pengcheng.pay.PayServiceFactory;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置分组 Controller
 */
@RestController
@RequestMapping("/sys/config-group")
@RequiredArgsConstructor
public class SysConfigGroupController {

    private final SysConfigGroupService configGroupService;
    private final SystemConfigHelper configHelper;
    private final PayServiceFactory payServiceFactory;
    private final EmailService emailService;
    private final SaTokenConfigLoader saTokenConfigLoader;

    /**
     * 获取所有配置分组
     */
    @GetMapping("/list")
    public Result<List<SysConfigGroup>> list() {
        return Result.ok(configGroupService.listAll());
    }

    /**
     * 获取指定分组配置
     */
    @GetMapping("/{groupCode}")
    public Result<SysConfigGroup> getByCode(@PathVariable String groupCode) {
        return Result.ok(configGroupService.getByGroupCode(groupCode));
    }

    /**
     * 保存配置
     */
    @PostMapping("/{groupCode}")
    public Result<Void> save(@PathVariable String groupCode, @RequestBody Map<String, Object> config) {
        try {
            String configValue = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(config);
            configGroupService.saveConfig(groupCode, configValue);

            // 如果保存的是安全配置，重新加载 Sa-Token 配置使其即时生效
            if ("security".equals(groupCode)) {
                saTokenConfigLoader.applyConfig();
            }

            return Result.ok();
        } catch (Exception e) {
            return Result.fail("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 刷新缓存
     */
    @PostMapping("/refresh")
    public Result<Void> refresh() {
        configGroupService.refreshCache();
        return Result.ok();
    }

    /**
     * 获取公开配置（不需要登录）
     * 用于前端显示站点信息、登录配置等
     */
    @GetMapping("/public")
    public Result<Map<String, Object>> getPublicConfig() {
        Map<String, Object> config = new HashMap<>();

        // 系统配置
        Map<String, Object> system = new HashMap<>();
        system.put("siteName", configHelper.getSiteName());
        system.put("siteDescription", configHelper.getSiteDescription());
        system.put("siteLogo", configHelper.getSiteLogo());
        system.put("copyright", configHelper.getCopyright());
        system.put("icp", configHelper.getIcp());
        system.put("watermarkEnabled", configHelper.isWatermarkEnabled());
        system.put("watermarkType", configHelper.getWatermarkType());
        system.put("watermarkOpacity", configHelper.getWatermarkOpacity());
        config.put("system", system);

        // 登录配置
        Map<String, Object> login = new HashMap<>();
        login.put("captchaEnabled", configHelper.isCaptchaEnabled());
        login.put("captchaType", configHelper.getCaptchaType()); // image, slider, sms
        login.put("maxRetryCount", configHelper.getMaxRetryCount());
        login.put("rememberMe", configHelper.isRememberMeEnabled());
        config.put("login", login);

        // 注册配置
        Map<String, Object> register = new HashMap<>();
        register.put("enabled", configHelper.isRegisterEnabled());
        register.put("verifyEmail", configHelper.isRegisterVerifyEmail());
        register.put("verifyPhone", configHelper.isRegisterVerifyPhone());
        register.put("needAudit", configHelper.isRegisterNeedAudit());
        config.put("register", register);

        // 密码配置（规则提示）
        Map<String, Object> password = new HashMap<>();
        password.put("minLength", configHelper.getPasswordMinLength());
        password.put("maxLength", configHelper.getPasswordMaxLength());
        password.put("requireUppercase", configHelper.isPasswordRequireUppercase());
        password.put("requireLowercase", configHelper.isPasswordRequireLowercase());
        password.put("requireNumber", configHelper.isPasswordRequireNumber());
        password.put("requireSpecial", configHelper.isPasswordRequireSpecial());
        config.put("password", password);

        // 文件上传配置
        Map<String, Object> storage = new HashMap<>();
        storage.put("maxSize", configHelper.getStorageMaxSize());
        storage.put("allowTypes", configHelper.getStorageAllowTypes());
        config.put("storage", storage);

        return Result.ok(config);
    }

    /**
     * 测试支付
     */
    @PostMapping("/test-payment")
    public Result<Map<String, String>> testPayment(@RequestBody TestPaymentRequest request) {
        try {
            if (!payServiceFactory.isSupported(request.getType())) {
                return Result.fail("不支持的支付类型: " + request.getType());
            }
            Map<String, String> result = payServiceFactory.createTestOrder(request.getType());
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @Data
    public static class TestPaymentRequest {
        private String type; // wechat 或 alipay
    }

    /**
     * 测试发送邮件
     */
    @PostMapping("/test-email")
    public Result<Void> testEmail(@RequestBody TestEmailRequest request) {
        try {
            emailService.sendTestMail(request.getTo());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @Data
    public static class TestEmailRequest {
        private String to; // 收件人邮箱
    }

    /**
     * 生成RSA密钥对
     */
    @PostMapping("/generate-keys")
    public Result<Map<String, String>> generateKeys() {
        try {
            Map<String, String> keyPair = com.pengcheng.common.util.RsaUtils.generateKeyPair();
            return Result.ok(keyPair);
        } catch (Exception e) {
            return Result.fail("生成密钥失败: " + e.getMessage());
        }
    }
}
