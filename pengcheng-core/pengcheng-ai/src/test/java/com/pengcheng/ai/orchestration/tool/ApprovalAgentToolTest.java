package com.pengcheng.ai.orchestration.tool;

import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AgentScene;
import com.pengcheng.ai.orchestration.AiToolContext;
import com.pengcheng.ai.orchestration.OrchestratedChatResult;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import com.pengcheng.realty.payment.mapper.PaymentRequestMapper;
import com.pengcheng.realty.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalAgentToolTest {

    @Mock
    private PaymentRequestMapper paymentRequestMapper;

    @Test
    void shouldMarkHighRiskItemsWhenHitlEnabled() {
        AiProperties properties = new AiProperties();
        properties.setApprovalHitlEnabled(true);
        properties.setApprovalHitlAmountThreshold(new BigDecimal("50000"));
        ApprovalAgentTool tool = new ApprovalAgentTool(paymentRequestMapper, properties);

        PaymentRequest highRisk = pendingRequest(1L, new BigDecimal("80000"));
        PaymentRequest normal = pendingRequest(2L, new BigDecimal("3000"));

        when(paymentRequestMapper.selectCount(any())).thenReturn(2L, 1L, 10L, 0L);
        when(paymentRequestMapper.selectList(any())).thenReturn(List.of(highRisk, normal));

        OrchestratedChatResult result = tool.execute(buildContext());

        assertThat(result.content()).contains("高风险审批（需人工复核）");
        assertThat(result.structuredData()).isNotNull();
        assertThat(result.structuredData().get("requiresHumanReview")).isEqualTo(true);
        assertThat(result.structuredData().get("highRiskCount")).isEqualTo(1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> highRiskItems = (List<Map<String, Object>>) result.structuredData().get("highRiskItems");
        assertThat(highRiskItems).hasSize(1);
        assertThat(highRiskItems.get(0).get("id")).isEqualTo(1L);
        assertThat(highRiskItems.get(0).get("riskLevel")).isEqualTo("HIGH");
    }

    @Test
    void shouldNotRequireHumanReviewWhenHitlDisabled() {
        AiProperties properties = new AiProperties();
        properties.setApprovalHitlEnabled(false);
        properties.setApprovalHitlAmountThreshold(new BigDecimal("50000"));
        ApprovalAgentTool tool = new ApprovalAgentTool(paymentRequestMapper, properties);

        PaymentRequest highRisk = pendingRequest(3L, new BigDecimal("90000"));
        when(paymentRequestMapper.selectCount(any())).thenReturn(1L, 0L, 0L, 0L);
        when(paymentRequestMapper.selectList(any())).thenReturn(List.of(highRisk));

        OrchestratedChatResult result = tool.execute(buildContext());

        assertThat(result.content()).doesNotContain("高风险审批（需人工复核）");
        assertThat(result.structuredData()).isNotNull();
        assertThat(result.structuredData().get("requiresHumanReview")).isEqualTo(false);
        assertThat(result.structuredData().get("highRiskCount")).isEqualTo(0);
    }

    private PaymentRequest pendingRequest(Long id, BigDecimal amount) {
        PaymentRequest request = PaymentRequest.builder()
                .requestType(PaymentService.TYPE_EXPENSE)
                .amount(amount)
                .applicantId(1000L)
                .status(PaymentService.STATUS_PENDING)
                .build();
        request.setId(id);
        request.setCreateTime(LocalDateTime.now());
        return request;
    }

    private AiToolContext buildContext() {
        return new AiToolContext(
                "conv-1",
                "审批概览",
                null,
                AgentScene.ADMIN,
                AgentIntent.APPROVAL,
                null,
                1L,
                List.of("admin"),
                null,
                null,
                "not_applicable",
                "not_applicable"
        );
    }
}
