package com.pengcheng.hr.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.hr.performance.entity.KpiPeriod;
import com.pengcheng.hr.performance.mapper.KpiPeriodMapper;
import com.pengcheng.hr.performance.service.KpiPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KpiPeriodServiceImpl implements KpiPeriodService {

    private final KpiPeriodMapper kpiPeriodMapper;

    @Override
    public IPage<KpiPeriod> page(Page<KpiPeriod> page, Integer periodType, Integer status) {
        LambdaQueryWrapper<KpiPeriod> q = new LambdaQueryWrapper<>();
        if (periodType != null) q.eq(KpiPeriod::getPeriodType, periodType);
        if (status != null) q.eq(KpiPeriod::getStatus, status);
        q.orderByDesc(KpiPeriod::getYear, KpiPeriod::getMonth);
        return kpiPeriodMapper.selectPage(page, q);
    }

    @Override
    public KpiPeriod getById(Long id) {
        return kpiPeriodMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(KpiPeriod period) {
        if (period == null || period.getName() == null || period.getStartDate() == null || period.getEndDate() == null) {
            throw new IllegalArgumentException("周期名称、开始日期、结束日期不能为空");
        }
        if (period.getStatus() == null) period.setStatus(KpiPeriod.STATUS_NOT_STARTED);
        kpiPeriodMapper.insert(period);
        return period.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(KpiPeriod period) {
        if (period == null || period.getId() == null) throw new IllegalArgumentException("周期ID不能为空");
        kpiPeriodMapper.updateById(period);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        kpiPeriodMapper.deleteById(id);
    }
}
