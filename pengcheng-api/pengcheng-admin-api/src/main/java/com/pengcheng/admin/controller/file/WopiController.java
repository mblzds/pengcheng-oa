package com.pengcheng.admin.controller.file;

import com.pengcheng.common.result.Result;
import com.pengcheng.file.service.SysFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WOPI (Web Application Open Platform Interface) 端点
 * 供 OnlyOffice Document Server 回调访问文件内容
 *
 * WOPI 协议规定了两个核心端点：
 * - CheckFileInfo: GET /wopi/files/{fileId} — 返回文件元数据
 * - GetFile: GET /wopi/files/{fileId}/contents — 返回文件二进制
 * - PutFile: POST /wopi/files/{fileId}/contents — 保存文件
 */
@Slf4j
@RestController
@RequestMapping("/wopi/files")
@RequiredArgsConstructor
public class WopiController {

    private final SysFileService fileService;

    @Value("${onlyoffice.jwt-secret:masterlife_onlyoffice_secret}")
    private String jwtSecret;

    /**
     * CheckFileInfo — 返回文件元数据
     * OnlyOffice 编辑器首先调用此接口获取文件信息
     */
    @GetMapping(value = "/{fileId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> checkFileInfo(
            @PathVariable Long fileId,
            @RequestHeader(value = "X-WOPI-Override", required = false) String wopiOverride) {

        var file = fileService.getById(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("BaseFileName", file.getOriginalName());
        info.put("Size", file.getFileSize() != null ? file.getFileSize() : 0L);
        info.put("OwnerId", file.getCreateBy() != null ? file.getCreateBy() : "system");
        info.put("UserId", "system");
        info.put("UserFriendlyName", "系统用户");
        info.put("Version", "1");
        info.put("UserCanWrite", true);
        info.put("UserCanNotWriteRelative", false);
        info.put("SupportsUpdate", true);
        info.put("SupportsLocks", false);

        String ext = getFileExtension(file.getOriginalName());
        info.put("FileExtension", "." + ext);

        return ResponseEntity.ok(info);
    }

    /**
     * GetFile — 返回文件二进制内容
     */
    @GetMapping(value = "/{fileId}/contents", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getFile(@PathVariable Long fileId) {
        var file = fileService.getById(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] bytes = fileService.getFileBytes(fileId);
            Resource resource = new ByteArrayResource(bytes);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            log.error("[WOPI] 加载文件失败: fileId={}", fileId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PutFile — 保存文件
     * OnlyOffice 编辑完成后回调此接口写入文件
     */
    @PostMapping(value = "/{fileId}/contents", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Map<String, Object>> putFile(
            @PathVariable Long fileId,
            @RequestBody byte[] content) {

        var file = fileService.getById(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            fileService.updateFileContent(fileId, content);
            log.info("[WOPI] 文件保存成功: fileId={}, size={}", fileId, content.length);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("LastModifiedTime", java.time.Instant.now().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[WOPI] 文件保存失败: fileId={}", fileId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 生成 OnlyOffice 编辑器配置（返回 Result 便于前端统一解析）
     */
    @GetMapping("/editor-url/{fileId}")
    public Result<Map<String, Object>> getEditorUrl(
            @PathVariable Long fileId,
            @Value("${onlyoffice.server-url:}") String serverUrl,
            @Value("${app.wopi-access-url:}") String wopiAccessUrl,
            HttpServletRequest request) {

        var file = fileService.getById(fileId);
        if (file == null) {
            return Result.fail("文件不存在");
        }

        String ext = getFileExtension(file.getOriginalName());
        String documentType = getDocumentType(ext);
        String requestBaseUrl = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());
        String wopiBaseUrl = (wopiAccessUrl == null || wopiAccessUrl.isBlank())
                ? requestBaseUrl + "/api"
                : wopiAccessUrl.replaceAll("/$", "");

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("documentServerUrl", (serverUrl == null || serverUrl.isBlank()) ? "" : serverUrl.replaceAll("/$", ""));
        config.put("documentType", documentType);

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileType", ext);
        document.put("key", fileId + "_" + System.currentTimeMillis());
        document.put("title", file.getOriginalName());
        document.put("url", wopiBaseUrl + "/wopi/files/" + fileId + "/contents");
        config.put("document", document);

        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("callbackUrl", wopiBaseUrl + "/wopi/files/" + fileId + "/contents");
        editorConfig.put("lang", "zh-CN");
        config.put("editorConfig", editorConfig);

        return Result.ok(config);
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private String getDocumentType(String ext) {
        return switch (ext) {
            case "doc", "docx", "odt", "rtf", "txt" -> "word";
            case "xls", "xlsx", "ods", "csv" -> "cell";
            case "ppt", "pptx", "odp" -> "slide";
            default -> "word";
        };
    }
}
