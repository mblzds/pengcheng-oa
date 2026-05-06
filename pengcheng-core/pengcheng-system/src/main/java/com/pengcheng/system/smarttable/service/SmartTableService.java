package com.pengcheng.system.smarttable.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.smarttable.entity.*;

import java.util.List;
import java.util.Map;

/**
 * 智能表格服务接口
 */
public interface SmartTableService {

    // ==================== 表格管理 ====================

    List<SmartTable> listMyTables(Long userId, Long deptId);

    SmartTable getTableById(Long id, Long userId);

    SmartTable createTable(SmartTable table);

    SmartTable updateTable(SmartTable table, Long userId);

    void deleteTable(Long id, Long userId);

    /**
     * 从模板创建表格
     */
    SmartTable createFromTemplate(Long templateId, String name, Long userId, Long deptId);

    // ==================== 字段管理 ====================

    List<SmartTableField> listFields(Long tableId);

    SmartTableField addField(SmartTableField field, Long userId);

    SmartTableField updateField(SmartTableField field, Long userId);

    void deleteField(Long fieldId, Long tableId, Long userId);

    void reorderFields(Long tableId, List<Long> fieldIds, Long userId);

    // ==================== 记录管理 ====================

    Page<SmartTableRecord> listRecords(Long tableId, int page, int pageSize, Map<String, Object> filters);

    SmartTableRecord addRecord(Long tableId, Map<String, Object> data, Long userId);

    SmartTableRecord updateRecord(Long recordId, Map<String, Object> data, Long userId);

    void deleteRecord(Long recordId, Long userId);

    void batchDeleteRecords(List<Long> recordIds, Long userId);

    // ==================== 视图管理 ====================

    List<SmartTableView> listViews(Long tableId);

    SmartTableView createView(SmartTableView view, Long userId);

    SmartTableView updateView(SmartTableView view, Long userId);

    void deleteView(Long viewId, Long userId);

    // ==================== 模板管理 ====================

    List<SmartTableTemplate> listTemplates(String category);

    SmartTableTemplate getTemplate(Long id);

    SmartTableTemplate createTemplate(SmartTableTemplate template);

    void updateTemplate(SmartTableTemplate template);

    void deleteTemplate(Long id);
}
