package com.pengcheng.realty.common.constants;

/**
 * 房产业务角色编码常量
 * <p>
 * 角色编码与 sys_role 表中的 code 字段对应。
 * 数据权限映射规则：
 * <ul>
 *   <li>驻场(resident) → 仅负责项目的客户</li>
 *   <li>渠道(channel) → 仅对接联盟商的客户</li>
 *   <li>驻场总监(resident_director) → 全部</li>
 *   <li>渠道总监(channel_director) → 全部</li>
 *   <li>行政总监(admin_director) → 全部</li>
 *   <li>行政文员(admin_clerk) → 全部</li>
 *   <li>联盟商负责人(alliance_manager) → 仅本联盟商数据</li>
 * </ul>
 */
public final class RealtyRoleConstants {

    private RealtyRoleConstants() {
    }

    /** 驻场 */
    public static final String RESIDENT = "resident";

    /** 渠道 */
    public static final String CHANNEL = "channel";

    /** 驻场总监 */
    public static final String RESIDENT_DIRECTOR = "resident_director";

    /** 渠道总监 */
    public static final String CHANNEL_DIRECTOR = "channel_director";

    /** 行政总监 */
    public static final String ADMIN_DIRECTOR = "admin_director";

    /** 行政文员 */
    public static final String ADMIN_CLERK = "admin_clerk";

    /** 联盟商负责人 */
    public static final String ALLIANCE_MANAGER = "alliance_manager";
}
