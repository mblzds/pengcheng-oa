package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 营销文案生成响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCopywritingVO {

    /** 生成的文案内容 */
    private String content;
}
