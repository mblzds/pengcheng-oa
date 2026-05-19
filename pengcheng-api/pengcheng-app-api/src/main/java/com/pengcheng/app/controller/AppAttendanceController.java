package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.app.dto.AppClockDTO;
import com.pengcheng.app.dto.AppQuickSignDTO;
import com.pengcheng.app.dto.AppSignDTO;
import com.pengcheng.app.dto.SignResultVO;
import com.pengcheng.common.result.Result;
import com.pengcheng.file.entity.SysFile;
import com.pengcheng.file.service.SysFileService;
import com.pengcheng.hr.attendance.dto.AttendanceMonthlyVO;
import com.pengcheng.hr.attendance.dto.ClockInDTO;
import com.pengcheng.hr.attendance.dto.SignInDTO;
import com.pengcheng.hr.attendance.entity.AttendanceRecord;
import com.pengcheng.hr.attendance.mapper.AttendanceRecordMapper;
import com.pengcheng.hr.attendance.service.AttendanceService;
import com.pengcheng.hr.attendance.service.BaiduGeocodeService;
import com.pengcheng.realty.project.entity.Project;
import com.pengcheng.realty.project.mapper.ProjectMapper;
import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

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
    private final SysFileService fileService;
    private final SystemConfigHelper systemConfigHelper;
    private final BaiduGeocodeService baiduGeocodeService;

    /**
     * 上下班时间配置（请假/调休等场景前端预校验用）
     * 返回 { workStartTime: "09:00", workEndTime: "18:00" }
     */
    @GetMapping("/work-hours")
    public Result<Map<String, String>> workHours() {
        return Result.ok(Map.of(
                "workStartTime", systemConfigHelper.getAttendanceWorkStartTime(),
                "workEndTime", systemConfigHelper.getAttendanceWorkEndTime()
        ));
    }

    /**
     * GPS打卡（上班/下班）
     * 请求体含 type("in"/"out")、latitude、longitude、clockTime
     * 内部构造 ClockInDTO，设置 location = "lat,lng" 格式
     */
    @PostMapping("/clock")
    public Result<AttendanceRecord> clock(@RequestBody AppClockDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime clockTime = dto.getClockTime() != null ? dto.getClockTime() : LocalDateTime.now();

        String location = (dto.getLatitude() != null && dto.getLongitude() != null)
                ? dto.getLatitude() + "," + dto.getLongitude()
                : null;

        ClockInDTO clockInDTO = ClockInDTO.builder()
                .userId(userId)
                .clockTime(clockTime)
                .location(location)
                .photoUrl(dto.getPhotoUrl())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();

        Long recordId = "out".equals(dto.getType())
                ? attendanceService.clockOut(clockInDTO)
                : attendanceService.clockIn(clockInDTO);
        return Result.ok(attendanceRecordMapper.selectById(recordId));
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
     * 拍照签到（参考钉钉签到）
     * 区别于 /sign 的扫码签到——本接口必须拍照，不需要项目码，签到不限频率。
     * 前端流程：
     *   1) 拍照 → 调 /upload-photo 拿 photoUrl
     *   2) 调 uni.getLocation 拿 GCJ-02 → 转 WGS-84，上报本接口
     *   3) 后端用「系统配置 → 考勤设置」里维护的百度 AK 做逆地理，写入 sign_in_record.address
     * photoUrl 为空时直接 400，不允许「无照片签到」。
     */
    @PostMapping("/quick-sign")
    public Result<SignResultVO> quickSign(@RequestBody AppQuickSignDTO dto) {
        if (dto.getPhotoUrl() == null || dto.getPhotoUrl().isBlank()) {
            return Result.fail(400, "签到必须拍照");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        String location = (dto.getLatitude() != null && dto.getLongitude() != null)
                ? dto.getLatitude() + "," + dto.getLongitude()
                : null;
        // 后端调百度逆地理；AK 未配置 / 服务未开 / 网络异常时返回 null，地点列回退显示经纬度
        String address = baiduGeocodeService.reverseGeocode(dto.getLatitude(), dto.getLongitude());

        SignInDTO signInDTO = SignInDTO.builder()
                .userId(userId)
                .signInTime(now)
                .location(location)
                .address(address)
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .photoUrl(dto.getPhotoUrl())
                .remark(dto.getRemark())
                .build();
        attendanceService.signIn(signInDTO);

        SignResultVO resultVO = SignResultVO.builder()
                .projectName("签到")
                .signTime(now)
                .locationDesc(address != null ? address : (location != null ? location : ""))
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

        // 走 service 的 WithAbsent 版本：整天没打卡的工作日会补一条"缺勤"占位，
        // 小程序日历视图就能把那些格子渲染成红色"缺勤"，跟月度统计的 absentDays 对得上
        List<AttendanceRecord> records = attendanceService.listAttendanceRecordsWithAbsent(
                userId, null, start, end);
        // 单人查询，按日期升序方便日历从月初渲染到月末
        records.sort((a, b) -> {
            LocalDate da = a.getAttendanceDate();
            LocalDate db = b.getAttendanceDate();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return da.compareTo(db);
        });
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

    /**
     * 上传打卡照片
     * 返回照片的访问 URL，前端拿到 URL 后再调用 /clock 接口提交打卡
     */
    @PostMapping("/upload-photo")
    public Result<String> uploadPhoto(@RequestParam("file") MultipartFile file) {
        SysFile sysFile = fileService.uploadImage(file);
        return Result.ok(sysFile.getUrl());
    }
}
