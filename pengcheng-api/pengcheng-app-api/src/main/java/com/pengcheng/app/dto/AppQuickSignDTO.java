package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拍照签到请求 DTO（区别于 AppSignDTO 的扫码签到）
 * 小程序拍照后先调 /upload-photo 拿 URL，再带 photoUrl + GPS 经纬度（WGS-84）+ 备注 提交本接口。
 * 逆地理（GPS → 中文地址）由后端用「系统配置 → 考勤设置」里的百度 AK 完成。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppQuickSignDTO {

    /** 签到照片 URL（必填，前端先调 /upload-photo 拿到） */
    private String photoUrl;

    /** 纬度（WGS-84），小程序 gcj02ToWgs84 后上报 */
    private Double latitude;

    /** 经度（WGS-84） */
    private Double longitude;

    /** 备注（可选） */
    private String remark;
}
