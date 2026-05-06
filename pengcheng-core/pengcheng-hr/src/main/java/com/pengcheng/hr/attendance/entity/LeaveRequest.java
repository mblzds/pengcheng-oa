package com.pengcheng.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 请假申请实体（公司级假勤）
 */
@TableName("leave_request")
public class LeaveRequest extends BaseEntity {

    private Long userId;
    private Integer leaveType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private Integer status;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getLeaveType() { return leaveType; }
    public void setLeaveType(Integer leaveType) { this.leaveType = leaveType; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long userId;
        private Integer leaveType;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String reason;
        private Integer status;
        public Builder userId(Long v) { this.userId = v; return this; }
        public Builder leaveType(Integer v) { this.leaveType = v; return this; }
        public Builder startTime(LocalDateTime v) { this.startTime = v; return this; }
        public Builder endTime(LocalDateTime v) { this.endTime = v; return this; }
        public Builder reason(String v) { this.reason = v; return this; }
        public Builder status(Integer v) { this.status = v; return this; }
        public LeaveRequest build() {
            LeaveRequest r = new LeaveRequest();
            r.setUserId(userId); r.setLeaveType(leaveType); r.setStartTime(startTime);
            r.setEndTime(endTime); r.setReason(reason); r.setStatus(status);
            return r;
        }
    }
}
