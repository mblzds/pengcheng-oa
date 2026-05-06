package com.pengcheng.realty.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户报备创建 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreateDTO {

    /** 带看项目ID列表（必填，支持多选） */
    private List<Long> projectIds;

    /** 客户姓氏（必填） */
    private String customerName;

    /** 联系方式/手机号（必填） */
    private String phone;

    /** 带看人数（必填） */
    private Integer visitCount;

    /** 带看时间（必填） */
    private LocalDateTime visitTime;

    /** 带看公司/联盟商ID（必填） */
    private Long allianceId;

    /** 经纪人姓名（必填） */
    private String agentName;

    /** 经纪人联系方式（必填） */
    private String agentPhone;
}
