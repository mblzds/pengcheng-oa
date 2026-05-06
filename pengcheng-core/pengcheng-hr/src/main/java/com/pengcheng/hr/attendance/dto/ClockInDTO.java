package com.pengcheng.hr.attendance.dto;

import java.time.LocalDateTime;

public class ClockInDTO {
    private Long userId;
    private LocalDateTime clockTime;
    private String location;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getClockTime() { return clockTime; }
    public void setClockTime(LocalDateTime clockTime) { this.clockTime = clockTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long userId;
        private LocalDateTime clockTime;
        private String location;
        public Builder userId(Long v) { this.userId = v; return this; }
        public Builder clockTime(LocalDateTime v) { this.clockTime = v; return this; }
        public Builder location(String v) { this.location = v; return this; }
        public ClockInDTO build() {
            ClockInDTO r = new ClockInDTO();
            r.setUserId(userId); r.setClockTime(clockTime); r.setLocation(location);
            return r;
        }
    }
}
