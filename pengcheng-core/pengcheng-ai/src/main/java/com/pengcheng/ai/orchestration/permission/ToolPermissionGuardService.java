package com.pengcheng.ai.orchestration.permission;

import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AgentScene;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 工具权限守卫
 * <p>
 * 提供角色域、数据域、项目域的基础校验。
 */
@Service
public class ToolPermissionGuardService {

    private static final List<String> FULL_SCOPE_ROLES = List.of(
            "admin", "admin_director", "admin_clerk", "finance",
            "field_director", "channel_director", "resident_director"
    );

    private static final List<String> PROJECT_SCOPE_ROLES = List.of(
            "field_agent", "resident"
    );

    private static final List<String> ALLIANCE_SCOPE_ROLES = List.of(
            "channel_agent", "channel", "alliance_manager"
    );

    public ToolPermissionDecision authorize(AgentScene scene,
                                            AgentIntent intent,
                                            Long projectIdHint,
                                            List<String> roleCodes) {
        List<String> roles = roleCodes != null ? roleCodes : List.of();

        // Admin 场景默认拥有管理权限（已由 Sa-Token 登录校验）
        if (scene == AgentScene.ADMIN) {
            return new ToolPermissionDecision(true, "", "ALL", projectScope(projectIdHint));
        }

        // App 场景进行更细粒度约束
        if (intent == AgentIntent.APPROVAL) {
            return new ToolPermissionDecision(false, "审批查询仅支持后台管理端使用", "NONE", "NONE");
        }

        if (intent == AgentIntent.KNOWLEDGE && projectIdHint == null) {
            return new ToolPermissionDecision(false, "知识库查询需要指定项目ID", "NONE", "NONE");
        }

        String dataScope = resolveDataScope(roles);
        String projectScope = projectScope(projectIdHint);
        return new ToolPermissionDecision(true, "", dataScope, projectScope);
    }

    private String resolveDataScope(List<String> roleCodes) {
        if (CollectionUtils.isEmpty(roleCodes)) {
            return "SELF";
        }
        if (roleCodes.stream().anyMatch(FULL_SCOPE_ROLES::contains)) {
            return "ALL";
        }
        if (roleCodes.stream().anyMatch(PROJECT_SCOPE_ROLES::contains)) {
            return "PROJECT_OWNER";
        }
        if (roleCodes.stream().anyMatch(ALLIANCE_SCOPE_ROLES::contains)) {
            return "ALLIANCE_OWNER";
        }
        return "SELF";
    }

    private String projectScope(Long projectIdHint) {
        return projectIdHint != null ? "PROJECT_" + projectIdHint : "GLOBAL";
    }
}
