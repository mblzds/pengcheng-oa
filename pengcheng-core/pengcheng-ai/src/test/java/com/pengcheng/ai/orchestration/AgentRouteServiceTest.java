package com.pengcheng.ai.orchestration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRouteServiceTest {

    private final AgentRouteService routeService = new AgentRouteService();

    @Test
    void shouldRouteToCopywriting() {
        assertThat(routeService.route("帮我写一条朋友圈营销文案"))
                .isEqualTo(AgentIntent.COPYWRITING);
    }

    @Test
    void shouldRouteToKnowledge() {
        assertThat(routeService.route("查询知识库里的合同流程"))
                .isEqualTo(AgentIntent.KNOWLEDGE);
    }

    @Test
    void shouldRouteToApproval() {
        assertThat(routeService.route("待审批列表还有多少条"))
                .isEqualTo(AgentIntent.APPROVAL);
    }

    @Test
    void shouldRouteToCustomerWhenCustomerAndReportKeywordsMixed() {
        assertThat(routeService.route("帮我查这个报备客户手机号13800138000"))
                .isEqualTo(AgentIntent.CUSTOMER);
    }

    @Test
    void shouldRouteToReport() {
        assertThat(routeService.route("本月成交转化报表"))
                .isEqualTo(AgentIntent.REPORT);
    }

    @Test
    void shouldRouteToGeneralForBlank() {
        assertThat(routeService.route(" "))
                .isEqualTo(AgentIntent.GENERAL);
    }
}
