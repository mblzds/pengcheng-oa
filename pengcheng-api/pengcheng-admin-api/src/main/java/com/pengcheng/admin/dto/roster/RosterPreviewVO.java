package com.pengcheng.admin.dto.roster;

import lombok.Data;

import java.util.List;

/**
 * CSV 预览结果（不入库，仅校验 + 计算 diff）
 * 前端按 stats 提示 + errors 列表展示，让 HR 决定是否提交导入
 */
@Data
public class RosterPreviewVO {

    private int totalRows;         // CSV 总数据行数
    private int validRows;         // 校验通过行数
    private int errorRows;         // 校验失败行数

    private int deptsToCreate;     // 将新建的部门数
    private int usersToCreate;     // 将新建的用户数
    private int usersToUpdate;     // 已存在工号 → 将更新的用户数
    private int usersToDeactivate; // 状态=离职 将软删的用户数

    private List<String> deptsToCreateList;  // 部门路径列表（按出现顺序）
    private List<RowError> errors;

    @Data
    public static class RowError {
        public RowError() {}
        public RowError(int lineNo, String employeeNo, String name, String reason) {
            this.lineNo = lineNo;
            this.employeeNo = employeeNo;
            this.name = name;
            this.reason = reason;
        }
        private int lineNo;
        private String employeeNo;
        private String name;
        private String reason;
    }
}
