package com.pengcheng.hr.performance.provider;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * KPI 实际值数据源提供者（各业务模块实现，供绩效模块按 data_source 自动拉数）
 * 如：auto_commission、auto_attendance、auto_quality
 */
public interface KpiDataSourceProvider {

    /** 支持的数据来源标识，与 hr_kpi_template.data_source 对应 */
    String getDataSource();

    /**
     * 按周期时间范围与用户返回该数据源下的实际值（用于建议填充）
     *
     * @param startDate 周期开始日期
     * @param endDate   周期结束日期
     * @param userId    被考核人
     * @param templateCode 指标编码（可选，用于同数据源多指标区分）
     * @return 实际值，无法提供时返回 null
     */
    BigDecimal getActualValue(LocalDate startDate, LocalDate endDate, Long userId, String templateCode);
}
