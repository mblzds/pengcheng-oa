package com.pengcheng.hr.performance.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.hr.performance.dto.*;
import com.pengcheng.hr.performance.entity.KpiPeerReview;
import com.pengcheng.hr.performance.entity.KpiReview360;
import com.pengcheng.hr.performance.entity.KpiReviewRelation;
import com.pengcheng.hr.performance.mapper.KpiPeerReviewMapper;
import com.pengcheng.hr.performance.mapper.KpiReview360Mapper;
import com.pengcheng.hr.performance.mapper.KpiReviewRelationMapper;
import com.pengcheng.system.entity.SysConfigGroup;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.mapper.SysUserMapper;
import com.pengcheng.system.service.SysConfigGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 360 度绩效评估服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KpiReview360Service {

    private final KpiReview360Mapper review360Mapper;
    private final KpiReviewRelationMapper relationMapper;
    private final KpiPeerReviewMapper peerReviewMapper;
    private final SysUserMapper userMapper;
    private final SysConfigGroupService configGroupService;

    /** 评估类型常量 */
    public static final int REVIEW_TYPE_SELF = 1;
    public static final int REVIEW_TYPE_MANAGER = 2;
    public static final int REVIEW_TYPE_PEER = 3;
    public static final int REVIEW_TYPE_SUBORDINATE = 4;

    /** sys_config_group 中存权重的 group_code */
    public static final String WEIGHT_CONFIG_GROUP_CODE = "kpi360Config";

    /** 关系记录启用状态 */
    private static final int RELATION_ACTIVE = 1;

    /**
     * 创建或更新 360 度评估
     */
    @Transactional
    public Long createOrUpdateReview(KpiReview360DTO dto) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("未登录，无法提交 360 度评估");
        }
        // 检查是否已存在评估
        LambdaQueryWrapper<KpiReview360> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KpiReview360::getPeriodId, dto.getPeriodId())
                .eq(KpiReview360::getUserId, dto.getUserId())
                .eq(KpiReview360::getReviewerId, currentUserId);

        KpiReview360 existing = review360Mapper.selectOne(wrapper);

        LocalDateTime now = LocalDateTime.now();

        if (existing != null) {
            // 更新现有评估
            existing.setTotalScore(dto.getTotalScore());
            existing.setComment(dto.getComment());
            existing.setStrengths(dto.getStrengths());
            existing.setImprovements(dto.getImprovements());
            existing.setStatus(2); // 已完成
            existing.setUpdateTime(now);
            review360Mapper.updateById(existing);
            log.info("360 度评估已更新：reviewId={}", existing.getId());
            return existing.getId();
        } else {
            // 创建新评估
            KpiReview360 review = new KpiReview360();
            review.setPeriodId(dto.getPeriodId());
            review.setUserId(dto.getUserId());
            review.setReviewerId(currentUserId);
            review.setReviewType(dto.getReviewType());
            review.setTotalScore(dto.getTotalScore());
            review.setComment(dto.getComment());
            review.setStrengths(dto.getStrengths());
            review.setImprovements(dto.getImprovements());
            review.setStatus(2); // 已完成
            review.setCreateTime(now);
            review.setUpdateTime(now);
            review360Mapper.insert(review);
            log.info("360 度评估已创建：reviewId={}", review.getId());
            return review.getId();
        }
    }

    /**
     * 获取用户的待评估列表
     */
    public PageResult<KpiReview360DTO> getPendingReviews(Long userId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<KpiReview360> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KpiReview360::getReviewerId, userId)
                .eq(KpiReview360::getStatus, 1); // 待评估

        IPage<KpiReview360> pageResult = review360Mapper.selectPage(
                new Page<>(page, pageSize), wrapper);

        List<KpiReview360DTO> dtoList = pageResult.getRecords().stream()
                .map(r -> KpiReview360DTO.builder()
                        .periodId(r.getPeriodId())
                        .userId(r.getUserId())
                        .reviewType(r.getReviewType())
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(dtoList, pageResult.getTotal(), (long) page, (long) pageSize);
    }

    /**
     * 获取 360 度评估结果
     */
    public KpiReview360ResultVO getReviewResult(Long periodId, Long userId) {
        // 查询所有评估记录
        LambdaQueryWrapper<KpiReview360> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KpiReview360::getPeriodId, periodId)
                .eq(KpiReview360::getUserId, userId)
                .eq(KpiReview360::getStatus, 2); // 已完成

        List<KpiReview360> reviews = review360Mapper.selectList(wrapper);

        if (reviews.isEmpty()) {
            return null;
        }

        // 按评估类型分组
        Map<Integer, List<KpiReview360>> byType = reviews.stream()
                .collect(Collectors.groupingBy(KpiReview360::getReviewType));

        // 计算各类型平均分
        BigDecimal selfScore = calculateAverage(byType.get(REVIEW_TYPE_SELF));
        BigDecimal managerScore = calculateAverage(byType.get(REVIEW_TYPE_MANAGER));
        BigDecimal peerScore = calculateAverage(byType.get(REVIEW_TYPE_PEER));
        BigDecimal subordinateScore = calculateAverage(byType.get(REVIEW_TYPE_SUBORDINATE));

        // 获取权重配置
        Kpi360WeightConfig config = getWeightConfig();

        // 计算加权平均分
        BigDecimal finalScore = selfScore.multiply(BigDecimal.valueOf(config.getSelfWeight()))
                .add(managerScore.multiply(BigDecimal.valueOf(config.getManagerWeight())))
                .add(peerScore.multiply(BigDecimal.valueOf(config.getPeerWeight())))
                .add(subordinateScore.multiply(BigDecimal.valueOf(config.getSubordinateWeight())))
                .setScale(2, RoundingMode.HALF_UP);

        // 确定等级
        String grade = calculateGrade(finalScore);

        // 构建统计信息
        ReviewCountStats stats = ReviewCountStats.builder()
                .selfCount(getListSize(byType.get(REVIEW_TYPE_SELF)))
                .managerCount(getListSize(byType.get(REVIEW_TYPE_MANAGER)))
                .peerCount(getListSize(byType.get(REVIEW_TYPE_PEER)))
                .subordinateCount(getListSize(byType.get(REVIEW_TYPE_SUBORDINATE)))
                .totalCount(reviews.size())
                .build();

        // 获取被评估人姓名
        SysUser user = userMapper.selectById(userId);
        String userName = user != null ? user.getNickname() : "未知用户";

        // 构建详细评价列表
        List<ReviewDetail> reviewDetails = reviews.stream()
                .map(r -> {
                    SysUser reviewer = userMapper.selectById(r.getReviewerId());
                    String reviewerName = config.getAnonymous() ? "***" :
                            (reviewer != null ? reviewer.getNickname() : "未知");
                    return ReviewDetail.builder()
                            .reviewType(r.getReviewType())
                            .reviewTypeName(getReviewTypeName(r.getReviewType()))
                            .reviewerName(reviewerName)
                            .score(r.getTotalScore())
                            .comment(r.getComment())
                            .strengths(r.getStrengths())
                            .improvements(r.getImprovements())
                            .build();
                })
                .collect(Collectors.toList());

        return KpiReview360ResultVO.builder()
                .userId(userId)
                .userName(userName)
                .selfScore(selfScore)
                .managerScore(managerScore)
                .peerScore(peerScore)
                .subordinateScore(subordinateScore)
                .finalScore(finalScore)
                .grade(grade)
                .stats(stats)
                .reviews(reviewDetails)
                .build();
    }

    /**
     * 批量创建评估任务（为周期内所有用户创建待评估记录）。
     * <p>
     * 生成 4 个维度任务（若存在对应关系）：
     * <ol>
     *   <li>自评：reviewer = user 自己</li>
     *   <li>上级评：查 kpi_review_relation.manager_id；若未配置则跳过并 warn</li>
     *   <li>同事互评：查 kpi_peer_review.peer_id 列表</li>
     *   <li>下级评：反查 kpi_review_relation 中以当前用户为 manager 的记录</li>
     * </ol>
     * 同一 (period, user, reviewer) 去重，避免重复插入。
     */
    @Transactional
    public int createReviewTasks(Long periodId, List<Long> userIds) {
        if (periodId == null || userIds == null || userIds.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        int count = 0;

        // 预加载整批的关系与同事关系，避免 N+1
        List<KpiReviewRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<KpiReviewRelation>()
                .eq(KpiReviewRelation::getPeriodId, periodId)
                .eq(KpiReviewRelation::getStatus, RELATION_ACTIVE));
        Map<Long, Long> userToManager = new HashMap<>();
        Map<Long, List<Long>> managerToSubordinates = new HashMap<>();
        for (KpiReviewRelation r : relations) {
            if (r.getUserId() != null && r.getManagerId() != null) {
                userToManager.put(r.getUserId(), r.getManagerId());
                managerToSubordinates.computeIfAbsent(r.getManagerId(), k -> new ArrayList<>())
                        .add(r.getUserId());
            }
        }

        List<KpiPeerReview> peers = peerReviewMapper.selectList(new LambdaQueryWrapper<KpiPeerReview>()
                .eq(KpiPeerReview::getPeriodId, periodId)
                .eq(KpiPeerReview::getStatus, RELATION_ACTIVE));
        Map<Long, List<Long>> userToPeers = new HashMap<>();
        for (KpiPeerReview p : peers) {
            if (p.getUserId() != null && p.getPeerId() != null) {
                userToPeers.computeIfAbsent(p.getUserId(), k -> new ArrayList<>())
                        .add(p.getPeerId());
            }
        }

        for (Long userId : userIds) {
            if (userId == null) continue;

            // 1) 自评
            count += upsertTask(periodId, userId, userId, REVIEW_TYPE_SELF, now);

            // 2) 上级评
            Long managerId = userToManager.get(userId);
            if (managerId != null) {
                count += upsertTask(periodId, userId, managerId, REVIEW_TYPE_MANAGER, now);
            } else {
                log.warn("[360] periodId={} userId={} 未配置上级关系，跳过上级评任务", periodId, userId);
            }

            // 3) 同事评（互评：每位同事生成一条针对当前 user 的任务）
            List<Long> peerIds = userToPeers.getOrDefault(userId, Collections.emptyList());
            for (Long peerId : peerIds) {
                if (peerId == null || peerId.equals(userId)) continue;
                count += upsertTask(periodId, userId, peerId, REVIEW_TYPE_PEER, now);
            }

            // 4) 下级评（该 user 作为 manager 的所有下级对其评价）
            List<Long> subordinates = managerToSubordinates.getOrDefault(userId, Collections.emptyList());
            for (Long subId : subordinates) {
                if (subId == null || subId.equals(userId)) continue;
                count += upsertTask(periodId, userId, subId, REVIEW_TYPE_SUBORDINATE, now);
            }
        }

        log.info("[360] periodId={} 为 {} 位用户生成 {} 个评估任务",
                periodId, userIds.size(), count);
        return count;
    }

    /**
     * 去重插入一条评估任务。返回 1 表示新建，0 表示已存在或跳过。
     * <p>
     * 用原生 QueryWrapper + 列名匹配，避免 MyBatis-Plus LambdaQueryWrapper 在单测场景下未初始化
     * 实体元数据导致的 "can not find lambda cache" 问题。
     */
    int upsertTask(Long periodId, Long userId, Long reviewerId, int reviewType, LocalDateTime now) {
        Long existing = review360Mapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<KpiReview360>()
                        .eq("period_id", periodId)
                        .eq("user_id", userId)
                        .eq("reviewer_id", reviewerId)
                        .eq("review_type", reviewType));
        if (existing != null && existing > 0) {
            return 0;
        }
        KpiReview360 r = new KpiReview360();
        r.setPeriodId(periodId);
        r.setUserId(userId);
        r.setReviewerId(reviewerId);
        r.setReviewType(reviewType);
        r.setStatus(1); // 待评估
        r.setCreateTime(now);
        r.setUpdateTime(now);
        review360Mapper.insert(r);
        return 1;
    }

    /**
     * 获取 360 度评估权重配置：从 sys_config_group(group_code='kpi360Config') 读取 JSON。
     * 缺失或解析失败回退到默认权重 0.1 / 0.4 / 0.3 / 0.2。
     * 原始数据由 V33__360_review.sql 初始化。
     */
    public Kpi360WeightConfig getWeightConfig() {
        try {
            SysConfigGroup group = configGroupService.getByGroupCode(WEIGHT_CONFIG_GROUP_CODE);
            if (group == null || group.getConfigValue() == null || group.getConfigValue().isBlank()) {
                return defaultWeightConfig();
            }
            return parseWeightConfigJson(group.getConfigValue());
        } catch (Exception e) {
            log.error("[360] 读取权重配置失败，回退默认值", e);
            return defaultWeightConfig();
        }
    }

    /**
     * 更新 360 度评估权重配置，校验合计 = 1.0 (±0.001)，持久化到 sys_config_group。
     */
    public void updateWeightConfig(Kpi360WeightConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("权重配置不能为空");
        }
        double sum = safe(config.getSelfWeight()) + safe(config.getManagerWeight())
                + safe(config.getPeerWeight()) + safe(config.getSubordinateWeight());
        if (Math.abs(sum - 1.0) > 0.001) {
            throw new IllegalArgumentException("四向权重合计必须等于 1.0，当前 = " + sum);
        }
        String json = toWeightConfigJson(config);
        configGroupService.saveConfig(WEIGHT_CONFIG_GROUP_CODE, json);
        log.info("[360] 权重配置已更新并持久化：{}", json);
    }

    private Kpi360WeightConfig defaultWeightConfig() {
        return Kpi360WeightConfig.builder()
                .selfWeight(0.1)
                .managerWeight(0.4)
                .peerWeight(0.3)
                .subordinateWeight(0.2)
                .minReviewers(3)
                .anonymous(true)
                .build();
    }

    /**
     * 解析 V33 初始化格式：{"weights":{"self":0.1,"manager":0.4,"peer":0.3,"subordinate":0.2},"minReviewers":3,"anonymous":true}
     */
    Kpi360WeightConfig parseWeightConfigJson(String json) {
        cn.hutool.json.JSONObject obj = JSONUtil.parseObj(json);
        cn.hutool.json.JSONObject weights = obj.getJSONObject("weights");
        Kpi360WeightConfig def = defaultWeightConfig();
        return Kpi360WeightConfig.builder()
                .selfWeight(weights != null ? weights.getDouble("self", def.getSelfWeight()) : def.getSelfWeight())
                .managerWeight(weights != null ? weights.getDouble("manager", def.getManagerWeight()) : def.getManagerWeight())
                .peerWeight(weights != null ? weights.getDouble("peer", def.getPeerWeight()) : def.getPeerWeight())
                .subordinateWeight(weights != null ? weights.getDouble("subordinate", def.getSubordinateWeight()) : def.getSubordinateWeight())
                .minReviewers(obj.getInt("minReviewers", def.getMinReviewers()))
                .anonymous(obj.getBool("anonymous", def.getAnonymous()))
                .build();
    }

    String toWeightConfigJson(Kpi360WeightConfig c) {
        cn.hutool.json.JSONObject weights = new cn.hutool.json.JSONObject()
                .set("self", c.getSelfWeight())
                .set("manager", c.getManagerWeight())
                .set("peer", c.getPeerWeight())
                .set("subordinate", c.getSubordinateWeight());
        return new cn.hutool.json.JSONObject()
                .set("weights", weights)
                .set("minReviewers", c.getMinReviewers())
                .set("anonymous", c.getAnonymous())
                .toString();
    }

    private static double safe(Double d) { return d == null ? 0d : d; }

    // ========== 辅助方法 ==========

    private BigDecimal calculateAverage(List<KpiReview360> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = reviews.stream()
                .map(KpiReview360::getTotalScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);
    }

    private int getListSize(List<KpiReview360> list) {
        return list != null ? list.size() : 0;
    }

    private String getReviewTypeName(Integer type) {
        switch (type) {
            case REVIEW_TYPE_SELF: return "自评";
            case REVIEW_TYPE_MANAGER: return "上级评价";
            case REVIEW_TYPE_PEER: return "同事评价";
            case REVIEW_TYPE_SUBORDINATE: return "下级评价";
            default: return "未知";
        }
    }

    private String calculateGrade(BigDecimal score) {
        if (score == null) return "D";
        if (score.compareTo(BigDecimal.valueOf(90)) >= 0) return "A";
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) return "B";
        if (score.compareTo(BigDecimal.valueOf(70)) >= 0) return "C";
        return "D";
    }

    /**
     * 从 Sa-Token 安全上下文取当前登录用户 ID。未登录时（如在定时任务调用路径）返回 null。
     * 调用方负责判空；若未登录仍执行评估写操作应抛异常，而非落库成 hardcoded=1。
     */
    Long getCurrentUserId() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignore) {
            // 某些非 Web 调用栈下 StpUtil 不可用
        }
        return null;
    }
}
