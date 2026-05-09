package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.admin.dto.roster.RosterImportResultVO;
import com.pengcheng.admin.dto.roster.RosterPreviewVO;
import com.pengcheng.admin.service.RosterImportService;
import com.pengcheng.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 花名册批量导入：上传 CSV → 预览校验 → 确认导入。
 * CSV 表头与 RosterImportServiceImpl.HEADERS 保持一致；模板由 GET /template 提供。
 */
@RestController
@RequestMapping("/admin/roster")
@RequiredArgsConstructor
@SaCheckLogin
public class RosterImportController {

    private final RosterImportService rosterImportService;

    /** 上传 CSV，仅校验不入库，返回 stats + errors，前端展示后由 HR 决定是否提交 */
    @PostMapping("/preview")
    @SaCheckPermission("sys:user:add")
    public Result<RosterPreviewVO> preview(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(rosterImportService.preview(file));
    }

    /** 上传 CSV 并实际入库（@Transactional），返回创建/更新/离职计数 + 错误明细 */
    @PostMapping("/import")
    @SaCheckPermission("sys:user:add")
    public Result<RosterImportResultVO> importRoster(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(rosterImportService.importRoster(file));
    }

    /** 下载模板 CSV（含表头 + 4 行示例），UTF-8 + BOM 让 Excel 正确识别中文 */
    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> template() {
        String csv = "﻿" + rosterImportService.template();  // BOM 防 Excel 中文乱码
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        String filename = URLEncoder.encode("员工花名册-模板.csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }
}
