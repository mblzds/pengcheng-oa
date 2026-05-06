package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.app.dto.NotificationVO;
import com.pengcheng.app.dto.WorkbenchVO;
import com.pengcheng.common.result.Result;
import com.pengcheng.message.entity.Notification;
import com.pengcheng.message.service.NotificationService;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.LeaveRequest;
import com.pengcheng.hr.attendance.mapper.CompensateRequestMapper;
import com.pengcheng.hr.attendance.mapper.LeaveRequestMapper;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.commission.service.CommissionService;
import com.pengcheng.realty.dashboard.dto.DashboardOverviewVO;
import com.pengcheng.realty.dashboard.service.DashboardService;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import com.pengcheng.realty.payment.mapper.PaymentRequestMapper;
import com.pengcheng.realty.payment.service.PaymentService;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * App端工作台控制器
 * 根据当前用户角色返回对应的统计数据、快捷入口和最近通知
 */
@RestController
@RequestMapping("/app/workbench")
@RequiredArgsConstructor
@SaCheckLogin
public class AppWorkbenchController {

    private final DashboardService dashboardService;
    private final NotificationService notificationService;
    private final SysUserService userService;
    private final LeaveRequestMapper leaveRequestMapper;
    private final CompensateRequestMapper compensateRequestMapper;
    private final PaymentRequestMapper paymentRequestMapper;
    private final CommissionMapper commissionMapper;

    // 角色编码常量
    static final String ROLE_FIELD_AGENT = "field_agent";       // 驻场人员
    static final String ROLE_CHANNEL_AGENT = "channel_agent";   // 渠道人员
    static final String ROLE_FIELD_DIRECTOR = "field_director";  // 驻场总监
    static final String ROLE_CHANNEL_DIRECTOR = "channel_director"; // 渠道总监
    static final String ROLE_ADMIN_DIRECTOR = "admin_director";  // 行政总监
    static final String ROLE_ADMIN_CLERK = "admin_clerk";        // 行政文员
    static final String ROLE_FINANCE = "finance";                // 财务人员

    /**
     * 获取工作台数据
     * 根据当前登录用户角色返回对应的统计卡片、快捷入口和最近通知
     */
    @GetMapping
    public Result<WorkbenchVO> getWorkbench() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<String> roleCodes = userService.getRoleCodes(userId);
        String primaryRole = determinePrimaryRole(roleCodes);

        // 获取角色对应的统计卡片
        List<WorkbenchVO.StatsCard> statsCards = buildStatsCards(primaryRole);

        // 构建九宫格快捷入口
        List<WorkbenchVO.QuickEntry> quickEntries = buildQuickEntries();

        // 获取最近5条通知
        List<Notification> notifications = notificationService.getRecentNotifications(userId, 5);
        List<NotificationVO> recentNotices = notifications.stream()
                .map(this::toNotificationVO)
                .toList();

        // 获取待审批数量
        int pendingCount = countPendingApprovals(userId, primaryRole);

        WorkbenchVO vo = WorkbenchVO.builder()
                .roleCode(primaryRole)
                .statsCards(statsCards)
                .quickEntries(quickEntries)
                .recentNotices(recentNotices)
                .pendingApprovalCount(pendingCount)
                .build();

        return Result.ok(vo);
    }

    /**
     * 确定用户的主要角色（优先级：总监 > 财务 > 行政文员 > 渠道 > 驻场）
     */
    String determinePrimaryRole(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return ROLE_FIELD_AGENT;
        }
        // 总监角色优先
        for (String code : roleCodes) {
            if (code.equals(ROLE_FIELD_DIRECTOR) || code.equals(ROLE_CHANNEL_DIRECTOR)
                    || code.equals(ROLE_ADMIN_DIRECTOR)) {
                return code;
            }
        }
        if (roleCodes.contains(ROLE_FINANCE)) return ROLE_FINANCE;
        if (roleCodes.contains(ROLE_ADMIN_CLERK)) return ROLE_ADMIN_CLERK;
        if (roleCodes.contains(ROLE_CHANNEL_AGENT)) return ROLE_CHANNEL_AGENT;
        if (roleCodes.contains(ROLE_FIELD_AGENT)) return ROLE_FIELD_AGENT;
        return roleCodes.get(0);
    }

    /**
     * 根据角色构建统计卡片
     */
    List<WorkbenchVO.StatsCard> buildStatsCards(String roleCode) {
        LocalDate today = LocalDate.now();
        DashboardOverviewVO overview = dashboardService.getOverview(today, today);
        List<WorkbenchVO.StatsCard> cards = new ArrayList<>();

        switch (roleCode) {
            case ROLE_FIELD_AGENT -> {
                cards.add(card("今日报备数", safeInt(overview.getReportCount()), "report"));
                cards.add(card("今日到访数", safeInt(overview.getVisitCount()), "visit"));
                cards.add(card("待跟进客户数", 0, "follow-up"));
            }
            case ROLE_CHANNEL_AGENT -> {
                cards.add(card("对接联盟商数量", 0, "alliance"));
                cards.add(card("本月上客数", safeInt(overview.getVisitCount()), "visit"));
                cards.add(card("本月成交数", safeInt(overview.getDealCount()), "deal"));
            }
            case ROLE_FIELD_DIRECTOR, ROLE_CHANNEL_DIRECTOR, ROLE_ADMIN_DIRECTOR -> {
                DashboardOverviewVO monthOverview = dashboardService.getOverview(
                        today.withDayOfMonth(1), today);
                cards.add(card("今日报备数", safeInt(overview.getReportCount()), "report"));
                cards.add(card("今日到访数", safeInt(overview.getVisitCount()), "visit"));
                cards.add(card("今日成交数", safeInt(overview.getDealCount()), "deal"));
                cards.add(card("本月业绩汇总", safeInt(monthOverview.getDealCount()), "performance"));
            }
            case ROLE_ADMIN_CLERK -> {
                long pendingCommissions = commissionMapper.selectCount(
                        new LambdaQueryWrapper<Commission>()
                                .eq(Commission::getAuditStatus, CommissionService.AUDIT_STATUS_PENDING));
                long pendingPayments = paymentRequestMapper.selectCount(
                        new LambdaQueryWrapper<PaymentRequest>()
                                .in(PaymentRequest::getStatus,
                                        PaymentService.STATUS_PENDING, PaymentService.STATUS_IN_PROGRESS));
                cards.add(card("待处理佣金录入数", (int) pendingCommissions, "commission"));
                cards.add(card("待处理付款申请数", (int) pendingPayments, "payment"));
            }
            case ROLE_FINANCE -> {
                long pendingAudit = commissionMapper.selectCount(
                        new LambdaQueryWrapper<Commission>()
                                .eq(Commission::getAuditStatus, CommissionService.AUDIT_STATUS_PENDING));
                long pendingPayApproval = paymentRequestMapper.selectCount(
                        new LambdaQueryWrapper<PaymentRequest>()
                                .in(PaymentRequest::getStatus,
                                        PaymentService.STATUS_PENDING, PaymentService.STATUS_IN_PROGRESS));
                cards.add(card("待审核佣金数", (int) pendingAudit, "commission-audit"));
                cards.add(card("待审批付款数", (int) pendingPayApproval, "payment-audit"));
            }
            default -> {
                cards.add(card("今日报备数", safeInt(overview.getReportCount()), "report"));
                cards.add(card("今日到访数", safeInt(overview.getVisitCount()), "visit"));
            }
        }
        return cards;
    }

    /**
     * 构建九宫格快捷入口
     */
    List<WorkbenchVO.QuickEntry> buildQuickEntries() {
        List<WorkbenchVO.QuickEntry> entries = new ArrayList<>();
        entries.add(entry("客户报备", "customer-report", "/pages/customer/report"));
        entries.add(entry("客户列表", "customer-list", "/pages/customer/list"));
        entries.add(entry("到访录入", "visit-add", "/pages/customer/visit"));
        entries.add(entry("考勤打卡", "attendance", "/pages/attendance/clock"));
        entries.add(entry("扫码签到", "scan-sign", "/pages/attendance/sign"));
        entries.add(entry("付款申请", "payment", "/pages/apply/list"));
        entries.add(entry("审批中心", "approval", "/pages/approval/list"));
        entries.add(entry("AI助手", "ai-chat", "/pages/ai/chat"));
        return entries;
    }

    /**
     * 统计当前用户的待审批数量
     */
    int countPendingApprovals(Long userId, String roleCode) {
        // 只有总监和财务角色有审批权限
        if (!isApprover(roleCode)) {
            return 0;
        }

        // 待审批请假
        long pendingLeaves = leaveRequestMapper.selectCount(
                new LambdaQueryWrapper<LeaveRequest>().eq(LeaveRequest::getStatus, 1));
        // 待审批调休
        long pendingCompensates = compensateRequestMapper.selectCount(
                new LambdaQueryWrapper<CompensateRequest>().eq(CompensateRequest::getStatus, 1));
        // 待审批付款
        long pendingPayments = paymentRequestMapper.selectCount(
                new LambdaQueryWrapper<PaymentRequest>()
                        .in(PaymentRequest::getStatus,
                                PaymentService.STATUS_PENDING, PaymentService.STATUS_IN_PROGRESS));
        // 待审核佣金
        long pendingCommissions = commissionMapper.selectCount(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getAuditStatus, CommissionService.AUDIT_STATUS_PENDING));

        return (int) (pendingLeaves + pendingCompensates + pendingPayments + pendingCommissions);
    }

    private boolean isApprover(String roleCode) {
        return ROLE_FIELD_DIRECTOR.equals(roleCode)
                || ROLE_CHANNEL_DIRECTOR.equals(roleCode)
                || ROLE_ADMIN_DIRECTOR.equals(roleCode)
                || ROLE_FINANCE.equals(roleCode);
    }

    private NotificationVO toNotificationVO(Notification n) {
        return NotificationVO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .type(n.getType())
                .bizType(n.getBizType())
                .bizId(n.getBizId())
                .readStatus(n.getReadStatus())
                .createTime(n.getCreateTime())
                .build();
    }

    private static WorkbenchVO.StatsCard card(String label, Integer value, String icon) {
        return WorkbenchVO.StatsCard.builder().label(label).value(value).icon(icon).build();
    }

    private static WorkbenchVO.QuickEntry entry(String label, String icon, String path) {
        return WorkbenchVO.QuickEntry.builder().label(label).icon(icon).path(path).build();
    }

    private static int safeInt(Long value) {
        return value != null ? value.intValue() : 0;
    }
}
