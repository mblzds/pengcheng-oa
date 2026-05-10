package com.pengcheng.admin.schedule;

import com.pengcheng.message.service.ChatRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 聊天消息保留期清理任务。
 * 每月 1 号凌晨 04:00 跑一次，私聊 > 365 天 / 群聊 > 1095 天的消息物理删除。
 * 配置见 application.yml 的 pengcheng.chat.retention.*；改 0 即可临时禁用相应分支。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageRetentionScheduler {

    private final ChatRetentionService retentionService;

    @Value("${pengcheng.chat.retention.private-days:365}")
    private int privateDays;

    @Value("${pengcheng.chat.retention.group-days:1095}")
    private int groupDays;

    @Value("${pengcheng.chat.retention.cleanup-batch:1000}")
    private int batchSize;

    @Scheduled(cron = "0 0 4 1 * ?")
    public void cleanup() {
        log.info("[chat retention] 开始按月清理: privateDays={}, groupDays={}, batchSize={}",
                privateDays, groupDays, batchSize);
        int p = retentionService.cleanupPrivate(privateDays, batchSize);
        int g = retentionService.cleanupGroup(groupDays, batchSize);
        log.info("[chat retention] 完成: private 删除 {} 条, group 删除 {} 条", p, g);
    }
}
