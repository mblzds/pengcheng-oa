package com.pengcheng.hr.performance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.hr.performance.entity.KpiPeriod;

/**
 * 考核周期服务（公司级）
 */
public interface KpiPeriodService {

    IPage<KpiPeriod> page(Page<KpiPeriod> page, Integer periodType, Integer status);

    KpiPeriod getById(Long id);

    Long create(KpiPeriod period);

    void update(KpiPeriod period);

    void delete(Long id);
}
