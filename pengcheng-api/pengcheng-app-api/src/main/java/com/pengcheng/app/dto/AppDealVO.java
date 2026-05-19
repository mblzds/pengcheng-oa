package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 小程序工作台「成交明细」列表项 VO。
 * 数据权限继承客户列表：仅展示当前用户可见客户的成交记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppDealVO {

    /** 成交记录 ID */
    private Long dealId;

    // ===== 客户信息 =====
    private Long customerId;
    private String customerName;
    /** 脱敏手机号（前 3 后 4），列表展示用 */
    private String phoneMasked;

    // ===== 项目信息 =====
    /** 关联项目 ID（取 customer_project 中第一个） */
    private Long projectId;
    private String projectName;

    // ===== 中介信息 =====
    /** 联盟商（带看公司）ID */
    private Long allianceId;
    /** 联盟商公司名 */
    private String allianceName;
    /** 经纪人姓名（来自 customer.agent_name） */
    private String agentName;
    /** 经纪人电话（来自 customer.agent_phone） */
    private String agentPhone;

    // ===== 成交字段 =====
    private String roomNo;
    private BigDecimal dealAmount;
    private LocalDateTime dealTime;
    /** 1-小订 2-大定 */
    private Integer subscribeType;
    /** 1-已签约 2-未签约 */
    private Integer signStatus;
    /** 0-未回款 1-部分回款 2-全部回款 */
    private Integer paymentStatus;
}
