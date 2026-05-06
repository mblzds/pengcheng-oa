package com.pengcheng.realty.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 佣金查询 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionQueryDTO {

    /** 当前页 */
    private Integer page;

    /** 每页条数 */
    private Integer pageSize;

    /** 项目ID */
    private Long projectId;

    /** 联盟商ID */
    private Long allianceId;

    /** 审核状态：1-待审核 2-审核通过 3-审核驳回 */
    private Integer auditStatus;

    public Integer getPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
