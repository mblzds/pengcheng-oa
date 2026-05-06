package com.pengcheng.ai.memory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.ai.memory.entity.AiMemory;
import com.pengcheng.ai.memory.entity.AiMemoryEpisode;
import com.pengcheng.ai.memory.entity.AiMemoryRefinementLog;
import com.pengcheng.ai.memory.mapper.AiMemoryEpisodeMapper;
import com.pengcheng.ai.memory.mapper.AiMemoryMapper;
import com.pengcheng.ai.memory.mapper.AiMemoryRefinementLogMapper;
import com.pengcheng.ai.memory.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 记忆服务实现
 * <p>
 * 当前使用 MySQL FULLTEXT 进行文本搜索，
 * 后续可集成 PGVector 实现 70% 向量 + 30% BM25 混合检索。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {

    private final AiMemoryMapper memoryMapper;
    private final AiMemoryEpisodeMapper episodeMapper;
    private final AiMemoryRefinementLogMapper refinementLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiMemory addMemory(Long userId, Long customerId, String type, String content, String source, String tags) {
        AiMemory memory = new AiMemory();
        memory.setUserId(userId);
        memory.setCustomerId(customerId);
        memory.setMemoryType(type);
        memory.setMemoryLevel("L1");
        memory.setContent(content);
        memory.setSource(source != null ? source : "chat");
        memory.setImportance(calculateInitialImportance(type));
        memory.setAccessCount(0);
        memory.setTags(tags);
        memory.setExpiresAt(LocalDateTime.now().plusDays(7));
        memory.setCreatedAt(LocalDateTime.now());
        memory.setUpdatedAt(LocalDateTime.now());
        memoryMapper.insert(memory);
        log.info("添加记忆: userId={}, type={}, content={}", userId, type, truncate(content, 50));
        return memory;
    }

    @Override
    public List<AiMemory> searchMemories(Long userId, String query, int topK) {
        String booleanQuery = toBooleanModeQuery(query);
        List<AiMemory> results = memoryMapper.searchByFulltext(userId, booleanQuery, topK);

        for (AiMemory m : results) {
            memoryMapper.incrementAccess(m.getId());
        }
        return results;
    }

    @Override
    public List<AiMemory> getCustomerProfileMemories(Long customerId, int topK) {
        return memoryMapper.findByCustomer(customerId, topK);
    }

    @Override
    public String buildMemoryContext(Long userId, Long customerId, String currentQuery) {
        StringBuilder ctx = new StringBuilder();

        List<AiMemory> relevantMemories = searchMemories(userId, currentQuery, 5);
        if (!relevantMemories.isEmpty()) {
            ctx.append("## 相关记忆\n");
            for (AiMemory m : relevantMemories) {
                ctx.append("- [").append(m.getMemoryType()).append("] ").append(m.getContent()).append("\n");
            }
        }

        if (customerId != null) {
            List<AiMemory> profileMemories = getCustomerProfileMemories(customerId, 5);
            if (!profileMemories.isEmpty()) {
                ctx.append("\n## 客户画像\n");
                for (AiMemory m : profileMemories) {
                    ctx.append("- ").append(m.getContent()).append("\n");
                }
            }
        }

        return ctx.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiMemoryEpisode saveEpisode(Long userId, String sessionId, String summary, List<String> keyFacts, String emotion) {
        AiMemoryEpisode episode = new AiMemoryEpisode();
        episode.setUserId(userId);
        episode.setSessionId(sessionId);
        episode.setSummary(summary);
        episode.setKeyFacts(keyFacts);
        episode.setEmotion(emotion != null ? emotion : "neutral");
        episode.setCreatedAt(LocalDateTime.now());
        episodeMapper.insert(episode);

        if (keyFacts != null) {
            for (String fact : keyFacts) {
                addMemory(userId, null, "fact", fact, "extraction", null);
            }
        }
        return episode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void promoteToLongTerm(Long memoryId) {
        AiMemory memory = memoryMapper.selectById(memoryId);
        if (memory == null || "L2".equals(memory.getMemoryLevel())) return;

        memory.setMemoryLevel("L2");
        memory.setExpiresAt(null);
        memory.setImportance(memory.getImportance().add(new BigDecimal("0.10")));
        memory.setUpdatedAt(LocalDateTime.now());
        memoryMapper.updateById(memory);

        logRefinement("promote", String.valueOf(memoryId), null, "访问频次或重要性达标，提升为长期记忆");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredMemories() {
        List<AiMemory> expired = memoryMapper.findExpiredL1Memories();
        int count = 0;
        for (AiMemory m : expired) {
            if (m.getAccessCount() >= 3 || m.getImportance().compareTo(new BigDecimal("0.70")) >= 0) {
                promoteToLongTerm(m.getId());
            } else {
                memoryMapper.deleteById(m.getId());
                logRefinement("evict", String.valueOf(m.getId()), null, "短期记忆过期且重要性不足");
                count++;
            }
        }
        log.info("清理过期短期记忆: 淘汰 {} 条", count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refineMemories(Long userId) {
        // 简化实现：标记长期记忆中重要性低且长时间未访问的记忆
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
               .eq(AiMemory::getMemoryLevel, "L2")
               .lt(AiMemory::getImportance, new BigDecimal("0.30"))
               .lt(AiMemory::getLastAccessedAt, LocalDateTime.now().minusDays(30));
        List<AiMemory> lowValueMemories = memoryMapper.selectList(wrapper);

        int demoted = 0;
        for (AiMemory m : lowValueMemories) {
            m.setMemoryLevel("L1");
            m.setExpiresAt(LocalDateTime.now().plusDays(3));
            m.setUpdatedAt(LocalDateTime.now());
            memoryMapper.updateById(m);
            logRefinement("demote", String.valueOf(m.getId()), null, "长期记忆降级：重要性低且长时间未访问");
            demoted++;
        }
        log.info("记忆精炼: 降级 {} 条低价值记忆", demoted);
        return demoted;
    }

    @Override
    public List<AiMemory> listUserMemories(Long userId, String type, int page, int pageSize) {
        Page<AiMemory> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(AiMemory::getMemoryType, type);
        }
        wrapper.orderByDesc(AiMemory::getUpdatedAt);
        return memoryMapper.selectPage(p, wrapper).getRecords();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMemory(Long id, Long userId) {
        AiMemory memory = memoryMapper.selectById(id);
        if (memory != null && memory.getUserId().equals(userId)) {
            memoryMapper.deleteById(id);
        }
    }

    // ==================== 内部方法 ====================

    private BigDecimal calculateInitialImportance(String type) {
        return switch (type) {
            case "decision" -> new BigDecimal("0.80");
            case "preference" -> new BigDecimal("0.70");
            case "profile" -> new BigDecimal("0.75");
            case "fact" -> new BigDecimal("0.50");
            case "event" -> new BigDecimal("0.40");
            default -> new BigDecimal("0.50");
        };
    }

    private String toBooleanModeQuery(String keyword) {
        if (keyword == null || keyword.isBlank()) return "";
        return Arrays.stream(keyword.trim().split("\\s+"))
                .filter(s -> !s.isBlank())
                .map(s -> "+" + s + "*")
                .collect(Collectors.joining(" "));
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private void logRefinement(String action, String sourceIds, Long targetId, String reason) {
        AiMemoryRefinementLog logEntry = new AiMemoryRefinementLog();
        logEntry.setAction(action);
        logEntry.setSourceIds(sourceIds);
        logEntry.setTargetId(targetId);
        logEntry.setReason(reason);
        logEntry.setCreatedAt(LocalDateTime.now());
        refinementLogMapper.insert(logEntry);
    }
}
