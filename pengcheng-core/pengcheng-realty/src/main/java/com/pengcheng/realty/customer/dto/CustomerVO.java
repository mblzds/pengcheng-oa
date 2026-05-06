package com.pengcheng.realty.customer.dto;

import com.pengcheng.realty.common.util.PhoneMaskUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户展示 VO
 * <p>
 * 手机号字段自动脱敏展示（前3位 + **** + 后4位）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVO {

    private Long id;

    /**
     * 报备编号
     */
    private String reportNo;

    /**
     * 客户姓氏
     */
    private String customerName;

    /**
     * 脱敏手机号（前3后4）
     */
    private String phoneMasked;

    /**
     * 带看人数
     */
    private Integer visitCount;

    /**
     * 带看时间
     */
    private LocalDateTime visitTime;

    /**
     * 带看公司（联盟商ID）
     */
    private Long allianceId;

    /**
     * 带看公司名称
     */
    private String allianceName;

    /**
     * 经纪人姓名
     */
    private String agentName;

    /**
     * 经纪人联系方式（脱敏）
     */
    private String agentPhoneMasked;

    /**
     * 状态：1-已报备 2-已到访 3-已成交
     */
    private Integer status;

    /**
     * 池类型：1-公海 2-私海
     */
    private Integer poolType;

    /**
     * 保护期到期时间
     */
    private LocalDateTime protectionExpireTime;

    /**
     * 最后跟进时间
     */
    private LocalDateTime lastFollowTime;

    /**
     * 成交概率评分（0-1）
     */
    private BigDecimal dealProbability;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 从 Customer 实体构建 VO，自动对手机号进行脱敏
     */
    public static CustomerVO fromEntity(com.pengcheng.realty.customer.entity.Customer customer) {
        if (customer == null) {
            return null;
        }
        return CustomerVO.builder()
                .id(customer.getId())
                .reportNo(customer.getReportNo())
                .customerName(customer.getCustomerName())
                .phoneMasked(PhoneMaskUtil.mask(customer.getPhone()))
                .visitCount(customer.getVisitCount())
                .visitTime(customer.getVisitTime())
                .allianceId(customer.getAllianceId())
                .agentName(customer.getAgentName())
                .agentPhoneMasked(PhoneMaskUtil.mask(customer.getAgentPhone()))
                .status(customer.getStatus())
                .poolType(customer.getPoolType())
                .protectionExpireTime(customer.getProtectionExpireTime())
                .lastFollowTime(customer.getLastFollowTime())
                .dealProbability(customer.getDealProbability())
                .createTime(customer.getCreateTime())
                .build();
    }
}
