package com.pengcheng.realty.receivable.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.event.DataChangeEvent;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.receivable.dto.ReceivablePlanCreateDTO;
import com.pengcheng.realty.receivable.dto.ReceivablePlanQueryDTO;
import com.pengcheng.realty.receivable.dto.ReceivableRecordCreateDTO;
import com.pengcheng.realty.receivable.entity.ReceivableAlert;
import com.pengcheng.realty.receivable.entity.ReceivablePlan;
import com.pengcheng.realty.receivable.entity.ReceivableRecord;
import com.pengcheng.realty.receivable.mapper.ReceivableAlertMapper;
import com.pengcheng.realty.receivable.mapper.ReceivablePlanMapper;
import com.pengcheng.realty.receivable.mapper.ReceivableRecordMapper;
import com.pengcheng.realty.receivable.vo.ReceivableStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 回款管理服务
 * <ul>
 *   <li>按成交生成回款计划（分期）</li>
 *   <li>登记实际到账流水 → 自动更新分期状态</li>
 *   <li>逾期判定 + 告警写入</li>
 *   <li>回款总览统计</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivablePlanMapper planMapper;
    private final ReceivableRecordMapper recordMapper;
    private final ReceivableAlertMapper alertMapper;
    private final CustomerDealMapper customerDealMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** 即将到期告警阈值：未来 N 天内 */
    public static final int UPCOMING_WINDOW_DAYS = 3;

    // ==================== 1. 生成回款计划 ====================

    /**
     * 按成交一次性生成 N 期回款计划。
     * <p>
     * 校验：
     * <ul>
     *   <li>成交记录必须存在</li>
     *   <li>分期列表非空且期号不重复</li>
     *   <li>分期金额合计可不等于成交金额（允许首付/贷款差额，但给 warn 日志）</li>
     * </ul>
     */
    @Transactional
    public List<Long> createPlan(ReceivablePlanCreateDTO dto) {
        if (dto == null || dto.getDealId() == null) {
            throw new IllegalArgumentException("成交记录ID不能为空");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("分期列表不能为空");
        }

        CustomerDeal deal = customerDealMapper.selectById(dto.getDealId());
        if (deal == null) {
            throw new IllegalArgumentException("成交记录不存在：" + dto.getDealId());
        }

        BigDecimal sum = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        java.util.Set<Integer> periodNos = new java.util.HashSet<>();

        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        for (ReceivablePlanCreateDTO.Item item : dto.getItems()) {
            if (item.getPeriodNo() == null || item.getPeriodNo() <= 0) {
                throw new IllegalArgumentException("期号必须大于0");
            }
            if (!periodNos.add(item.getPeriodNo())) {
                throw new IllegalArgumentException("期号重复：" + item.getPeriodNo());
            }
            if (item.getDueDate() == null) {
                throw new IllegalArgumentException("应付日期不能为空");
            }
            if (item.getDueAmount() == null
                    || item.getDueAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("应付金额必须大于0");
            }
            sum = sum.add(item.getDueAmount());

            int initStatus = item.getDueDate().isBefore(today)
                    ? ReceivablePlan.STATUS_OVERDUE
                    : ReceivablePlan.STATUS_PENDING;

            ReceivablePlan plan = ReceivablePlan.builder()
                    .dealId(dto.getDealId())
                    .periodNo(item.getPeriodNo())
                    .periodName(item.getPeriodName())
                    .dueDate(item.getDueDate())
                    .dueAmount(item.getDueAmount())
                    .paidAmount(BigDecimal.ZERO)
                    .status(initStatus)
                    .remark(item.getRemark())
                    .build();
            try {
                planMapper.insert(plan);
            } catch (DuplicateKeyException e) {
                throw new IllegalArgumentException(
                        "该成交已存在期号 " + item.getPeriodNo() + " 的分期，不可重复", e);
            }
            ids.add(plan.getId());
        }

        if (deal.getDealAmount() != null
                && deal.getDealAmount().compareTo(sum) != 0) {
            log.warn("[回款] 分期合计 {} 与成交金额 {} 不一致 dealId={}",
                    sum, deal.getDealAmount(), dto.getDealId());
        }

        eventPublisher.publishEvent(
                new DataChangeEvent(this, "create", "receivable_plan", dto.getDealId()));
        return ids;
    }

    // ==================== 2. 登记到账 ====================

    /**
     * 登记一笔实际到账流水，并自动更新所属分期的 paidAmount/status。
     */
    @Transactional
    public Long registerRecord(ReceivableRecordCreateDTO dto) {
        if (dto.getPlanId() == null) {
            throw new IllegalArgumentException("计划分期ID不能为空");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("到账金额必须大于0");
        }
        if (dto.getPaidDate() == null) {
            dto.setPaidDate(LocalDate.now());
        }

        ReceivablePlan plan = planMapper.selectById(dto.getPlanId());
        if (plan == null) {
            throw new IllegalArgumentException("计划分期不存在：" + dto.getPlanId());
        }

        BigDecimal newPaid = plan.getPaidAmount().add(dto.getAmount());
        if (newPaid.compareTo(plan.getDueAmount()) > 0) {
            log.warn("[回款] 累计到账 {} 超过应付 {} planId={}",
                    newPaid, plan.getDueAmount(), plan.getId());
        }

        ReceivableRecord record = ReceivableRecord.builder()
                .planId(dto.getPlanId())
                .amount(dto.getAmount())
                .paidDate(dto.getPaidDate())
                .payWay(dto.getPayWay() == null ? 1 : dto.getPayWay())
                .payer(dto.getPayer())
                .voucherNo(dto.getVoucherNo())
                .attachmentUrl(dto.getAttachmentUrl())
                .remark(dto.getRemark())
                .build();
        recordMapper.insert(record);

        plan.setPaidAmount(newPaid);
        plan.setStatus(resolveStatus(plan, LocalDate.now()));
        planMapper.updateById(plan);

        // 若已结清，关闭未处理的逾期告警
        if (plan.getStatus() == ReceivablePlan.STATUS_PAID) {
            closeAlertsOfPlan(plan.getId(), 1L, "回款已结清自动关闭");
        }

        eventPublisher.publishEvent(
                new DataChangeEvent(this, "pay", "receivable_plan", plan.getId()));
        return record.getId();
    }

    // ==================== 3. 状态判定 ====================

    /**
     * 根据应付金额、已付金额、应付日期综合判断状态。
     * 状态转换优先级：PAID > PARTIAL > OVERDUE > PENDING > NOT_DUE
     */
    int resolveStatus(ReceivablePlan plan, LocalDate today) {
        BigDecimal due = plan.getDueAmount() == null ? BigDecimal.ZERO : plan.getDueAmount();
        BigDecimal paid = plan.getPaidAmount() == null ? BigDecimal.ZERO : plan.getPaidAmount();

        if (paid.compareTo(due) >= 0 && due.compareTo(BigDecimal.ZERO) > 0) {
            return ReceivablePlan.STATUS_PAID;
        }
        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            // 部分回款：如果已逾期则归为 OVERDUE（保证调度器能继续告警）
            if (plan.getDueDate() != null && plan.getDueDate().isBefore(today)) {
                return ReceivablePlan.STATUS_OVERDUE;
            }
            return ReceivablePlan.STATUS_PARTIAL;
        }
        if (plan.getDueDate() != null && plan.getDueDate().isBefore(today)) {
            return ReceivablePlan.STATUS_OVERDUE;
        }
        if (plan.getDueDate() != null && !plan.getDueDate().isAfter(today)) {
            return ReceivablePlan.STATUS_PENDING;
        }
        return ReceivablePlan.STATUS_NOT_DUE;
    }

    // ==================== 4. 逾期扫描（供定时任务调用） ====================

    /**
     * 扫描所有未结清分期并刷新状态，对逾期/即将到期写告警记录。
     *
     * @return [overdueNew, upcomingNew] 本次新增告警数
     */
    @Transactional
    public int[] runOverdueCheck() {
        LocalDate today = LocalDate.now();
        LocalDate upcomingLimit = today.plusDays(UPCOMING_WINDOW_DAYS);
        List<ReceivablePlan> pending = planMapper.selectList(new LambdaQueryWrapper<ReceivablePlan>()
                .ne(ReceivablePlan::getStatus, ReceivablePlan.STATUS_PAID));

        int overdueNew = 0, upcomingNew = 0;
        for (ReceivablePlan p : pending) {
            int before = p.getStatus() == null ? -1 : p.getStatus();
            int now = resolveStatus(p, today);
            if (now != before) {
                p.setStatus(now);
                planMapper.updateById(p);
            }

            if (now == ReceivablePlan.STATUS_OVERDUE) {
                if (upsertAlert(p.getId(), ReceivableAlert.TYPE_OVERDUE)) {
                    overdueNew++;
                }
            } else if (p.getDueDate() != null
                    && !p.getDueDate().isBefore(today)
                    && !p.getDueDate().isAfter(upcomingLimit)) {
                if (upsertAlert(p.getId(), ReceivableAlert.TYPE_UPCOMING)) {
                    upcomingNew++;
                }
            }
        }

        log.info("[回款巡检] 共扫描 {} 期，新增逾期告警 {}，新增到期提醒 {}",
                pending.size(), overdueNew, upcomingNew);
        return new int[]{overdueNew, upcomingNew};
    }

    /**
     * 告警去重：按 (plan_id, alert_type) 唯一索引。
     * 首次插入 → 返回 true；已存在 → 更新 last_notified + notify_count，返回 false。
     */
    private boolean upsertAlert(Long planId, int type) {
        ReceivableAlert existing = alertMapper.selectOne(new LambdaQueryWrapper<ReceivableAlert>()
                .eq(ReceivableAlert::getPlanId, planId)
                .eq(ReceivableAlert::getAlertType, type)
                .eq(ReceivableAlert::getHandled, ReceivableAlert.HANDLED_NO));

        if (existing == null) {
            try {
                alertMapper.insert(ReceivableAlert.builder()
                        .planId(planId)
                        .alertType(type)
                        .alertTime(LocalDateTime.now())
                        .lastNotified(LocalDateTime.now())
                        .notifyCount(1)
                        .handled(ReceivableAlert.HANDLED_NO)
                        .build());
                return true;
            } catch (DuplicateKeyException e) {
                // 并发情况下回退为更新
                log.debug("[回款告警] 唯一索引冲突，改走更新 planId={} type={}", planId, type);
            }
        }
        ReceivableAlert update = existing != null ? existing :
                alertMapper.selectOne(new LambdaQueryWrapper<ReceivableAlert>()
                        .eq(ReceivableAlert::getPlanId, planId)
                        .eq(ReceivableAlert::getAlertType, type));
        if (update != null) {
            update.setLastNotified(LocalDateTime.now());
            update.setNotifyCount((update.getNotifyCount() == null ? 0 : update.getNotifyCount()) + 1);
            alertMapper.updateById(update);
        }
        return false;
    }

    private void closeAlertsOfPlan(Long planId, Long handledBy, String remark) {
        List<ReceivableAlert> open = alertMapper.selectList(new LambdaQueryWrapper<ReceivableAlert>()
                .eq(ReceivableAlert::getPlanId, planId)
                .eq(ReceivableAlert::getHandled, ReceivableAlert.HANDLED_NO));
        for (ReceivableAlert a : open) {
            a.setHandled(ReceivableAlert.HANDLED_YES);
            a.setHandledBy(handledBy);
            a.setHandledAt(LocalDateTime.now());
            a.setHandledRemark(remark);
            alertMapper.updateById(a);
        }
    }

    // ==================== 5. 查询 ====================

    public PageResult<ReceivablePlan> pagePlans(ReceivablePlanQueryDTO q) {
        LambdaQueryWrapper<ReceivablePlan> w = new LambdaQueryWrapper<>();
        if (q.getDealId() != null) w.eq(ReceivablePlan::getDealId, q.getDealId());
        if (q.getStatus() != null) w.eq(ReceivablePlan::getStatus, q.getStatus());
        if (q.getDueDateFrom() != null) w.ge(ReceivablePlan::getDueDate, q.getDueDateFrom());
        if (q.getDueDateTo() != null) w.le(ReceivablePlan::getDueDate, q.getDueDateTo());
        w.orderByAsc(ReceivablePlan::getDueDate);

        IPage<ReceivablePlan> page = planMapper.selectPage(
                new Page<>(q.getPage(), q.getPageSize()), w);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<ReceivableRecord> listRecordsOfPlan(Long planId) {
        return recordMapper.selectList(new LambdaQueryWrapper<ReceivableRecord>()
                .eq(ReceivableRecord::getPlanId, planId)
                .orderByDesc(ReceivableRecord::getPaidDate));
    }

    public List<ReceivableAlert> listOpenAlerts() {
        return alertMapper.selectList(new LambdaQueryWrapper<ReceivableAlert>()
                .eq(ReceivableAlert::getHandled, ReceivableAlert.HANDLED_NO)
                .orderByDesc(ReceivableAlert::getAlertTime));
    }

    /** 回款总览统计 */
    public ReceivableStatsVO stats() {
        List<ReceivablePlan> all = planMapper.selectList(new LambdaQueryWrapper<>());
        BigDecimal due = BigDecimal.ZERO, paid = BigDecimal.ZERO, overdue = BigDecimal.ZERO;
        long overdueCount = 0;
        for (ReceivablePlan p : all) {
            due = due.add(nullToZero(p.getDueAmount()));
            paid = paid.add(nullToZero(p.getPaidAmount()));
            if (p.getStatus() != null && p.getStatus() == ReceivablePlan.STATUS_OVERDUE) {
                overdue = overdue.add(nullToZero(p.getDueAmount()).subtract(nullToZero(p.getPaidAmount())));
                overdueCount++;
            }
        }
        return ReceivableStatsVO.builder()
                .totalDue(due)
                .totalPaid(paid)
                .totalUnpaid(due.subtract(paid))
                .totalOverdue(overdue)
                .overdueCount(overdueCount)
                .totalCount((long) all.size())
                .build();
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
