package com.pengcheng.realty.dashboard.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 排行榜查询 DTO
 */
@Data
public class RankingQueryDTO {

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;
}
