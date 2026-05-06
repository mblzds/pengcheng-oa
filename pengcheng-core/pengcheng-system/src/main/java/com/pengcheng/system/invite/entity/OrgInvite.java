package com.pengcheng.system.invite.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织邀请
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org_invite")
public class OrgInvite extends BaseEntity {

    /**
     * 邀请码
     */
    private String inviteCode;

    /**
     * 被邀请邮箱
     */
    private String email;

    /**
     * 被邀请手机号
     */
    private String phone;

    /**
     * 角色 ID CSV
     */
    private String roleIds;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 接受人 ID
     */
    private Long acceptedUserId;

    /**
     * 接受时间
     */
    private LocalDateTime acceptedAt;

    /**
     * 角色 ID 列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Long> roleIdList;
}
