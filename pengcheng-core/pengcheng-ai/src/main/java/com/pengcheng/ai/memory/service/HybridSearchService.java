package com.pengcheng.ai.memory.service;

import com.pengcheng.ai.memory.entity.AiMemory;
import com.pengcheng.ai.memory.mapper.AiMemoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务（70% 向量 + 30% BM25）
 * 融合 PGVector 语义搜索和 MySQL FULLTEXT 关键词搜索
 */
@Slf4j
@Service
public class HybridSearchService {

    private final AiMemoryMapper memoryMapper;
    @Autowired(required = false)
    private VectorStore vectorStore;

    private static final double VECTOR_WEIGHT = 0.7;
    private static final double BM25_WEIGHT = 0.3;

    public HybridSearchService(AiMemoryMapper memoryMapper) {
        this.memoryMapper = memoryMapper;
    }

    /**
     * 混合搜索：结合向量相似度和关键词匹配
     */
    public List<AiMemory> hybridSearch(Long userId, String query, int topK) {
        Map<Long, Double> scoreMap = new LinkedHashMap<>();

        List<AiMemory> bm25Results = bm25Search(userId, query, topK * 2);
        for (int i = 0; i < bm25Results.size(); i++) {
            double normalizedScore = 1.0 - (double) i / bm25Results.size();
            scoreMap.merge(bm25Results.get(i).getId(), normalizedScore * BM25_WEIGHT, (a, b) -> a + b);
        }

        if (vectorStore != null) {
            try {
                List<Document> vectorResults = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(query)
                                .topK(topK * 2)
                                .build());
                for (int i = 0; i < vectorResults.size(); i++) {
                    String memoryIdStr = vectorResults.get(i).getMetadata().get("memoryId") != null
                            ? vectorResults.get(i).getMetadata().get("memoryId").toString() : null;
                    if (memoryIdStr == null) continue;
                    try {
                        Long memoryId = Long.parseLong(memoryIdStr);
                        double normalizedScore = 1.0 - (double) i / vectorResults.size();
                        scoreMap.merge(memoryId, normalizedScore * VECTOR_WEIGHT, (a, b) -> a + b);
                    } catch (NumberFormatException ignored) {}
                }
            } catch (Exception e) {
                log.warn("[HybridSearch] 向量搜索失败，仅使用 BM25: {}", e.getMessage());
            }
        } else {
            log.debug("[HybridSearch] VectorStore 不可用，仅使用 BM25 关键词搜索");
        }

        List<Long> sortedIds = scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (sortedIds.isEmpty()) return List.of();

        Map<Long, AiMemory> memoryMap = new HashMap<>();
        for (AiMemory m : bm25Results) {
            memoryMap.put(m.getId(), m);
        }
        for (Long id : sortedIds) {
            if (!memoryMap.containsKey(id)) {
                AiMemory m = memoryMapper.selectById(id);
                if (m != null) memoryMap.put(id, m);
            }
        }
        return sortedIds.stream()
                .map(memoryMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<AiMemory> bm25Search(Long userId, String query, int topK) {
        String booleanQuery = toBooleanModeQuery(query);
        return memoryMapper.searchByFulltext(userId, booleanQuery, topK);
    }

    private String toBooleanModeQuery(String query) {
        if (query == null || query.isBlank()) return "";
        return Arrays.stream(query.trim().split("\\s+"))
                .filter(w -> w.length() >= 2)
                .map(w -> "+" + w + "*")
                .collect(Collectors.joining(" "));
    }

    public boolean isVectorStoreAvailable() {
        return vectorStore != null;
    }
}
