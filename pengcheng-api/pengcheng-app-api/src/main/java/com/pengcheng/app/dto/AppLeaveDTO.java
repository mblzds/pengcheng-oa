package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 请假申请请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppLeaveDTO {

    /** 请假类型：1-事假 2-病假 3-年假 4-婚假 5-产假 */
    private Integer leaveType;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 请假原因 */
    private String reason;
}
