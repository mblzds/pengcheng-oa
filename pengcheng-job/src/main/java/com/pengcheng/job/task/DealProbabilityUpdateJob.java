package com.pengcheng.job.task;

import com.pengcheng.ai.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 成交概率评分批量更新定时任务
 * <p>
 * 每日批量更新所有活跃客户（状态为已报备或已到访）的成交概率评分。
 * 通过 Quartz 调度框架配置执行周期（建议每日凌晨执行）。
 */
@Slf4j
@Component("dealProbabilityUpdateJob")
@RequiredArgsConstructor
public class DealProbabilityUpdateJob {

    private final AiAnalysisService aiAnalysisService;

    /**
     * 执行批量更新成交概率评分
     */
    public void execute() {
        log.info("开始执行成交概率评分批量更新任务...");
        try {
            int count = aiAnalysisService.batchUpdateDealProbability();
            log.info("成交概率评分批量更新任务完成，更新 {} 个客户", count);
        } catch (Exception e) {
            log.error("成交概率评分批量更新任务执行失败: {}", e.getMessage(), e);
        }
    }
}
