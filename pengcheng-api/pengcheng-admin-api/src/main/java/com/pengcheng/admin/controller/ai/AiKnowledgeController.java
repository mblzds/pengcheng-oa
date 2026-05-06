package com.pengcheng.admin.controller.ai;

import com.pengcheng.ai.rag.KnowledgeBaseService;
import com.pengcheng.ai.rag.KnowledgeDocRegistryService;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 知识库控制器
 * <p>
 * 提供文档上传和 RAG 检索问答接口。
 */
@RestController
@RequestMapping("/admin/ai/knowledge")
@RequiredArgsConstructor
public class AiKnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocRegistryService knowledgeDocRegistryService;

    /**
     * 上传知识库文档
     *
     * @param file      上传的文件（PDF/Word/Excel/TXT）
     * @param projectId 关联的项目ID（可选）
     * @return 处理的切片数量
     */
    @PostMapping("/upload")
    @Log(title = "知识库上传", businessType = BusinessType.INSERT)
    public Result<Integer> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "projectId", required = false) Long projectId) {
        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }
        KnowledgeDocRegistryService.KnowledgeDoc doc =
                knowledgeDocRegistryService.registerProcessing(file.getOriginalFilename(), projectId);
        try {
            int chunkCount = knowledgeBaseService.processDocument(file, projectId);
            knowledgeDocRegistryService.markDone(doc.id());
            return Result.ok(chunkCount);
        } catch (Exception e) {
            knowledgeDocRegistryService.markFailed(doc.id());
            return Result.fail("文档处理失败: " + e.getMessage());
        }
    }

    /**
     * 已上传文档列表
     */
    @GetMapping("/docs")
    public Result<List<KnowledgeDocVO>> listDocs() {
        List<KnowledgeDocVO> docs = knowledgeDocRegistryService.list().stream()
                .map(doc -> new KnowledgeDocVO(
                        doc.id(),
                        doc.fileName(),
                        doc.projectId(),
                        doc.status(),
                        doc.uploadTime()
                ))
                .toList();
        return Result.ok(docs);
    }

    /**
     * 删除文档（仅删除管理记录）
     */
    @DeleteMapping("/docs/{id}")
    @Log(title = "知识库文档删除", businessType = BusinessType.DELETE)
    public Result<Void> deleteDoc(@PathVariable Long id) {
        knowledgeDocRegistryService.delete(id);
        return Result.ok();
    }

    /**
     * RAG 知识库问答
     *
     * @param request 问答请求
     * @return 回答内容
     */
    @PostMapping("/query")
    public Result<String> queryKnowledge(@RequestBody KnowledgeQueryRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return Result.fail("问题不能为空");
        }
        String answer = knowledgeBaseService.queryKnowledge(request.question(), request.projectId());
        return Result.ok(answer);
    }

    /**
     * 知识库问答请求
     */
    public record KnowledgeQueryRequest(String question, Long projectId) {}

    /**
     * 文档元数据展示
     */
    public record KnowledgeDocVO(
            Long id,
            String fileName,
            Long projectId,
            String status,
            String uploadTime
    ) {}
}
