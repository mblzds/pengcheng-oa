package com.pengcheng.admin.controller.file;

import com.pengcheng.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 文件预览（对接 kkFileView）
 */
@Slf4j
@RestController
@RequestMapping("/sys/file")
public class FilePreviewController {

    @Value("${kkfileview.url:http://localhost:8012}")
    private String kkFileViewUrl;

    @Value("${app.file-access-url:http://host.docker.internal:8080/api/files}")
    private String fileAccessUrl;

    /**
     * 生成文件预览 URL
     * <p>
     * kkFileView 通过 URL 拉取文件并渲染预览，此接口将内部文件路径转换为 kkFileView 可访问的预览链接。
     *
     * @param filePath 文件相对路径（如 uploads/2026/03/01/xxx.docx）
     * @return 包含 previewUrl 的 Map
     */
    @GetMapping("/preview-url")
    public Result<Map<String, String>> getPreviewUrl(@RequestParam String filePath) {
        String baseUrl = fileAccessUrl.endsWith("/") ? fileAccessUrl.substring(0, fileAccessUrl.length() - 1) : fileAccessUrl;
        String normalizedFilePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        String fileUrl = baseUrl + "/" + normalizedFilePath;
        String encodedUrl = URLEncoder.encode(fileUrl, StandardCharsets.UTF_8);
        String previewUrl = kkFileViewUrl + "/onlinePreview?url=" + encodedUrl;

        log.info("生成预览 URL: fileUrl={}, previewUrl={}", fileUrl, previewUrl);
        return Result.ok(Map.of(
                "previewUrl", previewUrl,
                "fileUrl", fileUrl
        ));
    }
}
