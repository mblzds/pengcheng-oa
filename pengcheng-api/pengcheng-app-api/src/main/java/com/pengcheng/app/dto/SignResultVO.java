package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 扫码签到结果响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignResultVO {

    /** 项目名称 */
    private String projectName;

    /** 签到时间 */
    private LocalDateTime signTime;

    /** 签到位置文字描述 */
    private String locationDesc;
}
