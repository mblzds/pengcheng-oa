package com.pengcheng.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 直接上级用户ID（审批流 direct_supervisor 节点解析的优先来源；缺失则回退 sys_dept.leader_id）
     */
    private Long leaderId;

    /**
     * 部门名称（非数据库字段）
     */
    @TableField(exist = false)
    private String deptName;

    /**
     * 岗位名称列表（非数据库字段，逗号分隔）
     */
    @TableField(exist = false)
    private String postNames;

    /**
     * 用户名
     */
    private String username;

    /**
     * 工号（员工编号），唯一；花名册导入按此 upsert。null 仅在历史数据未补齐时出现
     */
    private String employeeNo;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    // 历史 DDL 列名是 nickname（无下划线），MyBatis-Plus 默认 camel→snake 会转出错误的 nick_name，必须显式标注
    @TableField("nickname")
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别(0-未知 1-男 2-女)
     */
    private Integer gender;

    /**
     * 状态(0-禁用 1-启用)
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 用户类型(admin-后台管理员 pc-PC前台用户 app-App/小程序用户)
     */
    private String userType;

    /**
     * 微信openId(微信扫码登录时使用)
     */
    private String openId;

    /**
     * 是否离职(0-否 1-是)
     */
    private Integer isQuit;
}
