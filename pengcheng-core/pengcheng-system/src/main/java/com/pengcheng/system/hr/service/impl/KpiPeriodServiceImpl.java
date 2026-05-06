package com.pengcheng.system.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.hr.entity.KpiPeriod;
import com.pengcheng.system.hr.mapper.KpiPeriodMapper;
import com.pengcheng.system.hr.service.KpiPeriodService;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 考核周期服务实现
 */
@RequiredArgsConstructor
public class KpiPeriodServiceImpl implements KpiPeriodService {

    private final KpiPeriodMapper periodMapper;

    @Override
    public KpiPeriod getById(Long id) {
        return periodMapper.selectById(id);
    }

    @Override
    public IPage<KpiPeriod> page(Page<KpiPeriod> page, Integer periodType, Integer year, Integer status) {
        LambdaQueryWrapper<KpiPeriod> wrapper = new LambdaQueryWrapper<>();
        if (periodType != null) wrapper.eq(KpiPeriod::getPeriodType, periodType);
        if (year != null) wrapper.eq(KpiPeriod::getYear, year);
        if (status != null) wrapper.eq(KpiPeriod::getStatus, status);
        wrapper.orderByDesc(KpiPeriod::getYear, KpiPeriod::getStartDate);
        return periodMapper.selectPage(page, wrapper);
    }

    @Override
    public List<KpiPeriod> listByYear(int year) {
        return periodMapper.selectList(
                new LambdaQueryWrapper<KpiPeriod>().eq(KpiPeriod::getYear, year).orderByAsc(KpiPeriod::getStartDate));
    }

    @Override
    public Long create(KpiPeriod period) {
        if (period.getStatus() == null) period.setStatus(1);
        periodMapper.insert(period);
        return period.getId();
    }

    @Override
    public void update(KpiPeriod period) {
        periodMapper.updateById(period);
    }

    @Override
    public void delete(Long id) {
        periodMapper.deleteById(id);
    }
}
