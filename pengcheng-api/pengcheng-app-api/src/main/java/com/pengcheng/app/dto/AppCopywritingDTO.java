package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 营销文案生成请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppCopywritingDTO {

    /** 项目ID */
    private Long projectId;

    /** 文案类型 */
    private String type;
}
