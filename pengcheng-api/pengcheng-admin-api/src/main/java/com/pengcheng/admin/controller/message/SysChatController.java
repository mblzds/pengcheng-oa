package com.pengcheng.admin.controller.message;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.admin.websocket.MessageWebSocketHandler;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.message.entity.SysChatMessage;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.entity.SysUserBlacklist;
import com.pengcheng.message.entity.MessageCategory;
import com.pengcheng.message.service.MessagePriorityService;
import com.pengcheng.message.service.SysChatMessageService;
import com.pengcheng.system.service.SysUserBlacklistService;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 即时聊天
 */
@RestController
@RequestMapping("/sys/chat")
@RequiredArgsConstructor
public class SysChatController {

    private final SysChatMessageService chatMessageService;
    private final SysUserService userService;
    private final SysUserBlacklistService blacklistService;
    private final MessageWebSocketHandler webSocketHandler;
    private final MessagePriorityService messagePriorityService;

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<SysChatMessage> send(@RequestBody SysChatMessage message) {
        Long senderId = StpUtil.getLoginIdAsLong();

        // 检查是否被对方拉黑
        if (blacklistService.isBlocked(senderId, message.getReceiverId())) {
            return Result.fail("消息发送失败，对方已将你拉黑");
        }

        // 检查自己是否拉黑了对方（如果拉黑了就不能发消息）
        if (blacklistService.isInMyBlacklist(senderId, message.getReceiverId())) {
            return Result.fail("请先移除黑名单后再发送消息");
        }

        SysChatMessage result = chatMessageService.send(message);

        // 通过WebSocket推送消息
        if (message.getReceiverId() != null && message.getReceiverId() > 0) {
            webSocketHandler.sendToUser(message.getReceiverId(),
                    "{\"type\":\"chat\",\"senderId\":" + result.getSenderId() +
                    ",\"senderName\":\"" + result.getSenderName() + "\"" +
                    ",\"content\":\"" + result.getContent().replace("\"", "\\\"") + "\"" +
                    ",\"msgType\":" + result.getMsgType() +
                    ",\"time\":" + System.currentTimeMillis() + "}");
        }

        return Result.ok(result);
    }

    /**
     * 获取聊天记录
     */
    @GetMapping("/history/{targetId}")
    public Result<PageResult<SysChatMessage>> getChatHistory(
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        var result = chatMessageService.getChatHistory(userId, targetId, page, pageSize);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 获取最近联系人
     */
    @GetMapping("/contacts")
    public Result<List<SysChatMessage>> getRecentContacts() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(chatMessageService.getRecentContacts(userId));
    }

    /**
     * 获取用户列表（用于选择聊天对象）
     */
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getUsers() {
        Long userId = StpUtil.getLoginIdAsLong();
        // 获取所有用户（排除自己）
        List<SysUser> users = userService.listAll();
        users.removeIf(u -> u.getId().equals(userId));

        // 构建返回数据，包含用户信息和最新消息
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (SysUser user : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("nickname", user.getNickname());
            item.put("avatar", user.getAvatar());

            // 获取与该用户的最新消息
            var latestMsg = chatMessageService.getLatestMessage(userId, user.getId());
            if (latestMsg != null) {
                item.put("lastMessage", latestMsg.getMsgType() == 2 ? "[图片]" : latestMsg.getContent());
                item.put("lastMessageTime", latestMsg.getSendTime());
            }

            // 检查拉黑状态
            item.put("isBlocked", blacklistService.isInMyBlacklist(userId, user.getId()));

            result.add(item);
        }
        return Result.ok(result);
    }

    /**
     * 标记消息为已读
     */
    @PostMapping("/read/{senderId}")
    public Result<Void> markAsRead(@PathVariable Long senderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        chatMessageService.markAsRead(userId, senderId);
        return Result.ok();
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(chatMessageService.getUnreadCount(userId));
    }

    /**
     * 获取消息统计（通知+聊天）
     */
    @GetMapping("/stats")
    public Result<Map<String, Integer>> getMessageStats() {
        Long userId = StpUtil.getLoginIdAsLong();
        Map<String, Integer> stats = new HashMap<>();
        stats.put("chatCount", chatMessageService.getUnreadCount(userId));
        return Result.ok(stats);
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/online/{userId}")
    public Result<Boolean> isOnline(@PathVariable Long userId) {
        return Result.ok(webSocketHandler.isOnline(userId));
    }

    /**
     * 清空与某人的聊天记录
     */
    @DeleteMapping("/clear/{targetId}")
    public Result<Void> clearHistory(@PathVariable Long targetId) {
        Long userId = StpUtil.getLoginIdAsLong();
        chatMessageService.clearHistory(userId, targetId);
        return Result.ok();
    }

    /**
     * 拉黑用户
     */
    @PostMapping("/block/{targetId}")
    public Result<Void> blockUser(@PathVariable Long targetId) {
        Long userId = StpUtil.getLoginIdAsLong();
        blacklistService.blockUser(userId, targetId);
        return Result.ok();
    }

    /**
     * 取消拉黑
     */
    @DeleteMapping("/block/{targetId}")
    public Result<Void> unblockUser(@PathVariable Long targetId) {
        Long userId = StpUtil.getLoginIdAsLong();
        blacklistService.unblockUser(userId, targetId);
        return Result.ok();
    }

    /**
     * 获取黑名单列表
     */
    @GetMapping("/blacklist")
    public Result<List<SysUserBlacklist>> getBlacklist() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(blacklistService.getBlacklist(userId));
    }

    /**
     * 检查是否拉黑
     */
    @GetMapping("/blocked/{targetId}")
    public Result<Boolean> isBlocked(@PathVariable Long targetId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(blacklistService.isInMyBlacklist(userId, targetId));
    }

    /**
     * 撤回消息（2 分钟内）
     */
    @PostMapping("/recall/{msgId}")
    public Result<Void> recallMessage(@PathVariable Long msgId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean success = chatMessageService.recallMessage(msgId, userId);
        if (!success) {
            return Result.fail("撤回失败，消息不存在或已超过 2 分钟");
        }
        webSocketHandler.sendToUser(userId,
                "{\"type\":\"recall\",\"messageId\":" + msgId + "}");
        return Result.ok();
    }

    /**
     * 搜索聊天记录
     */
    @GetMapping("/search")
    public Result<PageResult<SysChatMessage>> searchMessages(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        var result = chatMessageService.searchMessages(userId, keyword, page, pageSize);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 消息 ACK 确认送达
     */
    @PostMapping("/ack/{msgId}")
    public Result<Void> ackMessage(@PathVariable Long msgId) {
        Long userId = StpUtil.getLoginIdAsLong();
        chatMessageService.ackMessage(msgId, userId);
        return Result.ok();
    }

    /**
     * 拉取离线消息（上线时调用）
     */
    @GetMapping("/offline")
    public Result<List<SysChatMessage>> getOfflineMessages(
            @RequestParam(defaultValue = "0") Long lastSeq) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<SysChatMessage> messages = chatMessageService.getOfflineMessages(userId, lastSeq);
        if (!messages.isEmpty()) {
            chatMessageService.markOfflineDelivered(userId,
                    messages.stream().map(SysChatMessage::getId).toList());
        }
        return Result.ok(messages);
    }

    /**
     * 设置会话分类（关注/星标/静音/普通）
     */
    @PostMapping("/category")
    public Result<Void> setCategory(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        String chatType = (String) body.get("chatType");
        Long targetId = Long.valueOf(body.get("targetId").toString());
        String category = (String) body.get("category");
        messagePriorityService.setCategory(userId, chatType, targetId, category);
        return Result.ok();
    }

    /**
     * 获取用户的会话分类
     */
    @GetMapping("/categories")
    public Result<List<MessageCategory>> getCategories(
            @RequestParam(required = false) String category) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (category != null) {
            return Result.ok(messagePriorityService.getCategoryChats(userId, category));
        }
        return Result.ok(messagePriorityService.getUserCategories(userId));
    }

    /**
     * 删除会话分类
     */
    @DeleteMapping("/category")
    public Result<Void> removeCategory(
            @RequestParam String chatType,
            @RequestParam Long targetId) {
        Long userId = StpUtil.getLoginIdAsLong();
        messagePriorityService.removeCategory(userId, chatType, targetId);
        return Result.ok();
    }
}
