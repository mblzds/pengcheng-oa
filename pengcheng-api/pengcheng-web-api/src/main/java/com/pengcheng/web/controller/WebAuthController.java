package com.pengcheng.web.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.auth.LoginRequest;
import com.pengcheng.auth.LoginResult;
import com.pengcheng.auth.LoginStrategyFactory;
import com.pengcheng.auth.enums.ClientType;
import com.pengcheng.auth.enums.LoginType;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PC端认证控制器
 * 支持密码登录、短信登录、三方登录
 */
@RestController
@RequestMapping("/web/auth")
@RequiredArgsConstructor
public class WebAuthController {

    private final LoginStrategyFactory loginStrategyFactory;
    private final SysUserService userService;

    /**
     * 统一登录接口
     *
     * @param request loginType: password / sms / social
     */
    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginRequest request) {
        request.setClientType(ClientType.WEB);
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

        Map<String, Object> result = new HashMap<>();
        user.setPassword(null);
        result.put("user", user);
        result.put("roles", roles);
        result.put("permissions", permissions);
        return Result.ok(result);
    }

    /**
     * 获取支持的登录方式
     */
    @GetMapping("/login-types")
    public Result<?> loginTypes() {
        return Result.ok(loginStrategyFactory.getRegisteredTypes());
    }
}
