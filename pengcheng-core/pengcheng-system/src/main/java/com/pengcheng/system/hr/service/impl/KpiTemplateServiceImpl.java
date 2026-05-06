package com.pengcheng.system.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.hr.entity.KpiTemplate;
import com.pengcheng.system.hr.mapper.KpiTemplateMapper;
import com.pengcheng.system.hr.service.KpiTemplateService;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * KPI 指标模板服务实现
 */
@RequiredArgsConstructor
public class KpiTemplateServiceImpl implements KpiTemplateService {

    private final KpiTemplateMapper templateMapper;

    @Override
    public KpiTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    @Override
    public KpiTemplate getByCode(String code) {
        return templateMapper.selectOne(new LambdaQueryWrapper<KpiTemplate>().eq(KpiTemplate::getCode, code));
    }

    @Override
    public IPage<KpiTemplate> page(Page<KpiTemplate> page, Integer category, Integer status) {
        LambdaQueryWrapper<KpiTemplate> wrapper = new LambdaQueryWrapper<>();
        if (category != null) wrapper.eq(KpiTemplate::getCategory, category);
        if (status != null) wrapper.eq(KpiTemplate::getStatus, status);
        wrapper.orderByAsc(KpiTemplate::getSortOrder);
        return templateMapper.selectPage(page, wrapper);
    }

    @Override
    public List<KpiTemplate> listEnabled() {
        return templateMapper.selectList(
                new LambdaQueryWrapper<KpiTemplate>().eq(KpiTemplate::getStatus, 1).orderByAsc(KpiTemplate::getSortOrder));
    }

    @Override
    public Long create(KpiTemplate template) {
        if (template.getStatus() == null) template.setStatus(1);
        templateMapper.insert(template);
        return template.getId();
    }

    @Override
    public void update(KpiTemplate template) {
        templateMapper.updateById(template);
    }

    @Override
    public void delete(Long id) {
        templateMapper.deleteById(id);
    }
}
