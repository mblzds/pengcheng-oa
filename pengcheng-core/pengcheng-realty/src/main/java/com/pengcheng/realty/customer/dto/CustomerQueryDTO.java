package com.pengcheng.realty.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户查询 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerQueryDTO {

    /** 当前页 */
    private Integer page;

    /** 每页条数 */
    private Integer pageSize;

    /** 客户姓氏（模糊搜索） */
    private String customerName;

    /** 联系方式（模糊搜索） */
    private String phone;

    /** 项目ID */
    private Long projectId;

    /** 联盟商ID */
    private Long allianceId;

    /** 报备时间范围-开始 */
    private LocalDateTime startTime;

    /** 报备时间范围-结束 */
    private LocalDateTime endTime;

    /** 客户状态：1-已报备 2-已到访 3-已成交 */
    private Integer status;

    public Integer getPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
