package com.pengcheng.realty.receivable.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReceivablePlanQueryDTO {

    private Long dealId;
    private Integer status;
    private LocalDate dueDateFrom;
    private LocalDate dueDateTo;

    private Integer page = 1;
    private Integer pageSize = 20;
}
