package com.pengcheng.ai.function;

import com.pengcheng.realty.commission.dto.CommissionDetailDTO;
import com.pengcheng.realty.commission.service.CommissionCalculator;
import com.pengcheng.realty.project.entity.ProjectCommissionRule;
import com.pengcheng.realty.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 佣金计算 Function（供 AI Function Calling 使用）
 * <p>
 * AI 智能佣金计算时，根据项目佣金规则自动计算各项佣金。
 * 计算结果自动填充到佣金录入表单，行政文员可确认或修改后提交。
 * 佣金规则不完整或存在歧义时标记需人工确认的项并提示具体原因。
 */
@Slf4j
@Component("commissionCalcFunction")
@RequiredArgsConstructor
public class CommissionCalcFunction implements Function<CommissionCalcFunction.Request, CommissionCalcFunction.Response> {

    private final ProjectService projectService;
    private final CommissionCalculator commissionCalculator;

    @Override
    public Response apply(Request request) {
        log.debug("AI 佣金计算 Function 调用: projectId={}, dealAmount={}, dealCount={}",
                request.projectId(), request.dealAmount(), request.dealCount());
        try {
            ProjectCommissionRule rule = projectService.getActiveCommissionRule(request.projectId());
            if (rule == null) {
                return new Response(false, null, Collections.emptyList(),
                        "项目ID=" + request.projectId() + " 无生效的佣金规则，请手动录入佣金");
            }

            BigDecimal dealAmount = new BigDecimal(request.dealAmount());
            int dealCount = request.dealCount() != null ? request.dealCount() : 1;

            CommissionCalculator.CalcResult result = commissionCalculator.calculate(rule, dealAmount, dealCount);

            if (!result.isSuccess()) {
                return new Response(false, null, Collections.emptyList(), result.getMessage());
            }

            return new Response(
                    true,
                    result.getDetail(),
                    result.getManualConfirmItems() != null ? result.getManualConfirmItems() : Collections.emptyList(),
                    result.getMessage()
            );
        } catch (Exception e) {
            log.error("AI 佣金计算失败: {}", e.getMessage());
            return new Response(false, null, Collections.emptyList(),
                    "佣金计算失败: " + e.getMessage() + "，请手动录入佣金");
        }
    }

    /**
     * Function Calling 请求参数
     *
     * @param projectId  项目ID
     * @param dealAmount 成交金额（字符串，避免精度丢失）
     * @param dealCount  该项目累计成交套数（含本次），为空默认1
     */
    public record Request(Long projectId, String dealAmount, Integer dealCount) {}

    /**
     * Function Calling 响应
     *
     * @param success            是否计算成功
     * @param detail             佣金明细
     * @param manualConfirmItems 需人工确认的项
     * @param message            结果消息
     */
    public record Response(
            boolean success,
            CommissionDetailDTO detail,
            List<String> manualConfirmItems,
            String message
    ) {}
}
