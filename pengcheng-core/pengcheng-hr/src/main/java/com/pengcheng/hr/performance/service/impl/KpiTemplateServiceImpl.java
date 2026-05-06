package com.pengcheng.hr.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.hr.performance.entity.KpiTemplate;
import com.pengcheng.hr.performance.mapper.KpiTemplateMapper;
import com.pengcheng.hr.performance.service.KpiTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("performanceKpiTemplateService")
@RequiredArgsConstructor
public class KpiTemplateServiceImpl implements KpiTemplateService {

    private final KpiTemplateMapper kpiTemplateMapper;

    @Override
    public IPage<KpiTemplate> page(Page<KpiTemplate> page, Integer category, Integer status) {
        LambdaQueryWrapper<KpiTemplate> q = new LambdaQueryWrapper<>();
        if (category != null) q.eq(KpiTemplate::getCategory, category);
        if (status != null) q.eq(KpiTemplate::getStatus, status);
        q.orderByAsc(KpiTemplate::getSortOrder);
        return kpiTemplateMapper.selectPage(page, q);
    }

    @Override
    public List<KpiTemplate> listByStatus(Integer status) {
        LambdaQueryWrapper<KpiTemplate> q = new LambdaQueryWrapper<>();
        if (status != null) q.eq(KpiTemplate::getStatus, status);
        q.orderByAsc(KpiTemplate::getSortOrder);
        return kpiTemplateMapper.selectList(q);
    }

    @Override
    public KpiTemplate getById(Long id) {
        return kpiTemplateMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(KpiTemplate template) {
        if (template == null || template.getName() == null || template.getCode() == null) {
            throw new IllegalArgumentException("指标名称、编码不能为空");
        }
        if (template.getStatus() == null) template.setStatus(1);
        kpiTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(KpiTemplate template) {
        if (template == null || template.getId() == null) throw new IllegalArgumentException("模板ID不能为空");
        kpiTemplateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        kpiTemplateMapper.deleteById(id);
    }
}
