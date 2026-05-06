package com.pengcheng.hr.performance.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 考核实际值建议服务：按 data_source 从各业务模块拉取建议值
 */
public interface KpiSuggestService {

    /**
     * 为指定周期、用户生成各指标的建议实际值（仅对 data_source 为 auto_* 的模板拉数）
     *
     * @param periodId 考核周期 ID
     * @param userId   被考核人
     * @return templateId -> 建议 actualValue，无建议的模板不包含在 map 中
     */
    Map<Long, BigDecimal> suggestActualValues(Long periodId, Long userId);
}
