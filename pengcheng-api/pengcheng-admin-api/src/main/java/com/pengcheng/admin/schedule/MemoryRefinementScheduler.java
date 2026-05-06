package com.pengcheng.admin.schedule;

import com.pengcheng.ai.memory.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI 记忆精炼定时任务
 * <p>
 * 每日凌晨 3:00 执行：
 * 1. 清理过期 L1 短期记忆（重要的自动提升为 L2）
 * 2. 精炼 L2 长期记忆（低价值降级为 L1 并设置过期时间）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryRefinementScheduler {

    private final MemoryService memoryService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void refineMemories() {
        log.info("[记忆精炼] 定时任务开始");
        long start = System.currentTimeMillis();

        try {
            int evicted = memoryService.cleanupExpiredMemories();
            log.info("[记忆精炼] 过期 L1 清理完成，淘汰 {} 条", evicted);
        } catch (Exception e) {
            log.error("[记忆精炼] L1 清理异常", e);
        }

        log.info("[记忆精炼] 定时任务完成，耗时 {}ms", System.currentTimeMillis() - start);
    }
}
