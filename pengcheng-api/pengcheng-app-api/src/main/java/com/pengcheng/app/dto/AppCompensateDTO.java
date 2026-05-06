package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 调休申请请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppCompensateDTO {

    /** 调休日期 */
    private LocalDate compensateDate;

    /** 调休原因 */
    private String reason;
}
