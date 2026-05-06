package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.project.dto.ProjectCommissionRuleDTO;
import com.pengcheng.realty.project.dto.ProjectCreateDTO;
import com.pengcheng.realty.project.dto.ProjectQueryDTO;
import com.pengcheng.realty.project.dto.ProjectVO;
import com.pengcheng.realty.project.entity.ProjectCommissionRule;
import com.pengcheng.realty.project.service.ProjectService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目管理控制器
 */
@RestController
@RequestMapping("/admin/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 分页查询项目列表
     */
    @GetMapping("/page")
    @SaCheckPermission("realty:project:list")
    public Result<PageResult<ProjectVO>> page(ProjectQueryDTO query) {
        return Result.ok(projectService.pageProjects(query));
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("realty:project:list")
    public Result<ProjectVO> detail(@PathVariable Long id) {
        return Result.ok(projectService.getProject(id));
    }

    /**
     * 创建项目
     */
    @PostMapping("/create")
    @SaCheckPermission("realty:project:add")
    @Log(title = "项目管理", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody ProjectCreateDTO dto) {
        return Result.ok(projectService.createProject(dto));
    }

    /**
     * 编辑项目
     */
    @PutMapping("/update")
    @SaCheckPermission("realty:project:edit")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody ProjectCreateDTO dto) {
        projectService.updateProject(dto);
        return Result.ok();
    }

    /**
     * 录入/更新佣金规则
     */
    @PostMapping("/commission-rule")
    @SaCheckPermission("realty:project:rule")
    @Log(title = "佣金规则", businessType = BusinessType.INSERT)
    public Result<Long> saveCommissionRule(@RequestBody ProjectCommissionRuleDTO dto) {
        return Result.ok(projectService.saveCommissionRule(dto));
    }

    /**
     * 审批佣金规则
     */
    @PostMapping("/commission-rule/approve")
    @SaCheckPermission("realty:project:ruleApprove")
    @Log(title = "佣金规则审批", businessType = BusinessType.UPDATE)
    public Result<Void> approveCommissionRule(@RequestParam Long ruleId,
                                              @RequestParam boolean approved) {
        projectService.approveCommissionRule(ruleId, approved);
        return Result.ok();
    }

    /**
     * 获取项目当前生效的佣金规则
     */
    @GetMapping("/commission-rule/active/{projectId}")
    public Result<ProjectCommissionRule> activeCommissionRule(@PathVariable Long projectId) {
        return Result.ok(projectService.getActiveCommissionRule(projectId));
    }

    /**
     * 获取项目所有佣金规则版本
     */
    @GetMapping("/commission-rule/versions/{projectId}")
    public Result<List<ProjectCommissionRule>> commissionRuleVersions(@PathVariable Long projectId) {
        return Result.ok(projectService.getCommissionRuleVersions(projectId));
    }
}
