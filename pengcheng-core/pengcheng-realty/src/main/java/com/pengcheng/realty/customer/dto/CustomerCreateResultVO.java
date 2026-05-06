package com.pengcheng.realty.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 客户报备创建结果 VO
 * <p>
 * 包含报备结果和 AI 智能判客分析信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreateResultVO {

    /** 客户ID */
    private Long customerId;

    /** 报备编号 */
    private String reportNo;

    /** AI 判客是否发现重复 */
    private boolean hasDuplicate;

    /** AI 判客分析消息 */
    private String analysisMessage;

    /** 已有客户信息列表 */
    private List<ExistingCustomerInfo> existingCustomers;

    /**
     * 已有客户信息摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExistingCustomerInfo {
        private Long id;
        private String customerName;
        private String phoneMasked;
        private String statusText;
        private String poolTypeText;
        private String reportNo;
    }
}
