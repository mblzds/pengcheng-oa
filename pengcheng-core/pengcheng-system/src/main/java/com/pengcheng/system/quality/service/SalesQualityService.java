package com.pengcheng.system.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.quality.entity.SalesQualityScore;
import com.pengcheng.system.quality.mapper.SalesQualityScoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 销售质检服务
 * 基于跟进记录自动评分，生成能力雷达图数据和排行榜
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesQualityService {

    private final SalesQualityScoreMapper scoreMapper;
    private final JdbcTemplate jdbcTemplate;

    public SalesQualityScore getLatestScore(Long userId) {
        List<SalesQualityScore> scores = scoreMapper.getRecentScores(userId, 1);
        return scores.isEmpty() ? null : scores.get(0);
    }

    public List<SalesQualityScore> getScoreHistory(Long userId, int limit) {
        return scoreMapper.getRecentScores(userId, limit);
    }

    public List<SalesQualityScore> getRanking(LocalDate date) {
        return scoreMapper.getRankingByDate(date.toString());
    }

    /**
     * 按日期范围取该用户质检综合分平均值（供绩效 auto_quality 指标拉数）
     */
    public Double getAverageOverallScoreInRange(Long userId, LocalDate start, LocalDate end) {
        if (userId == null || start == null || end == null) return null;
        List<SalesQualityScore> list = scoreMapper.selectList(
            new LambdaQueryWrapper<SalesQualityScore>()
                .eq(SalesQualityScore::getUserId, userId)
                .ge(SalesQualityScore::getScoreDate, start)
                .le(SalesQualityScore::getScoreDate, end));
        if (list == null || list.isEmpty()) return null;
        int sum = 0, count = 0;
        for (SalesQualityScore s : list) {
            if (s.getOverallScore() != null) { sum += s.getOverallScore(); count++; }
        }
        return count == 0 ? null : (double) sum / count;
    }

    /**
     * 为指定销售人员生成质检评分
     */
    public SalesQualityScore evaluateSales(Long userId, LocalDate date) {
        SalesQualityScore existing = scoreMapper.selectOne(
            new LambdaQueryWrapper<SalesQualityScore>()
                .eq(SalesQualityScore::getUserId, userId)
                .eq(SalesQualityScore::getScoreDate, date));
        if (existing != null) return existing;

        SalesQualityScore score = new SalesQualityScore();
        score.setUserId(userId);
        score.setScoreDate(date);

        int recordCount = evaluateFollowUpMetrics(score, userId, date);
        score.setEvaluatedRecords(recordCount);
        calculateOverall(score);
        generateAiComment(score);

        scoreMapper.insert(score);
        return score;
    }

    private int evaluateFollowUpMetrics(SalesQualityScore score, Long userId, LocalDate date) {
        int recordCount = 0;

        try {
            LocalDate monthStart = date.withDayOfMonth(1);

            String countSql = "SELECT COUNT(*) FROM realty_customer WHERE creator_id = ? AND updated_at >= ? AND updated_at < ?";
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, userId, monthStart, date.plusDays(1));
            recordCount = count != null ? count : 0;

            // 跟进频率评分（基于月跟进量）
            score.setFollowUpFrequencyScore(Math.min(100, recordCount * 5));

            // 响应时效评分（基于最近跟进间隔）
            String gapSql = "SELECT AVG(DATEDIFF(NOW(), updated_at)) FROM realty_customer WHERE creator_id = ? AND status != '已成交' AND updated_at >= ?";
            Double avgGap = jdbcTemplate.queryForObject(gapSql, Double.class, userId, monthStart);
            if (avgGap != null) {
                score.setResponseTimeScore(avgGap <= 2 ? 95 : avgGap <= 5 ? 75 : avgGap <= 10 ? 55 : 30);
            } else {
                score.setResponseTimeScore(50);
            }

            // 沟通评分（基于跟进记录内容长度——越详细越好）
            String contentSql = "SELECT AVG(CHAR_LENGTH(COALESCE(follow_record, ''))) FROM realty_customer WHERE creator_id = ? AND updated_at >= ?";
            Double avgLen = jdbcTemplate.queryForObject(contentSql, Double.class, userId, monthStart);
            if (avgLen != null) {
                score.setCommunicationScore(avgLen >= 100 ? 90 : avgLen >= 50 ? 70 : avgLen >= 20 ? 50 : 30);
            } else {
                score.setCommunicationScore(50);
            }

            // 成交转化评分
            String dealSql = "SELECT COUNT(*) FROM realty_customer WHERE creator_id = ? AND status = '已成交' AND updated_at >= ?";
            Integer dealCount = jdbcTemplate.queryForObject(dealSql, Integer.class, userId, monthStart);
            int deals = dealCount != null ? dealCount : 0;
            score.setClosingAbilityScore(deals >= 5 ? 95 : deals >= 3 ? 80 : deals >= 1 ? 60 : 35);

            // 需求挖掘和异议处理基于记录质量模拟评分
            score.setDemandMiningScore(Math.min(100, (score.getCommunicationScore() + score.getFollowUpFrequencyScore()) / 2 + 5));
            score.setObjectionHandlingScore(Math.min(100, (score.getResponseTimeScore() + score.getClosingAbilityScore()) / 2));

        } catch (Exception e) {
            log.debug("质检评分数据采集异常: {}", e.getMessage());
            score.setCommunicationScore(50);
            score.setDemandMiningScore(50);
            score.setObjectionHandlingScore(50);
            score.setClosingAbilityScore(50);
            score.setFollowUpFrequencyScore(50);
            score.setResponseTimeScore(50);
        }

        return recordCount;
    }

    private void calculateOverall(SalesQualityScore score) {
        int overall = (int) (
            score.getCommunicationScore() * 0.2 +
            score.getDemandMiningScore() * 0.15 +
            score.getObjectionHandlingScore() * 0.15 +
            score.getClosingAbilityScore() * 0.25 +
            score.getFollowUpFrequencyScore() * 0.15 +
            score.getResponseTimeScore() * 0.1
        );
        score.setOverallScore(overall);
    }

    private void generateAiComment(SalesQualityScore score) {
        StringBuilder comment = new StringBuilder();
        StringBuilder suggestion = new StringBuilder();

        if (score.getOverallScore() >= 80) {
            comment.append("整体表现优秀！");
        } else if (score.getOverallScore() >= 60) {
            comment.append("表现良好，仍有提升空间。");
        } else {
            comment.append("需要重点关注和改进。");
        }

        int minScore = Math.min(score.getCommunicationScore(),
            Math.min(score.getDemandMiningScore(),
                Math.min(score.getObjectionHandlingScore(),
                    Math.min(score.getClosingAbilityScore(),
                        Math.min(score.getFollowUpFrequencyScore(), score.getResponseTimeScore())))));

        if (minScore == score.getFollowUpFrequencyScore()) {
            suggestion.append("跟进频率偏低，建议制定每日客户拜访计划，确保每周至少跟进 5 位意向客户。");
        } else if (minScore == score.getResponseTimeScore()) {
            suggestion.append("响应时效需提升，建议关注客户咨询消息的及时回复，控制在 2 小时内。");
        } else if (minScore == score.getCommunicationScore()) {
            suggestion.append("沟通记录不够详尽，建议跟进后及时记录客户需求、异议和下步计划。");
        } else if (minScore == score.getClosingAbilityScore()) {
            suggestion.append("成交转化率有待提高，建议加强逼定技巧培训，把握成交时机。");
        } else {
            suggestion.append("继续保持良好势头，可以尝试更深入的客户需求挖掘。");
        }

        score.setAiComment(comment.toString());
        score.setAiSuggestion(suggestion.toString());
    }

    /**
     * 团队质检评分
     */
    public int evaluateTeam(LocalDate date) {
        int count = 0;
        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT u.user_id FROM sys_user u JOIN sys_user_role ur ON u.user_id = ur.user_id WHERE u.status = '0'");
            for (Map<String, Object> user : users) {
                Long userId = ((Number) user.get("user_id")).longValue();
                try {
                    evaluateSales(userId, date);
                    count++;
                } catch (Exception e) {
                    log.error("评估用户 {} 失败", userId, e);
                }
            }
        } catch (Exception e) {
            log.error("团队质检评分失败", e);
        }
        return count;
    }
}
