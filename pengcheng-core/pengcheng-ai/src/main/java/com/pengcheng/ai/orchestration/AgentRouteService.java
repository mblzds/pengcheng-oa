package com.pengcheng.ai.orchestration;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 智能体路由服务（规则路由）
 * <p>
 * 按关键词映射到意图枚举；与 ChatClient 路由二选一或 A/B，由 AgentRouterService 统一调度。
 */
@Service
public class AgentRouteService {

    public AgentIntent route(String message) {
        if (!StringUtils.hasText(message)) {
            return AgentIntent.GENERAL;
        }

        String text = message.toLowerCase();

        if (containsAny(text, "文案", "朋友圈", "营销", "推广", "短视频", "sms")) {
            return AgentIntent.COPYWRITING;
        }
        if (containsAny(text, "知识库", "文档", "政策", "制度", "合同", "手册", "流程")) {
            return AgentIntent.KNOWLEDGE;
        }
        if (containsAny(text, "审批", "待审批", "待办", "驳回", "通过", "付款申请", "付款审批")) {
            return AgentIntent.APPROVAL;
        }
        if (containsAny(text, "客户", "报备", "手机号", "公海", "私海", "成交概率", "判客")) {
            return AgentIntent.CUSTOMER;
        }
        if (containsAny(text, "报表", "统计", "排行", "业绩", "漏斗", "转化", "成交", "报备", "到访", "本月", "本周", "今日")) {
            return AgentIntent.REPORT;
        }
        return AgentIntent.GENERAL;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
