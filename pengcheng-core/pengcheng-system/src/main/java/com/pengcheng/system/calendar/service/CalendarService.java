package com.pengcheng.system.calendar.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.calendar.entity.CalendarEvent;
import com.pengcheng.system.calendar.mapper.CalendarEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 日历服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarEventMapper eventMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 按日期范围查询事件
     */
    public List<CalendarEvent> getEvents(Long userId, LocalDate start, LocalDate end) {
        return eventMapper.findByUserAndRange(userId,
                start.atStartOfDay(),
                end.plusDays(1).atStartOfDay());
    }

    /**
     * 查询指定月份的事件
     */
    public List<CalendarEvent> getMonthEvents(Long userId, int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.plusMonths(1).minusDays(1);
        return getEvents(userId, first, last);
    }

    /**
     * 查询今日事件
     */
    public List<CalendarEvent> getTodayEvents(Long userId) {
        LocalDate today = LocalDate.now();
        return getEvents(userId, today, today);
    }

    /**
     * 查询团队日程（经理可见）
     */
    public List<CalendarEvent> getTeamEvents(Long deptId, LocalDate start, LocalDate end) {
        return eventMapper.selectList(new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getStatus, 1)
                .ge(CalendarEvent::getStartTime, start.atStartOfDay())
                .lt(CalendarEvent::getStartTime, end.plusDays(1).atStartOfDay())
                .orderByAsc(CalendarEvent::getStartTime));
    }

    /**
     * 创建事件
     */
    public CalendarEvent createEvent(CalendarEvent event) {
        event.setStatus(1);
        event.setReminderSent(false);
        eventMapper.insert(event);
        return event;
    }

    /**
     * 更新事件
     */
    public void updateEvent(CalendarEvent event) {
        eventMapper.updateById(event);
    }

    /**
     * 取消事件（软删除）
     */
    public void cancelEvent(Long eventId, Long userId) {
        CalendarEvent event = eventMapper.selectById(eventId);
        if (event != null && event.getUserId().equals(userId)) {
            event.setStatus(0);
            eventMapper.updateById(event);
        }
    }

    /**
     * 获取待发送的提醒
     */
    public List<CalendarEvent> getPendingReminders() {
        return eventMapper.findPendingReminders();
    }

    /**
     * 标记提醒已发送
     */
    public void markReminderSent(Long eventId) {
        CalendarEvent event = new CalendarEvent();
        event.setId(eventId);
        event.setReminderSent(true);
        eventMapper.updateById(event);
    }

    /**
     * 获取合并后的日历事件（包含手动创建 + 客户拜访 + 合同节点）
     */
    public List<CalendarEvent> getMergedEvents(Long userId, LocalDate start, LocalDate end) {
        List<CalendarEvent> events = new ArrayList<>(getEvents(userId, start, end));
        events.addAll(getCustomerVisitEvents(userId, start, end));
        events.addAll(getContractMilestoneEvents(userId, start, end));
        return events;
    }

    /**
     * 从客户跟进记录中自动生成拜访日程
     */
    public List<CalendarEvent> getCustomerVisitEvents(Long userId, LocalDate start, LocalDate end) {
        List<CalendarEvent> events = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT c.id, c.name, c.next_follow_time " +
                    "FROM realty_customer c " +
                    "WHERE c.creator_id = ? " +
                    "AND c.next_follow_time BETWEEN ? AND ? " +
                    "AND c.status NOT IN ('signed', 'lost')",
                    userId, start.atStartOfDay(), end.plusDays(1).atStartOfDay()
            );
            for (Map<String, Object> row : rows) {
                CalendarEvent event = new CalendarEvent();
                event.setId(-((Number) row.get("id")).longValue());
                event.setTitle("跟进：" + row.get("name"));
                event.setEventType("visit");
                event.setColor("#ed8936");
                event.setUserId(userId);
                event.setCustomerId(((Number) row.get("id")).longValue());
                Object followTime = row.get("next_follow_time");
                if (followTime instanceof LocalDateTime) {
                    event.setStartTime((LocalDateTime) followTime);
                    event.setEndTime(((LocalDateTime) followTime).plusHours(1));
                }
                event.setAllDay(false);
                event.setStatus(1);
                events.add(event);
            }
        } catch (Exception e) {
            log.debug("查询客户拜访日程异常: {}", e.getMessage());
        }
        return events;
    }

    /**
     * 从合同/回款数据中自动生成里程碑节点
     */
    public List<CalendarEvent> getContractMilestoneEvents(Long userId, LocalDate start, LocalDate end) {
        List<CalendarEvent> events = new ArrayList<>();
        try {
            List<Map<String, Object>> payments = jdbcTemplate.queryForList(
                    "SELECT p.id, p.amount, p.due_date, c.name AS customer_name " +
                    "FROM realty_payment p " +
                    "JOIN realty_customer c ON p.customer_id = c.id " +
                    "WHERE c.creator_id = ? AND p.due_date BETWEEN ? AND ? AND p.status = 'pending'",
                    userId, start, end
            );
            for (Map<String, Object> row : payments) {
                CalendarEvent event = new CalendarEvent();
                event.setId(-10000 - ((Number) row.get("id")).longValue());
                event.setTitle("回款：" + row.get("customer_name") + " ¥" + row.get("amount"));
                event.setEventType("payment");
                event.setColor("#e53e3e");
                event.setUserId(userId);
                Object dueDate = row.get("due_date");
                if (dueDate instanceof LocalDate) {
                    event.setStartTime(((LocalDate) dueDate).atTime(9, 0));
                    event.setEndTime(((LocalDate) dueDate).atTime(10, 0));
                }
                event.setAllDay(true);
                event.setStatus(1);
                events.add(event);
            }

            List<Map<String, Object>> deals = jdbcTemplate.queryForList(
                    "SELECT c.id, c.name, c.deal_date " +
                    "FROM realty_customer c " +
                    "WHERE c.creator_id = ? AND c.deal_date BETWEEN ? AND ? AND c.status = 'signed'",
                    userId, start, end
            );
            for (Map<String, Object> row : deals) {
                CalendarEvent event = new CalendarEvent();
                event.setId(-20000 - ((Number) row.get("id")).longValue());
                event.setTitle("签约：" + row.get("name"));
                event.setEventType("sign");
                event.setColor("#38a169");
                event.setUserId(userId);
                event.setCustomerId(((Number) row.get("id")).longValue());
                Object dealDate = row.get("deal_date");
                if (dealDate instanceof LocalDate) {
                    event.setStartTime(((LocalDate) dealDate).atTime(10, 0));
                    event.setEndTime(((LocalDate) dealDate).atTime(11, 0));
                }
                event.setAllDay(true);
                event.setStatus(1);
                events.add(event);
            }
        } catch (Exception e) {
            log.debug("查询合同节点异常: {}", e.getMessage());
        }
        return events;
    }
}
