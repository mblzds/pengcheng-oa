package com.pengcheng.message.service.impl;

import com.pengcheng.message.mapper.ChatGroupMessageMapper;
import com.pengcheng.message.mapper.SysChatMessageMapper;
import com.pengcheng.message.service.ChatRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRetentionServiceImpl implements ChatRetentionService {

    private final SysChatMessageMapper privateMapper;
    private final ChatGroupMessageMapper groupMapper;

    /** 单次 cron 最多循环这么多批；按 batchSize=1000 计算上限是 100k 行/次清理，避免长事务 */
    private static final int MAX_BATCHES = 100;

    @Override
    public int cleanupPrivate(int retentionDays, int batchSize) {
        if (retentionDays <= 0) {
            log.warn("[chat retention] privateDays={} 非法，跳过", retentionDays);
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int total = 0;
        for (int i = 0; i < MAX_BATCHES; i++) {
            int deleted = privateMapper.deleteOlderThan(cutoff, batchSize);
            total += deleted;
            if (deleted < batchSize) break;
        }
        if (total > 0) {
            log.info("[chat retention] private cleanup done: cutoff={}, deleted={}", cutoff, total);
        }
        return total;
    }

    @Override
    public int cleanupGroup(int retentionDays, int batchSize) {
        if (retentionDays <= 0) {
            log.warn("[chat retention] groupDays={} 非法，跳过", retentionDays);
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int total = 0;
        for (int i = 0; i < MAX_BATCHES; i++) {
            int deleted = groupMapper.deleteOlderThan(cutoff, batchSize);
            total += deleted;
            if (deleted < batchSize) break;
        }
        if (total > 0) {
            log.info("[chat retention] group cleanup done: cutoff={}, deleted={}", cutoff, total);
        }
        return total;
    }
}
