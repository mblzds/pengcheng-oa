package com.pengcheng.hr.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * KPI 指标模板（公司级，数据来源可插拔：手工或各业务模块）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_kpi_template")
public class KpiTemplate extends BaseEntity {

    /** 指标分类：1-业绩 2-考勤 3-过程质量 4-综合 5-其他 */
    public static final int CATEGORY_PERFORMANCE = 1;
    public static final int CATEGORY_ATTENDANCE = 2;
    public static final int CATEGORY_QUALITY = 3;
    public static final int CATEGORY_COMPREHENSIVE = 4;
    public static final int CATEGORY_OTHER = 5;

    /** 数据来源：manual 手工；auto_* 由各业务模块对接 */
    public static final String SOURCE_MANUAL = "manual";

    private String name;
    private String code;
    private Integer category;
    private BigDecimal weight;
    private String dataSource;
    private String formula;
    private Integer sortOrder;
    private Integer status;
}
