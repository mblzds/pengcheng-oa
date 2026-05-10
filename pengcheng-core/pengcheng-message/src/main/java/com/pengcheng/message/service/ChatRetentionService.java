package com.pengcheng.message.service;

/**
 * 聊天消息留存清理服务。物理删除超过保留期的私聊 / 群聊消息。
 * 由 admin-api 的 ChatMessageRetentionScheduler 每月调用，也可手动触发用于运维。
 */
public interface ChatRetentionService {

    /**
     * 清理超过 retentionDays 的私聊消息（sys_chat_message）。
     * 内部按 batchSize 分批 DELETE，避免单次大事务锁表；累计批数有上限保护。
     *
     * @return 实际删除行数
     */
    int cleanupPrivate(int retentionDays, int batchSize);

    /** 同上，针对群聊（sys_chat_group_message） */
    int cleanupGroup(int retentionDays, int batchSize);
}
