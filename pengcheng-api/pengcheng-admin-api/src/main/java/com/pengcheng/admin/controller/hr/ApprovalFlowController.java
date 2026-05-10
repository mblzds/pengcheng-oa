package com.pengcheng.admin.controller.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.approval.dto.ApprovalFlowNodeVO;
import com.pengcheng.hr.approval.dto.BusinessTypeOption;
import com.pengcheng.hr.approval.entity.ApprovalBusinessType;
import com.pengcheng.hr.approval.service.ApprovalBusinessTypeService;
import com.pengcheng.hr.approval.service.ApprovalFlowService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批流模板配置（请假 / 调休 / 报销 / 垫佣 / 预付佣 / 自定义类型）。
 * 业务类型不再硬编码白名单——前端通过 /business-types 拉可用 tab，提交则由
 * approvalFlowService.replaceTemplate 接受任意字符串 business_type。
 */
@RestController
@RequestMapping("/admin/approval-flow")
@RequiredArgsConstructor
public class ApprovalFlowController {

    private final ApprovalFlowService approvalFlowService;
    private final ApprovalBusinessTypeService businessTypeService;

    /**
     * 列举管理后台 tab 用的业务类型（数据源：approval_business_type 表）。
     * 含 builtin 标识与 nodeCount 已配置节点数。
     */
    @GetMapping("/business-types")
    @SaCheckPermission("system:approval-flow:list")
    public Result<List<BusinessTypeOption>> businessTypes() {
        return Result.ok(approvalFlowService.listBusinessTypes());
    }

    /**
     * 新建业务类型
     */
    @PostMapping("/business-types")
    @SaCheckPermission("system:approval-flow:edit")
    @Log(title = "新建审批业务类型", businessType = BusinessType.INSERT)
    public Result<Long> createBusinessType(@RequestBody ApprovalBusinessType bean) {
        return Result.ok(businessTypeService.create(bean));
    }

    /**
     * 编辑业务类型；内置类型禁改 key
     */
    @PutMapping("/business-types/{id}")
    @SaCheckPermission("system:approval-flow:edit")
    @Log(title = "编辑审批业务类型", businessType = BusinessType.UPDATE)
    public Result<Void> updateBusinessType(@PathVariable Long id, @RequestBody ApprovalBusinessType bean) {
        bean.setId(id);
        businessTypeService.update(bean);
        return Result.ok();
    }

    /**
     * 删除业务类型；内置类型禁删
     */
    @DeleteMapping("/business-types/{id}")
    @SaCheckPermission("system:approval-flow:edit")
    @Log(title = "删除审批业务类型", businessType = BusinessType.DELETE)
    public Result<Void> deleteBusinessType(@PathVariable Long id) {
        businessTypeService.delete(id);
        return Result.ok();
    }

    /**
     * 获取某 business_type 的节点配置（按 seq 升序）
     */
    @GetMapping("/{businessType}")
    @SaCheckPermission("system:approval-flow:list")
    public Result<List<ApprovalFlowNodeVO>> list(@PathVariable String businessType) {
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
        if (businessType == null || businessType.isBlank()) {
            return Result.fail(400, "业务类型不能为空");
        }
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
}
