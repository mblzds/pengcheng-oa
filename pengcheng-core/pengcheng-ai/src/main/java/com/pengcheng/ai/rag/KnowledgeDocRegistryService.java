package com.pengcheng.ai.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 知识库文档元数据注册表（Redis）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocRegistryService {

    private static final String DOC_ID_KEY = "pengcheng:ai:kb:doc:id";
    private static final String DOC_STORE_KEY = "pengcheng:ai:kb:docs";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public KnowledgeDoc registerProcessing(String fileName, Long projectId) {
        Long id = redisTemplate.opsForValue().increment(DOC_ID_KEY);
        if (id == null) {
            throw new IllegalStateException("生成知识库文档ID失败");
        }

        KnowledgeDoc doc = new KnowledgeDoc(
                id,
                StringUtils.hasText(fileName) ? fileName : "unknown",
                projectId,
                "PROCESSING",
                LocalDateTime.now().toString()
        );
        save(doc);
        return doc;
    }

    public void markDone(Long id) {
        updateStatus(id, "DONE");
    }

    public void markFailed(Long id) {
        updateStatus(id, "FAILED");
    }

    public void delete(Long id) {
        redisTemplate.opsForHash().delete(DOC_STORE_KEY, String.valueOf(id));
    }

    public List<KnowledgeDoc> list() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(DOC_STORE_KEY);
        List<KnowledgeDoc> docs = new ArrayList<>();
        for (Object value : entries.values()) {
            if (value == null) {
                continue;
            }
            try {
                docs.add(objectMapper.readValue(String.valueOf(value), KnowledgeDoc.class));
            } catch (JsonProcessingException e) {
                log.warn("解析知识库文档元数据失败", e);
            }
        }
        docs.sort(Comparator.comparing(KnowledgeDoc::uploadTime).reversed());
        return docs;
    }

    private void updateStatus(Long id, String status) {
        if (id == null) {
            return;
        }
        KnowledgeDoc current = get(id);
        if (current == null) {
            return;
        }
        save(new KnowledgeDoc(
                current.id(),
                current.fileName(),
                current.projectId(),
                status,
                current.uploadTime()
        ));
    }

    private KnowledgeDoc get(Long id) {
        Object raw = redisTemplate.opsForHash().get(DOC_STORE_KEY, String.valueOf(id));
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.readValue(String.valueOf(raw), KnowledgeDoc.class);
        } catch (JsonProcessingException e) {
            log.warn("读取知识库文档元数据失败, id={}", id, e);
            return null;
        }
    }

    private void save(KnowledgeDoc doc) {
        try {
            redisTemplate.opsForHash().put(DOC_STORE_KEY, String.valueOf(doc.id()), objectMapper.writeValueAsString(doc));
            redisTemplate.expire(DOC_STORE_KEY, 90, TimeUnit.DAYS);
            redisTemplate.expire(DOC_ID_KEY, 90, TimeUnit.DAYS);
        } catch (JsonProcessingException e) {
            throw new KnowledgeBaseException("保存知识库文档元数据失败", e);
        }
    }

    public record KnowledgeDoc(
            Long id,
            String fileName,
            Long projectId,
            String status,
            String uploadTime
    ) {
    }
}

