package com.pengcheng.hr.attendance.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.approval.constant.ApprovalConstants;
import com.pengcheng.hr.approval.event.ApprovalFinalizedEvent;
import com.pengcheng.hr.attendance.entity.AttendanceRecord;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.LeaveRequest;
import com.pengcheng.hr.attendance.mapper.AttendanceRecordMapper;
import com.pengcheng.hr.attendance.mapper.CompensateRequestMapper;
import com.pengcheng.hr.attendance.mapper.LeaveRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 审批流终态监听：请假/调休通过后写入考勤豁免
 *
 * 行为：
 * - 已存在当日 attendance_record 且 exempt_reason 为空 → 仅补写 exempt_reason，不动 clock 字段
 * - 已存在且 exempt_reason 非空 → 跳过（避免覆盖已有豁免，例如同一天有两份请假）
 * - 不存在 → 新增一行（仅 user_id / attendance_date / exempt_reason）
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AttendanceExemptListener {

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final CompensateRequestMapper compensateRequestMapper;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalFinalized(ApprovalFinalizedEvent event) {
        if (!event.isApproved()) {
            return;
        }
        switch (event.getBusinessType()) {
            case ApprovalConstants.BUSINESS_TYPE_LEAVE -> handleLeave(event.getBusinessId());
            case ApprovalConstants.BUSINESS_TYPE_COMPENSATE -> handleCompensate(event.getBusinessId());
            default -> { /* 其他业务类型忽略 */ }
        }
    }

    private void handleLeave(Long leaveId) {
        LeaveRequest lr = leaveRequestMapper.selectById(leaveId);
        if (lr == null || lr.getStartTime() == null || lr.getEndTime() == null) {
            log.warn("AttendanceExemptListener: 请假记录缺失或时间字段为空, id={}", leaveId);
            return;
        }
        String reason = "leave-" + leaveTypeLabel(lr.getLeaveType());
        LocalDate start = lr.getStartTime().toLocalDate();
        LocalDate end = lr.getEndTime().toLocalDate();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            writeExempt(lr.getUserId(), d, reason);
        }
    }

    private void handleCompensate(Long id) {
        CompensateRequest cr = compensateRequestMapper.selectById(id);
        if (cr == null || cr.getCompensateDate() == null) {
            log.warn("AttendanceExemptListener: 调休记录缺失或日期为空, id={}", id);
            return;
        }
        writeExempt(cr.getUserId(), cr.getCompensateDate(), "compensate");
    }

    private void writeExempt(Long userId, LocalDate date, String reason) {
        AttendanceRecord existing = attendanceRecordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecord>()
                        .eq(AttendanceRecord::getUserId, userId)
                        .eq(AttendanceRecord::getAttendanceDate, date)
                        .last("LIMIT 1"));
        if (existing == null) {
            AttendanceRecord r = AttendanceRecord.builder()
                    .userId(userId)
                    .attendanceDate(date)
                    .exemptReason(reason)
                    .build();
            attendanceRecordMapper.insert(r);
        } else if (existing.getExemptReason() == null || existing.getExemptReason().isBlank()) {
            existing.setExemptReason(reason);
            attendanceRecordMapper.updateById(existing);
        }
        // 已有 exempt_reason 的不覆盖
    }

    private String leaveTypeLabel(Integer t) {
        if (t == null) {
            return "其他";
        }
        return switch (t) {
            case 1 -> "事假";
            case 2 -> "病假";
            case 3 -> "年假";
            case 4 -> "婚假";
            case 5 -> "产假";
            case 6 -> "调休";
            default -> "其他";
        };
    }
}
