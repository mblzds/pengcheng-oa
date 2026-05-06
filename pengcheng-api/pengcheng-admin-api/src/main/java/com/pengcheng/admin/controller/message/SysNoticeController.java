package com.pengcheng.admin.controller.message;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.admin.websocket.MessageWebSocketHandler;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.RepeatSubmit;
import com.pengcheng.system.annotation.Log.BusinessType;
import com.pengcheng.message.entity.SysNotice;
import com.pengcheng.message.service.SysNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统通知管理
 */
@RestController
@RequestMapping("/sys/notice")
@RequiredArgsConstructor
public class SysNoticeController {

    private final SysNoticeService noticeService;
    private final MessageWebSocketHandler webSocketHandler;

    /**
     * 分页查询通知列表（管理端）
     */
    @GetMapping("/page")
    @SaCheckPermission("sys:notice:list")
    public Result<PageResult<SysNotice>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer noticeType,
            @RequestParam(required = false) Integer status) {
        var result = noticeService.page(page, pageSize, title, noticeType, status);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 获取当前用户的通知列表
     */
    @GetMapping("/my")
    public Result<PageResult<SysNotice>> myNotices(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer isRead) {
        Long userId = StpUtil.getLoginIdAsLong();
        var result = noticeService.getUserNotices(userId, page, pageSize, isRead);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 获取通知详情
     */
    @GetMapping("/{id}")
    public Result<SysNotice> detail(@PathVariable Long id) {
        return Result.ok(noticeService.getById(id));
    }

    /**
     * 创建通知
     */
    @PostMapping
    @SaCheckPermission("sys:notice:add")
    @RepeatSubmit
    @Log(title = "新增通知", businessType = BusinessType.INSERT)
    public Result<Void> create(@RequestBody SysNotice notice) {
        noticeService.create(notice);
        return Result.ok();
    }

    /**
     * 更新通知
     */
    @PutMapping
    @SaCheckPermission("sys:notice:edit")
    @Log(title = "修改通知", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody SysNotice notice) {
        noticeService.update(notice);
        return Result.ok();
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:notice:delete")
    @Log(title = "删除通知", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }

    /**
     * 发布通知
     */
    @PostMapping("/{id}/publish")
    @SaCheckPermission("sys:notice:edit")
    @Log(title = "发布通知", businessType = BusinessType.UPDATE)
    public Result<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);

        // 通过WebSocket推送通知
        SysNotice notice = noticeService.getById(id);
        if (notice != null) {
            webSocketHandler.sendNotice(null, notice.getTitle(), notice.getContent());
        }

        return Result.ok();
    }

    /**
     * 标记通知为已读
     */
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        noticeService.markAsRead(userId, id);
        return Result.ok();
    }

    /**
     * 标记所有通知为已读
     */
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        noticeService.markAllAsRead(userId);
        return Result.ok();
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(noticeService.getUnreadCount(userId));
    }
}
