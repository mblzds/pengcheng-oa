package com.pengcheng.web.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.alliance.dto.AllianceDashboardVO;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.alliance.service.WebAllianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 联盟商系统 - 业务概览接口
 * <p>
 * 展示当前联盟商的本月报备数、到访数、成交数、待结佣金额。
 */
@RestController
@RequestMapping("/web/dashboard")
@RequiredArgsConstructor
public class WebDashboardController {

    private final WebAllianceService webAllianceService;
    private final AllianceMapper allianceMapper;

    /**
     * 业务概览
     */
    @GetMapping("/overview")
    public Result<AllianceDashboardVO> overview() {
        Long allianceId = resolveAllianceId();
        return Result.ok(webAllianceService.getDashboardOverview(allianceId));
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
