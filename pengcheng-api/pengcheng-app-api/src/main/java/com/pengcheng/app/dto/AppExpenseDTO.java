package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报销申请请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppExpenseDTO {

    /** 报销类型：1-交通费 2-餐饮费 3-住宿费 4-办公用品 5-其他 */
    private Integer expenseType;

    /** 报销金额 */
    private BigDecimal amount;

    /** 费用发生时间 */
    private LocalDateTime occurTime;

    /** 费用说明 */
    private String description;

    /** 票据附件URL列表 */
    private List<String> attachments;
}
