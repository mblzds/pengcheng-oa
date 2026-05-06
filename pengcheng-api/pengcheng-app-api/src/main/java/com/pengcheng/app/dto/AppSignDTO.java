package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 扫码签到请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppSignDTO {

    /** 项目编号（扫码获取） */
    private String projectCode;

    /** 纬度 */
    private Double latitude;

    /** 经度 */
    private Double longitude;
}
