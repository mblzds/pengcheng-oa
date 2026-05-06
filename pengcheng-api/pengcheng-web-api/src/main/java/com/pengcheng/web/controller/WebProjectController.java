package com.pengcheng.web.controller;

import com.pengcheng.common.result.Result;
import com.pengcheng.realty.alliance.service.WebAllianceService;
import com.pengcheng.realty.project.dto.ProjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 联盟商系统 - 项目接口
 * <p>
 * 仅返回在售项目，支持按片区筛选；项目详情包含佣金规则和驻场联系信息。
 */
@RestController
@RequestMapping("/web/project")
@RequiredArgsConstructor
public class WebProjectController {

    private final WebAllianceService webAllianceService;

    /**
     * 项目列表（仅在售项目，支持按片区筛选）
     */
    @GetMapping("/list")
    public Result<List<ProjectVO>> list(@RequestParam(required = false) String district) {
        return Result.ok(webAllianceService.listOnSaleProjects(district));
    }

    /**
     * 项目详情（含佣金比例、奖励政策、联系驻场人员信息）
     */
    @GetMapping("/{id}")
    public Result<ProjectVO> detail(@PathVariable Long id) {
        return Result.ok(webAllianceService.getProjectDetail(id));
    }
}
