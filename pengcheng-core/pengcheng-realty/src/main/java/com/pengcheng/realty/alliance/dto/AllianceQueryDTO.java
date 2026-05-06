package com.pengcheng.realty.alliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 联盟商查询 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllianceQueryDTO {

    /** 当前页 */
    private Integer page;

    /** 每页条数 */
    private Integer pageSize;

    /** 公司名称（模糊搜索） */
    private String companyName;

    /** 状态：1-启用 0-停用 */
    private Integer status;

    /** 联盟商等级 */
    private Integer level;

    public Integer getPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
