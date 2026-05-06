package com.pengcheng.realty.customer.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import com.pengcheng.realty.common.handler.PhoneEncryptTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "customer", autoResultMap = true)
public class Customer extends BaseEntity {

    /**
     * 报备编号
     */
    private String reportNo;

    /**
     * 客户姓氏
     */
    private String customerName;

    /**
     * 联系方式（AES加密存储，通过 TypeHandler 透明加解密）
     */
    @TableField(typeHandler = PhoneEncryptTypeHandler.class)
    private String phone;

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
     * 经纪人姓名
     */
    private String agentName;

    /**
     * 经纪人联系方式
     */
    private String agentPhone;

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
     * 录入驻场人员ID
     */
    private Long creatorId;
}
