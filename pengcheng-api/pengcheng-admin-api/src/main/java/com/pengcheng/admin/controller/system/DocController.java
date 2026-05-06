package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.doc.entity.Doc;
import com.pengcheng.system.doc.entity.DocSpace;
import com.pengcheng.system.doc.entity.DocVersion;
import com.pengcheng.system.doc.service.DocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 云文档管理接口
 */
@RestController
@RequestMapping("/doc")
@RequiredArgsConstructor
public class DocController {

    private final DocService docService;

    // ========== 空间 ==========

    @GetMapping("/spaces")
    public Result<List<DocSpace>> getSpaces() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(docService.getUserSpaces(userId));
    }

    @PostMapping("/space")
    public Result<DocSpace> createSpace(@RequestBody DocSpace space) {
        space.setOwnerId(StpUtil.getLoginIdAsLong());
        return Result.ok(docService.createSpace(space));
    }

    @PutMapping("/space")
    public Result<Void> updateSpace(@RequestBody DocSpace space) {
        docService.updateSpace(space);
        return Result.ok();
    }

    @DeleteMapping("/space/{id}")
    public Result<Void> deleteSpace(@PathVariable Long id) {
        docService.deleteSpace(id);
        return Result.ok();
    }

    // ========== 文档 ==========

    @GetMapping("/tree/{spaceId}")
    public Result<List<Doc>> getDocTree(@PathVariable Long spaceId) {
        return Result.ok(docService.getDocTree(spaceId));
    }

    @GetMapping("/{id}")
    public Result<Doc> getDoc(@PathVariable Long id) {
        return Result.ok(docService.getDoc(id));
    }

    @PostMapping("/create")
    public Result<Doc> createDoc(@RequestBody Doc doc) {
        doc.setCreatorId(StpUtil.getLoginIdAsLong());
        return Result.ok(docService.createDoc(doc));
    }

    @PutMapping("/update")
    public Result<Void> updateDoc(@RequestBody Doc doc) {
        Long editorId = StpUtil.getLoginIdAsLong();
        docService.updateDoc(doc, editorId);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteDoc(@PathVariable Long id) {
        docService.deleteDoc(id);
        return Result.ok();
    }

    @PostMapping("/move")
    public Result<Void> moveDoc(@RequestParam Long docId, @RequestParam Long newParentId,
                           @RequestParam(defaultValue = "0") Integer sortOrder) {
        docService.moveDoc(docId, newParentId, sortOrder);
        return Result.ok();
    }

    // ========== 版本 ==========

    @GetMapping("/versions/{docId}")
    public Result<List<DocVersion>> getVersions(@PathVariable Long docId) {
        return Result.ok(docService.getVersionHistory(docId));
    }

    @PostMapping("/versions/restore")
    public Result<Void> restoreVersion(@RequestParam Long docId, @RequestParam Integer version) {
        Long editorId = StpUtil.getLoginIdAsLong();
        docService.restoreVersion(docId, version, editorId);
        return Result.ok();
    }

    // ========== 搜索 ==========

    @GetMapping("/search")
    public Result<List<Doc>> searchDocs(@RequestParam Long spaceId, @RequestParam String keyword) {
        return Result.ok(docService.searchDocs(spaceId, keyword));
    }

    // ========== 导出 ==========

    /**
     * 导出文档为可打印 HTML（前端通过 window.print() 生成 PDF）
     */
    @GetMapping(value = "/export/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String exportDocAsHtml(@PathVariable Long id) {
        Doc doc = docService.getDoc(id);
        if (doc == null) return "<html><body><p>文档不存在</p></body></html>";

        String markdownContent = doc.getContent() != null ? doc.getContent() : "";
        String htmlContent = markdownToHtml(markdownContent);

        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
            <meta charset="UTF-8">
            <title>%s</title>
            <style>
              @page { margin: 2cm; }
              body { font-family: -apple-system, 'Microsoft YaHei', sans-serif; line-height: 1.8; color: #333; max-width: 800px; margin: 0 auto; padding: 40px 20px; }
              h1 { font-size: 24px; border-bottom: 2px solid #333; padding-bottom: 8px; }
              h2 { font-size: 20px; border-bottom: 1px solid #ddd; padding-bottom: 6px; }
              h3 { font-size: 16px; }
              p { margin: 8px 0; }
              code { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
              pre { background: #f5f5f5; padding: 16px; border-radius: 6px; overflow-x: auto; }
              blockquote { border-left: 4px solid #18a058; padding-left: 16px; margin: 12px 0; color: #666; }
              table { border-collapse: collapse; width: 100%%; margin: 12px 0; }
              th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
              th { background: #f5f5f5; }
              ul, ol { padding-left: 24px; }
              .doc-meta { color: #999; font-size: 12px; margin-bottom: 20px; }
              @media print { .no-print { display: none; } }
            </style>
            </head>
            <body>
            <h1>%s</h1>
            <div class="doc-meta">版本 v%d | 字数 %d | 导出时间 %s</div>
            <div class="doc-content">%s</div>
            <div class="no-print" style="margin-top:40px;text-align:center;">
              <button onclick="window.print()" style="padding:10px 30px;font-size:16px;cursor:pointer;background:#18a058;color:white;border:none;border-radius:6px;">打印 / 导出 PDF</button>
            </div>
            </body>
            </html>
            """.formatted(
                doc.getTitle(),
                doc.getTitle(),
                doc.getVersion() != null ? doc.getVersion() : 1,
                doc.getWordCount() != null ? doc.getWordCount() : 0,
                java.time.LocalDateTime.now().toString().replace("T", " ").substring(0, 19),
                htmlContent
        );
    }

    /**
     * 简易 Markdown → HTML 转换（覆盖常见语法）
     */
    private String markdownToHtml(String md) {
        if (md == null || md.isEmpty()) return "";

        String html = md;
        html = html.replaceAll("(?m)^### (.+)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^## (.+)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^# (.+)$", "<h1>$1</h1>");
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        html = html.replaceAll("`([^`]+)`", "<code>$1</code>");
        html = html.replaceAll("(?m)^> (.+)$", "<blockquote>$1</blockquote>");
        html = html.replaceAll("(?m)^- (.+)$", "<li>$1</li>");
        html = html.replaceAll("(?m)^---$", "<hr/>");
        html = html.replaceAll("(<li>.*</li>)", "<ul>$1</ul>");
        html = html.replaceAll("</ul>\n<ul>", "\n");
        html = html.replaceAll("(?m)^(?!<[hublop/]|<li|<hr)(.+)$", "<p>$1</p>");
        html = html.replace("<p></p>", "");

        return html;
    }
}
