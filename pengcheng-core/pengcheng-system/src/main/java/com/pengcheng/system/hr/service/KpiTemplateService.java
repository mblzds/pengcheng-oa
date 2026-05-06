package com.pengcheng.system.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.hr.entity.KpiTemplate;

import java.util.List;

/**
 * KPI 指标模板服务
 */
public interface KpiTemplateService {

    KpiTemplate getById(Long id);

    KpiTemplate getByCode(String code);

    IPage<KpiTemplate> page(Page<KpiTemplate> page, Integer category, Integer status);

    List<KpiTemplate> listEnabled();

    Long create(KpiTemplate template);

    void update(KpiTemplate template);

    void delete(Long id);
}
