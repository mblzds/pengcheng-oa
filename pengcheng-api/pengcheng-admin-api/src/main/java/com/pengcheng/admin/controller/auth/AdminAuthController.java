package com.pengcheng.admin.controller.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.pengcheng.auth.LoginRequest;
import com.pengcheng.auth.LoginResult;
import com.pengcheng.auth.LoginStrategyFactory;
import com.pengcheng.auth.enums.ClientType;
import com.pengcheng.auth.enums.LoginType;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.entity.SysMenu;
import com.pengcheng.system.entity.SysRole;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.entity.SysUserRole;
import com.pengcheng.system.helper.SystemConfigHelper;
import com.pengcheng.system.service.*;
import com.pengcheng.sms.SmsServiceFactory;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 后台认证控制器
 * 登录通过 pengcheng-auth 统一策略工厂处理
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final SysUserService userService;
    private final SysMenuService menuService;
    private final SysRoleService roleService;
    private final SysUserRoleService userRoleService;
    private final SystemConfigHelper configHelper;
    private final StringRedisTemplate redisTemplate;
    private final SmsServiceFactory smsServiceFactory;
    private final LoginStrategyFactory loginStrategyFactory;

    private static final String CAPTCHA_KEY = "captcha:";
    private static final String SMS_CODE_KEY = "sms:login:";

    /**
     * 获取验证码（纯 SVG 实现，不依赖 AWT 原生库，兼容 macOS/Linux/Docker）
     */
    @GetMapping("/captcha")
    public Result<Map<String, Object>> captcha() {
        String uuid = IdUtil.simpleUUID();
        String code = generateCaptchaCode(4);
        String svg = renderCaptchaSvg(code, 130, 48);
        String imageBase64 = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        redisTemplate.opsForValue().set(CAPTCHA_KEY + uuid, code.toLowerCase(), 5, TimeUnit.MINUTES);

        Map<String, Object> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("img", imageBase64);
        return Result.ok(result);
    }

    /** 生成随机验证码字符 */
    private String generateCaptchaCode(int length) {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /** 使用 SVG 渲染验证码图片（无 AWT 依赖） */
    private String renderCaptchaSvg(String code, int width, int height) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder svg = new StringBuilder();
        svg.append(String.format("<svg xmlns='http://www.w3.org/2000/svg' width='%d' height='%d'>", width, height));
        svg.append(String.format("<rect width='%d' height='%d' fill='#f0f0f0'/>", width, height));

        // 干扰线
        String[] colors = {"#ccc", "#bbb", "#ddd", "#aaa", "#c0c0c0"};
        for (int i = 0; i < 6; i++) {
            svg.append(String.format("<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='%s' stroke-width='1'/>",
                    r.nextInt(width), r.nextInt(height), r.nextInt(width), r.nextInt(height), colors[r.nextInt(colors.length)]));
        }

        // 干扰点
        for (int i = 0; i < 30; i++) {
            svg.append(String.format("<circle cx='%d' cy='%d' r='1' fill='%s'/>",
                    r.nextInt(width), r.nextInt(height), colors[r.nextInt(colors.length)]));
        }

        // 字符
        String[] textColors = {"#333", "#555", "#222", "#444", "#666"};
        int charWidth = width / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            int x = charWidth * (i + 1) - 5 + r.nextInt(6) - 3;
            int y = height / 2 + 6 + r.nextInt(8) - 4;
            int rotate = r.nextInt(30) - 15;
            int fontSize = 20 + r.nextInt(6);
            svg.append(String.format("<text x='%d' y='%d' font-size='%d' fill='%s' font-family='Arial,sans-serif' font-weight='bold' transform='rotate(%d %d %d)'>%c</text>",
                    x, y, fontSize, textColors[r.nextInt(textColors.length)], rotate, x, y, code.charAt(i)));
        }

        svg.append("</svg>");
        return svg.toString();
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@RequestBody SmsCodeRequest request) {
        String phone = request.getPhone();
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的手机号");
        }

        String limitKey = "sms:limit:" + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new BusinessException("发送太频繁，请稍后再试");
        }

        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        boolean success = smsServiceFactory.sendCode(phone, code);
        if (!success) {
            throw new BusinessException("短信发送失败，请稍后重试");
        }

        redisTemplate.opsForValue().set(SMS_CODE_KEY + phone, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS);
        return Result.ok();
    }

    /**
     * 登录（通过 pengcheng-auth 统一策略）
     */
    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginRequest request) {
        request.setClientType(ClientType.ADMIN);
        if (request.getLoginType() == null) {
            request.setLoginType(LoginType.PASSWORD);
        }
        LoginResult result = loginStrategyFactory.login(request);
        return Result.ok(result);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getDetail(userId);
        List<String> roles = userService.getRoleCodes(userId);
        List<String> permissions = userService.getPermissions(userId);
        List<SysMenu> menus = menuService.getUserMenuTree(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roles", roles);
        result.put("permissions", permissions);
        result.put("menus", menus);
        return Result.ok(result);
    }

    /**
     * 获取个人信息
     */
    @GetMapping("/profile")
    public Result<SysUser> profile() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.getDetail(userId));
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody SysUser user) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updateProfile(userId, user);
        return Result.ok();
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    public Result<Void> updatePassword(@RequestBody PasswordRequest request) {
        configHelper.validatePassword(request.getNewPassword());
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updatePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterRequest request) {
        if (!configHelper.isRegisterEnabled()) {
            throw new BusinessException("系统暂未开放注册");
        }

        if (configHelper.isCaptchaEnabled()) {
            if (request.getUuid() == null || request.getCode() == null) {
                throw new BusinessException("请输入验证码");
            }
            String cacheCode = redisTemplate.opsForValue().get(CAPTCHA_KEY + request.getUuid());
            redisTemplate.delete(CAPTCHA_KEY + request.getUuid());
            if (cacheCode == null || !cacheCode.equalsIgnoreCase(request.getCode())) {
                throw new BusinessException("验证码错误或已过期");
            }
        }

        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        if (!request.getUsername().matches("^[a-zA-Z0-9_]{4,20}$")) {
            throw new BusinessException("用户名只能包含字母、数字、下划线，长度4-20位");
        }

        SysUser existUser = userService.getByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        configHelper.validatePassword(request.getPassword());

        if (configHelper.isRegisterVerifyEmail()) {
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new BusinessException("请输入邮箱");
            }
        }
        if (configHelper.isRegisterVerifyPhone()) {
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                throw new BusinessException("请输入手机号");
            }
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setGender(0);
        user.setUserType("admin");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(configHelper.isRegisterNeedAudit() ? 2 : 1);

        userService.save(user);

        String defaultRoleCode = configHelper.getRegisterDefaultRole();
        if (defaultRoleCode != null && !defaultRoleCode.isEmpty()) {
            SysRole role = roleService.getByCode(defaultRoleCode);
            if (role != null) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(role.getId());
                userRoleService.save(userRole);
            }
        }

        if (configHelper.isRegisterNeedAudit()) {
            Result<String> result = new Result<>();
            result.setCode(200);
            result.setMessage("注册成功，请等待管理员审核通过后再登录");
            result.setData("needAudit");
            return result;
        }
        return Result.ok();
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String nickname;
        private String email;
        private String phone;
        private String uuid;
        private String code;
    }

    @Data
    public static class PasswordRequest {
        private String oldPassword;
        private String newPassword;
    }

    @Data
    public static class SmsCodeRequest {
        private String phone;
    }
}
