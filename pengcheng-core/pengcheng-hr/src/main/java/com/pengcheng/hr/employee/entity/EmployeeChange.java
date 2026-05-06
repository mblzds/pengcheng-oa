package com.pengcheng.hr.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 人事异动实体（入职/离职/调岗/调薪等，公司级）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_change")
public class EmployeeChange extends BaseEntity {

    /** 异动类型：1-入职 2-离职 3-调岗 4-调薪 5-其他 */
    public static final int TYPE_JOIN = 1;
    public static final int TYPE_LEAVE = 2;
    public static final int TYPE_TRANSFER = 3;
    public static final int TYPE_SALARY = 4;
    public static final int TYPE_OTHER = 5;

    /** 状态：1-草稿 2-已生效 */
    public static final int STATUS_DRAFT = 1;
    public static final int STATUS_EFFECTIVE = 2;

    private Long userId;
    private Integer changeType;
    private LocalDate changeDate;
    private Long beforeDeptId;
    private Long afterDeptId;
    private Long beforePostId;
    private Long afterPostId;
    private String reason;
    private String attachment;
    private Integer status;
}
