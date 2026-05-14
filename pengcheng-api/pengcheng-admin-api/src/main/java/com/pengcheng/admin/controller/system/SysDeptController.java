package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.RepeatSubmit;
import com.pengcheng.system.annotation.Log.BusinessType;
import com.pengcheng.system.entity.SysDept;
import com.pengcheng.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 */
@RestController
@RequestMapping("/sys/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    /**
     * 通讯录用部门树：权限码 sys:chat:list（普通员工可访问），仅返回启用部门。
     */
    @GetMapping("/contacts-tree")
    @SaCheckPermission("sys:chat:list")
    public Result<List<SysDept>> contactsTree() {
        return Result.ok(deptService.tree(null, 1));
    }

    /**
     * 获取部门树
     */
    @GetMapping("/tree")
    @SaCheckPermission("sys:dept:list")
    public Result<List<SysDept>> tree(
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "false") Boolean includeDisabled) {
        // 默认只返回启用部门；部门管理页传 includeDisabled=true 看全量
        Integer effectiveStatus = status;
        if (effectiveStatus == null && !Boolean.TRUE.equals(includeDisabled)) {
            effectiveStatus = 1;
        }
        return Result.ok(deptService.tree(deptName, effectiveStatus));
    }

    /**
     * 获取部门列表
     */
    @GetMapping("/list")
    public Result<List<SysDept>> list() {
        return Result.ok(deptService.listAll());
    }

    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("sys:dept:list")
    public Result<SysDept> detail(@PathVariable Long id) {
        return Result.ok(deptService.getById(id));
    }

    /**
     * 创建
     */
    @PostMapping
    @SaCheckPermission("sys:dept:add")
    @RepeatSubmit
    @Log(title = "部门管理", businessType = BusinessType.INSERT)
    public Result<Void> create(@RequestBody SysDept dept) {
        deptService.create(dept);
        return Result.ok();
    }

    /**
     * 更新
     */
    @PutMapping
    @SaCheckPermission("sys:dept:edit")
    @Log(title = "部门管理", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody SysDept dept) {
        deptService.update(dept);
        return Result.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:dept:delete")
    @Log(title = "部门管理", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }

    /**
     * 移动部门（拖拽修改层级和排序）
     */
    @PutMapping("/move")
    @SaCheckPermission("sys:dept:edit")
    @Log(title = "部门管理", businessType = BusinessType.UPDATE)
    public Result<Void> move(@RequestParam Long id, @RequestParam Long parentId, @RequestParam(required = false) Integer sort) {
        deptService.move(id, parentId, sort);
        return Result.ok();
    }
}
