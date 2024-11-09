package com.sky.admin.controller;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.AuditVideoParam;
import com.sky.admin.service.PendingVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pending_video")
@Tag(name = "审核视频接口")
@Validated
public class PendingVideoController {

    @Resource
    private PendingVideoService pendingVideoService;

    @GetMapping("/get_pending_videos")
    @Operation(summary = "获取待审核视频列表")
    public Result getPendingVideos(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return pendingVideoService.getPendingVideos(page, pageSize);
    }

    @PostMapping("/audit_video")
    @Operation(summary = "审核视频")
    public Result auditVideo(@Valid @RequestBody AuditVideoParam auditVideoParam) {
        Long adminId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return pendingVideoService.auditVideo(auditVideoParam, adminId);
    }
}