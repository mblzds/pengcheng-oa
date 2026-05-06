package com.pengcheng.admin.controller.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.performance.entity.KpiPeriod;
import com.pengcheng.hr.performance.entity.KpiScore;
import com.pengcheng.hr.performance.entity.KpiTemplate;
import com.pengcheng.hr.performance.dto.KpiReview360DTO;
import com.pengcheng.hr.performance.dto.KpiReview360ResultVO;
import com.pengcheng.hr.performance.dto.Kpi360WeightConfig;
import com.pengcheng.hr.performance.service.KpiPeriodService;
import com.pengcheng.hr.performance.service.KpiScoreService;
import com.pengcheng.hr.performance.service.KpiSuggestService;
import com.pengcheng.hr.performance.service.KpiTemplateService;
import com.pengcheng.hr.performance.service.KpiReview360Service;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 绩效管理 - 考核周期、KPI 模板、考核记录（公司级公共服务）
 */
@Tag(name = "绩效管理", description = "KPI 考核周期、模板、评分、360 度评估")
@RestController("hrKpiControllerKpi")
@RequestMapping("/admin/hr/kpi")
@RequiredArgsConstructor
public class HrKpiController {

    private final KpiPeriodService kpiPeriodService;
    private final KpiTemplateService kpiTemplateService;
    private final KpiScoreService kpiScoreService;
    private final KpiSuggestService kpiSuggestService;
    private final KpiReview360Service kpiReview360Service;

    // ---------- 考核周期 ----------
    @GetMapping("/periods")
    public Result<IPage<KpiPeriod>> periodPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer periodType,
            @RequestParam(required = false) Integer status) {
        return Result.ok(kpiPeriodService.page(new Page<>(pageNum, pageSize), periodType, status));
    }

    @GetMapping("/periods/{id}")
    public Result<KpiPeriod> getPeriod(@PathVariable Long id) {
        return Result.ok(kpiPeriodService.getById(id));
    }

    @PostMapping("/periods")
    @SaCheckPermission("hr:kpi:period")
    @Log(title = "考核周期", businessType = BusinessType.INSERT)
    public Result<Long> createPeriod(@RequestBody KpiPeriod period) {
        return Result.ok(kpiPeriodService.create(period));
    }

    @PutMapping("/periods")
    @SaCheckPermission("hr:kpi:period")
    @Log(title = "考核周期", businessType = BusinessType.UPDATE)
    public Result<Void> updatePeriod(@RequestBody KpiPeriod period) {
        kpiPeriodService.update(period);
        return Result.ok();
    }

    @DeleteMapping("/periods/{id}")
    @SaCheckPermission("hr:kpi:period")
    @Log(title = "考核周期", businessType = BusinessType.DELETE)
    public Result<Void> deletePeriod(@PathVariable Long id) {
        kpiPeriodService.delete(id);
        return Result.ok();
    }

    // ---------- KPI 模板 ----------
    @GetMapping("/templates")
    public Result<IPage<KpiTemplate>> templatePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) Integer status) {
        return Result.ok(kpiTemplateService.page(new Page<>(pageNum, pageSize), category, status));
    }

    @GetMapping("/templates/list")
    public Result<List<KpiTemplate>> templateList(@RequestParam(required = false) Integer status) {
        return Result.ok(kpiTemplateService.listByStatus(status));
    }

    @GetMapping("/templates/{id}")
    public Result<KpiTemplate> getTemplate(@PathVariable Long id) {
        return Result.ok(kpiTemplateService.getById(id));
    }

    @PostMapping("/templates")
    @SaCheckPermission("hr:kpi:template")
    @Log(title = "KPI模板", businessType = BusinessType.INSERT)
    public Result<Long> createTemplate(@RequestBody KpiTemplate template) {
        return Result.ok(kpiTemplateService.create(template));
    }

    @PutMapping("/templates")
    @SaCheckPermission("hr:kpi:template")
    @Log(title = "KPI模板", businessType = BusinessType.UPDATE)
    public Result<Void> updateTemplate(@RequestBody KpiTemplate template) {
        kpiTemplateService.update(template);
        return Result.ok();
    }

    @DeleteMapping("/templates/{id}")
    @SaCheckPermission("hr:kpi:template")
    @Log(title = "KPI模板", businessType = BusinessType.DELETE)
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        kpiTemplateService.delete(id);
        return Result.ok();
    }

    // ---------- 考核记录 ----------
    @GetMapping("/scores")
    public Result<List<KpiScore>> scoreList(
            @RequestParam Long periodId,
            @RequestParam Long userId) {
        return Result.ok(kpiScoreService.listByPeriodAndUser(periodId, userId));
    }

    /** 按 data_source 自动拉取各业务模块建议实际值（供前端批量填写预填） */
    @GetMapping("/scores/suggest")
    public Result<Map<Long, java.math.BigDecimal>> suggestScores(
            @RequestParam Long periodId,
            @RequestParam Long userId) {
        return Result.ok(kpiSuggestService.suggestActualValues(periodId, userId));
    }

    @PostMapping("/scores")
    @SaCheckPermission("hr:kpi:score")
    @Log(title = "考核记录", businessType = BusinessType.UPDATE)
    public Result<Void> saveScore(@RequestBody KpiScore score) {
        kpiScoreService.saveOrUpdate(score);
        return Result.ok();
    }

    @PostMapping("/scores/batch")
    @SaCheckPermission("hr:kpi:score")
    @Log(title = "考核记录", businessType = BusinessType.UPDATE)
    public Result<Void> saveScoreBatch(
            @RequestParam Long periodId,
            @RequestParam Long userId,
            @RequestBody List<KpiScore> scores) {
        kpiScoreService.saveBatch(periodId, userId, scores);
        return Result.ok();
    }

    // ========== 360 度评估 ==========

    @Operation(summary = "创建/更新 360 度评估", description = "支持自评、上级、同事、下级多维度评估")
    @PostMapping("/review/360")
    @SaCheckPermission("hr:review360:submit")
    @Log(title = "360 度评估", businessType = BusinessType.INSERT)
    public Result<Long> createReview(@RequestBody KpiReview360DTO dto) {
        return Result.ok(kpiReview360Service.createOrUpdateReview(dto));
    }

    @Operation(summary = "获取待评估列表", description = "获取当前用户待完成的 360 度评估任务")
    @GetMapping("/review/360/pending")
    public Result<Object> getPendingReviews(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(kpiReview360Service.getPendingReviews(StpUtil.getLoginIdAsLong(), page, pageSize));
    }

    @Operation(summary = "获取 360 度评估结果", description = "获取指定用户在指定周期的 360 度评估详细结果")
    @GetMapping("/review/360/result")
    public Result<KpiReview360ResultVO> getReviewResult(
            @RequestParam Long periodId,
            @RequestParam Long userId) {
        return Result.ok(kpiReview360Service.getReviewResult(periodId, userId));
    }

    @Operation(summary = "获取权重配置", description = "获取 360 度评估各维度权重配置")
    @GetMapping("/review/360/config")
    public Result<Kpi360WeightConfig> getWeightConfig() {
        return Result.ok(kpiReview360Service.getWeightConfig());
    }

    @Operation(summary = "更新权重配置", description = "更新 360 度评估各维度权重配置")
    @PostMapping("/review/360/config")
    @SaCheckPermission("hr:review360:config")
    @Log(title = "360 度评估配置", businessType = BusinessType.UPDATE)
    public Result<Void> updateWeightConfig(@RequestBody Kpi360WeightConfig config) {
        kpiReview360Service.updateWeightConfig(config);
        return Result.ok();
    }

    @Operation(summary = "批量创建评估任务", description = "为周期内所有用户创建 360 度评估任务")
    @PostMapping("/review/360/tasks")
    @SaCheckPermission("hr:review360:task")
    @Log(title = "360 度评估任务", businessType = BusinessType.INSERT)
    public Result<Integer> createReviewTasks(
            @RequestParam Long periodId,
            @RequestBody List<Long> userIds) {
        return Result.ok(kpiReview360Service.createReviewTasks(periodId, userIds));
    }
}
