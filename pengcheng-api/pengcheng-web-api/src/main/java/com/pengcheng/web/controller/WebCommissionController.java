package com.pengcheng.web.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.alliance.service.WebAllianceService;
import com.pengcheng.realty.commission.dto.CommissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 联盟商系统 - 佣金接口
 * <p>
 * 展示当前登录联盟商所有成交的佣金金额和结佣状态。
 */
@RestController
@RequestMapping("/web/commission")
@RequiredArgsConstructor
public class WebCommissionController {

    private final WebAllianceService webAllianceService;
    private final AllianceMapper allianceMapper;

    /**
     * 佣金分页列表（当前联盟商的佣金）
     */
    @GetMapping("/page")
    public Result<PageResult<CommissionVO>> page(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        Long allianceId = resolveAllianceId();
        return Result.ok(webAllianceService.pageCommissionsByAlliance(allianceId, page, pageSize));
    }

    private Long resolveAllianceId() {
        Long userId = StpUtil.getLoginIdAsLong();
        Alliance alliance = allianceMapper.selectByUserId(userId);
        if (alliance == null) {
            throw new IllegalStateException("当前用户未关联联盟商");
        }
        return alliance.getId();
    }
}
