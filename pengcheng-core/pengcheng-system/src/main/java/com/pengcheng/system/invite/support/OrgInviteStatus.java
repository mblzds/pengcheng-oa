package com.pengcheng.system.invite.support;

/**
 * 组织邀请状态
 */
public final class OrgInviteStatus {

    private OrgInviteStatus() {
    }

    /**
     * 待接受
     */
    public static final int PENDING = 0;

    /**
     * 已接受
     */
    public static final int ACCEPTED = 1;

    /**
     * 已撤销
     */
    public static final int REVOKED = 2;

    /**
     * 已过期
     */
    public static final int EXPIRED = 3;
}
