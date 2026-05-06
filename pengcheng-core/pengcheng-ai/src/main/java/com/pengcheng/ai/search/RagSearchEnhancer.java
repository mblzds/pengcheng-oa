package com.pengcheng.ai.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG 增强搜索服务
 * 将自然语言查询转换为结构化搜索 + 向量语义检索
 */
@Slf4j
@Service
public class RagSearchEnhancer {

    private final ChatClient chatClient;
    @Autowired(required = false)
    private VectorStore vectorStore;

    public RagSearchEnhancer(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 自然语言查询转结构化搜索参数
     *
     * @param naturalQuery 用户的自然语言查询
     * @return 结构化搜索参数（keywords、scope、timeRange 等）
     */
    public Map<String, String> parseNaturalQuery(String naturalQuery) {
        try {
            String systemPrompt = """
                你是搜索助手。将自然语言查询解析为结构化搜索参数。
                输出 JSON 格式：
                {"keywords": "核心关键词", "scope": "搜索范围", "timeRange": "时间范围"}
                scope 可选值：all, customer, project, alliance, chat, notice, pm_project, pm_task
                timeRange 可选值：null（无限制）, today, week, month, year
                仅输出 JSON。
                """;
            String result = chatClient.prompt()
                    .system(systemPrompt)
                    .user(naturalQuery)
                    .call()
                    .content();
            if (result == null) return Map.of("keywords", naturalQuery);

            String cleaned = result.trim();
            int s = cleaned.indexOf('{'), e = cleaned.lastIndexOf('}');
            if (s >= 0 && e > s) {
                cleaned = cleaned.substring(s, e + 1);
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, String> parsed = om.readValue(cleaned, Map.class);
                return parsed;
            }
        } catch (Exception e) {
            log.warn("[RAG] 自然语言解析失败，使用原始查询: {}", e.getMessage());
        }
        return Map.of("keywords", naturalQuery);
    }

    /**
     * 向量语义检索相关文档
     */
    public List<String> semanticSearch(String query, int topK) {
        if (vectorStore == null) {
            log.debug("[RAG] VectorStore 未配置，语义搜索不可用");
            return List.of();
        }
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .build());
            List<String> results = new ArrayList<>();
            for (Document doc : docs) {
                results.add(doc.getText());
            }
            return results;
        } catch (Exception e) {
            log.warn("[RAG] 语义搜索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * RAG 增强回答：基于检索到的文档上下文，使用 LLM 生成总结性回答
     */
    public String ragAnswer(String query, List<String> contexts) {
        if (contexts.isEmpty()) return null;
        try {
            String contextStr = String.join("\n---\n", contexts);
            String systemPrompt = "你是房产销售系统的知识助手。基于以下检索到的文档片段，回答用户问题。" +
                    "如果文档中没有相关信息，请明确说明。\n\n参考文档：\n" + contextStr;
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(query)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("[RAG] 增强回答生成失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean isVectorStoreAvailable() {
        return vectorStore != null;
    }
}
