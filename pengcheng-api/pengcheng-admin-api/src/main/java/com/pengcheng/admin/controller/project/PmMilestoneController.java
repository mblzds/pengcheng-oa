package com.pengcheng.admin.controller.project;

import com.pengcheng.common.result.Result;
import com.pengcheng.system.project.entity.PmMilestone;
import com.pengcheng.system.project.service.PmMilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目里程碑：CRUD、标记完成
 */
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class PmMilestoneController {

    private final PmMilestoneService milestoneService;

    @GetMapping("/{projectId}/milestones")
    public Result<List<PmMilestone>> list(@PathVariable Long projectId) {
        return Result.ok(milestoneService.listByProject(projectId));
    }

    @PostMapping("/{projectId}/milestones")
    public Result<Long> create(@PathVariable Long projectId, @RequestBody PmMilestone milestone) {
        milestone.setProjectId(projectId);
        return Result.ok(milestoneService.create(milestone));
    }

    @PutMapping("/milestone/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody PmMilestone milestone) {
        milestone.setId(id);
        milestoneService.update(milestone);
        return Result.ok();
    }

    @DeleteMapping("/milestone/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        milestoneService.delete(id);
        return Result.ok();
    }

    @PutMapping("/milestone/{id}/complete")
    public Result<Void> setComplete(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean complete) {
        milestoneService.setComplete(id, complete);
        return Result.ok();
    }
}
