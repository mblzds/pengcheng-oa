package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.crypto.EncryptResponse;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.RepeatSubmit;
import com.pengcheng.system.annotation.Log.BusinessType;
import com.pengcheng.system.entity.SysRole;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.service.SysRoleService;
import com.pengcheng.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理", description = "系统用户 CRUD、角色分配、岗位分配等操作")
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;
    private final SysRoleService roleService;

    /**
     * 分页查询
     */
    @Operation(summary = "分页查询用户", description = "支持按用户名、状态、用户类型、部门、岗位筛选")
    @GetMapping("/page")
    @SaCheckPermission("sys:user:list")
    @EncryptResponse
    public Result<PageResult<SysUser>> page(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "状态 (0-禁用 1-启用)") @RequestParam(required = false) Integer status,
            @Parameter(description = "用户类型") @RequestParam(required = false) String userType,
            @Parameter(description = "部门 ID") @RequestParam(required = false) Long deptId,
            @Parameter(description = "岗位 ID") @RequestParam(required = false) Long postId) {
        return Result.ok(userService.page(page, pageSize, username, status, userType, deptId, postId));
    }

    /**
     * 获取详情
     */
    @Operation(summary = "获取用户详情", description = "包含用户基本信息、角色列表、岗位列表")
    @GetMapping("/{id}")
    @SaCheckPermission("sys:user:list")
    public Result<Map<String, Object>> detail(
            @Parameter(description = "用户 ID") @PathVariable Long id) {
        SysUser user = userService.getDetail(id);
        List<SysRole> roles = roleService.listByUserId(id);
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());

        // 获取岗位关联
        List<Long> postIds = userService.getPostIds(id);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roleIds", roleIds);
        result.put("postIds", postIds);
        return Result.ok(result);
    }

    /**
     * 创建
     */
    @PostMapping
    @SaCheckPermission("sys:user:add")
    @RepeatSubmit
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    public Result<Void> create(@RequestBody UserRequest request) {
        userService.create(request.getUser(), request.getRoleIds(), request.getPostIds());
        return Result.ok();
    }

    /**
     * 更新
     */
    @PutMapping
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody UserRequest request) {
        userService.update(request.getUser(), request.getRoleIds(), request.getPostIds());
        return Result.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:user:delete")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    /**
     * 重置密码
     */
    @PostMapping("/{id}/reset-password")
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.ok();
    }

    /**
     * 切换离职状态
     */
    @PostMapping("/{id}/quit")
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<Void> toggleQuit(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setIsQuit(user.getIsQuit() == 1 ? 0 : 1);
        userService.updateById(user);
        return Result.ok();
    }

    /**
     * 审核通过
     */
    @PostMapping("/{id}/approve")
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<Void> approve(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() != 2) {
            throw new BusinessException("该用户不在待审核状态");
        }
        user.setStatus(1);
        userService.updateById(user);
        return Result.ok();
    }

    /**
     * 审核拒绝
     */
    @PostMapping("/{id}/reject")
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<Void> reject(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() != 2) {
            throw new BusinessException("该用户不在待审核状态");
        }
        user.setStatus(3);
        userService.updateById(user);
        return Result.ok();
    }

    @Data
    public static class UserRequest {
        private SysUser user;
        private List<Long> roleIds;
        private List<Long> postIds;
    }
}
