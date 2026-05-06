package com.pengcheng.hr.attendance.dto;

import java.time.LocalDateTime;

public class SignInDTO {
    private Long userId;
    private LocalDateTime signInTime;
    private String location;
    private String remark;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getSignInTime() { return signInTime; }
    public void setSignInTime(LocalDateTime signInTime) { this.signInTime = signInTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long userId;
        private LocalDateTime signInTime;
        private String location;
        private String remark;
        public Builder userId(Long v) { this.userId = v; return this; }
        public Builder signInTime(LocalDateTime v) { this.signInTime = v; return this; }
        public Builder location(String v) { this.location = v; return this; }
        public Builder remark(String v) { this.remark = v; return this; }
        public SignInDTO build() {
            SignInDTO r = new SignInDTO();
            r.setUserId(userId); r.setSignInTime(signInTime); r.setLocation(location); r.setRemark(remark);
            return r;
        }
    }
}
