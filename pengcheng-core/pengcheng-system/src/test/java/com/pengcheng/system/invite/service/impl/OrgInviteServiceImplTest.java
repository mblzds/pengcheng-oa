package com.pengcheng.system.invite.service.impl;

import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.entity.SysDept;
import com.pengcheng.system.entity.SysRole;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.entity.SysUserRole;
import com.pengcheng.system.invite.entity.OrgInvite;
import com.pengcheng.system.invite.mapper.OrgInviteMapper;
import com.pengcheng.system.invite.support.OrgInviteStatus;
import com.pengcheng.system.mapper.SysUserRoleMapper;
import com.pengcheng.system.service.SysDeptService;
import com.pengcheng.system.service.SysRoleService;
import com.pengcheng.system.service.SysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrgInviteServiceImpl")
class OrgInviteServiceImplTest {

    @Mock
    private OrgInviteMapper inviteMapper;

    @Mock
    private SysDeptService deptService;

    @Mock
    private SysRoleService roleService;

    @Mock
    private SysUserService userService;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    private OrgInviteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new OrgInviteServiceImpl(deptService, roleService, userService, userRoleMapper));
        ReflectionTestUtils.setField(service, "baseMapper", inviteMapper);
    }

    @Test
    @DisplayName("createInvite: 候选 code 冲突时继续生成直到唯一")
    void createInvite_generatesUniqueCodeWhenCandidateCollides() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 25, 10, 0);
        when(deptService.getById(9L)).thenReturn(dept(9L));
        when(roleService.listByIds(List.of(11L, 22L))).thenReturn(List.of(role(11L), role(22L)));
        doReturn(now).when(service).currentTime();
        doReturn("DUPLICATE01", "UNIQUE999").when(service).nextInviteCodeCandidate();
        doReturn(true, false).when(service).inviteCodeExists(anyString());
        when(inviteMapper.insert(any(OrgInvite.class))).thenAnswer(invocation -> {
            OrgInvite invite = invocation.getArgument(0);
            invite.setId(1L);
            return 1;
        });

        OrgInvite created = service.createInvite("agent@pengcheng.com", null, List.of(11L, 22L), 9L, null);

        assertThat(created.getInviteCode()).isEqualTo("UNIQUE999");
        assertThat(created.getRoleIds()).isEqualTo("11,22");
        assertThat(created.getStatus()).isEqualTo(OrgInviteStatus.PENDING);
        assertThat(created.getExpiresAt()).isEqualTo(now.plusDays(7));
        verify(service, times(2)).nextInviteCodeCandidate();
        verify(inviteMapper).insert(any(OrgInvite.class));
    }

    @Test
    @DisplayName("acceptInvite: 已过期邀请不可接受")
    void acceptInvite_rejectsExpiredInvite() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 25, 12, 0);
        OrgInvite invite = invite("EXPIRED01", OrgInviteStatus.PENDING, now.minusMinutes(1), 9L, "11,22");
        when(userService.getById(100L)).thenReturn(user(100L, 1L));
        doReturn(now).when(service).currentTime();
        doReturn(invite).when(service).getByInviteCode("EXPIRED01");

        assertThatThrownBy(() -> service.acceptInvite("EXPIRED01", 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("过期");

        assertThat(invite.getStatus()).isEqualTo(OrgInviteStatus.EXPIRED);
        verify(inviteMapper).updateById(invite);
        verify(userService, never()).updateById(any(SysUser.class));
        verify(userRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    @DisplayName("acceptInvite: 已撤销邀请不可接受")
    void acceptInvite_rejectsRevokedInvite() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 25, 12, 0);
        OrgInvite invite = invite("REVOKED01", OrgInviteStatus.REVOKED, now.plusDays(1), 9L, "11,22");
        when(userService.getById(100L)).thenReturn(user(100L, 1L));
        doReturn(invite).when(service).getByInviteCode("REVOKED01");

        assertThatThrownBy(() -> service.acceptInvite("REVOKED01", 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("撤销");

        verify(inviteMapper, never()).updateById(any(OrgInvite.class));
        verify(userService, never()).updateById(any(SysUser.class));
        verify(userRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    @DisplayName("acceptInvite: 接受后写入 accepted_user/status 并绑定部门和缺失角色")
    void acceptInvite_marksAcceptedAndBindsDeptAndRoles() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 25, 12, 0);
        OrgInvite invite = invite("ACCEPT01", OrgInviteStatus.PENDING, now.plusDays(1), 9L, "11,22");
        SysUser user = user(100L, 1L);
        when(deptService.getById(9L)).thenReturn(dept(9L));
        when(roleService.listByIds(List.of(11L, 22L))).thenReturn(List.of(role(11L), role(22L)));
        when(userService.getById(100L)).thenReturn(user);
        when(userService.updateById(any(SysUser.class))).thenReturn(true);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(100L, 11L)));
        when(userRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);
        doReturn(now).when(service).currentTime();
        doReturn(invite).when(service).getByInviteCode("ACCEPT01");

        OrgInvite accepted = service.acceptInvite("ACCEPT01", 100L);

        assertThat(accepted.getStatus()).isEqualTo(OrgInviteStatus.ACCEPTED);
        assertThat(accepted.getAcceptedUserId()).isEqualTo(100L);
        assertThat(accepted.getAcceptedAt()).isEqualTo(now);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateById(userCaptor.capture());
        assertThat(userCaptor.getValue().getId()).isEqualTo(100L);
        assertThat(userCaptor.getValue().getDeptId()).isEqualTo(9L);

        ArgumentCaptor<SysUserRole> roleCaptor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(userRoleMapper).insert(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getUserId()).isEqualTo(100L);
        assertThat(roleCaptor.getValue().getRoleId()).isEqualTo(22L);
        verify(inviteMapper).updateById(invite);
    }

    private static OrgInvite invite(String code, int status, LocalDateTime expiresAt, Long deptId, String roleIds) {
        OrgInvite invite = new OrgInvite();
        invite.setId(1L);
        invite.setInviteCode(code);
        invite.setStatus(status);
        invite.setExpiresAt(expiresAt);
        invite.setDeptId(deptId);
        invite.setRoleIds(roleIds);
        return invite;
    }

    private static SysDept dept(Long id) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setDeptName("企业销售部");
        return dept;
    }

    private static SysRole role(Long id) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setName("渠道经理");
        role.setCode("role-" + id);
        return role;
    }

    private static SysUser user(Long id, Long deptId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setDeptId(deptId);
        return user;
    }

    private static SysUserRole userRole(Long userId, Long roleId) {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }
}
