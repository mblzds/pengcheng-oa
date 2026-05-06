package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * GPS打卡请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppClockDTO {

    /** 打卡类型："in"=上班打卡 "out"=下班打卡 */
    private String type;

    /** 纬度 */
    private Double latitude;

    /** 经度 */
    private Double longitude;

    /** 打卡时间 */
    private LocalDateTime clockTime;
}
