package com.pengcheng.hr.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.performance.entity.KpiPeriod;
import com.pengcheng.hr.performance.entity.KpiTemplate;
import com.pengcheng.hr.performance.mapper.KpiPeriodMapper;
import com.pengcheng.hr.performance.mapper.KpiTemplateMapper;
import com.pengcheng.hr.performance.provider.KpiDataSourceProvider;
import com.pengcheng.hr.performance.service.KpiSuggestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按 data_source 从各业务模块拉取 KPI 建议实际值
 */
@Service
@RequiredArgsConstructor
public class KpiSuggestServiceImpl implements KpiSuggestService {

    private final KpiPeriodMapper kpiPeriodMapper;
    private final KpiTemplateMapper kpiTemplateMapper;

    @Autowired(required = false)
    private List<KpiDataSourceProvider> providers = Collections.emptyList();

    @Override
    public Map<Long, BigDecimal> suggestActualValues(Long periodId, Long userId) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (periodId == null || userId == null) return result;

        KpiPeriod period = kpiPeriodMapper.selectById(periodId);
        if (period == null || period.getStartDate() == null || period.getEndDate() == null) return result;

        if (providers == null || providers.isEmpty()) return result;

        LambdaQueryWrapper<KpiTemplate> q = new LambdaQueryWrapper<>();
        q.eq(KpiTemplate::getStatus, 1);
        q.isNotNull(KpiTemplate::getDataSource);
        q.ne(KpiTemplate::getDataSource, "");
        List<KpiTemplate> templates = kpiTemplateMapper.selectList(q);
        if (templates == null) return result;

        Map<String, KpiDataSourceProvider> providerMap = new HashMap<>();
        for (KpiDataSourceProvider p : providers) {
            if (p.getDataSource() != null) providerMap.put(p.getDataSource(), p);
        }

        for (KpiTemplate t : templates) {
            String ds = t.getDataSource();
            if (ds == null || ds.equalsIgnoreCase("manual")) continue;
            KpiDataSourceProvider p = providerMap.get(ds);
            if (p == null) continue;
            try {
                BigDecimal value = p.getActualValue(
                    period.getStartDate(), period.getEndDate(), userId,
                    t.getCode() != null ? t.getCode() : "");
                if (value != null && t.getId() != null) result.put(t.getId(), value);
            } catch (Exception ignored) { }
        }
        return result;
    }
}
