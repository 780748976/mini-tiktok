package com.sky.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.sky.admin.config.ExecutorConfig;
import com.sky.common.utils.Result;
import com.sky.pojo.constant.InternalMessageReceiverType;
import com.sky.pojo.constant.PendingVideoAuditStatus;
import com.sky.pojo.constant.VideoStatus;
import com.sky.pojo.dto.AuditVideoParam;
import com.sky.pojo.entity.Admin;
import com.sky.pojo.entity.PendingVideo;
import com.sky.pojo.entity.Video;
import com.sky.pojo.mapper.AdminMapper;
import com.sky.pojo.mapper.PendingVideoMapper;
import com.sky.admin.service.PendingVideoService;
import com.sky.pojo.mapper.VideoMapper;
import com.sky.pojo.mapper.VideoTagMapper;
import com.sky.pojo.vo.UserPendingVideo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sky.web.service.InternalMessageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class PendingVideoServiceImpl implements PendingVideoService {

    @Resource
    private PendingVideoMapper pendingVideoMapper;
    @Resource
    AdminMapper adminMapper;
    @Resource
    KafkaTemplate<String, String> kafkaTemplate;
    @Resource
    VideoMapper videoMapper;
    @Resource
    Gson gson;
    @Resource
    InternalMessageService internalMessageService;
    @Resource
    VideoTagMapper videoTagMapper;
    @Autowired
    ThreadPoolTaskExecutor asyncServiceExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result auditVideo(AuditVideoParam auditVideoParam, Long adminId) throws ExecutionException, InterruptedException {
        if (!Objects.equals(auditVideoParam.getAuditStatus(), PendingVideoAuditStatus.PASS) &&
                !Objects.equals(auditVideoParam.getAuditStatus(), PendingVideoAuditStatus.REJECT)) {
            return Result.failed("审核状态错误");
        }
    
        LambdaQueryWrapper<PendingVideo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PendingVideo::getId, auditVideoParam.getVideoId());
        queryWrapper.eq(PendingVideo::getIsAudited, false);
    
        PendingVideo pendingVideo = pendingVideoMapper.selectOne(queryWrapper);
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
        
        // 同步更新待审核视频状态
        pendingVideoMapper.updateById(pendingVideo);
        
        // 只有审核通过时才处理视频和标签
        if (Objects.equals(auditVideoParam.getAuditStatus(), PendingVideoAuditStatus.PASS)) {
            Video video = new Video();
            video.setTitle(pendingVideo.getTitle());
            video.setCover(pendingVideo.getCover());
            video.setDescription(pendingVideo.getDescription());
            video.setUrl(pendingVideo.getUrl());
            video.setUserId(pendingVideo.getUserId());
            video.setUploadTime(pendingVideo.getUploadTime());
            video.setStatus(VideoStatus.NORMAL);
            
            // 同步插入视频，确保video.getId()有值
            videoMapper.insert(video);
            
            // 处理标签
            if (pendingVideo.getTags() != null && !pendingVideo.getTags().isEmpty()) {
                String[] tags = pendingVideo.getTags().split(" ");
                List<String> tagList = List.of(tags);
                // 同步插入标签关联
                videoTagMapper.insertBatch(video.getId(), tagList);
            }
            
            // 发送Kafka消息
            kafkaTemplate.send("video_audit", gson.toJson(video));
        }
    
        // 发送系统消息
        if (Objects.equals(auditVideoParam.getAuditStatus(), PendingVideoAuditStatus.PASS)) {
            String message = "您的视频" + pendingVideo.getTitle() + "已审核通过";
            if (auditVideoParam.getAuditComment() != null) {
                message = message + "，原因：" + auditVideoParam.getAuditComment();
            }
            internalMessageService.sendSystemMessage(pendingVideo.getUserId(), message,
                    pendingVideo.getId(), InternalMessageReceiverType.PENDING_VIDEO);
        }
        else if (Objects.equals(auditVideoParam.getAuditStatus(), PendingVideoAuditStatus.REJECT)) {
            String message = "您的视频" + pendingVideo.getTitle() + "未审核通过";
            if (auditVideoParam.getAuditComment() != null) {
                message = message + "，原因：" + auditVideoParam.getAuditComment();
            }
            internalMessageService.sendSystemMessage(pendingVideo.getUserId(), message,
                    pendingVideo.getId(), InternalMessageReceiverType.PENDING_VIDEO);
        }
    
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