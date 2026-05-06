package com.pengcheng.hr.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.performance.entity.KpiScore;
import com.pengcheng.hr.performance.entity.KpiTemplate;
import com.pengcheng.hr.performance.mapper.KpiScoreMapper;
import com.pengcheng.hr.performance.mapper.KpiTemplateMapper;
import com.pengcheng.hr.performance.service.KpiScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KpiScoreServiceImpl implements KpiScoreService {

    private final KpiScoreMapper kpiScoreMapper;
    private final KpiTemplateMapper kpiTemplateMapper;

    @Override
    public List<KpiScore> listByPeriodAndUser(Long periodId, Long userId) {
        LambdaQueryWrapper<KpiScore> q = new LambdaQueryWrapper<>();
        q.eq(KpiScore::getPeriodId, periodId).eq(KpiScore::getUserId, userId);
        return kpiScoreMapper.selectList(q);
    }

    @Override
    public KpiScore getOrCreate(Long periodId, Long userId, Long templateId) {
        LambdaQueryWrapper<KpiScore> q = new LambdaQueryWrapper<>();
        q.eq(KpiScore::getPeriodId, periodId).eq(KpiScore::getUserId, userId).eq(KpiScore::getTemplateId, templateId);
        KpiScore one = kpiScoreMapper.selectOne(q);
        if (one != null) return one;
        KpiScore score = KpiScore.builder().periodId(periodId).userId(userId).templateId(templateId).build();
        kpiScoreMapper.insert(score);
        return score;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(KpiScore score) {
        if (score == null || score.getPeriodId() == null || score.getUserId() == null || score.getTemplateId() == null) {
            throw new IllegalArgumentException("周期、用户、指标不能为空");
        }
        if (score.getWeightedScore() == null && score.getScore() != null) {
            KpiTemplate t = kpiTemplateMapper.selectById(score.getTemplateId());
            if (t != null && t.getWeight() != null) {
                score.setWeightedScore(score.getScore().multiply(t.getWeight()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
        }
        LambdaQueryWrapper<KpiScore> q = new LambdaQueryWrapper<>();
        q.eq(KpiScore::getPeriodId, score.getPeriodId()).eq(KpiScore::getUserId, score.getUserId()).eq(KpiScore::getTemplateId, score.getTemplateId());
        KpiScore existing = kpiScoreMapper.selectOne(q);
        if (existing != null) {
            score.setId(existing.getId());
            kpiScoreMapper.updateById(score);
        } else {
            kpiScoreMapper.insert(score);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(Long periodId, Long userId, List<KpiScore> scores) {
        if (scores == null) return;
        for (KpiScore s : scores) {
            s.setPeriodId(periodId);
            s.setUserId(userId);
            saveOrUpdate(s);
        }
    }
}
