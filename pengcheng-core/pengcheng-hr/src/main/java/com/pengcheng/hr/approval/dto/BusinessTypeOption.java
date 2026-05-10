package com.pengcheng.hr.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批流业务类型选项（供管理后台 tab 列表用）。
 * 业务类型本身是字符串，{@link #label} 是中文显示名；nodeCount = 当前已配置的启用节点数（含 0）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTypeOption {
    /** 业务类型 key，如 "leave"、"expense" */
    private String businessType;
    /** 中文显示名，未注册的自定义类型直接回显原 key */
    private String label;
    /** 备注说明 */
    private String description;
    /** 1=内置（leave/compensate/expense/advance/prepay）：禁删除、禁改 key */
    private Integer builtin;
    /** tab 显示顺序 */
    private Integer sort;
    /** 已配置节点数（按 enabled+未删除统计） */
    private Integer nodeCount;
    /** 数据库主键（前端编辑/删除需要） */
    private Long id;
}
