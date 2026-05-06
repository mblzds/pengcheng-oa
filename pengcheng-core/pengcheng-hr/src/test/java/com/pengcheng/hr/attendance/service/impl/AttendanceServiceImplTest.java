package com.pengcheng.hr.attendance.service.impl;

import com.pengcheng.common.event.DataChangeEvent;
import com.pengcheng.hr.attendance.dto.AttendanceMonthlyVO;
import com.pengcheng.hr.attendance.dto.ClockInDTO;
import com.pengcheng.hr.attendance.dto.LeaveRequestDTO;
import com.pengcheng.hr.attendance.dto.SignInDTO;
import com.pengcheng.hr.attendance.entity.AttendanceRecord;
import com.pengcheng.hr.attendance.entity.CompensateRequest;
import com.pengcheng.hr.attendance.entity.LeaveRequest;
import com.pengcheng.hr.attendance.entity.SignInRecord;
import com.pengcheng.hr.attendance.mapper.AttendanceRecordMapper;
import com.pengcheng.hr.attendance.mapper.CompensateRequestMapper;
import com.pengcheng.hr.attendance.mapper.LeaveRequestMapper;
import com.pengcheng.hr.attendance.mapper.SignInRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AttendanceServiceImpl")
class AttendanceServiceImplTest {

    private AttendanceRecordMapper attendanceRecordMapper;
    private LeaveRequestMapper leaveRequestMapper;
    private SignInRecordMapper signInRecordMapper;
    private CompensateRequestMapper compensateRequestMapper;
    private ApplicationEventPublisher eventPublisher;
    private AttendanceServiceImpl service;

    @BeforeEach
    void setUp() {
        attendanceRecordMapper = mock(AttendanceRecordMapper.class);
        leaveRequestMapper = mock(LeaveRequestMapper.class);
        signInRecordMapper = mock(SignInRecordMapper.class);
        compensateRequestMapper = mock(CompensateRequestMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new AttendanceServiceImpl(
                attendanceRecordMapper,
                leaveRequestMapper,
                signInRecordMapper,
                compensateRequestMapper,
                eventPublisher
        );
    }

    @Test
    @DisplayName("clockIn / clockOut 可创建记录并计算迟到早退状态")
    void clockInAndOutCreateOrUpdateRecords() {
        when(attendanceRecordMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            AttendanceRecord record = invocation.getArgument(0);
            record.setId(6001L);
            return 1;
        }).when(attendanceRecordMapper).insert(any(AttendanceRecord.class));

        Long clockInId = service.clockIn(ClockInDTO.builder()
                .userId(88L)
                .clockTime(LocalDateTime.of(2026, 4, 22, 9, 30))
                .location("总部")
                .build());

        AttendanceRecord existing = AttendanceRecord.builder()
                .userId(88L)
                .attendanceDate(LocalDate.of(2026, 4, 22))
                .clockInTime(LocalDateTime.of(2026, 4, 22, 9, 30))
                .clockInStatus(AttendanceServiceImpl.CLOCK_IN_LATE)
                .build();
        existing.setId(6001L);
        when(attendanceRecordMapper.selectOne(any())).thenReturn(existing);

        Long clockOutId = service.clockOut(ClockInDTO.builder()
                .userId(88L)
                .clockTime(LocalDateTime.of(2026, 4, 22, 17, 30))
                .location("总部")
                .build());

        assertThat(clockInId).isEqualTo(6001L);
        assertThat(clockOutId).isEqualTo(6001L);
        assertThat(existing.getClockOutStatus()).isEqualTo(AttendanceServiceImpl.CLOCK_OUT_EARLY);
        verify(attendanceRecordMapper).updateById(existing);

        ArgumentCaptor<DataChangeEvent> eventCaptor = ArgumentCaptor.forClass(DataChangeEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeast(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).extracting(DataChangeEvent::getBizType).containsOnly("attendance");
    }

    @Test
    @DisplayName("submitLeaveRequest / submitCompensateRequest / signIn 创建申请或签到记录")
    void submitRequestsAndSignIn() {
        doAnswer(invocation -> {
            LeaveRequest request = invocation.getArgument(0);
            request.setId(7001L);
            return 1;
        }).when(leaveRequestMapper).insert(any(LeaveRequest.class));
        doAnswer(invocation -> {
            CompensateRequest request = invocation.getArgument(0);
            request.setId(7002L);
            return 1;
        }).when(compensateRequestMapper).insert(any(CompensateRequest.class));
        doAnswer(invocation -> {
            SignInRecord record = invocation.getArgument(0);
            record.setId(7003L);
            return 1;
        }).when(signInRecordMapper).insert(any(SignInRecord.class));

        Long leaveId = service.submitLeaveRequest(LeaveRequestDTO.builder()
                .userId(1L)
                .leaveType(2)
                .startTime(LocalDateTime.of(2026, 4, 23, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 24, 18, 0))
                .reason("家中有事")
                .build());
        Long compensateId = service.submitCompensateRequest(1L, LocalDate.of(2026, 4, 25), "周末加班");
        Long signInId = service.signIn(SignInDTO.builder()
                .userId(1L)
                .signInTime(LocalDateTime.of(2026, 4, 22, 9, 0))
                .location("一楼大堂")
                .remark("晨会签到")
                .build());

        assertThat(leaveId).isEqualTo(7001L);
        assertThat(compensateId).isEqualTo(7002L);
        assertThat(signInId).isEqualTo(7003L);
    }

    @Test
    @DisplayName("getMonthlySummary 统计出勤、迟到、早退与请假天数")
    void getMonthlySummaryAggregatesRecords() {
        AttendanceRecord late = AttendanceRecord.builder()
                .clockInTime(LocalDateTime.of(2026, 4, 1, 9, 30))
                .clockInStatus(AttendanceServiceImpl.CLOCK_IN_LATE)
                .clockOutTime(LocalDateTime.of(2026, 4, 1, 18, 30))
                .clockOutStatus(AttendanceServiceImpl.CLOCK_OUT_NORMAL)
                .build();
        AttendanceRecord early = AttendanceRecord.builder()
                .clockInTime(LocalDateTime.of(2026, 4, 2, 8, 50))
                .clockInStatus(AttendanceServiceImpl.CLOCK_IN_NORMAL)
                .clockOutTime(LocalDateTime.of(2026, 4, 2, 17, 20))
                .clockOutStatus(AttendanceServiceImpl.CLOCK_OUT_EARLY)
                .build();
        when(attendanceRecordMapper.selectList(any())).thenReturn(List.of(late, early));

        LeaveRequest approvedLeave = LeaveRequest.builder()
                .userId(1L)
                .status(2)
                .startTime(LocalDateTime.of(2026, 4, 10, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 10, 18, 0))
                .build();
        when(leaveRequestMapper.selectList(any())).thenReturn(List.of(approvedLeave));

        AttendanceMonthlyVO summary = service.getMonthlySummary(1L, 2026, 4);

        assertThat(summary.getAttendanceDays()).isEqualTo(2);
        assertThat(summary.getLateTimes()).isEqualTo(1);
        assertThat(summary.getEarlyLeaveTimes()).isEqualTo(1);
        assertThat(summary.getLeaveDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("determineClockStatus 与基础校验生效")
    void determineStatusAndValidationWork() {
        assertThat(service.determineClockInStatus(LocalTime.of(9, 1))).isEqualTo(AttendanceServiceImpl.CLOCK_IN_LATE);
        assertThat(service.determineClockOutStatus(LocalTime.of(17, 59))).isEqualTo(AttendanceServiceImpl.CLOCK_OUT_EARLY);

        assertThatThrownBy(() -> service.submitLeaveRequest(LeaveRequestDTO.builder()
                .userId(1L)
                .leaveType(1)
                .startTime(LocalDateTime.of(2026, 4, 24, 18, 0))
                .endTime(LocalDateTime.of(2026, 4, 24, 9, 0))
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("开始时间必须早于结束时间");
    }

    @Test
    @DisplayName("listAttendanceRecords / listLeaveRequests / listCompensateRequests 返回 Mapper 结果")
    void listMethodsReturnMapperResults() {
        AttendanceRecord attendance = AttendanceRecord.builder().userId(1L).build();
        LeaveRequest leave = LeaveRequest.builder().userId(1L).status(1).build();
        CompensateRequest compensate = CompensateRequest.builder().userId(1L).status(1).build();

        when(attendanceRecordMapper.selectList(any())).thenReturn(List.of(attendance));
        when(leaveRequestMapper.selectList(any())).thenReturn(List.of(leave));
        when(compensateRequestMapper.selectList(any())).thenReturn(List.of(compensate));

        assertThat(service.listAttendanceRecords(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .containsExactly(attendance);
        assertThat(service.listLeaveRequests(1L, 1)).containsExactly(leave);
        assertThat(service.listCompensateRequests(1L, 1)).containsExactly(compensate);
    }

    @Test
    @DisplayName("clockIn / signIn 缺失必填参数时拒绝执行")
    void clockInAndSignInValidateRequiredFields() {
        assertThatThrownBy(() -> service.clockIn(ClockInDTO.builder().userId(1L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("打卡时间不能为空");

        assertThatThrownBy(() -> service.signIn(SignInDTO.builder().signInTime(LocalDateTime.now()).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("员工ID不能为空");
    }
}
