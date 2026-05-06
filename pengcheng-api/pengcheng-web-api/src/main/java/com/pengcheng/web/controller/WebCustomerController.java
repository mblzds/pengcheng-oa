package com.pengcheng.web.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.alliance.service.WebAllianceService;
import com.pengcheng.realty.customer.dto.CustomerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 联盟商系统 - 客户接口
 * <p>
 * 展示当前登录联盟商的所有客户报备/到访/成交状态。
 */
@RestController
@RequestMapping("/web/customer")
@RequiredArgsConstructor
public class WebCustomerController {

    private final WebAllianceService webAllianceService;
    private final AllianceMapper allianceMapper;

    /**
     * 客户分页列表（当前联盟商的客户）
     */
    @GetMapping("/page")
    public Result<PageResult<CustomerVO>> page(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        Long allianceId = resolveAllianceId();
        return Result.ok(webAllianceService.pageCustomersByAlliance(allianceId, page, pageSize));
    }

    /**
     * 根据当前登录用户获取关联的联盟商ID
     */
    private Long resolveAllianceId() {
        Long userId = StpUtil.getLoginIdAsLong();
        Alliance alliance = allianceMapper.selectByUserId(userId);
        if (alliance == null) {
            throw new IllegalStateException("当前用户未关联联盟商");
        }
        return alliance.getId();
    }
}
