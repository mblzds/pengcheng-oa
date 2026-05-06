package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.alliance.dto.AllianceCreateDTO;
import com.pengcheng.realty.alliance.dto.AllianceQueryDTO;
import com.pengcheng.realty.alliance.dto.AllianceStatsVO;
import com.pengcheng.realty.alliance.dto.AllianceVO;
import com.pengcheng.realty.alliance.service.AllianceService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 联盟商管理控制器
 */
@RestController
@RequestMapping("/admin/alliance")
@RequiredArgsConstructor
public class AllianceController {

    private final AllianceService allianceService;

    /**
     * 分页查询联盟商列表
     */
    @GetMapping("/page")
    @SaCheckPermission("realty:alliance:list")
    public Result<PageResult<AllianceVO>> page(AllianceQueryDTO query) {
        return Result.ok(allianceService.pageAlliances(query));
    }

    /**
     * 获取联盟商详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("realty:alliance:list")
    public Result<AllianceVO> detail(@PathVariable Long id) {
        return Result.ok(allianceService.getAlliance(id));
    }

    /**
     * 创建联盟商
     */
    @PostMapping("/create")
    @SaCheckPermission("realty:alliance:add")
    @Log(title = "联盟商管理", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody AllianceCreateDTO dto) {
        return Result.ok(allianceService.createAlliance(dto));
    }

    /**
     * 编辑联盟商
     */
    @PutMapping("/update")
    @SaCheckPermission("realty:alliance:edit")
    @Log(title = "联盟商管理", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody AllianceCreateDTO dto) {
        allianceService.updateAlliance(dto);
        return Result.ok();
    }

    /**
     * 启用联盟商
     */
    @PostMapping("/enable/{id}")
    @SaCheckPermission("realty:alliance:toggle")
    @Log(title = "联盟商管理", businessType = BusinessType.UPDATE)
    public Result<Void> enable(@PathVariable Long id) {
        allianceService.enableAlliance(id);
        return Result.ok();
    }

    /**
     * 停用联盟商
     */
    @PostMapping("/disable/{id}")
    @SaCheckPermission("realty:alliance:toggle")
    @Log(title = "联盟商管理", businessType = BusinessType.UPDATE)
    public Result<Void> disable(@PathVariable Long id) {
        allianceService.disableAlliance(id);
        return Result.ok();
    }

    /**
     * 查询联盟商运营数据统计
     */
    @GetMapping("/stats/{id}")
    public Result<AllianceStatsVO> stats(@PathVariable Long id) {
        return Result.ok(allianceService.getAllianceStats(id));
    }

    /**
     * 查询启用状态的联盟商列表（用于报备选择）
     */
    @GetMapping("/enabled")
    public Result<List<AllianceVO>> listEnabled() {
        return Result.ok(allianceService.listEnabled());
    }
}
