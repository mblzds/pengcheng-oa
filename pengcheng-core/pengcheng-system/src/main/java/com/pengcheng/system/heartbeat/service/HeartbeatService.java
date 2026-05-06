package com.pengcheng.system.heartbeat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.heartbeat.entity.HeartbeatLog;
import com.pengcheng.system.heartbeat.mapper.HeartbeatLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 巡检服务
 * 定时扫描业务数据，自动发现风险并生成告警
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private final HeartbeatLogMapper heartbeatLogMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 执行全量巡检
     */
    public int runFullCheck() {
        int total = 0;
        total += checkCustomerFollowup();
        total += checkCommissionOverdue();
        total += checkContractExpiry();
        total += checkPaymentOverdue();
        total += checkTaskOverdue();
        total += checkMilestoneOverdue();
        log.info("[Heartbeat] 巡检完成，共生成 {} 条告警", total);
        return total;
    }

    /**
     * 项目任务逾期检查（pm_task 截止日已过且未完成 → 通知执行人）
     */
    public int checkTaskOverdue() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT t.id, t.title, t.due_date, t.assignee_id, t.project_id " +
                "FROM pm_task t " +
                "WHERE t.deleted = 0 AND t.due_date < CURDATE() AND t.status != '已完成'"
        );
        int count = 0;
        for (Map<String, Object> row : rows) {
            Long taskId = ((Number) row.get("id")).longValue();
            String title = (String) row.get("title");
            Long assigneeId = row.get("assignee_id") != null ? ((Number) row.get("assignee_id")).longValue() : null;

            if (existsRecent(assigneeId, taskId, "task")) continue;

            HeartbeatLog alert = new HeartbeatLog();
            alert.setCheckType("task_overdue");
            alert.setUserId(assigneeId);
            alert.setTargetId(taskId);
            alert.setTargetType("task");
            alert.setSeverity("warn");
            alert.setTitle("任务逾期：「" + (title != null ? title : "任务#" + taskId) + "」");
            alert.setContent("截止日期 " + row.get("due_date") + " 已过，任务尚未完成。");
            alert.setSuggestion("请尽快更新任务状态或调整截止日期，避免影响项目进度。");
            alert.setHandled(false);
            heartbeatLogMapper.insert(alert);
            count++;
        }
        log.info("[Heartbeat] 任务逾期检查完成，新增 {} 条告警", count);
        return count;
    }

    /**
     * 客户跟进超期检查（超过 7 天未跟进的客户）
     */
    public int checkCustomerFollowup() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.id, c.name, c.creator_id, c.last_follow_time " +
                "FROM realty_customer c " +
                "WHERE c.status NOT IN ('signed', 'lost') " +
                "AND c.last_follow_time < DATE_SUB(NOW(), INTERVAL 7 DAY)"
        );
        int count = 0;
        for (Map<String, Object> row : rows) {
            Long customerId = ((Number) row.get("id")).longValue();
            String customerName = (String) row.get("name");
            Long userId = ((Number) row.get("creator_id")).longValue();

            if (existsRecent(userId, customerId, "customer_followup")) continue;

            HeartbeatLog alert = new HeartbeatLog();
            alert.setCheckType("customer_followup");
            alert.setUserId(userId);
            alert.setTargetId(customerId);
            alert.setTargetType("customer");
            alert.setSeverity("warn");
            alert.setTitle("客户「" + customerName + "」已超 7 天未跟进");
            alert.setContent("该客户最后跟进时间为 " + row.get("last_follow_time") + "，建议尽快联系以避免客户流失。");
            alert.setSuggestion("建议立即电话回访，了解客户最新需求变化。如客户仍有意向，可安排带看或推荐新房源。");
            alert.setHandled(false);
            heartbeatLogMapper.insert(alert);
            count++;
        }
        log.info("[Heartbeat] 客户跟进检查完成，新增 {} 条告警", count);
        return count;
    }

    /**
     * 佣金结算超期检查（超过 30 天待结算）
     */
    public int checkCommissionOverdue() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.id, c.alliance_name, c.expected_amount, c.user_id, c.created_at " +
                "FROM realty_commission c " +
                "WHERE c.status = 'pending' " +
                "AND c.created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)"
        );
        int count = 0;
        for (Map<String, Object> row : rows) {
            Long commissionId = ((Number) row.get("id")).longValue();
            Long userId = row.get("user_id") != null ? ((Number) row.get("user_id")).longValue() : null;
            String allianceName = (String) row.get("alliance_name");

            if (existsRecent(userId, commissionId, "commission")) continue;

            HeartbeatLog alert = new HeartbeatLog();
            alert.setCheckType("commission");
            alert.setUserId(userId);
            alert.setTargetId(commissionId);
            alert.setTargetType("commission");
            alert.setSeverity("warn");
            alert.setTitle("佣金待结算超 30 天 - " + (allianceName != null ? allianceName : "佣金#" + commissionId));
            alert.setContent("该笔佣金创建于 " + row.get("created_at") + "，预计金额 " + row.get("expected_amount") + " 元，已超 30 天未结算。");
            alert.setSuggestion("请联系财务部门确认结算进度，如有争议及时协调解决。");
            alert.setHandled(false);
            heartbeatLogMapper.insert(alert);
            count++;
        }
        log.info("[Heartbeat] 佣金超期检查完成，新增 {} 条告警", count);
        return count;
    }

    /**
     * 合同到期提醒（30 天内即将到期）
     */
    public int checkContractExpiry() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.id, c.name AS customer_name, c.creator_id " +
                "FROM realty_customer c " +
                "WHERE c.status = 'signed' " +
                "AND c.deal_date IS NOT NULL " +
                "AND DATE_ADD(c.deal_date, INTERVAL 1 YEAR) BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 30 DAY)"
        );
        int count = 0;
        for (Map<String, Object> row : rows) {
            Long customerId = ((Number) row.get("id")).longValue();
            String customerName = (String) row.get("customer_name");
            Long userId = ((Number) row.get("creator_id")).longValue();

            if (existsRecent(userId, customerId, "contract")) continue;

            HeartbeatLog alert = new HeartbeatLog();
            alert.setCheckType("contract");
            alert.setUserId(userId);
            alert.setTargetId(customerId);
            alert.setTargetType("customer");
            alert.setSeverity("info");
            alert.setTitle("客户「" + customerName + "」签约即将满一年");
            alert.setContent("该客户签约已满一年，建议跟进交房及售后事宜。");
            alert.setSuggestion("可安排客户回访，了解入住体验，维护客户关系以获取转介绍机会。");
            alert.setHandled(false);
            heartbeatLogMapper.insert(alert);
            count++;
        }
        log.info("[Heartbeat] 合同到期检查完成，新增 {} 条告警", count);
        return count;
    }

    /**
     * 回款逾期检查
     */
    public int checkPaymentOverdue() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT p.id, p.customer_id, p.amount, p.due_date, c.name AS customer_name, c.creator_id " +
                "FROM realty_payment p " +
                "JOIN realty_customer c ON p.customer_id = c.id " +
                "WHERE p.status = 'pending' " +
                "AND p.due_date < CURDATE()"
        );
        int count = 0;
        for (Map<String, Object> row : rows) {
            Long paymentId = ((Number) row.get("id")).longValue();
            String customerName = (String) row.get("customer_name");
            Long userId = ((Number) row.get("creator_id")).longValue();

            if (existsRecent(userId, paymentId, "overdue")) continue;

            HeartbeatLog alert = new HeartbeatLog();
            alert.setCheckType("overdue");
            alert.setUserId(userId);
            alert.setTargetId(paymentId);
            alert.setTargetType("payment");
            alert.setSeverity("critical");
            alert.setTitle("回款逾期 - 客户「" + customerName + "」");
            alert.setContent("应付日期 " + row.get("due_date") + "，金额 " + row.get("amount") + " 元，已逾期。");
            alert.setSuggestion("请立即联系客户确认回款计划，必要时通知法务部门介入。");
            alert.setHandled(false);
            heartbeatLogMapper.insert(alert);
            count++;
        }
        log.info("[Heartbeat] 回款逾期检查完成，新增 {} 条告警", count);
        return count;
    }

    /**
     * 里程碑到期提醒（3 天内即将到期或已逾期的未完成里程碑 → 通知项目全体成员）
     */
    public int checkMilestoneOverdue() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT m.id, m.name, m.due_date, m.project_id " +
                "FROM pm_milestone m " +
                "WHERE m.deleted = 0 AND m.status = 0 " +
                "AND m.due_date <= DATE_ADD(CURDATE(), INTERVAL 3 DAY)"
        );
        int count = 0;
        for (Map<String, Object> row : rows) {
            Long milestoneId = ((Number) row.get("id")).longValue();
            Long projectId = ((Number) row.get("project_id")).longValue();
            String name = (String) row.get("name");
            boolean overdue = row.get("due_date") != null &&
                    LocalDate.parse(row.get("due_date").toString()).isBefore(LocalDate.now());
            String severity = overdue ? "warn" : "info";
            String prefix = overdue ? "里程碑已逾期" : "里程碑即将到期";

            List<Map<String, Object>> members = jdbcTemplate.queryForList(
                    "SELECT user_id FROM pm_project_member WHERE project_id = ? AND deleted = 0", projectId);

            for (Map<String, Object> mr : members) {
                Long userId = ((Number) mr.get("user_id")).longValue();
                if (existsRecent(userId, milestoneId, "milestone")) continue;

                HeartbeatLog alert = new HeartbeatLog();
                alert.setCheckType("milestone_due");
                alert.setUserId(userId);
                alert.setTargetId(milestoneId);
                alert.setTargetType("milestone");
                alert.setSeverity(severity);
                alert.setTitle(prefix + "：「" + name + "」");
                alert.setContent("目标日期 " + row.get("due_date") + (overdue ? " 已过" : " 即将到达") + "，里程碑尚未完成。");
                alert.setSuggestion("请检查里程碑关联任务进度，确保按时交付。");
                alert.setHandled(false);
                heartbeatLogMapper.insert(alert);
                count++;
            }
        }
        log.info("[Heartbeat] 里程碑到期检查完成，新增 {} 条告警", count);
        return count;
    }

    /** 避免重复告警：同一目标 3 天内不重复生成 */
    private boolean existsRecent(Long userId, Long targetId, String targetType) {
        Long count = heartbeatLogMapper.selectCount(
                new LambdaQueryWrapper<HeartbeatLog>()
                        .eq(userId != null, HeartbeatLog::getUserId, userId)
                        .eq(HeartbeatLog::getTargetId, targetId)
                        .eq(HeartbeatLog::getTargetType, targetType)
                        .ge(HeartbeatLog::getCreatedAt, LocalDateTime.now().minusDays(3))
        );
        return count > 0;
    }

    // ==================== 查询接口 ====================

    /**
     * 获取用户的巡检告警列表
     */
    public Page<HeartbeatLog> getUserAlerts(Long userId, String severity, Boolean handled, int page, int pageSize) {
        LambdaQueryWrapper<HeartbeatLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, HeartbeatLog::getUserId, userId);
        if (severity != null && !severity.isEmpty()) {
            wrapper.eq(HeartbeatLog::getSeverity, severity);
        }
        if (handled != null) {
            wrapper.eq(HeartbeatLog::getHandled, handled);
        }
        wrapper.orderByDesc(HeartbeatLog::getCreatedAt);
        return heartbeatLogMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 标记告警为已处理
     */
    public void markHandled(Long id) {
        HeartbeatLog log = heartbeatLogMapper.selectById(id);
        if (log != null) {
            log.setHandled(true);
            log.setHandledAt(LocalDateTime.now());
            heartbeatLogMapper.updateById(log);
        }
    }

    /**
     * 批量标记已处理
     */
    public void batchMarkHandled(List<Long> ids) {
        for (Long id : ids) {
            markHandled(id);
        }
    }

    /**
     * 获取未处理告警统计
     */
    public Map<String, Object> getAlertStats(Long userId) {
        int unhandled = heartbeatLogMapper.countUnhandled(userId);
        Long critical = heartbeatLogMapper.selectCount(
                new LambdaQueryWrapper<HeartbeatLog>()
                        .eq(userId != null, HeartbeatLog::getUserId, userId)
                        .eq(HeartbeatLog::getHandled, false)
                        .eq(HeartbeatLog::getSeverity, "critical")
        );
        Long warn = heartbeatLogMapper.selectCount(
                new LambdaQueryWrapper<HeartbeatLog>()
                        .eq(userId != null, HeartbeatLog::getUserId, userId)
                        .eq(HeartbeatLog::getHandled, false)
                        .eq(HeartbeatLog::getSeverity, "warn")
        );
        return Map.of(
                "unhandled", unhandled,
                "critical", critical,
                "warn", warn
        );
    }
}
