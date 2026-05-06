package com.pengcheng.system.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.channel.service.ChannelPushService;
import com.pengcheng.system.report.entity.DailyReport;
import com.pengcheng.system.report.mapper.DailyReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * AI 日报服务
 * 自动汇总各维度数据并生成日报模板，支持 LLM 自然语言摘要
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyReportService {

    private final DailyReportMapper reportMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ChannelPushService channelPushService;

    /** 外部注入 LLM 摘要生成器（由 admin-api 层设置） */
    private volatile BiFunction<DailyReport, Void, String> llmSummaryGenerator;

    public List<DailyReport> getUserReports(Long userId, LocalDate start, LocalDate end) {
        return reportMapper.selectList(
            new LambdaQueryWrapper<DailyReport>()
                .eq(DailyReport::getUserId, userId)
                .ge(start != null, DailyReport::getReportDate, start)
                .le(end != null, DailyReport::getReportDate, end)
                .orderByDesc(DailyReport::getReportDate)
        );
    }

    public DailyReport getReport(Long userId, LocalDate date) {
        return reportMapper.findByUserAndDate(userId, date.toString());
    }

    /**
     * 自动生成日报（汇总当日各维度数据）
     */
    public DailyReport generateReport(Long userId, LocalDate date) {
        DailyReport existing = getReport(userId, date);
        if (existing != null) return existing;

        DailyReport report = new DailyReport();
        report.setUserId(userId);
        report.setReportDate(date);

        collectCustomerData(report, userId, date);
        collectDealData(report, userId, date);
        collectTodoData(report, userId, date);
        generateSummary(report);

        reportMapper.insert(report);
        return report;
    }

    private void collectCustomerData(DailyReport report, Long userId, LocalDate date) {
        try {
            String followUpSql = "SELECT COUNT(*) FROM realty_customer WHERE creator_id = ? AND DATE(updated_at) = ?";
            Integer followUp = jdbcTemplate.queryForObject(followUpSql, Integer.class, userId, date);
            report.setCustomerFollowUp(followUp != null ? followUp : 0);

            String newSql = "SELECT COUNT(*) FROM realty_customer WHERE creator_id = ? AND DATE(created_at) = ?";
            Integer newCount = jdbcTemplate.queryForObject(newSql, Integer.class, userId, date);
            report.setNewCustomers(newCount != null ? newCount : 0);
        } catch (Exception e) {
            log.debug("采集客户数据时表可能不存在: {}", e.getMessage());
            report.setCustomerFollowUp(0);
            report.setNewCustomers(0);
        }
    }

    private void collectDealData(DailyReport report, Long userId, LocalDate date) {
        try {
            String dealSql = "SELECT COUNT(*), COALESCE(SUM(deal_amount), 0) FROM realty_commission WHERE creator_id = ? AND DATE(deal_date) = ?";
            Map<String, Object> deal = jdbcTemplate.queryForMap(dealSql, userId, date);
            report.setDealCount(((Number) deal.getOrDefault("COUNT(*)", 0)).intValue());
            report.setDealAmount((BigDecimal) deal.getOrDefault("COALESCE(SUM(deal_amount), 0)", BigDecimal.ZERO));
        } catch (Exception e) {
            log.debug("采集签约数据时表可能不存在: {}", e.getMessage());
            report.setDealCount(0);
            report.setDealAmount(BigDecimal.ZERO);
        }
    }

    private void collectTodoData(DailyReport report, Long userId, LocalDate date) {
        try {
            String completedSql = "SELECT COUNT(*) FROM sys_todo WHERE user_id = ? AND status = 2 AND DATE(completed_at) = ?";
            Integer completed = jdbcTemplate.queryForObject(completedSql, Integer.class, userId, date);
            report.setTodoCompleted(completed != null ? completed : 0);

            String pendingSql = "SELECT COUNT(*) FROM sys_todo WHERE user_id = ? AND status IN (0, 1)";
            Integer pending = jdbcTemplate.queryForObject(pendingSql, Integer.class, userId);
            report.setTodoPending(pending != null ? pending : 0);
        } catch (Exception e) {
            log.debug("采集待办数据异常: {}", e.getMessage());
            report.setTodoCompleted(0);
            report.setTodoPending(0);
        }
    }

    public void setLlmSummaryGenerator(BiFunction<DailyReport, Void, String> gen) {
        this.llmSummaryGenerator = gen;
    }

    private void generateSummary(DailyReport report) {
        if (llmSummaryGenerator != null) {
            try {
                String llmSummary = llmSummaryGenerator.apply(report, null);
                if (llmSummary != null && !llmSummary.isBlank()) {
                    report.setSummary(llmSummary);
                    int sugIdx = llmSummary.indexOf("建议");
                    report.setAiSuggestions(sugIdx >= 0 ? llmSummary.substring(sugIdx) : llmSummary);
                    return;
                }
            } catch (Exception e) {
                log.warn("[DailyReport] LLM 摘要生成失败，回退到模板: {}", e.getMessage());
            }
        }
        generateTemplateSummary(report);
    }

    private void generateTemplateSummary(DailyReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 ").append(report.getReportDate()).append(" 工作日报\n\n");

        sb.append("【客户跟进】跟进客户 ").append(report.getCustomerFollowUp())
          .append(" 位，新增客户 ").append(report.getNewCustomers()).append(" 位\n");

        if (report.getDealCount() > 0) {
            sb.append("【签约成交】签约 ").append(report.getDealCount())
              .append(" 单，金额 ¥").append(report.getDealAmount()).append("\n");
        }

        sb.append("【待办事项】今日完成 ").append(report.getTodoCompleted())
          .append(" 项，剩余待办 ").append(report.getTodoPending()).append(" 项\n");

        sb.append("\n💡 改进建议：");
        if (report.getCustomerFollowUp() == 0) {
            sb.append("今日无客户跟进记录，建议安排客户拜访或电话回访。");
        } else if (report.getCustomerFollowUp() < 3) {
            sb.append("跟进客户较少，可适当增加客户触达量以提升成交概率。");
        } else {
            sb.append("跟进节奏良好，继续保持。注意重点客户的深度跟进。");
        }

        report.setSummary(sb.toString());
        report.setAiSuggestions(sb.substring(sb.indexOf("💡")));
    }

    /**
     * 批量生成所有销售人员日报
     */
    public int generateAllReports(LocalDate date) {
        int count = 0;
        StringBuilder teamSummary = new StringBuilder();
        teamSummary.append("📋 **团队日报汇总** (").append(date).append(")\n\n");

        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT u.user_id, u.nick_name FROM sys_user u JOIN sys_user_role ur ON u.user_id = ur.user_id WHERE u.status = '0'");
            for (Map<String, Object> user : users) {
                Long userId = ((Number) user.get("user_id")).longValue();
                String nickName = (String) user.getOrDefault("nick_name", "用户" + userId);
                try {
                    DailyReport report = generateReport(userId, date);
                    count++;
                    teamSummary.append("**").append(nickName).append("**：")
                            .append("跟进 ").append(report.getCustomerFollowUp()).append(" 位客户，")
                            .append("新增 ").append(report.getNewCustomers()).append(" 位");
                    if (report.getDealCount() > 0) {
                        teamSummary.append("，签约 ").append(report.getDealCount()).append(" 单");
                    }
                    teamSummary.append("\n");
                } catch (Exception e) {
                    log.error("生成用户 {} 日报失败", userId, e);
                }
            }
        } catch (Exception e) {
            log.error("批量生成日报失败", e);
        }

        if (count > 0) {
            pushDailyReportToChannels(date, teamSummary.toString(), count);
        }
        return count;
    }

    /**
     * 将日报汇总推送到多渠道（钉钉/飞书/企微）
     */
    private void pushDailyReportToChannels(LocalDate date, String teamSummary, int totalCount) {
        try {
            String title = date + " 团队工作日报（共 " + totalCount + " 人）";
            channelPushService.broadcast(title, teamSummary, "daily_report");
            log.info("[DailyReport] 日报已推送到多渠道");
        } catch (Exception e) {
            log.warn("[DailyReport] 日报推送失败（不影响日报生成）: {}", e.getMessage());
        }
    }
}
