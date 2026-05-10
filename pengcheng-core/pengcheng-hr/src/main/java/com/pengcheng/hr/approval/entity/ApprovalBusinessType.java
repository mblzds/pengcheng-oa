package com.pengcheng.hr.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批流业务类型注册表（管理后台 tab 列表的数据源）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_business_type")
public class ApprovalBusinessType extends BaseEntity {

    /** 业务类型 key（小写字母+下划线，对应 approval_flow_node.business_type） */
    private String businessType;

    /** 中文显示名 */
    private String label;

    /** 备注说明，可空 */
    private String description;

    /** 1=内置（leave/compensate/expense/advance/prepay）：禁删除、禁改 key，label 仍可改 */
    private Integer builtin;

    /** 0 禁用 1 启用 */
    private Integer enabled;

    /** tab 显示顺序（升序） */
    private Integer sort;
}
