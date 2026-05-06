package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.calendar.entity.CalendarEvent;
import com.pengcheng.system.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 销售日历接口
 */
@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/events")
    public Result<List<CalendarEvent>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(calendarService.getEvents(userId, start, end));
    }

    @GetMapping("/month")
    public Result<List<CalendarEvent>> getMonthEvents(@RequestParam int year, @RequestParam int month) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(calendarService.getMonthEvents(userId, year, month));
    }

    @GetMapping("/today")
    public Result<List<CalendarEvent>> getTodayEvents() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(calendarService.getTodayEvents(userId));
    }

    @PostMapping("/event")
    public Result<CalendarEvent> createEvent(@RequestBody CalendarEvent event) {
        event.setUserId(StpUtil.getLoginIdAsLong());
        return Result.ok(calendarService.createEvent(event));
    }

    @PutMapping("/event")
    public Result<Void> updateEvent(@RequestBody CalendarEvent event) {
        calendarService.updateEvent(event);
        return Result.ok();
    }

    @DeleteMapping("/event/{id}")
    public Result<Void> cancelEvent(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        calendarService.cancelEvent(id, userId);
        return Result.ok();
    }

    /**
     * 合并视图（手动事件 + 客户拜访 + 合同/回款节点）
     */
    @GetMapping("/merged")
    public Result<List<CalendarEvent>> getMergedEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(calendarService.getMergedEvents(userId, start, end));
    }

    /**
     * 团队日程共享（经理/管理员可查看团队成员日程）
     */
    @GetMapping("/team")
    public Result<List<CalendarEvent>> getTeamEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long deptId) {
        return Result.ok(calendarService.getTeamEvents(deptId, start, end));
    }
}
