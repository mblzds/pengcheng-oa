package com.pengcheng.realty.receivable.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 登记一笔回款到账流水
 */
@Data
public class ReceivableRecordCreateDTO {

    private Long planId;
    private BigDecimal amount;
    private LocalDate paidDate;
    private Integer payWay;
    private String payer;
    private String voucherNo;
    private String attachmentUrl;
    private String remark;
}
