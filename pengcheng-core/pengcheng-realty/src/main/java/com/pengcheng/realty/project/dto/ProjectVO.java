package com.pengcheng.realty.project.dto;

import com.pengcheng.realty.project.entity.Project;
import com.pengcheng.realty.project.entity.ProjectCommissionRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目展示 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectVO {

    private Long id;
    private String projectName;
    private String developerName;
    private String address;
    private Integer projectType;
    private Integer status;
    private String district;
    private LocalDate agencyStartDate;
    private LocalDate agencyEndDate;
    private String contactPerson;
    private String contactPhone;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 当前生效的佣金规则（项目详情时填充） */
    private ProjectCommissionRule commissionRule;

    public static ProjectVO fromEntity(Project project) {
        if (project == null) {
            return null;
        }
        return ProjectVO.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .developerName(project.getDeveloperName())
                .address(project.getAddress())
                .projectType(project.getProjectType())
                .status(project.getStatus())
                .district(project.getDistrict())
                .agencyStartDate(project.getAgencyStartDate())
                .agencyEndDate(project.getAgencyEndDate())
                .contactPerson(project.getContactPerson())
                .contactPhone(project.getContactPhone())
                .description(project.getDescription())
                .createTime(project.getCreateTime())
                .updateTime(project.getUpdateTime())
                .build();
    }
}
