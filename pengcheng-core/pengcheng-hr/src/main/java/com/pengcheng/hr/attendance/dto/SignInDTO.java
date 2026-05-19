package com.pengcheng.hr.attendance.dto;

import java.time.LocalDateTime;

public class SignInDTO {
    private Long userId;
    private LocalDateTime signInTime;
    private String location;
    private String address;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String remark;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getSignInTime() { return signInTime; }
    public void setSignInTime(LocalDateTime signInTime) { this.signInTime = signInTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long userId;
        private LocalDateTime signInTime;
        private String location;
        private String address;
        private Double latitude;
        private Double longitude;
        private String photoUrl;
        private String remark;
        public Builder userId(Long v) { this.userId = v; return this; }
        public Builder signInTime(LocalDateTime v) { this.signInTime = v; return this; }
        public Builder location(String v) { this.location = v; return this; }
        public Builder address(String v) { this.address = v; return this; }
        public Builder latitude(Double v) { this.latitude = v; return this; }
        public Builder longitude(Double v) { this.longitude = v; return this; }
        public Builder photoUrl(String v) { this.photoUrl = v; return this; }
        public Builder remark(String v) { this.remark = v; return this; }
        public SignInDTO build() {
            SignInDTO r = new SignInDTO();
            r.setUserId(userId); r.setSignInTime(signInTime);
            r.setLocation(location); r.setAddress(address);
            r.setLatitude(latitude); r.setLongitude(longitude);
            r.setPhotoUrl(photoUrl); r.setRemark(remark);
            return r;
        }
    }
}
