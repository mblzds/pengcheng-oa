package com.pengcheng.realty.alliance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 联盟商实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("alliance")
public class Alliance extends BaseEntity {

    /**
     * 联盟公司名称
     */
    private String companyName;

    /**
     * 办公地址
     */
    private String officeAddress;

    /**
     * 负责人姓名
     */
    private String contactName;

    /**
     * 联系方式
     */
    private String contactPhone;

    /**
     * 人员规模
     */
    private Integer staffSize;

    /**
     * 联盟商等级：1-普通 2-银牌 3-金牌 4-钻石
     */
    private Integer level;

    /**
     * 状态：1-启用 0-停用
     */
    private Integer status;

    /**
     * 关联系统账号ID
     */
    private Long userId;

    /**
     * 对接渠道人员ID
     */
    private Long channelUserId;
}
