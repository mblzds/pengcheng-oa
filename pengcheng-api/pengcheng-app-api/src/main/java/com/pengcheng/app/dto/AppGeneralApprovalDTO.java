package com.pengcheng.app.dto;

import lombok.Data;

@Data
public class AppGeneralApprovalDTO {
    /** 业务类型 key（必须是 approval_business_type 中已注册的非内置类型） */
    private String businessType;
    /** 申请标题（必填） */
    private String title;
    /** 申请说明（可选） */
    private String description;
}
