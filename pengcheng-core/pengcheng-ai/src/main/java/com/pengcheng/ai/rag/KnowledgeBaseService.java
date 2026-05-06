package com.pengcheng.ai.rag;

import com.pengcheng.ai.service.AiFallbackHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库管理服务
 * <p>
 * 提供文档上传处理和 RAG 检索问答能力。
 * <ul>
 *   <li>上传文档：解析 → 切片 → 向量化 → 存入向量数据库</li>
 *   <li>RAG 问答：向量检索相关内容 → 结合大模型生成回答</li>
 *   <li>无相关内容时明确告知用户并建议联系相关人员</li>
 * </ul>
 * <p>
 * 降级策略：AI 服务不可用时返回"AI服务暂时不可用"提示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final DocumentProcessor documentProcessor;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final AiFallbackHandler fallbackHandler;

    /** 向量检索返回的最大文档数 */
    private static final int TOP_K = 5;
    /** 相似度阈值，低于此值认为无相关内容 */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    private static final String RAG_SYSTEM_PROMPT =
            "你是一个房产项目知识库助手。请根据以下参考资料回答用户的问题。\n"
            + "如果参考资料中没有相关信息，请明确告知用户当前知识库无法回答该问题，并建议联系相关人员。\n"
            + "请用中文回答，回答要准确、简洁。\n\n"
            + "参考资料：\n%s";

    private static final String NO_RELEVANT_CONTENT_MESSAGE =
            "当前知识库中未找到与您问题相关的内容，建议联系相关项目负责人或驻场人员获取帮助。";

    /**
     * 上传并处理文档
     *
     * @param file      上传的文件
     * @param projectId 关联的项目ID（可选）
     * @return 处理的切片数量
     */
    public int processDocument(MultipartFile file, Long projectId) {
        return documentProcessor.processDocument(file, projectId);
    }

    /**
     * RAG 检索问答
     *
     * @param question  用户问题
     * @param projectId 关联的项目ID（可选，用于过滤检索范围）
     * @return 回答内容
     */
    public String queryKnowledge(String question, Long projectId) {
        return fallbackHandler.executeWithFallback(
                () -> doQueryKnowledge(question, projectId),
                fallbackHandler::ragFallbackMessage,
                "RAG知识库问答"
        );
    }

    private String doQueryKnowledge(String question, Long projectId) {
        // 1. 向量检索相关文档
        List<Document> relevantDocs = searchRelevantDocuments(question, projectId);

        // 2. 无相关内容时明确告知
        if (relevantDocs.isEmpty()) {
            log.info("知识库中未找到与问题相关的内容: {}", question);
            return NO_RELEVANT_CONTENT_MESSAGE;
        }

        // 3. 构建上下文并调用大模型生成回答
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        String systemPrompt = String.format(RAG_SYSTEM_PROMPT, context);

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();

        return answer;
    }

    /**
     * 向量检索相关文档
     */
    List<Document> searchRelevantDocuments(String question, Long projectId) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        // 如果指定了项目ID，过滤出该项目的文档
        if (projectId != null && !results.isEmpty()) {
            results = results.stream()
                    .filter(doc -> {
                        Object docProjectId = doc.getMetadata().get("projectId");
                        return docProjectId == null
                                || Long.valueOf(0L).equals(docProjectId)
                                || projectId.equals(((Number) docProjectId).longValue());
                    })
                    .toList();
        }

        return results;
    }
}
