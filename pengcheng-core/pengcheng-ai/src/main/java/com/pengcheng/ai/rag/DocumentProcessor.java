package com.pengcheng.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文档解析与向量化处理器
 * <p>
 * 使用 Apache Tika 解析 PDF/Word/Excel/PPT 等格式，进行文本切片和向量化存储。
 * 切片策略：按固定字符数切片（默认 500 字符），相邻切片有 100 字符重叠以保持上下文连贯。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessor {

    private final VectorStore vectorStore;
    private final Tika tika = new Tika();

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_CHUNK_OVERLAP = 100;
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "text/csv", "text/markdown"
    );

    /**
     * 处理上传的文档：解析文本 → 切片 → 向量化 → 存入向量数据库
     */
    public int processDocument(MultipartFile file, Long projectId) {
        String fileName = file.getOriginalFilename();
        log.info("开始处理文档: {}, 大小: {}KB, 关联项目: {}",
                fileName, file.getSize() / 1024, projectId);

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new KnowledgeBaseException("文件大小超过限制（50MB）: " + fileName);
        }

        String contentType = file.getContentType();
        if (contentType != null && !SUPPORTED_TYPES.contains(contentType)) {
            throw new KnowledgeBaseException("不支持的文件类型: " + contentType);
        }

        String text = extractText(file);
        if (text == null || text.isBlank()) {
            log.warn("文档内容为空: {}", fileName);
            return 0;
        }

        List<String> chunks = splitText(text, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
        log.info("文档 {} 切片完成，共 {} 个切片", fileName, chunks.size());

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = Map.of(
                    "source", fileName != null ? fileName : "unknown",
                    "chunkIndex", i,
                    "totalChunks", chunks.size(),
                    "projectId", projectId != null ? projectId : 0L
            );
            documents.add(new Document(chunks.get(i), metadata));
        }

        vectorStore.add(documents);
        log.info("文档 {} 向量化完成，已存入 {} 个向量", fileName, documents.size());
        return documents.size();
    }

    /**
     * 使用 Apache Tika 提取文档文本，支持 PDF/Word/Excel/PPT 等主流格式
     */
    String extractText(MultipartFile file) {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        try (InputStream is = file.getInputStream()) {
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            String text = tika.parseToString(is, metadata);
            String detectedType = metadata.get(Metadata.CONTENT_TYPE);
            log.info("Tika 检测类型: {}, 提取文本长度: {} 字符", detectedType, text.length());
            return text;
        } catch (Exception e) {
            log.error("文档解析失败: {}", fileName, e);
            throw new KnowledgeBaseException("文档解析失败: " + fileName, e);
        }
    }

    /**
     * 文本切片：按固定字符数切片，相邻切片有重叠
     */
    List<String> splitText(String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be >= 0 and < chunkSize");
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            start += chunkSize - chunkOverlap;
        }
        return chunks;
    }
}
