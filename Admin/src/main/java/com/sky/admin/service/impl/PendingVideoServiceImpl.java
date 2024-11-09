package com.sky.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.common.utils.Result;
import com.sky.pojo.dto.AuditVideoParam;
import com.sky.pojo.entity.Admin;
import com.sky.pojo.entity.PendingVideo;
import com.sky.pojo.mapper.AdminMapper;
import com.sky.pojo.mapper.PendingVideoMapper;
import com.sky.admin.service.PendingVideoService;
import com.sky.pojo.vo.UserPendingVideo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PendingVideoServiceImpl implements PendingVideoService {

    @Resource
    private PendingVideoMapper pendingVideoMapper;
    @Resource
    AdminMapper adminMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result auditVideo(AuditVideoParam auditVideoParam, Long adminId) {
        PendingVideo pendingVideo = pendingVideoMapper.selectById(auditVideoParam.getVideoId());
        if (pendingVideo == null) {
            return Result.failed("视频不存在");
        }
        Admin admin = adminMapper.selectById(adminId);
        pendingVideo.setAuditStatus(auditVideoParam.getAuditStatus());
        pendingVideo.setAuditComment(auditVideoParam.getAuditComment());
        pendingVideo.setAuditTime(LocalDateTime.now());
        pendingVideo.setAuditUserId(adminId);
        pendingVideo.setAuditUsername(admin.getUsername());
        pendingVideo.setIsAudited(true);
        pendingVideoMapper.updateById(pendingVideo);
        return Result.success("审核成功");
    }

    @Override
    public Result getPendingVideos(Integer page, Integer pageSize) {
        LambdaQueryWrapper<PendingVideo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PendingVideo::getIsAudited, false);
        queryWrapper.orderByDesc(PendingVideo::getUploadTime);
        Page<PendingVideo> pendingVideoPage = pendingVideoMapper.selectPage(new Page<>(page, pageSize), queryWrapper);
        List<PendingVideo> pendingVideoList = pendingVideoPage.getRecords();
        List<UserPendingVideo> userPendingVideoList = pendingVideoList.stream().map(pendingVideo -> {
            UserPendingVideo userPendingVideo = new UserPendingVideo();
            userPendingVideo.setTitle(pendingVideo.getTitle());
            userPendingVideo.setCover(pendingVideo.getCover());
            userPendingVideo.setDescription(pendingVideo.getDescription());
            userPendingVideo.setUrl(pendingVideo.getUrl());
            userPendingVideo.setUserId(pendingVideo.getUserId());
            userPendingVideo.setUploadTime(String.valueOf(pendingVideo.getUploadTime()));
            return userPendingVideo;
        }).toList();
        return Result.success(userPendingVideoList);
    }
}