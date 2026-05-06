package com.pengcheng.realty.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 代理项目实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("project")
public class Project extends BaseEntity {

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 开发商名称
     */
    private String developerName;

    /**
     * 项目地址
     */
    private String address;

    /**
     * 项目类型：1-住宅 2-商业 3-办公 4-综合体
     */
    private Integer projectType;

    /**
     * 项目状态：1-在售 2-待售 3-售罄 4-已到期
     */
    private Integer status;

    /**
     * 所属片区
     */
    private String district;

    /**
     * 代理开始时间
     */
    private LocalDate agencyStartDate;

    /**
     * 代理结束时间
     */
    private LocalDate agencyEndDate;

    /**
     * 联系驻场
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 项目介绍
     */
    private String description;
}
