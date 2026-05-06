package com.pengcheng.hr.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 考核周期实体（公司级，月/季/年）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_kpi_period")
public class KpiPeriod extends BaseEntity {

    /** 周期类型：1-月度 2-季度 3-年度 */
    public static final int TYPE_MONTH = 1;
    public static final int TYPE_QUARTER = 2;
    public static final int TYPE_YEAR = 3;

    /** 状态：1-未开始 2-考核中 3-已结束 */
    public static final int STATUS_NOT_STARTED = 1;
    public static final int STATUS_IN_PROGRESS = 2;
    public static final int STATUS_ENDED = 3;

    private String name;
    private Integer periodType;
    private Integer year;
    private Integer quarter;
    private Integer month;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer status;
}
