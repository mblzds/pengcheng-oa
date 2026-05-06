package com.pengcheng.admin.controller.realty;

import com.pengcheng.common.result.Result;
import com.pengcheng.realty.dashboard.dto.*;
import com.pengcheng.realty.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 数据统计仪表盘控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class RealtyDashboardController {

    private final DashboardService dashboardService;

    /** 概览空数据，避免异常时前端收到 500 导致「系统繁忙」 */
    private static final DashboardOverviewVO EMPTY_OVERVIEW = DashboardOverviewVO.builder()
            .reportCount(0L)
            .visitCount(0L)
            .dealCount(0L)
            .dealAmount(BigDecimal.ZERO)
            .receivableCommission(BigDecimal.ZERO)
            .settledCommission(BigDecimal.ZERO)
            .todayDealCount(0)
            .totalDealAmount(BigDecimal.ZERO)
            .pendingFollowUp(0)
            .pendingApproval(0)
            .dealCountTrend(0)
            .dealAmountTrend(0)
            .build();

    /**
     * 核心指标概览
     */
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            return Result.ok(dashboardService.getOverview(startDate, endDate));
        } catch (Exception e) {
            log.warn("仪表盘概览查询异常，返回空数据: {}", e.getMessage());
            return Result.ok(EMPTY_OVERVIEW);
        }
    }

    /**
     * 报备-到访-成交转化漏斗
     */
    @GetMapping("/funnel")
    public Result<FunnelVO> funnel(FunnelQueryDTO query) {
        return Result.ok(dashboardService.getFunnel(query));
    }

    /**
     * 业绩排行榜
     */
    @GetMapping("/ranking")
    public Result<RankingVO> ranking(RankingQueryDTO query) {
        return Result.ok(dashboardService.getRanking(query));
    }

    /**
     * AI 洞察推送（工作台首页展示）
     * <p>
     * 当前返回静态模板洞察，后续可对接 LLM 动态生成。
     */
    @GetMapping("/ai-insights")
    public Result<List<Map<String, String>>> aiInsights() {
        try {
            DashboardOverviewVO overview = dashboardService.getOverview(null, null);
            List<Map<String, String>> insights = new java.util.ArrayList<>();

            if (overview.getPendingFollowUp() != null && overview.getPendingFollowUp() > 5) {
                insights.add(Map.of(
                        "icon", "⚠️",
                        "content", "你有 " + overview.getPendingFollowUp() + " 位客户超过 3 天未跟进，建议尽快联系以避免流失。",
                        "level", "warn"
                ));
            }

            if (overview.getTodayDealCount() != null && overview.getTodayDealCount() > 0) {
                insights.add(Map.of(
                        "icon", "🎉",
                        "content", "今日已签约 " + overview.getTodayDealCount() + " 单，继续保持！",
                        "level", "info"
                ));
            }

            if (insights.isEmpty()) {
                insights.add(Map.of(
                        "icon", "💡",
                        "content", "数据分析中...后续将推送业务洞察和建议。",
                        "level", "info"
                ));
            }

            return Result.ok(insights);
        } catch (Exception e) {
            log.warn("AI 洞察查询异常，返回默认提示: {}", e.getMessage());
            return Result.ok(List.of(Map.of(
                    "icon", "💡",
                    "content", "数据分析中...后续将推送业务洞察和建议。",
                    "level", "info"
            )));
        }
    }
}
