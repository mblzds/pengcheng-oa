package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 请假/调休记录响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRecordVO {

    /** 记录ID */
    private Long id;

    /** 类型：leave=请假 compensate=调休 */
    private String type;

    /** 请假类型：1-事假 2-病假 3-年假 4-婚假 5-产假 */
    private Integer leaveType;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 原因 */
    private String reason;

    /** 审批状态：1-待审批 2-已通过 3-已驳回 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
