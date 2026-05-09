package com.pengcheng.admin.dto.roster;

import lombok.Data;

import java.util.List;

/**
 * 实际导入完成后的结果（commit 调用返回）
 */
@Data
public class RosterImportResultVO {

    private int deptsCreated;
    private int usersCreated;
    private int usersUpdated;
    private int usersDeactivated;
    private int errorRows;

    /** 错误明细，前端可下载为 CSV 报告给 HR 查 */
    private List<RosterPreviewVO.RowError> errors;

    /** 默认初始密码（提示给 HR：新员工首次登录用此密码） */
    private String defaultPassword;
}
