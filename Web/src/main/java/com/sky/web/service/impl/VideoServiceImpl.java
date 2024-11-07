package com.sky.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.common.utils.AliyunOss;
import com.sky.common.utils.ErrorLogUtil;
import com.sky.common.utils.PageInfo;
import com.sky.common.utils.Result;
import com.sky.pojo.constant.InternalMessageReceiverType;
import com.sky.pojo.constant.WebRedisConstants;
import com.sky.pojo.dto.UploadPendingVideoParam;
import com.sky.pojo.entity.*;
import com.sky.pojo.mapper.*;
import com.sky.pojo.vo.UserVideo;
import com.sky.web.service.InternalMessageService;
import com.sky.web.service.UserFavoriteTagService;
import com.sky.web.service.VideoService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VideoServiceImpl implements VideoService {

    @Resource
    VideoMapper videoMapper;
    @Resource
    UserMapper userMapper;
    @Resource
    UserInfoMapper userInfoMapper;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    AliyunOss aliyunOss;
    @Resource
    PendingVideoMapper pendingVideoMapper;
    @Autowired
    ThreadPoolTaskExecutor asyncServiceExecutor;
    @Resource
    UserFavoriteTagService userFavoriteTagService;
    @Resource
    UserVideoActionMapper userVideoActionMapper;
    @Resource
    UserWatchRecordMapper userWatchRecordMapper;
    @Resource
    InternalMessageService internalMessageService;

    private static final int LIKE = 1;
    private static final int DISLIKE = 2;

    @Override
    public Result getUserVideoList(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Video::getUserId, userId);
        Page<Video> videoPage = new Page<>(page, size);
        Page<Video> videoList = videoMapper.selectPage(videoPage, queryWrapper);
        return Result.success(PageInfo.restPage(videoList));
    }

    @Override
    public Result uploadPendingVideo(UploadPendingVideoParam uploadPendingVideoParam, String userId) {
        boolean ifFileExist = aliyunOss.findFile(uploadPendingVideoParam.getUrl());
        if (!ifFileExist) {
            return Result.failed("视频不存在");
        }
        PendingVideo pendingVideo = new PendingVideo();
        pendingVideo.setTitle(uploadPendingVideoParam.getTitle())
                .setCover(uploadPendingVideoParam.getCover())
                .setUrl(uploadPendingVideoParam.getUrl())
                .setDescription(uploadPendingVideoParam.getDescription())
                .setUploadTime(LocalDateTime.now())
                .setUserId(userId);
        Integer id = pendingVideoMapper.insert(pendingVideo);
        stringRedisTemplate.opsForZSet().remove(WebRedisConstants.FILE_UPLOAD_RECORD_KEY,
                uploadPendingVideoParam.getUrl());
        return Result.success(id);
    }

    @Override
    public Result viewVideo(Long videoId, String userId) {
        LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Video::getId, videoId);
        Video video = videoMapper.selectOne(queryWrapper);
        if (video == null) {
            return Result.failed("视频不存在");
        }
        LambdaQueryWrapper<UserInfo> userInfoQueryWrapper = new LambdaQueryWrapper<>();
        userInfoQueryWrapper.eq(UserInfo::getId, userId)
                .select(UserInfo::getNickname);
        stringRedisTemplate.opsForHyperLogLog().add(WebRedisConstants.VIEW_VIDEO_KEY + userId,
                String.valueOf(video.getUserId()));
        UserVideo userVideo = new UserVideo();
        userVideo.setVideoId(video.getId())
                .setUserId(Long.valueOf(video.getUserId()))
                .setUrl(video.getUrl())
                .setViews(stringRedisTemplate.opsForHyperLogLog()
                                .size(WebRedisConstants.VIEW_VIDEO_KEY + video.getUserId()) + video.getViews())
                .setTitle(video.getTitle())
                .setLikes(video.getLikes())
                .setNickname(userInfoMapper.selectOne(userInfoQueryWrapper).getNickname())
                .setCover(video.getCover());
        asyncServiceExecutor.execute(() -> {
            try {
                //增加观看记录
                UserWatchRecord userWatchRecord = new UserWatchRecord()
                        .setUserId(userId)
                        .setVideoId(videoId)
                        .setWatchTime(LocalDateTime.now());
                userWatchRecordMapper.insert(userWatchRecord);
                //增加用户观看视频标签
                userFavoriteTagService.viewVideoAddProbability(video.getId(), userId);
            }
            catch (Exception ex){
                ErrorLogUtil.save(ex);
            }
        });
        return Result.success(userVideo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result likeVideo(Long videoId, String userId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return Result.failed("视频不存在");
        }

        LambdaQueryWrapper<UserVideoAction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserVideoAction::getUserId, userId)
                .eq(UserVideoAction::getVideoId, videoId)
                .last("limit 1");
        UserVideoAction action = userVideoActionMapper.selectOne(queryWrapper);

        if (action != null && action.getActionType() == LIKE) {
            // 取消点赞
            videoMapper.cancelLike(videoId);
            userVideoActionMapper.deleteById(action.getId());
            return Result.failed("取消点赞成功");
        }
        else if (action != null && action.getActionType() == DISLIKE) {
            // 取消点踩并进行点赞
            videoMapper.cancelDislike(videoId);
            videoMapper.like(videoId); // 增加点赞数
            action.setActionType(LIKE);
            action.setActionTime(LocalDateTime.now());
            userVideoActionMapper.updateById(action);
            return Result.success("取消点踩并点赞成功");
        }

        // 进行点赞
        videoMapper.like(videoId);
        UserVideoAction newAction = new UserVideoAction()
                .setUserId(userId)
                .setVideoId(videoId)
                .setActionType(LIKE)
                .setActionTime(LocalDateTime.now());
        userVideoActionMapper.insert(newAction);
        internalMessageService.sendLikeMessage(video.getUserId(), video.getId(),
                InternalMessageReceiverType.VIDEO, Long.valueOf(userId));
        return Result.success("点赞成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result dislikeVideo(Long videoId, String userId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return Result.failed("视频不存在");
        }

        LambdaQueryWrapper<UserVideoAction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserVideoAction::getUserId, userId)
                .eq(UserVideoAction::getVideoId, videoId)
                .last("limit 1");
        UserVideoAction action = userVideoActionMapper.selectOne(queryWrapper);
        if (action != null && action.getActionType() == DISLIKE) {
            //取消点踩
            videoMapper.cancelDislike(videoId);
            userVideoActionMapper.deleteById(action.getId());
            return Result.success("取消点踩成功");
        }
        else if (action != null && action.getActionType() == LIKE) {
            //取消点赞 点踩
            videoMapper.cancelLike(videoId);
            videoMapper.dislike(videoId);
            action.setActionType(DISLIKE);
            action.setActionTime(LocalDateTime.now());
            userVideoActionMapper.updateById(action);
            return Result.success("点踩成功");
        }

        videoMapper.dislike(videoId);

        action = new UserVideoAction()
                .setUserId(userId)
                .setVideoId(videoId)
                .setActionType(DISLIKE)
                .setActionTime(LocalDateTime.now());
        userVideoActionMapper.insert(action);

        internalMessageService.sendDislikeMessage(video.getUserId(), video.getId(),
                InternalMessageReceiverType.VIDEO, Long.valueOf(userId));
        return Result.success("点踩成功");
    }
}
