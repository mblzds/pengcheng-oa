package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.RepeatSubmit;
import com.pengcheng.system.annotation.Log.BusinessType;
import com.pengcheng.system.invite.entity.OrgInvite;
import com.pengcheng.system.invite.service.OrgInviteService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织邀请控制器
 */
@RestController
@RequestMapping("/sys/org/invite")
@RequiredArgsConstructor
public class OrgInviteController {

    private final OrgInviteService orgInviteService;

    @GetMapping("/list")
    @SaCheckPermission("sys:dept:list")
    public Result<List<OrgInvite>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(orgInviteService.listInvites(status));
    }

    @PostMapping
    @SaCheckPermission("sys:dept:edit")
    @RepeatSubmit
    @Log(title = "组织邀请", businessType = BusinessType.INSERT)
    public Result<OrgInvite> create(@RequestBody CreateInviteRequest request) {
        return Result.ok(orgInviteService.createInvite(
                request.getEmail(),
                request.getPhone(),
                request.getRoleIds(),
                request.getDeptId(),
                request.getExpiresAt()
        ));
    }

    @PostMapping("/{id}/revoke")
    @SaCheckPermission("sys:dept:edit")
    @Log(title = "组织邀请", businessType = BusinessType.UPDATE)
    public Result<Void> revoke(@PathVariable Long id) {
        orgInviteService.revokeInvite(id);
        return Result.ok();
    }

    @PostMapping("/accept")
    @RepeatSubmit
    @Log(title = "组织邀请", businessType = BusinessType.UPDATE)
    public Result<OrgInvite> accept(@RequestBody AcceptInviteRequest request) {
        return Result.ok(orgInviteService.acceptInvite(request.getCode(), StpUtil.getLoginIdAsLong()));
    }

    @Data
    public static class CreateInviteRequest {
        private String email;
        private String phone;
        private List<Long> roleIds;
        private Long deptId;
        private LocalDateTime expiresAt;
    }

    @Data
    public static class AcceptInviteRequest {
        private String code;
    }
}
