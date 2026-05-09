package com.pengcheng.admin.service;

import com.pengcheng.admin.dto.roster.RosterImportResultVO;
import com.pengcheng.admin.dto.roster.RosterPreviewVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface RosterImportService {

    /** 解析 + 校验，不入库 */
    RosterPreviewVO preview(MultipartFile file) throws IOException;

    /** 解析 + 校验 + 入库（@Transactional） */
    RosterImportResultVO importRoster(MultipartFile file) throws IOException;

    /** 模板 CSV（含表头 + 3 行示例） */
    String template();
}
