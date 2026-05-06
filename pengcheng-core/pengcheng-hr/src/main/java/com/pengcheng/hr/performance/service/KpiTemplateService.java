package com.pengcheng.hr.performance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.hr.performance.entity.KpiTemplate;

import java.util.List;

/**
 * KPI 指标模板服务（公司级，支持多业务维度）
 */
public interface KpiTemplateService {

    IPage<KpiTemplate> page(Page<KpiTemplate> page, Integer category, Integer status);

    List<KpiTemplate> listByStatus(Integer status);

    KpiTemplate getById(Long id);

    Long create(KpiTemplate template);

    void update(KpiTemplate template);

    void delete(Long id);
}
