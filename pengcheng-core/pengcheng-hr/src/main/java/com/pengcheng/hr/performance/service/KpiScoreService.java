package com.pengcheng.hr.performance.service;

import com.pengcheng.hr.performance.entity.KpiScore;

import java.util.List;

/**
 * 考核记录服务（公司级）
 */
public interface KpiScoreService {

    List<KpiScore> listByPeriodAndUser(Long periodId, Long userId);

    KpiScore getOrCreate(Long periodId, Long userId, Long templateId);

    void saveOrUpdate(KpiScore score);

    void saveBatch(Long periodId, Long userId, List<KpiScore> scores);
}
