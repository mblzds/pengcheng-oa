package com.pengcheng.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.message.entity.SysChatMessage;

import java.util.List;

/**
 * 聊天消息服务接口
 */
public interface SysChatMessageService {

    SysChatMessage send(SysChatMessage message);

    Page<SysChatMessage> getChatHistory(Long userId, Long targetId, Integer page, Integer pageSize);

    List<SysChatMessage> getRecentContacts(Long userId);

    void markAsRead(Long userId, Long senderId);

    int getUnreadCount(Long userId);

    int getUnreadCountWithUser(Long userId, Long senderId);
    
    SysChatMessage getLatestMessage(Long userId, Long targetId);
    
    void clearHistory(Long userId, Long targetId);

    /**
     * 撤回消息（2 分钟内可撤回）
     */
    boolean recallMessage(Long messageId, Long userId);

    /**
     * 搜索消息
     */
    Page<SysChatMessage> searchMessages(Long userId, String keyword, Integer page, Integer pageSize);

    /**
     * 生成下一个全局消息序列号
     */
    long nextSeq();

    /**
     * 确认消息送达（ACK）
     */
    void ackMessage(Long messageId, Long userId);

    /**
     * 获取用户的离线消息（上线时拉取）
     */
    List<SysChatMessage> getOfflineMessages(Long userId, Long lastSeq);

    /**
     * 批量确认离线消息已投递
     */
    void markOfflineDelivered(Long userId, List<Long> messageIds);
}
