package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.app.dto.AppGeneralApprovalDTO;
import com.pengcheng.app.dto.AppGeneralTypeVO;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.approval.entity.ApprovalBusinessType;
import com.pengcheng.hr.approval.entity.GeneralApprovalRequest;
import com.pengcheng.hr.approval.service.ApprovalBusinessTypeService;
import com.pengcheng.hr.approval.service.GeneralApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * App 端通用审批入口（Phase 2 of 通用流水线）。
 * 给非内置 business_type 提供：可选类型列表、提交、列表、撤销。
 * 审批操作仍走 AppApprovalController.approve（其内部对 generic 类型分支调本服务）。
 */
@RestController
@RequestMapping("/app/general-approval")
@RequiredArgsConstructor
@SaCheckLogin
public class AppGeneralApprovalController {

    private final GeneralApprovalService generalApprovalService;
    private final ApprovalBusinessTypeService businessTypeService;

    /**
     * 列出可发起的非内置业务类型（小程序 FAB 菜单数据源）。
     * 内置类型有专属提交页，不在此返回。
     */
    @GetMapping("/types")
    public Result<List<AppGeneralTypeVO>> types() {
        List<ApprovalBusinessType> all = businessTypeService.listAll();
        List<AppGeneralTypeVO> result = all.stream()
                .filter(t -> t.getBuiltin() == null || t.getBuiltin() == 0)
                .map(t -> AppGeneralTypeVO.builder()
                        .businessType(t.getBusinessType())
                        .label(t.getLabel())
                        .description(t.getDescription())
                        .build())
                .toList();
        return Result.ok(result);
    }

    /**
     * 提交一条通用审批申请
     */
    @PostMapping
    public Result<Long> apply(@RequestBody AppGeneralApprovalDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long id = generalApprovalService.submit(
                dto.getBusinessType(), userId, dto.getTitle(), dto.getDescription());
        return Result.ok(id);
    }

    /**
     * 当前用户的通用审批申请列表
     */
    @GetMapping("/list")
    public Result<PageResult<GeneralApprovalRequest>> list(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<GeneralApprovalRequest> p =
                generalApprovalService.pageMine(userId, businessType, status, page, pageSize);
        return Result.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    /**
     * 申请人主动撤销
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        generalApprovalService.cancel(id, userId);
        return Result.ok();
    }
}
