package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.quality.entity.SalesQualityScore;
import com.pengcheng.system.quality.service.SalesQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 销售质检接口
 */
@RestController
@RequestMapping("/quality")
@RequiredArgsConstructor
public class SalesQualityController {

    private final SalesQualityService qualityService;

    @GetMapping("/latest")
    public Result<SalesQualityScore> latest() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(qualityService.getLatestScore(userId));
    }

    @GetMapping("/history")
    public Result<List<SalesQualityScore>> history(@RequestParam(defaultValue = "12") int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(qualityService.getScoreHistory(userId, limit));
    }

    @GetMapping("/ranking")
    public Result<List<SalesQualityScore>> ranking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        return Result.ok(qualityService.getRanking(date));
    }

    @PostMapping("/evaluate")
    public Result<SalesQualityScore> evaluate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (date == null) date = LocalDate.now();
        return Result.ok(qualityService.evaluateSales(userId, date));
    }

    @PostMapping("/evaluate-team")
    public Result<Map<String, Object>> evaluateTeam(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        int count = qualityService.evaluateTeam(date);
        return Result.ok(Map.of("evaluated", count));
    }
}
