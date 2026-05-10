package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.app.dto.AppCompensateDTO;
import com.pengcheng.app.dto.AppLeaveDTO;
import com.pengcheng.app.dto.LeaveRecordVO;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.approval.constant.ApprovalConstants;
import com.pengcheng.hr.approval.entity.ApprovalRecordNode;
import com.pengcheng.hr.approval.mapper.ApprovalRecordNodeMapper;
import com.pengcheng.hr.approval.service.ApprovalFlowService;
import com.pengcheng.hr.attendance.dto.LeaveRequestDTO;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.LeaveRequest;
import com.pengcheng.hr.attendance.mapper.CompensateRequestMapper;
import com.pengcheng.hr.attendance.mapper.LeaveRequestMapper;
import com.pengcheng.hr.attendance.service.AttendanceService;
import com.pengcheng.hr.attendance.util.LeaveDaysUtil;
import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * App端请假调休控制器
 * 提供请假申请、调休申请、申请记录查询接口
 */
@RestController
@RequestMapping("/app/leave")
@RequiredArgsConstructor
@SaCheckLogin
public class AppLeaveController {

    private final AttendanceService attendanceService;
    private final LeaveRequestMapper leaveRequestMapper;
    private final CompensateRequestMapper compensateRequestMapper;
    private final ApprovalFlowService approvalFlowService;
    private final ApprovalRecordNodeMapper approvalRecordNodeMapper;
    private final SystemConfigHelper systemConfigHelper;

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 提交请假申请
     * 请求体含 leaveType/startTime/endTime/reason
     */
    @PostMapping("/apply")
    public Result<Long> apply(@RequestBody AppLeaveDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 服务端校验：起止时段必须落在配置的工作时段内（兜底前端校验）
        LocalTime workStart = parseHHmm(systemConfigHelper.getAttendanceWorkStartTime(), LocalTime.of(9, 0));
        LocalTime workEnd = parseHHmm(systemConfigHelper.getAttendanceWorkEndTime(), LocalTime.of(18, 0));
        if (dto.getStartTime() != null && !inWorkHours(dto.getStartTime().toLocalTime(), workStart, workEnd)) {
            return Result.fail(400, String.format("开始时间需在 %s~%s 之间",
                    workStart.format(HHMM), workEnd.format(HHMM)));
        }
        if (dto.getEndTime() != null && !inWorkHours(dto.getEndTime().toLocalTime(), workStart, workEnd)) {
            return Result.fail(400, String.format("结束时间需在 %s~%s 之间",
                    workStart.format(HHMM), workEnd.format(HHMM)));
        }

        LeaveRequestDTO requestDTO = LeaveRequestDTO.builder()
                .userId(userId)
                .leaveType(dto.getLeaveType())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .reason(dto.getReason())
                .build();

        Long requestId = attendanceService.submitLeaveRequest(requestDTO);
        return Result.ok(requestId);
    }

    private static boolean inWorkHours(LocalTime t, LocalTime start, LocalTime end) {
        return !t.isBefore(start) && !t.isAfter(end);
    }

    /**
     * 提交调休申请
     * 请求体含 compensateDate/reason
     */
    @PostMapping("/compensate")
    public Result<Long> compensate(@RequestBody AppCompensateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long requestId = attendanceService.submitCompensateRequest(
                userId, dto.getCompensateDate(), dto.getReason());
        return Result.ok(requestId);
    }

    /**
     * 申请人主动撤销请假/调休申请
     * type=leave 时 id 为 leave_request.id；type=compensate 时为 realty_compensate_request.id
     * 仅业务单整体仍处于「审批中」时允许撤销，并校验当前登录用户必须是申请人本人。
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam String type) {
        Long userId = StpUtil.getLoginIdAsLong();
        String businessType;
        if ("leave".equals(type)) {
            businessType = ApprovalConstants.BUSINESS_TYPE_LEAVE;
        } else if ("compensate".equals(type)) {
            businessType = ApprovalConstants.BUSINESS_TYPE_COMPENSATE;
        } else {
            return Result.fail(400, "无效的撤销类型: " + type);
        }
        approvalFlowService.cancel(businessType, id, userId);
        return Result.ok();
    }

    /**
     * 查询当前用户请假/调休记录
     * 支持 type(leave/compensate)/status/page/pageSize 参数
     */
    @GetMapping("/list")
    public Result<PageResult<LeaveRecordVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {

        Long userId = StpUtil.getLoginIdAsLong();
        List<LeaveRecordVO> allRecords = new ArrayList<>();

        // 查询请假记录（type 为空或 "leave" 时）
        if (type == null || "leave".equals(type)) {
            LambdaQueryWrapper<LeaveRequest> leaveWrapper = new LambdaQueryWrapper<>();
            leaveWrapper.eq(LeaveRequest::getUserId, userId);
            if (status != null) {
                leaveWrapper.eq(LeaveRequest::getStatus, status);
            }
            List<LeaveRequest> leaveRequests = leaveRequestMapper.selectList(leaveWrapper);
            LocalTime workStart = parseHHmm(systemConfigHelper.getAttendanceWorkStartTime(), LocalTime.of(9, 0));
            LocalTime workEnd = parseHHmm(systemConfigHelper.getAttendanceWorkEndTime(), LocalTime.of(18, 0));
            for (LeaveRequest lr : leaveRequests) {
                allRecords.add(LeaveRecordVO.builder()
                        .id(lr.getId())
                        .type("leave")
                        .leaveType(lr.getLeaveType())
                        .startTime(lr.getStartTime())
                        .endTime(lr.getEndTime())
                        .reason(lr.getReason())
                        .status(lr.getStatus())
                        .createTime(lr.getCreateTime())
                        .currentNodeName(resolveCurrentNodeName(ApprovalConstants.BUSINESS_TYPE_LEAVE, lr.getId(), lr.getStatus()))
                        .rejectReason(resolveRejectReason(ApprovalConstants.BUSINESS_TYPE_LEAVE, lr.getId(), lr.getStatus()))
                        .days(LeaveDaysUtil.calcDays(lr.getStartTime(), lr.getEndTime(), workStart, workEnd))
                        .build());
            }
        }

        // 查询调休记录（type 为空或 "compensate" 时）
        if (type == null || "compensate".equals(type)) {
            LambdaQueryWrapper<CompensateRequest> compWrapper = new LambdaQueryWrapper<>();
            compWrapper.eq(CompensateRequest::getUserId, userId);
            if (status != null) {
                compWrapper.eq(CompensateRequest::getStatus, status);
            }
            List<CompensateRequest> compensateRequests = compensateRequestMapper.selectList(compWrapper);
            for (CompensateRequest cr : compensateRequests) {
                allRecords.add(LeaveRecordVO.builder()
                        .id(cr.getId())
                        .type("compensate")
                        .leaveType(null)
                        .startTime(cr.getCompensateDate() != null ? cr.getCompensateDate().atStartOfDay() : null)
                        .endTime(null)
                        .reason(cr.getReason())
                        .status(cr.getStatus())
                        .createTime(cr.getCreateTime())
                        .currentNodeName(resolveCurrentNodeName(ApprovalConstants.BUSINESS_TYPE_COMPENSATE, cr.getId(), cr.getStatus()))
                        .rejectReason(resolveRejectReason(ApprovalConstants.BUSINESS_TYPE_COMPENSATE, cr.getId(), cr.getStatus()))
                        .days(1.0)
                        .build());
            }
        }

        // 按创建时间降序排序
        allRecords.sort(Comparator.comparing(LeaveRecordVO::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // 手动分页
        long total = allRecords.size();
        int fromIndex = (int) ((page - 1) * pageSize);
        int toIndex = (int) Math.min(fromIndex + pageSize, total);
        List<LeaveRecordVO> pageList = fromIndex < total
                ? allRecords.subList(fromIndex, toIndex)
                : new ArrayList<>();

        return Result.ok(PageResult.of(pageList, total, page, pageSize));
    }

    /**
     * 仅当业务单状态为「审批中」时返回当前节点名，便于列表页展示「等待 XX 审批」
     */
    private LocalTime parseHHmm(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return LocalTime.parse(value, HHMM);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String resolveCurrentNodeName(String businessType, Long businessId, Integer status) {
        if (status == null || status != ApprovalConstants.STATUS_PENDING) {
            return null;
        }
        ApprovalRecordNode current = approvalFlowService.getCurrentNode(businessType, businessId);
        return current == null ? null : current.getNodeName();
    }

    /**
     * 仅当业务单状态为「已驳回」时返回驳回原因（首个 result=驳回 节点的 remark）
     */
    private String resolveRejectReason(String businessType, Long businessId, Integer status) {
        if (status == null || status != ApprovalConstants.STATUS_REJECTED) {
            return null;
        }
        ApprovalRecordNode rejected = approvalRecordNodeMapper.selectOne(
                new LambdaQueryWrapper<ApprovalRecordNode>()
                        .eq(ApprovalRecordNode::getBusinessType, businessType)
                        .eq(ApprovalRecordNode::getBusinessId, businessId)
                        .eq(ApprovalRecordNode::getResult, ApprovalConstants.RESULT_REJECTED)
                        .orderByAsc(ApprovalRecordNode::getSeq)
                        .last("LIMIT 1"));
        return rejected == null ? null : rejected.getRemark();
    }
}
