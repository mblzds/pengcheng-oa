package com.pengcheng.realty.dashboard.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 转化漏斗查询 DTO
 */
@Data
public class FunnelQueryDTO {

    /** 项目ID（可选筛选） */
    private Long projectId;

    /** 联盟商ID（可选筛选） */
    private Long allianceId;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;
}
