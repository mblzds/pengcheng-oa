package com.pengcheng.hr.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用审批申请实体（Phase 2 of 通用流水线）
 * 让管理员新建的非内置业务类型 (e.g. travel/overtime) 拥有自己的提交记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("general_approval_request")
public class GeneralApprovalRequest extends BaseEntity {

    /** 业务类型 key（必须在 approval_business_type 表中已注册且非内置） */
    private String businessType;

    /** 申请人 sys_user.id */
    private Long applicantId;

    /** 申请标题（必填） */
    private String title;

    /** 详细说明 */
    private String description;

    /** ApprovalConstants.STATUS_*: 1=审批中 2=已通过 3=已驳回 4=已撤销 */
    private Integer status;
}
