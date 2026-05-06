package com.pengcheng.system.invite.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pengcheng.system.invite.entity.OrgInvite;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织邀请服务
 */
public interface OrgInviteService extends IService<OrgInvite> {

    /**
     * 创建邀请
     */
    OrgInvite createInvite(String email, String phone, List<Long> roleIds, Long deptId, LocalDateTime expiresAt);

    /**
     * 邀请列表
     */
    List<OrgInvite> listInvites(Integer status);

    /**
     * 撤销邀请
     */
    void revokeInvite(Long id);

    /**
     * 接受邀请
     */
    OrgInvite acceptInvite(String code, Long userId);
}
