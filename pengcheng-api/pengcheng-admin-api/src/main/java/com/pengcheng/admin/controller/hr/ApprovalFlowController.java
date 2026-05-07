package com.pengcheng.admin.controller.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.approval.dto.ApprovalFlowNodeVO;
import com.pengcheng.hr.approval.service.ApprovalFlowService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 审批流模板配置（请假 / 调休）
 */
@RestController
@RequestMapping("/admin/approval-flow")
@RequiredArgsConstructor
public class ApprovalFlowController {

    private final ApprovalFlowService approvalFlowService;

    private static final Set<String> SUPPORTED_BUSINESS_TYPES = Set.of("leave", "compensate");

    /**
     * 获取某 business_type 的节点配置（按 seq 升序）
     */
    @GetMapping("/{businessType}")
    @SaCheckPermission("system:approval-flow:list")
    public Result<List<ApprovalFlowNodeVO>> list(@PathVariable String businessType) {
        validate(businessType);
        return Result.ok(approvalFlowService.listTemplate(businessType));
    }

    /**
     * 全量替换某 business_type 的节点配置
     */
    @PutMapping("/{businessType}")
    @SaCheckPermission("system:approval-flow:edit")
    @Log(title = "审批流配置", businessType = BusinessType.UPDATE)
    public Result<Void> save(@PathVariable String businessType,
                             @RequestBody List<ApprovalFlowNodeVO> nodes) {
        validate(businessType);
        if (nodes == null || nodes.isEmpty()) {
            return Result.fail(400, "至少需要配置一个审批节点");
        }
        for (ApprovalFlowNodeVO n : nodes) {
            if (n.getNodeName() == null || n.getNodeName().isBlank()) {
                return Result.fail(400, "节点名不能为空");
            }
            if (n.getApproverType() == null || n.getApproverType().isBlank()) {
                return Result.fail(400, "节点【" + n.getNodeName() + "】未选择审批人类型");
            }
            if (("role".equals(n.getApproverType()) || "user".equals(n.getApproverType()))
                    && (n.getApproverValue() == null || n.getApproverValue().isBlank())) {
                return Result.fail(400, "节点【" + n.getNodeName() + "】缺少审批人/角色配置");
            }
        }
        approvalFlowService.replaceTemplate(businessType, nodes);
        return Result.ok();
    }

    private void validate(String businessType) {
        if (!SUPPORTED_BUSINESS_TYPES.contains(businessType)) {
            throw new IllegalArgumentException("不支持的业务类型: " + businessType);
        }
    }
}
