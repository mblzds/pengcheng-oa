package com.pengcheng.ai.orchestration.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.ai.config.AiProperties;
import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AiToolContext;
import com.pengcheng.ai.orchestration.OrchestratedChatResult;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import com.pengcheng.realty.payment.mapper.PaymentRequestMapper;
import com.pengcheng.realty.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批智能体工具
 */
@Service
@RequiredArgsConstructor
public class ApprovalAgentTool implements AiAgentTool {

    private final PaymentRequestMapper paymentRequestMapper;
    private final AiProperties aiProperties;

    @Override
    public AgentIntent supportedIntent() {
        return AgentIntent.APPROVAL;
    }

    @Override
    public String toolName() {
        return "approval-agent";
    }

    @Override
    public OrchestratedChatResult execute(AiToolContext context) {
        long pendingCount = countByStatus(PaymentService.STATUS_PENDING);
        long inProgressCount = countByStatus(PaymentService.STATUS_IN_PROGRESS);
        long approvedCount = countByStatus(PaymentService.STATUS_APPROVED);
        long rejectedCount = countByStatus(PaymentService.STATUS_REJECTED);

        List<PaymentRequest> recentPending = paymentRequestMapper.selectList(
                new LambdaQueryWrapper<PaymentRequest>()
                        .eq(PaymentRequest::getStatus, PaymentService.STATUS_PENDING)
                        .orderByDesc(PaymentRequest::getCreateTime)
                        .last("LIMIT 5")
        );

        StringBuilder sb = new StringBuilder();
        sb.append("审批概览：")
                .append("\n- 待审批：").append(pendingCount)
                .append("\n- 审批中：").append(inProgressCount)
                .append("\n- 已通过：").append(approvedCount)
                .append("\n- 已驳回：").append(rejectedCount);

        if (recentPending.isEmpty()) {
            sb.append("\n\n最近暂无待审批记录。");
        } else {
            sb.append("\n\n最近待审批（最多5条）：");
            for (PaymentRequest req : recentPending) {
                sb.append("\n- #").append(req.getId())
                        .append(" 类型=").append(typeText(req.getRequestType()))
                        .append(" 金额=").append(req.getAmount())
                        .append(" 申请人=").append(req.getApplicantId());
            }
        }

        boolean hitlEnabled = aiProperties.isApprovalHitlEnabled();
        BigDecimal threshold = safeThreshold(aiProperties.getApprovalHitlAmountThreshold());

        List<Map<String, Object>> items = recentPending.stream()
                .map(req -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    boolean highRisk = isHighRisk(req, hitlEnabled, threshold);
                    row.put("id", req.getId());
                    row.put("requestType", req.getRequestType());
                    row.put("requestTypeText", typeText(req.getRequestType()));
                    row.put("amount", req.getAmount());
                    row.put("applicantId", req.getApplicantId());
                    row.put("status", req.getStatus());
                    row.put("createTime", req.getCreateTime());
                    row.put("requiresHumanReview", highRisk);
                    row.put("riskLevel", highRisk ? "HIGH" : "NORMAL");
                    if (highRisk) {
                        row.put("riskReason", "金额大于等于阈值 " + threshold);
                    }
                    return row;
                })
                .toList();

        List<Map<String, Object>> highRiskItems = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("requiresHumanReview")))
                .toList();
        boolean requiresHumanReview = !highRiskItems.isEmpty();

        if (requiresHumanReview) {
            sb.append("\n\n高风险审批（需人工复核）：");
            for (Map<String, Object> item : highRiskItems) {
                sb.append("\n- #").append(item.get("id"))
                        .append(" 类型=").append(item.get("requestTypeText"))
                        .append(" 金额=").append(item.get("amount"))
                        .append(" 原因=").append(item.get("riskReason"));
            }
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("pendingCount", pendingCount);
        metrics.put("inProgressCount", inProgressCount);
        metrics.put("approvedCount", approvedCount);
        metrics.put("rejectedCount", rejectedCount);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "approval");
        payload.put("agent", toolName());
        payload.put("metrics", metrics);
        payload.put("items", items);
        payload.put("hitlEnabled", hitlEnabled);
        payload.put("hitlAmountThreshold", threshold);
        payload.put("requiresHumanReview", requiresHumanReview);
        payload.put("highRiskCount", highRiskItems.size());
        payload.put("highRiskItems", highRiskItems);

        return new OrchestratedChatResult(
                sb.toString(),
                "text",
                context.conversationId(),
                toolName(),
                payload
        );
    }

    private long countByStatus(Integer status) {
        return paymentRequestMapper.selectCount(
                new LambdaQueryWrapper<PaymentRequest>().eq(PaymentRequest::getStatus, status)
        );
    }

    private String typeText(Integer type) {
        if (type == null) {
            return "未知";
        }
        return switch (type) {
            case PaymentService.TYPE_EXPENSE -> "费用报销";
            case PaymentService.TYPE_ADVANCE_COMMISSION -> "垫佣";
            case PaymentService.TYPE_PREPAY_COMMISSION -> "预付佣";
            default -> "未知";
        };
    }

    private boolean isHighRisk(PaymentRequest request, boolean hitlEnabled, BigDecimal threshold) {
        if (!hitlEnabled || request == null || request.getAmount() == null) {
            return false;
        }
        return request.getAmount().compareTo(threshold) >= 0;
    }

    private BigDecimal safeThreshold(BigDecimal threshold) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("50000");
        }
        return threshold;
    }
}
