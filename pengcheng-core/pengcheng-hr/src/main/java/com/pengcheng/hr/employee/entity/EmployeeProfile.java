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
 * 员工档案扩展实体（关联 sys_user，公司级人事档案）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_profile")
public class EmployeeProfile extends BaseEntity {

    /** 关联 sys_user.id */
    private Long userId;
    /** 工号 */
    private String employeeNo;
    /** 入职日期 */
    private LocalDate joinDate;
    /** 转正日期 */
    private LocalDate formalDate;
    /** 合同开始 */
    private LocalDate contractStart;
    /** 合同结束 */
    private LocalDate contractEnd;
    /** 职级 */
    private String jobLevel;
    /** 工作地点 */
    private String workLocation;
    /** 紧急联系人 */
    private String emergencyContact;
    /** 紧急联系电话 */
    private String emergencyPhone;
    /** 备注 */
    private String remark;
}
