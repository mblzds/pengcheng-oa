package com.pengcheng.system.visit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.visit.entity.SalesVisit;
import com.pengcheng.system.visit.entity.SalesVisitTag;
import com.pengcheng.system.visit.mapper.SalesVisitMapper;
import com.pengcheng.system.visit.mapper.SalesVisitTagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售拜访记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesVisitService {

    private final SalesVisitMapper visitMapper;
    private final SalesVisitTagMapper tagMapper;

    /** 创建拜访记录 */
    public SalesVisit createVisit(SalesVisit visit) {
        if (visit.getStatus() == null) {
            visit.setStatus(1);
        }
        if (visit.getVisitTime() == null) {
            visit.setVisitTime(LocalDateTime.now());
        }
        visitMapper.insert(visit);
        return visit;
    }

    /** 更新拜访记录 */
    public void updateVisit(SalesVisit visit) {
        visitMapper.updateById(visit);
    }

    /** 删除拜访记录（逻辑删除） */
    public void deleteVisit(Long id) {
        visitMapper.deleteById(id);
    }

    /** 获取拜访详情 */
    public SalesVisit getVisit(Long id) {
        return visitMapper.selectById(id);
    }

    /** 分页查询拜访记录 */
    public IPage<SalesVisit> listVisits(Long userId, Long customerId, String visitType,
                                         LocalDate startDate, LocalDate endDate,
                                         int page, int size) {
        LambdaQueryWrapper<SalesVisit> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SalesVisit::getUserId, userId);
        }
        if (customerId != null) {
            wrapper.eq(SalesVisit::getCustomerId, customerId);
        }
        if (StringUtils.hasText(visitType)) {
            wrapper.eq(SalesVisit::getVisitType, visitType);
        }
        if (startDate != null) {
            wrapper.ge(SalesVisit::getVisitTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(SalesVisit::getVisitTime, endDate.atTime(23, 59, 59));
        }
        wrapper.orderByDesc(SalesVisit::getVisitTime);
        return visitMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /** 获取拜访标签 */
    public List<SalesVisitTag> getVisitTags(Long visitId) {
        return tagMapper.selectList(
                new LambdaQueryWrapper<SalesVisitTag>().eq(SalesVisitTag::getVisitId, visitId));
    }

    /** 添加分析标签 */
    public void addTag(SalesVisitTag tag) {
        tagMapper.insert(tag);
    }

    /** 批量保存分析标签 */
    public void saveTags(Long visitId, List<SalesVisitTag> tags) {
        tagMapper.delete(new LambdaQueryWrapper<SalesVisitTag>()
                .eq(SalesVisitTag::getVisitId, visitId));
        for (SalesVisitTag tag : tags) {
            tag.setVisitId(visitId);
            tagMapper.insert(tag);
        }
    }

    /** 保存 AI 分析结果 */
    public void saveAiAnalysis(Long visitId, String aiAnalysis, Integer aiScore) {
        SalesVisit update = new SalesVisit();
        update.setId(visitId);
        update.setAiAnalysis(aiAnalysis);
        update.setAiScore(aiScore);
        visitMapper.updateById(update);
        log.info("[SalesVisit] AI 分析结果已保存, visitId={}, score={}", visitId, aiScore);
    }

    /** 保存 ASR 转写结果 */
    public void saveTranscript(Long visitId, String transcript) {
        SalesVisit update = new SalesVisit();
        update.setId(visitId);
        update.setTranscript(transcript);
        visitMapper.updateById(update);
        log.info("[SalesVisit] 转写结果已保存, visitId={}", visitId);
    }

    /** 用户拜访统计 */
    public Map<String, Object> getUserStats(Long userId) {
        List<Map<String, Object>> typeCounts = visitMapper.countByTypeLastMonth(userId);
        long total = typeCounts.stream()
                .mapToLong(m -> ((Number) m.get("cnt")).longValue())
                .sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("byType", typeCounts);

        LambdaQueryWrapper<SalesVisit> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(SalesVisit::getUserId, userId)
                .ge(SalesVisit::getVisitTime, LocalDate.now().atStartOfDay())
                .eq(SalesVisit::getDeleted, 0);
        stats.put("todayCount", visitMapper.selectCount(todayWrapper));

        return stats;
    }

    /** 团队拜访排行 */
    public List<Map<String, Object>> getTeamRanking(Long deptId) {
        return visitMapper.teamRanking(deptId);
    }
}
