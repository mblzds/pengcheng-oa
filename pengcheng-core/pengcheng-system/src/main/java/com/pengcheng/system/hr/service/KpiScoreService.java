package com.pengcheng.system.hr.service;

import com.pengcheng.system.hr.entity.KpiScore;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 考核记录服务
 */
public interface KpiScoreService {

    KpiScore getById(Long id);

    List<KpiScore> listByPeriodAndUser(Long periodId, Long userId);

    List<Map<String, Object>> listScoresByPeriod(Long periodId);

    /** 按周期+人汇总加权得分 */
    BigDecimal sumWeightedScore(Long periodId, Long userId);

    void saveOrUpdate(KpiScore score);

    void batchFill(Long periodId, Long userId, List<KpiScore> scores);

    /** 按 data_source 自动拉取实际值并计算得分（留桩） */
    void autoFillByDataSource(Long periodId, Long userId);
}
