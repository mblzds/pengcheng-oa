package com.pengcheng.system.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.hr.entity.KpiPeriod;

import java.util.List;

/**
 * 考核周期服务
 */
public interface KpiPeriodService {

    KpiPeriod getById(Long id);

    IPage<KpiPeriod> page(Page<KpiPeriod> page, Integer periodType, Integer year, Integer status);

    List<KpiPeriod> listByYear(int year);

    Long create(KpiPeriod period);

    void update(KpiPeriod period);

    void delete(Long id);
}
