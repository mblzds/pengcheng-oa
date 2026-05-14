package com.pengcheng.realty.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

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

    /**
     * 可见的创建人 userId 集合（由 controller 层根据"基础职级 + 佣金模块加成"算出）。
     *   null = 全员可见；空集合 = 一个都看不到；非空 = 限定 create_by 在集合内
     */
    private Set<Long> allowedCreatorIds;

    public Integer getPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
