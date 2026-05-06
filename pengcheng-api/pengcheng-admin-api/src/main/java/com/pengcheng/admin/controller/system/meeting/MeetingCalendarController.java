package com.pengcheng.admin.controller.system.meeting;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.meeting.dto.MeetingCalendarSaveRequest;
import com.pengcheng.system.meeting.dto.MeetingMinutesSaveRequest;
import com.pengcheng.system.meeting.dto.ReminderConfigDTO;
import com.pengcheng.system.meeting.service.MeetingCalendarService;
import com.pengcheng.system.meeting.vo.MeetingCalendarVO;
import com.pengcheng.system.meeting.vo.MeetingMinutesVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/meeting/calendar")
@RequiredArgsConstructor
public class MeetingCalendarController {

    private final MeetingCalendarService meetingCalendarService;

    @GetMapping("/month")
    public Result<List<MeetingCalendarVO>> getMonthMeetings(@RequestParam int year, @RequestParam int month) {
        return Result.ok(meetingCalendarService.getMonthMeetings(StpUtil.getLoginIdAsLong(), year, month));
    }

    @GetMapping("/day")
    public Result<List<MeetingCalendarVO>> getDayMeetings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(meetingCalendarService.getDayMeetings(StpUtil.getLoginIdAsLong(), date));
    }

    @GetMapping("/{id}")
    public Result<MeetingCalendarVO> getMeetingDetail(@PathVariable Long id) {
        return Result.ok(meetingCalendarService.getMeetingDetail(StpUtil.getLoginIdAsLong(), id));
    }

    @PostMapping
    public Result<MeetingCalendarVO> createMeeting(@RequestBody MeetingCalendarSaveRequest request) {
        return Result.ok(meetingCalendarService.createMeeting(StpUtil.getLoginIdAsLong(), request));
    }

    @PutMapping("/{id}")
    public Result<MeetingCalendarVO> updateMeeting(@PathVariable Long id, @RequestBody MeetingCalendarSaveRequest request) {
        return Result.ok(meetingCalendarService.updateMeeting(StpUtil.getLoginIdAsLong(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> cancelMeeting(@PathVariable Long id) {
        meetingCalendarService.cancelMeeting(StpUtil.getLoginIdAsLong(), id);
        return Result.ok();
    }

    @PutMapping("/{id}/minutes")
    public Result<MeetingMinutesVO> saveMinutes(@PathVariable Long id, @RequestBody MeetingMinutesSaveRequest request) {
        return Result.ok(meetingCalendarService.saveMinutes(StpUtil.getLoginIdAsLong(), id, request));
    }

    @GetMapping("/config/reminder")
    public Result<ReminderConfigDTO> getReminderConfig() {
        return Result.ok(meetingCalendarService.getReminderConfig());
    }

    @PostMapping("/config/reminder")
    public Result<Void> saveReminderConfig(@RequestBody ReminderConfigDTO request) {
        meetingCalendarService.saveReminderConfig(request);
        return Result.ok();
    }
}
