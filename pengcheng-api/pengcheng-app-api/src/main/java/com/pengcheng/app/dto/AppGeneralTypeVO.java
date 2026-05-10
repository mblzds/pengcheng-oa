package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序「新增申请」FAB 菜单里可选的非内置业务类型条目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppGeneralTypeVO {
    private String businessType;
    private String label;
    private String description;
}
