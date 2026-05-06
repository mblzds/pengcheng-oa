package com.pengcheng.system.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.hr.entity.KpiPeriod;
import com.pengcheng.system.hr.entity.KpiScore;
import com.pengcheng.system.hr.entity.KpiTemplate;
import com.pengcheng.system.hr.mapper.KpiPeriodMapper;
import com.pengcheng.system.hr.mapper.KpiScoreMapper;
import com.pengcheng.system.hr.mapper.KpiTemplateMapper;
import com.pengcheng.system.hr.service.KpiScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 考核记录服务实现
 */
@Slf4j
@RequiredArgsConstructor
public class KpiScoreServiceImpl implements KpiScoreService {

    private final KpiScoreMapper scoreMapper;
    private final KpiTemplateMapper templateMapper;
    private final KpiPeriodMapper periodMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public KpiScore getById(Long id) {
        return scoreMapper.selectById(id);
    }

    @Override
    public List<KpiScore> listByPeriodAndUser(Long periodId, Long userId) {
        return scoreMapper.selectList(
                new LambdaQueryWrapper<KpiScore>()
                        .eq(KpiScore::getPeriodId, periodId)
                        .eq(KpiScore::getUserId, userId)
                        .orderByAsc(KpiScore::getTemplateId));
    }

    @Override
    public List<Map<String, Object>> listScoresByPeriod(Long periodId) {
        List<KpiScore> list = scoreMapper.selectList(
                new LambdaQueryWrapper<KpiScore>().eq(KpiScore::getPeriodId, periodId));
        Map<Long, Map<String, Object>> userMap = new LinkedHashMap<>();
        for (KpiScore s : list) {
            userMap.computeIfAbsent(s.getUserId(), k -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("userId", s.getUserId());
                row.put("totalWeighted", sumWeightedScore(periodId, s.getUserId()));
                row.put("scores", new ArrayList<KpiScore>());
                return row;
            });
            @SuppressWarnings("unchecked")
            List<KpiScore> scores = (List<KpiScore>) userMap.get(s.getUserId()).get("scores");
            scores.add(s);
        }
        return new ArrayList<>(userMap.values());
    }

    @Override
    public BigDecimal sumWeightedScore(Long periodId, Long userId) {
        List<KpiScore> list = listByPeriodAndUser(periodId, userId);
        return list.stream()
                .map(KpiScore::getWeightedScore)
                .filter(w -> w != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(KpiScore score) {
        if (score.getScore() != null && score.getTemplateId() != null) {
            KpiTemplate t = templateMapper.selectById(score.getTemplateId());
            if (t != null && t.getWeight() != null) {
                score.setWeightedScore(score.getScore().multiply(t.getWeight()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
        }
        if (score.getId() != null) {
            scoreMapper.updateById(score);
        } else {
            scoreMapper.insert(score);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchFill(Long periodId, Long userId, List<KpiScore> scores) {
        for (KpiScore s : scores) {
            s.setPeriodId(periodId);
            s.setUserId(userId);
            KpiScore existing = scoreMapper.selectOne(
                    new LambdaQueryWrapper<KpiScore>()
                            .eq(KpiScore::getPeriodId, periodId)
                            .eq(KpiScore::getUserId, userId)
                            .eq(KpiScore::getTemplateId, s.getTemplateId()));
            if (existing != null) {
                s.setId(existing.getId());
            }
            saveOrUpdate(s);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoFillByDataSource(Long periodId, Long userId) {
        KpiPeriod period = periodMapper.selectById(periodId);
        if (period == null) { log.warn("[KpiScore] 周期不存在: {}", periodId); return; }

        List<KpiTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<KpiTemplate>().eq(KpiTemplate::getStatus, 1));
        String start = period.getStartDate().toString();
        String end = period.getEndDate().toString();

        for (KpiTemplate t : templates) {
            if (t.getDataSource() == null || "manual".equals(t.getDataSource())) continue;
            BigDecimal actual = null;
            BigDecimal target = null;
            try {
                switch (t.getDataSource()) {
                    case "auto_commission" -> {
                        if ("deal_count".equals(t.getCode())) {
                            actual = queryDecimal(
                                    "SELECT COUNT(*) FROM realty_commission WHERE user_id = ? AND status = 'settled' AND created_at BETWEEN ? AND ?",
                                    userId, start, end);
                        } else if ("deal_amount".equals(t.getCode())) {
                            actual = queryDecimal(
                                    "SELECT COALESCE(SUM(expected_amount),0) FROM realty_commission WHERE user_id = ? AND status = 'settled' AND created_at BETWEEN ? AND ?",
                                    userId, start, end);
                        }
                    }
                    case "auto_attendance" -> {
                        BigDecimal totalDays = queryDecimal(
                                "SELECT COUNT(DISTINCT DATE(clock_in_time)) FROM attendance_record WHERE user_id = ? AND clock_in_time BETWEEN ? AND ?",
                                userId, start, end);
                        long workDays = java.time.temporal.ChronoUnit.DAYS.between(period.getStartDate(), period.getEndDate()) + 1;
                        long weekends = 0;
                        for (long d = 0; d < workDays; d++) {
                            java.time.DayOfWeek dow = period.getStartDate().plusDays(d).getDayOfWeek();
                            if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY) weekends++;
                        }
                        long shouldAttend = workDays - weekends;
                        target = BigDecimal.valueOf(shouldAttend);
                        actual = totalDays;
                    }
                    case "auto_quality" -> {
                        actual = queryDecimal(
                                "SELECT COALESCE(AVG(overall_score),0) FROM sys_sales_quality_score WHERE user_id = ? AND score_date BETWEEN ? AND ?",
                                userId, start, end);
                        target = BigDecimal.valueOf(100);
                    }
                }
            } catch (Exception e) {
                log.warn("[KpiScore] 自动拉数异常: template={}, userId={}, error={}", t.getCode(), userId, e.getMessage());
                continue;
            }
            if (actual == null) continue;

            BigDecimal score = BigDecimal.ZERO;
            if (target != null && target.compareTo(BigDecimal.ZERO) > 0) {
                score = actual.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100));
            } else if ("auto_quality".equals(t.getDataSource())) {
                score = actual;
            } else {
                score = actual.min(BigDecimal.valueOf(100));
            }

            KpiScore ks = new KpiScore();
            ks.setPeriodId(periodId);
            ks.setUserId(userId);
            ks.setTemplateId(t.getId());
            ks.setTargetValue(target);
            ks.setActualValue(actual);
            ks.setScore(score);
            ks.setRemark("自动拉取(" + t.getDataSource() + ")");
            saveOrUpdate(ks);
        }
        log.info("[KpiScore] 自动拉数完成: periodId={}, userId={}", periodId, userId);
    }

    private BigDecimal queryDecimal(String sql, Object... args) {
        Map<String, Object> row = jdbcTemplate.queryForMap(sql, args);
        Object val = row.values().iterator().next();
        return val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO;
    }
}
