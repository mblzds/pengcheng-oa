package com.pengcheng.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 签到记录实体（公司级假勤）
 * location 保留为原始位置字符串（lat,lng 兜底）；address 是逆地理翻译后的中文地址，
 * 供管理后台「签到记录」直接展示。
 */
@TableName("sign_in_record")
public class SignInRecord extends BaseEntity {

    private Long userId;
    private LocalDateTime signInTime;
    private String location;
    private String address;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String remark;

    /** 列表展示用：批量回填，不映射数据库列 */
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String employeeNo;
    @TableField(exist = false)
    private String deptName;

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
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

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
        public SignInRecord build() {
            SignInRecord r = new SignInRecord();
            r.setUserId(userId); r.setSignInTime(signInTime);
            r.setLocation(location); r.setAddress(address);
            r.setLatitude(latitude); r.setLongitude(longitude);
            r.setPhotoUrl(photoUrl); r.setRemark(remark);
            return r;
        }
    }
}
