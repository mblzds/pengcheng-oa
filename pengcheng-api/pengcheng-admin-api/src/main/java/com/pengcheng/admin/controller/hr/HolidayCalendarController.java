package com.pengcheng.admin.controller.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.attendance.entity.HolidayCalendar;
import com.pengcheng.hr.attendance.service.HolidayCalendarService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 节假日 / 调休补班日历管理（考勤月报算缺勤的依据）
 */
@RestController
@RequestMapping("/admin/holiday-calendar")
@RequiredArgsConstructor
public class HolidayCalendarController {

    private final HolidayCalendarService service;

    /** 列出某年全部条目（按日期升序） */
    @GetMapping
    @SaCheckPermission("system:holiday:list")
    public Result<List<HolidayCalendar>> list(@RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return Result.ok(service.listByYear(y));
    }

    /** 新建/更新（按 holidayDate 唯一约束做 upsert） */
    @PostMapping
    @SaCheckPermission("system:holiday:edit")
    @Log(title = "节假日新增/更新", businessType = BusinessType.UPDATE)
    public Result<Long> save(@RequestBody HolidayCalendar item) {
        return Result.ok(service.save(item));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:holiday:edit")
    @Log(title = "节假日编辑", businessType = BusinessType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @RequestBody HolidayCalendar item) {
        item.setId(id);
        service.save(item);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:holiday:edit")
    @Log(title = "节假日删除", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
