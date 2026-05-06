package com.pengcheng.realty.customer.service;

import com.pengcheng.realty.customer.dto.CustomerCreateResultVO;

import java.util.List;

/**
 * 客户判重检查接口
 * <p>
 * 由 AI 模块（pengcheng-ai）实现，在客户报备时自动比对公海池和私海池中已有客户。
 * 如果 AI 模块未加载，CustomerService 将跳过 AI 判客步骤。
 */
public interface CustomerDuplicateChecker {

    /**
     * 检查新报备客户是否与已有客户重复
     *
     * @param phone 新报备客户手机号
     * @return 判客结果
     */
    DuplicateCheckResult checkDuplicate(String phone);

    /**
     * 判客结果
     */
    record DuplicateCheckResult(
            boolean hasDuplicate,
            List<CustomerCreateResultVO.ExistingCustomerInfo> existingCustomers,
            String analysisMessage
    ) {}
}
