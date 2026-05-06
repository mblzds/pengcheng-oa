package com.pengcheng.realty.receivable.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建回款计划请求（按成交一次性生成 N 期）
 */
@Data
public class ReceivablePlanCreateDTO {

    /** 成交记录 ID */
    private Long dealId;

    /** 分期列表 */
    private List<Item> items;

    @Data
    public static class Item {
        private Integer periodNo;
        private String periodName;
        private LocalDate dueDate;
        private BigDecimal dueAmount;
        private String remark;
    }
}
