package com.pengcheng.system.smarttable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.smarttable.entity.*;
import com.pengcheng.system.smarttable.mapper.*;
import com.pengcheng.system.smarttable.service.SmartTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 智能表格服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartTableServiceImpl implements SmartTableService {

    private final SmartTableMapper tableMapper;
    private final SmartTableFieldMapper fieldMapper;
    private final SmartTableRecordMapper recordMapper;
    private final SmartTableViewMapper viewMapper;
    private final SmartTableTemplateMapper templateMapper;

    // ==================== 表格管理 ====================

    @Override
    public List<SmartTable> listMyTables(Long userId, Long deptId) {
        LambdaQueryWrapper<SmartTable> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(SmartTable::getOwnerId, userId)
                .or()
                .eq(SmartTable::getVisibility, "all")
                .or(w2 -> w2.eq(SmartTable::getVisibility, "dept").eq(SmartTable::getDeptId, deptId))
        );
        wrapper.orderByDesc(SmartTable::getUpdatedAt);
        return tableMapper.selectList(wrapper);
    }

    @Override
    public SmartTable getTableById(Long id, Long userId) {
        SmartTable table = tableMapper.selectById(id);
        if (table == null) {
            throw new RuntimeException("表格不存在");
        }
        checkTableAccess(table, userId);
        return table;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmartTable createTable(SmartTable table) {
        table.setRecordCount(0);
        table.setCreatedAt(LocalDateTime.now());
        table.setUpdatedAt(LocalDateTime.now());
        tableMapper.insert(table);

        createDefaultView(table.getId());
        return table;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmartTable updateTable(SmartTable table, Long userId) {
        SmartTable existing = getTableById(table.getId(), userId);
        checkTableOwner(existing, userId);
        existing.setName(table.getName());
        existing.setDescription(table.getDescription());
        existing.setIcon(table.getIcon());
        existing.setVisibility(table.getVisibility());
        existing.setUpdatedAt(LocalDateTime.now());
        tableMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(Long id, Long userId) {
        SmartTable table = getTableById(id, userId);
        checkTableOwner(table, userId);
        tableMapper.deleteById(id);

        LambdaQueryWrapper<SmartTableField> fieldW = new LambdaQueryWrapper<>();
        fieldW.eq(SmartTableField::getTableId, id);
        fieldMapper.delete(fieldW);

        LambdaQueryWrapper<SmartTableRecord> recordW = new LambdaQueryWrapper<>();
        recordW.eq(SmartTableRecord::getTableId, id);
        recordMapper.delete(recordW);

        LambdaQueryWrapper<SmartTableView> viewW = new LambdaQueryWrapper<>();
        viewW.eq(SmartTableView::getTableId, id);
        viewMapper.delete(viewW);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmartTable createFromTemplate(Long templateId, String name, Long userId, Long deptId) {
        SmartTableTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        SmartTable table = new SmartTable();
        table.setName(name);
        table.setDescription(template.getDescription());
        table.setIcon(template.getIcon());
        table.setTemplateId(templateId);
        table.setOwnerId(userId);
        table.setDeptId(deptId);
        table.setVisibility("private");
        createTable(table);

        List<Map<String, Object>> fieldsConfig = template.getFieldsConfig();
        if (fieldsConfig != null) {
            for (int i = 0; i < fieldsConfig.size(); i++) {
                Map<String, Object> fc = fieldsConfig.get(i);
                SmartTableField field = new SmartTableField();
                field.setTableId(table.getId());
                field.setName((String) fc.get("name"));
                field.setFieldKey((String) fc.get("field_key"));
                field.setFieldType((String) fc.get("field_type"));
                field.setRequired(Boolean.TRUE.equals(fc.get("required")));
                if (fc.containsKey("options")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> opts = (Map<String, Object>) fc.get("options");
                    field.setOptions(opts);
                }
                field.setSortOrder(i);
                field.setWidth(150);
                field.setHidden(false);
                field.setCreatedAt(LocalDateTime.now());
                field.setUpdatedAt(LocalDateTime.now());
                fieldMapper.insert(field);
            }
        }

        templateMapper.update(null, new LambdaUpdateWrapper<SmartTableTemplate>()
                .eq(SmartTableTemplate::getId, templateId)
                .setSql("usage_count = usage_count + 1"));

        return table;
    }

    // ==================== 字段管理 ====================

    @Override
    public List<SmartTableField> listFields(Long tableId) {
        LambdaQueryWrapper<SmartTableField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmartTableField::getTableId, tableId)
               .orderByAsc(SmartTableField::getSortOrder);
        return fieldMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmartTableField addField(SmartTableField field, Long userId) {
        getTableById(field.getTableId(), userId);

        Long maxSort = fieldMapper.selectCount(new LambdaQueryWrapper<SmartTableField>()
                .eq(SmartTableField::getTableId, field.getTableId()));
        field.setSortOrder(maxSort.intValue());
        if (field.getWidth() == null) field.setWidth(150);
        if (field.getHidden() == null) field.setHidden(false);
        if (field.getRequired() == null) field.setRequired(false);
        field.setCreatedAt(LocalDateTime.now());
        field.setUpdatedAt(LocalDateTime.now());
        fieldMapper.insert(field);
        return field;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmartTableField updateField(SmartTableField field, Long userId) {
        SmartTableField existing = fieldMapper.selectById(field.getId());
        if (existing == null) throw new RuntimeException("字段不存在");
        getTableById(existing.getTableId(), userId);

        existing.setName(field.getName());
        existing.setFieldType(field.getFieldType());
        existing.setRequired(field.getRequired());
        existing.setOptions(field.getOptions());
        existing.setDefaultValue(field.getDefaultValue());
        existing.setWidth(field.getWidth());
        existing.setHidden(field.getHidden());
        existing.setUpdatedAt(LocalDateTime.now());
        fieldMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteField(Long fieldId, Long tableId, Long userId) {
        getTableById(tableId, userId);
        fieldMapper.deleteById(fieldId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorderFields(Long tableId, List<Long> fieldIds, Long userId) {
        getTableById(tableId, userId);
        for (int i = 0; i < fieldIds.size(); i++) {
            fieldMapper.update(null, new LambdaUpdateWrapper<SmartTableField>()
                    .eq(SmartTableField::getId, fieldIds.get(i))
                    .eq(SmartTableField::getTableId, tableId)
                    .set(SmartTableField::getSortOrder, i));
        }
    }

    // ==================== 记录管理 ====================

    @Override
    public Page<SmartTableRecord> listRecords(Long tableId, int page, int pageSize, Map<String, Object> filters) {
        Page<SmartTableRecord> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SmartTableRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmartTableRecord::getTableId, tableId)
               .orderByAsc(SmartTableRecord::getSortOrder)
               .orderByDesc(SmartTableRecord::getCreatedAt);
        return recordMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmartTableRecord addRecord(Long tableId, Map<String, Object> data, Long userId) {
        getTableById(tableId, userId);
        SmartTableRecord record = new SmartTableRecord();
        record.setTableId(tableId);
        record.setData(data);
        record.setCreatorId(userId);
        record.setSortOrder(0);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        tableMapper.update(null, new LambdaUpdateWrapper<SmartTable>()
                .eq(SmartTable::getId, tableId)
                .setSql("record_count = record_count + 1"));
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmartTableRecord updateRecord(Long recordId, Map<String, Object> data, Long userId) {
        SmartTableRecord record = recordMapper.selectById(recordId);
        if (record == null) throw new RuntimeException("记录不存在");
        getTableById(record.getTableId(), userId);
        record.setData(data);
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.updateById(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long recordId, Long userId) {
        SmartTableRecord record = recordMapper.selectById(recordId);
        if (record == null) return;
        getTableById(record.getTableId(), userId);
        recordMapper.deleteById(recordId);
        tableMapper.update(null, new LambdaUpdateWrapper<SmartTable>()
                .eq(SmartTable::getId, record.getTableId())
                .setSql("record_count = GREATEST(record_count - 1, 0)"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteRecords(List<Long> recordIds, Long userId) {
        for (Long id : recordIds) {
            deleteRecord(id, userId);
        }
    }

    // ==================== 视图管理 ====================

    @Override
    public List<SmartTableView> listViews(Long tableId) {
        LambdaQueryWrapper<SmartTableView> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmartTableView::getTableId, tableId)
               .orderByAsc(SmartTableView::getSortOrder);
        return viewMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmartTableView createView(SmartTableView view, Long userId) {
        getTableById(view.getTableId(), userId);
        view.setCreatedAt(LocalDateTime.now());
        view.setUpdatedAt(LocalDateTime.now());
        viewMapper.insert(view);
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmartTableView updateView(SmartTableView view, Long userId) {
        SmartTableView existing = viewMapper.selectById(view.getId());
        if (existing == null) throw new RuntimeException("视图不存在");
        getTableById(existing.getTableId(), userId);
        existing.setName(view.getName());
        existing.setConfig(view.getConfig());
        existing.setUpdatedAt(LocalDateTime.now());
        viewMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteView(Long viewId, Long userId) {
        SmartTableView view = viewMapper.selectById(viewId);
        if (view == null) return;
        if (Boolean.TRUE.equals(view.getIsDefault())) {
            throw new RuntimeException("默认视图不能删除");
        }
        getTableById(view.getTableId(), userId);
        viewMapper.deleteById(viewId);
    }

    // ==================== 模板管理 ====================

    @Override
    public List<SmartTableTemplate> listTemplates(String category) {
        LambdaQueryWrapper<SmartTableTemplate> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(SmartTableTemplate::getCategory, category);
        }
        wrapper.orderByDesc(SmartTableTemplate::getUsageCount);
        return templateMapper.selectList(wrapper);
    }

    @Override
    public SmartTableTemplate getTemplate(Long id) {
        return templateMapper.selectById(id);
    }

    @Override
    public SmartTableTemplate createTemplate(SmartTableTemplate template) {
        template.setBuiltIn(false);
        template.setUsageCount(0);
        templateMapper.insert(template);
        return template;
    }

    @Override
    public void updateTemplate(SmartTableTemplate template) {
        templateMapper.updateById(template);
    }

    @Override
    public void deleteTemplate(Long id) {
        SmartTableTemplate tpl = templateMapper.selectById(id);
        if (tpl != null && !Boolean.TRUE.equals(tpl.getBuiltIn())) {
            templateMapper.deleteById(id);
        }
    }

    // ==================== 内部方法 ====================

    private void checkTableAccess(SmartTable table, Long userId) {
        if ("all".equals(table.getVisibility())) return;
        if (table.getOwnerId().equals(userId)) return;
        throw new RuntimeException("无权访问此表格");
    }

    private void checkTableOwner(SmartTable table, Long userId) {
        if (!table.getOwnerId().equals(userId)) {
            throw new RuntimeException("只有表格创建者可以执行此操作");
        }
    }

    private void createDefaultView(Long tableId) {
        SmartTableView defaultView = new SmartTableView();
        defaultView.setTableId(tableId);
        defaultView.setName("默认表格");
        defaultView.setViewType("grid");
        defaultView.setIsDefault(true);
        defaultView.setSortOrder(0);
        defaultView.setCreatedAt(LocalDateTime.now());
        defaultView.setUpdatedAt(LocalDateTime.now());
        viewMapper.insert(defaultView);
    }
}
