package com.pengcheng.ai.agent;

import com.pengcheng.realty.customer.dto.CustomerCreateResultVO;
import com.pengcheng.realty.customer.service.CustomerDuplicateChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 智能判客适配器
 * <p>
 * 实现 pengcheng-realty 定义的 CustomerDuplicateChecker 接口，
 * 将调用委托给 CustomerAnalysisAgent。
 */
@Component
@RequiredArgsConstructor
public class CustomerDuplicateCheckerAdapter implements CustomerDuplicateChecker {

    private final CustomerAnalysisAgent customerAnalysisAgent;

    @Override
    public DuplicateCheckResult checkDuplicate(String phone) {
        CustomerAnalysisAgent.DuplicateCheckResult agentResult =
                customerAnalysisAgent.checkDuplicate(phone);

        List<CustomerCreateResultVO.ExistingCustomerInfo> existingCustomers =
                agentResult.existingCustomers().stream()
                        .map(c -> CustomerCreateResultVO.ExistingCustomerInfo.builder()
                                .id(c.id())
                                .customerName(c.customerName())
                                .phoneMasked(c.phoneMasked())
                                .statusText(c.statusText())
                                .poolTypeText(c.poolTypeText())
                                .reportNo(c.reportNo())
                                .build())
                        .toList();

        return new DuplicateCheckResult(
                agentResult.hasDuplicate(),
                existingCustomers,
                agentResult.analysisMessage()
        );
    }
}
