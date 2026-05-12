package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.pengcheng.common.result.Result;
import com.pengcheng.file.entity.SysFile;
import com.pengcheng.file.service.SysFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 小程序端文件上传
 * <p>
 * 单独于 SysFileController(/sys/file/upload, 需 sys:file:upload 权限)，
 * 用于聊天发图、修改头像、报销发票等普通员工日常上传场景。仅校验登录，
 * 不依赖后台的"文件管理"权限节点。
 */
@RestController
@RequestMapping("/app/file")
@RequiredArgsConstructor
public class AppFileController {

    private final SysFileService fileService;

    @PostMapping("/upload")
    @SaCheckLogin
    public Result<SysFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String path) {
        return Result.ok(fileService.upload(file, path));
    }

    /**
     * 上传图片（带 images/ 子目录前缀，与后台 /sys/file/upload/image 行为一致）。
     * 用于聊天发图、富文本图等需要分组到图片目录的场景。
     */
    @PostMapping("/upload/image")
    @SaCheckLogin
    public Result<SysFile> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.uploadImage(file));
    }
}
