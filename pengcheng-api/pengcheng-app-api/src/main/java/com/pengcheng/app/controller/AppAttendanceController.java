package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.app.dto.AppClockDTO;
import com.pengcheng.app.dto.AppSignDTO;
import com.pengcheng.app.dto.SignResultVO;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.attendance.dto.AttendanceMonthlyVO;
import com.pengcheng.hr.attendance.dto.ClockInDTO;
import com.pengcheng.hr.attendance.dto.SignInDTO;
import com.pengcheng.hr.attendance.entity.AttendanceRecord;
import com.pengcheng.hr.attendance.mapper.AttendanceRecordMapper;
import com.pengcheng.hr.attendance.service.AttendanceService;
import com.pengcheng.realty.project.entity.Project;
import com.pengcheng.realty.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * App端考勤控制器
 * 提供GPS打卡、扫码签到、考勤记录查询、月度汇总接口
 */
@RestController
@RequestMapping("/app/attendance")
@RequiredArgsConstructor
@SaCheckLogin
public class AppAttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final ProjectMapper projectMapper;

    /**
     * GPS打卡（上班/下班）
     * 请求体含 type("in"/"out")、latitude、longitude、clockTime
     * 内部构造 ClockInDTO，设置 location = "lat,lng" 格式
     */
    @PostMapping("/clock")
    public Result<Void> clock(@RequestBody AppClockDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime clockTime = dto.getClockTime() != null ? dto.getClockTime() : LocalDateTime.now();

        String location = (dto.getLatitude() != null && dto.getLongitude() != null)
                ? dto.getLatitude() + "," + dto.getLongitude()
                : null;

        ClockInDTO clockInDTO = ClockInDTO.builder()
                .userId(userId)
                .clockTime(clockTime)
                .location(location)
                .build();

        if ("out".equals(dto.getType())) {
            attendanceService.clockOut(clockInDTO);
        } else {
            attendanceService.clockIn(clockInDTO);
        }
        return Result.ok();
    }

    /**
     * 扫码签到
     * 请求体含 projectCode、latitude、longitude
     * 通过 projectCode 查询项目，构造 SignInDTO 提交签到
     */
    @PostMapping("/sign")
    public Result<SignResultVO> sign(@RequestBody AppSignDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 通过 projectCode 查找项目（projectCode 为项目ID的字符串形式）
        Long projectId;
        try {
            projectId = Long.parseLong(dto.getProjectCode());
        } catch (NumberFormatException e) {
            return Result.fail(400, "无效的签到二维码");
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return Result.fail(400, "无效的签到二维码");
        }

        String location = (dto.getLatitude() != null && dto.getLongitude() != null)
                ? dto.getLatitude() + "," + dto.getLongitude()
                : null;

        LocalDateTime now = LocalDateTime.now();
        SignInDTO signInDTO = SignInDTO.builder()
                .userId(userId)
                .signInTime(now)
                .location(location)
                .remark("项目签到: " + project.getProjectName())
                .build();

        attendanceService.signIn(signInDTO);

        SignResultVO resultVO = SignResultVO.builder()
                .projectName(project.getProjectName())
                .signTime(now)
                .locationDesc(location != null ? "经纬度: " + location : "未获取位置")
                .build();
        return Result.ok(resultVO);
    }

    /**
     * 考勤记录查询
     * 查询参数：year, month
     * 返回指定月份的每日考勤记录列表
     */
    @GetMapping("/records")
    public Result<List<AttendanceRecord>> getRecords(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        Long userId = StpUtil.getLoginIdAsLong();

        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceRecord::getUserId, userId)
                .ge(AttendanceRecord::getAttendanceDate, start)
                .le(AttendanceRecord::getAttendanceDate, end)
                .orderByAsc(AttendanceRecord::getAttendanceDate);
        List<AttendanceRecord> records = attendanceRecordMapper.selectList(wrapper);

        return Result.ok(records);
    }

    /**
     * 月度考勤汇总
     * 查询参数：year, month
     * 返回出勤天数、迟到次数、早退次数、请假天数等汇总数据
     */
    @GetMapping("/monthly")
    public Result<AttendanceMonthlyVO> getMonthlySummary(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        Long userId = StpUtil.getLoginIdAsLong();
        AttendanceMonthlyVO summary = attendanceService.getMonthlySummary(userId, year, month);
        return Result.ok(summary);
    }
}
