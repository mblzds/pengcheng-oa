package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.smarttable.entity.*;
import com.pengcheng.system.smarttable.service.SmartTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能表格
 */
@RestController
@RequestMapping("/sys/smart-table")
@RequiredArgsConstructor
public class SmartTableController {

    private final SmartTableService smartTableService;

    // ==================== 表格 ====================

    @GetMapping("/list")
    public Result<List<SmartTable>> listTables(@RequestParam(required = false) Long deptId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(smartTableService.listMyTables(userId, deptId));
    }

    @GetMapping("/{id}")
    public Result<SmartTable> getTable(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(smartTableService.getTableById(id, userId));
    }

    @PostMapping
    public Result<SmartTable> createTable(@RequestBody SmartTable table) {
        Long userId = StpUtil.getLoginIdAsLong();
        table.setOwnerId(userId);
        return Result.ok(smartTableService.createTable(table));
    }

    @PutMapping
    public Result<SmartTable> updateTable(@RequestBody SmartTable table) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(smartTableService.updateTable(table, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTable(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        smartTableService.deleteTable(id, userId);
        return Result.ok();
    }

    @PostMapping("/from-template")
    public Result<SmartTable> createFromTemplate(
            @RequestParam Long templateId,
            @RequestParam String name,
            @RequestParam(required = false) Long deptId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(smartTableService.createFromTemplate(templateId, name, userId, deptId));
    }

    // ==================== 字段 ====================

    @GetMapping("/{tableId}/fields")
    public Result<List<SmartTableField>> listFields(@PathVariable Long tableId) {
        return Result.ok(smartTableService.listFields(tableId));
    }

    @PostMapping("/{tableId}/fields")
    public Result<SmartTableField> addField(@PathVariable Long tableId, @RequestBody SmartTableField field) {
        Long userId = StpUtil.getLoginIdAsLong();
        field.setTableId(tableId);
        return Result.ok(smartTableService.addField(field, userId));
    }

    @PutMapping("/{tableId}/fields")
    public Result<SmartTableField> updateField(@PathVariable Long tableId, @RequestBody SmartTableField field) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(smartTableService.updateField(field, userId));
    }

    @DeleteMapping("/{tableId}/fields/{fieldId}")
    public Result<Void> deleteField(@PathVariable Long tableId, @PathVariable Long fieldId) {
        Long userId = StpUtil.getLoginIdAsLong();
        smartTableService.deleteField(fieldId, tableId, userId);
        return Result.ok();
    }

    @PutMapping("/{tableId}/fields/reorder")
    public Result<Void> reorderFields(@PathVariable Long tableId, @RequestBody List<Long> fieldIds) {
        Long userId = StpUtil.getLoginIdAsLong();
        smartTableService.reorderFields(tableId, fieldIds, userId);
        return Result.ok();
    }

    // ==================== 记录 ====================

    @GetMapping("/{tableId}/records")
    public Result<PageResult<SmartTableRecord>> listRecords(
            @PathVariable Long tableId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        smartTableService.getTableById(tableId, userId);
        var result = smartTableService.listRecords(tableId, page, pageSize, null);
        return Result.ok(PageResult.of(result));
    }

    @PostMapping("/{tableId}/records")
    public Result<SmartTableRecord> addRecord(@PathVariable Long tableId, @RequestBody Map<String, Object> data) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(smartTableService.addRecord(tableId, data, userId));
    }

    @PutMapping("/records/{recordId}")
    public Result<SmartTableRecord> updateRecord(@PathVariable Long recordId, @RequestBody Map<String, Object> data) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(smartTableService.updateRecord(recordId, data, userId));
    }

    @DeleteMapping("/records/{recordId}")
    public Result<Void> deleteRecord(@PathVariable Long recordId) {
        Long userId = StpUtil.getLoginIdAsLong();
        smartTableService.deleteRecord(recordId, userId);
        return Result.ok();
    }

    @DeleteMapping("/records/batch")
    public Result<Void> batchDeleteRecords(@RequestBody List<Long> recordIds) {
        Long userId = StpUtil.getLoginIdAsLong();
        smartTableService.batchDeleteRecords(recordIds, userId);
        return Result.ok();
    }

    // ==================== 视图 ====================

    @GetMapping("/{tableId}/views")
    public Result<List<SmartTableView>> listViews(@PathVariable Long tableId) {
        return Result.ok(smartTableService.listViews(tableId));
    }

    @PostMapping("/{tableId}/views")
    public Result<SmartTableView> createView(@PathVariable Long tableId, @RequestBody SmartTableView view) {
        Long userId = StpUtil.getLoginIdAsLong();
        view.setTableId(tableId);
        return Result.ok(smartTableService.createView(view, userId));
    }

    @PutMapping("/views/{viewId}")
    public Result<SmartTableView> updateView(@PathVariable Long viewId, @RequestBody SmartTableView view) {
        Long userId = StpUtil.getLoginIdAsLong();
        view.setId(viewId);
        return Result.ok(smartTableService.updateView(view, userId));
    }

    @DeleteMapping("/views/{viewId}")
    public Result<Void> deleteView(@PathVariable Long viewId) {
        Long userId = StpUtil.getLoginIdAsLong();
        smartTableService.deleteView(viewId, userId);
        return Result.ok();
    }

    // ==================== 模板 ====================

    @GetMapping("/templates")
    public Result<List<SmartTableTemplate>> listTemplates(@RequestParam(required = false) String category) {
        return Result.ok(smartTableService.listTemplates(category));
    }

    @GetMapping("/templates/{id}")
    public Result<SmartTableTemplate> getTemplate(@PathVariable Long id) {
        return Result.ok(smartTableService.getTemplate(id));
    }

    @PostMapping("/templates")
    public Result<SmartTableTemplate> createTemplate(@RequestBody SmartTableTemplate template) {
        return Result.ok(smartTableService.createTemplate(template));
    }

    @PutMapping("/templates")
    public Result<Void> updateTemplate(@RequestBody SmartTableTemplate template) {
        smartTableService.updateTemplate(template);
        return Result.ok();
    }

    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        smartTableService.deleteTemplate(id);
        return Result.ok();
    }
}
