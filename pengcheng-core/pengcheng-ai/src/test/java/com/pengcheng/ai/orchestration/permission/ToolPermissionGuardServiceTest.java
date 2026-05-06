package com.pengcheng.ai.orchestration.permission;

import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AgentScene;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ToolPermissionGuardServiceTest {

    private final ToolPermissionGuardService service = new ToolPermissionGuardService();

    @Test
    void adminSceneShouldAllowApproval() {
        ToolPermissionDecision decision = service.authorize(
                AgentScene.ADMIN, AgentIntent.APPROVAL, null, List.of("admin")
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.dataScope()).isEqualTo("ALL");
    }

    @Test
    void appSceneShouldDenyApproval() {
        ToolPermissionDecision decision = service.authorize(
                AgentScene.APP, AgentIntent.APPROVAL, null, List.of("resident")
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("后台管理端");
    }

    @Test
    void appSceneKnowledgeShouldRequireProject() {
        ToolPermissionDecision decision = service.authorize(
                AgentScene.APP, AgentIntent.KNOWLEDGE, null, List.of("resident")
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("项目ID");
    }

    @Test
    void appSceneKnowledgeWithProjectShouldPass() {
        ToolPermissionDecision decision = service.authorize(
                AgentScene.APP, AgentIntent.KNOWLEDGE, 1001L, List.of("resident")
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.dataScope()).isEqualTo("PROJECT_OWNER");
        assertThat(decision.projectScope()).isEqualTo("PROJECT_1001");
    }

    @Test
    void allianceRoleShouldMapToAllianceScope() {
        ToolPermissionDecision decision = service.authorize(
                AgentScene.APP, AgentIntent.CUSTOMER, null, List.of("alliance_manager")
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.dataScope()).isEqualTo("ALLIANCE_OWNER");
    }
}
