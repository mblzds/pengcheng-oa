package com.pengcheng.admin.schedule;

import com.pengcheng.system.automation.service.AutomationService;
import com.pengcheng.realty.commission.service.CommissionService;
import com.pengcheng.system.calendar.entity.CalendarEvent;
import com.pengcheng.system.calendar.service.CalendarService;
import com.pengcheng.system.report.service.DailyReportService;
import com.pengcheng.system.quality.service.SalesQualityService;
import com.pengcheng.realty.receivable.service.ReceivableService;
import com.pengcheng.system.heartbeat.service.HeartbeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 综合定时调度：自动化规则 + 日历提醒 + 日报生成 + 质检评分 + AI 巡检
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutomationScheduler {

    private final AutomationService automationService;
    private final CalendarService calendarService;
    private final DailyReportService dailyReportService;
    private final SalesQualityService salesQualityService;
    private final HeartbeatService heartbeatService;
    private final ReceivableService receivableService;
    private final CommissionService commissionService;

    /**
     * 每天 8:00 执行时间触发类规则
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void executeTimeRules() {
        log.info("[Automation] 开始执行定时规则...");
        automationService.executeTimeBasedRules();
        log.info("[Automation] 定时规则执行完成");
    }

    /**
     * 每 5 分钟检查日历提醒
     */
    @Scheduled(fixedRate = 300000)
    public void checkCalendarReminders() {
        List<CalendarEvent> pending = calendarService.getPendingReminders();
        for (CalendarEvent event : pending) {
            log.info("[Calendar] 提醒: userId={}, event={}", event.getUserId(), event.getTitle());
            calendarService.markReminderSent(event.getId());
        }
    }

    /**
     * 每晚 22:00 自动生成全员日报
     */
    @Scheduled(cron = "0 0 22 * * ?")
    public void generateDailyReports() {
        log.info("[DailyReport] 开始生成全员日报...");
        int count = dailyReportService.generateAllReports(LocalDate.now());
        log.info("[DailyReport] 已生成 {} 份日报", count);
    }

    /**
     * 每月 1 日凌晨 4:00 执行全员质检评分
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    public void evaluateTeamQuality() {
        log.info("[Quality] 开始执行全员质检评分...");
        int count = salesQualityService.evaluateTeam(LocalDate.now().minusDays(1));
        log.info("[Quality] 已评估 {} 名销售人员", count);
    }

    /**
     * 每天 9:00 执行 AI 巡检（客户跟进/佣金/合同/回款逾期检查）
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void runHeartbeat() {
        log.info("[Heartbeat] 开始执行 AI 巡检...");
        int count = heartbeatService.runFullCheck();
        log.info("[Heartbeat] 巡检完成，共生成 {} 条告警", count);
    }

    /**
     * 每天 08:30 执行回款巡检（刷新分期状态 + 写入逾期/即将到期告警）
     */
    @Scheduled(cron = "0 30 8 * * ?")
    public void runReceivableCheck() {
        log.info("[Receivable] 开始执行回款巡检...");
        int[] result = receivableService.runOverdueCheck();
        log.info("[Receivable] 巡检完成：新增逾期 {} 条，新增即将到期 {} 条", result[0], result[1]);
    }

    /**
     * 每月最后一天 23:00 自动扫描满足结佣条件的成交，生成待审核佣金单。
     */
    @Scheduled(cron = "0 0 23 L * ?")
    public void autoCreatePendingCommissions() {
        log.info("[Commission] 开始执行月末自动结算扫描...");
        int created = commissionService.autoCreatePendingCommissions();
        log.info("[Commission] 自动结算扫描完成：新增待审核佣金 {} 条", created);
    }
}
