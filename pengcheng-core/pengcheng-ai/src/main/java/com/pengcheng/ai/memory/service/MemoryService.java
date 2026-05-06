package com.pengcheng.ai.memory.service;

import com.pengcheng.ai.memory.entity.AiMemory;
import com.pengcheng.ai.memory.entity.AiMemoryEpisode;

import java.util.List;

/**
 * AI 记忆服务
 */
public interface MemoryService {

    /**
     * 添加记忆
     */
    AiMemory addMemory(Long userId, Long customerId, String type, String content, String source, String tags);

    /**
     * 混合搜索记忆（FULLTEXT + 向量近似，后续集成 PGVector）
     */
    List<AiMemory> searchMemories(Long userId, String query, int topK);

    /**
     * 获取客户画像记忆
     */
    List<AiMemory> getCustomerProfileMemories(Long customerId, int topK);

    /**
     * 构建注入到 AI 对话的记忆上下文
     */
    String buildMemoryContext(Long userId, Long customerId, String currentQuery);

    /**
     * 保存对话摘要
     */
    AiMemoryEpisode saveEpisode(Long userId, String sessionId, String summary, List<String> keyFacts, String emotion);

    /**
     * 提升记忆层级（L1 → L2）
     */
    void promoteToLongTerm(Long memoryId);

    /**
     * 清理过期短期记忆
     */
    int cleanupExpiredMemories();

    /**
     * 精炼记忆（合并相似记忆）
     */
    int refineMemories(Long userId);

    /**
     * 获取用户全部记忆（分页）
     */
    List<AiMemory> listUserMemories(Long userId, String type, int page, int pageSize);

    /**
     * 删除记忆
     */
    void deleteMemory(Long id, Long userId);
}
