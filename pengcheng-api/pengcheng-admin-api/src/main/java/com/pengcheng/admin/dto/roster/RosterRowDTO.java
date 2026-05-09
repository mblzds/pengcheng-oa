package com.pengcheng.admin.dto.roster;

import lombok.Data;

/**
 * 花名册 CSV 一行 = 一个员工。
 * 字段对应 CSV 列名（中文表头）：
 *   工号 / 姓名 / 登录名 / 手机 / 邮箱 / 部门路径 / 本部门负责人 / 直接上级姓名 / 角色 / 性别 / 入职日期 / 状态
 */
@Data
public class RosterRowDTO {
    /** 行号（含表头则数据行从 2 开始），用于错误回报 */
    private int lineNo;

    private String employeeNo;     // 工号
    private String name;           // 姓名（nickname）
    private String username;       // 登录名（空则用工号）
    private String phone;          // 手机
    private String email;          // 邮箱
    private String deptPath;       // 部门路径，"/" 分隔
    private boolean isDeptLeader;  // 本部门负责人 Y/N
    private String leaderName;     // 直接上级姓名
    private String roleNames;      // 角色，分号分隔
    private String gender;         // M / F / 未知
    private String hireDate;       // YYYY-MM-DD
    private String status;         // 在职 / 离职
}
