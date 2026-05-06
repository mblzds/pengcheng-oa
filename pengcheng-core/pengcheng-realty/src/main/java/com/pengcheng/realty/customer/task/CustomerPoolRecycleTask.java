package com.pengcheng.realty.customer.task;

import com.pengcheng.realty.customer.service.CustomerPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 客户公海池自动回收定时任务
 * <p>
 * 每日执行，将满足回收条件的私海池客户回收至公海池：
 * <ul>
 *   <li>超过配置天数（默认7天）无跟进记录</li>
 *   <li>超过配置天数（默认30天）未到访</li>
 * </ul>
 * 通过 Quartz 调度，调用目标：customerPoolRecycleTask.execute
 */
@Slf4j
@Component("customerPoolRecycleTask")
@RequiredArgsConstructor
public class CustomerPoolRecycleTask {

    private final CustomerPoolService customerPoolService;

    /**
     * 执行公海池回收任务
     */
    public void execute() {
        log.info("开始执行公海池自动回收任务...");
        int recycled = customerPoolService.recycleToPublicPool();
        log.info("公海池自动回收任务完成，共回收 {} 个客户", recycled);
    }
}
