package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.ai.service.AiLlmService;
import com.pengcheng.ai.service.AiMultiModalService;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.visit.entity.SalesVisit;
import com.pengcheng.system.visit.entity.SalesVisitTag;
import com.pengcheng.system.visit.service.SalesVisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 销售拜访记录管理接口
 */
@RestController
@RequestMapping("/realty/visit")
@RequiredArgsConstructor
public class SalesVisitController {

    private final SalesVisitService visitService;
    @Autowired(required = false)
    private AiLlmService aiLlmService;
    @Autowired(required = false)
    private AiMultiModalService multiModalService;

    /** 分页查询拜访记录 */
    @GetMapping("/list")
    public Result<IPage<SalesVisit>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String visitType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(visitService.listVisits(userId, customerId, visitType, startDate, endDate, page, size));
    }

    /** 获取拜访详情 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        SalesVisit visit = visitService.getVisit(id);
        if (visit == null) {
            return Result.fail("拜访记录不存在");
        }
        List<SalesVisitTag> tags = visitService.getVisitTags(id);
        return Result.ok(Map.of("visit", visit, "tags", tags));
    }

    /** 创建拜访记录 */
    @PostMapping
    public Result<SalesVisit> create(@RequestBody SalesVisit visit) {
        visit.setUserId(StpUtil.getLoginIdAsLong());
        return Result.ok(visitService.createVisit(visit));
    }

    /** 更新拜访记录 */
    @PutMapping
    public Result<Void> update(@RequestBody SalesVisit visit) {
        visitService.updateVisit(visit);
        return Result.ok();
    }

    /** 删除拜访记录 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        visitService.deleteVisit(id);
        return Result.ok();
    }

    /** 保存分析标签 */
    @PostMapping("/{id}/tags")
    public Result<Void> saveTags(@PathVariable Long id, @RequestBody List<SalesVisitTag> tags) {
        visitService.saveTags(id, tags);
        return Result.ok();
    }

    /** 用户拜访统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(required = false) Long userId) {
        Long uid = userId != null ? userId : StpUtil.getLoginIdAsLong();
        return Result.ok(visitService.getUserStats(uid));
    }

    /** 团队拜访排行 */
    @GetMapping("/ranking")
    public Result<List<Map<String, Object>>> ranking(@RequestParam Long deptId) {
        return Result.ok(visitService.getTeamRanking(deptId));
    }

    /** 上传录音并触发 ASR 转写 */
    @PostMapping("/{id}/audio")
    public Result<Map<String, String>> uploadAudio(@PathVariable Long id, @RequestParam String audioUrl) {
        SalesVisit update = new SalesVisit();
        update.setId(id);
        update.setAudioUrl(audioUrl);
        visitService.updateVisit(update);

        if (multiModalService != null) {
            String transcript = multiModalService.transcribeAudio(audioUrl);
            if (transcript != null) {
                visitService.saveTranscript(id, transcript);
                return Result.ok(Map.of("status", "success", "transcript", transcript));
            }
        }
        return Result.ok(Map.of("status", "uploaded", "message", "录音已上传。ASR 转写需配置 DashScope API Key。"));
    }

    /** 触发 AI 分析拜访内容 */
    @PostMapping("/{id}/analyze")
    public Result<Map<String, Object>> analyze(@PathVariable Long id) {
        SalesVisit visit = visitService.getVisit(id);
        if (visit == null) return Result.fail("拜访记录不存在");
        if (aiLlmService == null) return Result.fail("AI 服务未启用");

        String content = visit.getTranscript() != null ? visit.getTranscript() : visit.getSummary();
        if (content == null || content.isBlank()) return Result.fail("无可分析的拜访内容（转写或摘要为空）");

        String analysis = aiLlmService.analyzeVisit(content,
                visit.getCustomerName() != null ? visit.getCustomerName() : "未知客户",
                visit.getVisitType() != null ? visit.getVisitType() : "拜访");
        Integer score = analysis != null ? aiLlmService.scoreVisit(analysis) : null;

        visitService.saveAiAnalysis(id, analysis, score);
        return Result.ok(Map.of("analysis", analysis != null ? analysis : "", "score", score != null ? score : 0));
    }
}
