package com.pengcheng.system.invite.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.config.StpInterfaceImpl;
import com.pengcheng.system.entity.SysRole;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.entity.SysUserRole;
import com.pengcheng.system.invite.entity.OrgInvite;
import com.pengcheng.system.invite.mapper.OrgInviteMapper;
import com.pengcheng.system.invite.service.OrgInviteService;
import com.pengcheng.system.invite.support.OrgInviteStatus;
import com.pengcheng.system.mapper.SysUserRoleMapper;
import com.pengcheng.system.service.SysDeptService;
import com.pengcheng.system.service.SysRoleService;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组织邀请服务实现
 */
@Service
@RequiredArgsConstructor
public class OrgInviteServiceImpl extends ServiceImpl<OrgInviteMapper, OrgInvite> implements OrgInviteService {

    private static final int DEFAULT_EXPIRE_DAYS = 7;
    private static final int MAX_CODE_GENERATE_ATTEMPTS = 10;

    private final SysDeptService deptService;
    private final SysRoleService roleService;
    private final SysUserService userService;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrgInvite createInvite(String email, String phone, List<Long> roleIds, Long deptId, LocalDateTime expiresAt) {
        if (!StringUtils.hasText(email) && !StringUtils.hasText(phone)) {
            throw new BusinessException("邮箱或手机号至少填写一项");
        }
        validateDept(deptId);
        List<Long> normalizedRoleIds = validateAndNormalizeRoleIds(roleIds);

        OrgInvite invite = new OrgInvite();
        invite.setInviteCode(generateUniqueInviteCode());
        invite.setEmail(normalizeText(email));
        invite.setPhone(normalizeText(phone));
        invite.setRoleIds(joinRoleIds(normalizedRoleIds));
        invite.setRoleIdList(normalizedRoleIds);
        invite.setDeptId(deptId);
        invite.setStatus(OrgInviteStatus.PENDING);
        invite.setExpiresAt(expiresAt != null ? expiresAt : currentTime().plusDays(DEFAULT_EXPIRE_DAYS));
        this.save(invite);
        return hydrateInvite(invite);
    }

    @Override
    public List<OrgInvite> listInvites(Integer status) {
        LocalDateTime now = currentTime();
        LambdaQueryWrapper<OrgInvite> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            if (status == OrgInviteStatus.EXPIRED) {
                wrapper.and(q -> q.eq(OrgInvite::getStatus, OrgInviteStatus.EXPIRED)
                        .or()
                        .eq(OrgInvite::getStatus, OrgInviteStatus.PENDING)
                        .lt(OrgInvite::getExpiresAt, now));
            } else {
                wrapper.eq(OrgInvite::getStatus, status);
            }
        }
        wrapper.orderByDesc(OrgInvite::getCreateTime).orderByDesc(OrgInvite::getId);
        return this.list(wrapper).stream().map(this::hydrateInvite).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeInvite(Long id) {
        OrgInvite invite = this.getById(id);
        if (invite == null) {
            throw new BusinessException("邀请不存在");
        }
        if (OrgInviteStatus.ACCEPTED == invite.getStatus()) {
            throw new BusinessException("邀请已被接受，无法撤销");
        }
        if (OrgInviteStatus.REVOKED == invite.getStatus()) {
            return;
        }
        invite.setStatus(OrgInviteStatus.REVOKED);
        this.updateById(invite);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrgInvite acceptInvite(String code, Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        OrgInvite invite = getByInviteCode(code);
        if (invite == null) {
            throw new BusinessException("邀请不存在");
        }
        if (OrgInviteStatus.ACCEPTED == invite.getStatus()) {
            throw new BusinessException("邀请已被使用");
        }
        if (OrgInviteStatus.REVOKED == invite.getStatus()) {
            throw new BusinessException("邀请已撤销");
        }
        if (OrgInviteStatus.EXPIRED == invite.getStatus()) {
            throw new BusinessException("邀请已过期");
        }

        LocalDateTime now = currentTime();
        if (invite.getExpiresAt() != null && !invite.getExpiresAt().isAfter(now)) {
            invite.setStatus(OrgInviteStatus.EXPIRED);
            this.updateById(invite);
            throw new BusinessException("邀请已过期");
        }

        boolean permissionChanged = false;
        if (invite.getDeptId() != null && !invite.getDeptId().equals(user.getDeptId())) {
            validateDept(invite.getDeptId());
            user.setDeptId(invite.getDeptId());
            if (!userService.updateById(user)) {
                throw new BusinessException("绑定用户部门失败");
            }
            permissionChanged = true;
        }

        List<Long> inviteRoleIds = validateAndNormalizeRoleIds(parseRoleIds(invite.getRoleIds()));
        if (!inviteRoleIds.isEmpty()) {
            Set<Long> existingRoleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, userId))
                    .stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toSet());

            for (Long roleId : inviteRoleIds) {
                if (existingRoleIds.contains(roleId)) {
                    continue;
                }
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
                permissionChanged = true;
            }
        }

        if (permissionChanged) {
            StpInterfaceImpl.clearPermissionCache(userId);
        }

        invite.setStatus(OrgInviteStatus.ACCEPTED);
        invite.setAcceptedUserId(userId);
        invite.setAcceptedAt(now);
        this.updateById(invite);
        return hydrateInvite(invite);
    }

    protected LocalDateTime currentTime() {
        return LocalDateTime.now();
    }

    protected String nextInviteCodeCandidate() {
        return RandomUtil.randomStringUpper(10);
    }

    protected boolean inviteCodeExists(String code) {
        return this.count(new LambdaQueryWrapper<OrgInvite>().eq(OrgInvite::getInviteCode, code)) > 0;
    }

    protected OrgInvite getByInviteCode(String code) {
        return this.getOne(new LambdaQueryWrapper<OrgInvite>().eq(OrgInvite::getInviteCode, code));
    }

    private String generateUniqueInviteCode() {
        for (int i = 0; i < MAX_CODE_GENERATE_ATTEMPTS; i++) {
            String candidate = nextInviteCodeCandidate();
            if (!inviteCodeExists(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("邀请码生成失败，请稍后重试");
    }

    private void validateDept(Long deptId) {
        if (deptId != null && deptId > 0 && deptService.getById(deptId) == null) {
            throw new BusinessException("部门不存在");
        }
    }

    private List<Long> validateAndNormalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalized = new ArrayList<>(new LinkedHashSet<>(roleIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList())));
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<SysRole> roles = roleService.listByIds(normalized);
        if (roles.size() != normalized.size()) {
            throw new BusinessException("包含不存在的角色");
        }
        return normalized;
    }

    private OrgInvite hydrateInvite(OrgInvite invite) {
        invite.setRoleIdList(parseRoleIds(invite.getRoleIds()));
        if (OrgInviteStatus.PENDING == invite.getStatus()
                && invite.getExpiresAt() != null
                && !invite.getExpiresAt().isAfter(currentTime())) {
            invite.setStatus(OrgInviteStatus.EXPIRED);
        }
        return invite;
    }

    private List<Long> parseRoleIds(String roleIds) {
        if (!StringUtils.hasText(roleIds)) {
            return List.of();
        }
        return List.of(roleIds.split(",")).stream()
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private String joinRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return null;
        }
        return roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
