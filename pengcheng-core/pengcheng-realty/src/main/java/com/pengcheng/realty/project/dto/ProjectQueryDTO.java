package com.pengcheng.realty.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目查询 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectQueryDTO {

    /** 当前页 */
    private Integer page;

    /** 每页条数 */
    private Integer pageSize;

    /** 项目名称（模糊搜索） */
    private String projectName;

    /** 所属片区 */
    private String district;

    /** 项目类型：1-住宅 2-商业 3-办公 4-综合体 */
    private Integer projectType;

    /** 项目状态：1-在售 2-待售 3-售罄 4-已到期 */
    private Integer status;

    public Integer getPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
