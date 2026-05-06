package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.ai.service.AiLlmService;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.report.entity.DailyReport;
import com.pengcheng.system.report.service.DailyReportService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * AI 日报接口
 */
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class DailyReportController {

    private final DailyReportService reportService;
    @Autowired(required = false)
    private AiLlmService aiLlmService;

    @PostConstruct
    void initLlm() {
        if (aiLlmService != null) {
            reportService.setLlmSummaryGenerator((report, v) ->
                    aiLlmService.generateDailyReportSummary(
                            report.getCustomerFollowUp(), report.getNewCustomers(),
                            report.getDealCount(),
                            report.getDealAmount() != null ? report.getDealAmount().toPlainString() : "0",
                            report.getTodoCompleted(), report.getTodoPending()));
        }
    }

    @GetMapping("/list")
    public Result<List<DailyReport>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(reportService.getUserReports(userId, start, end));
    }

    @GetMapping("/detail")
    public Result<DailyReport> detail(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(reportService.getReport(userId, date));
    }

    @PostMapping("/generate")
    public Result<DailyReport> generate(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (date == null) date = LocalDate.now();
        return Result.ok(reportService.generateReport(userId, date));
    }

    @PostMapping("/generate-all")
    public Result<Map<String, Object>> generateAll(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        int count = reportService.generateAllReports(date);
        return Result.ok(Map.of("generated", count, "date", date.toString()));
    }
}
