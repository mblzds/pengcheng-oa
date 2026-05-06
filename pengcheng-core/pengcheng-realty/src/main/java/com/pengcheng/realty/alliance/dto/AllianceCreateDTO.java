package com.pengcheng.realty.alliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 联盟商创建/编辑 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllianceCreateDTO {

    /** 联盟商ID（编辑时传入） */
    private Long id;

    /** 联盟公司名称（必填） */
    private String companyName;

    /** 办公地址（必填） */
    private String officeAddress;

    /** 负责人姓名（必填） */
    private String contactName;

    /** 联系方式（必填） */
    private String contactPhone;

    /** 人员规模（必填） */
    private Integer staffSize;

    /** 联盟商等级：1-普通 2-银牌 3-金牌 4-钻石（必填） */
    private Integer level;

    /** 对接渠道人员ID */
    private Long channelUserId;
}
