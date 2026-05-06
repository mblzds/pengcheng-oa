package com.pengcheng.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调休申请实体（公司级假勤，表名沿用 realty_compensate_request）
 */
@TableName("realty_compensate_request")
public class CompensateRequest implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private LocalDate compensateDate;
    private String reason;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getCompensateDate() { return compensateDate; }
    public void setCompensateDate(LocalDate compensateDate) { this.compensateDate = compensateDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long userId;
        private LocalDate compensateDate;
        private String reason;
        private Integer status;
        public Builder userId(Long v) { this.userId = v; return this; }
        public Builder compensateDate(LocalDate v) { this.compensateDate = v; return this; }
        public Builder reason(String v) { this.reason = v; return this; }
        public Builder status(Integer v) { this.status = v; return this; }
        public CompensateRequest build() {
            CompensateRequest r = new CompensateRequest();
            r.setUserId(userId); r.setCompensateDate(compensateDate); r.setReason(reason); r.setStatus(status);
            return r;
        }
    }
}
