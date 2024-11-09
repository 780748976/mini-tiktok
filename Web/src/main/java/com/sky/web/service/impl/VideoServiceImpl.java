package com.sky.web.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class VideoServiceImpl implements VideoService {

    @Resource
    VideoMapper videoMapper;
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
    @Resource
    UserFavoriteVideoMapper userFavoriteVideoMapper;

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
    public Result uploadPendingVideo(UploadPendingVideoParam uploadPendingVideoParam, Long userId) {
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
    public Result viewVideo(Long videoId, Long userId) throws ExecutionException, InterruptedException {
        LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Video::getId, videoId);
        Video video = videoMapper.selectOne(queryWrapper);
        if (video == null) {
            return Result.failed("视频不存在");
        }
        stringRedisTemplate.opsForHyperLogLog().add(WebRedisConstants.VIEW_VIDEO_KEY + videoId,
                String.valueOf(video.getUserId()));
        List<UserVideo> userVideo = fillVideo(List.of(video), userId);
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
    public Result likeVideo(Long videoId, Long userId) {
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
    public List<UserVideo> fillVideo(List<Video> videos, Long userId) throws ExecutionException, InterruptedException {
        //是否点赞
        LambdaQueryWrapper<UserVideoAction> likeActionQueryWrapper = new LambdaQueryWrapper<>();
        likeActionQueryWrapper.eq(UserVideoAction::getUserId, userId)
                .in(UserVideoAction::getVideoId, videos.stream().map(Video::getId).toArray())
                .eq(UserVideoAction::getActionType, LIKE)
                .select(UserVideoAction::getVideoId);
        //是否点踩
        LambdaQueryWrapper<UserVideoAction> dislikeActionQueryWrapper = new LambdaQueryWrapper<>();
        dislikeActionQueryWrapper.eq(UserVideoAction::getUserId, userId)
                .in(UserVideoAction::getVideoId, videos.stream().map(Video::getId).toArray())
                .eq(UserVideoAction::getActionType, DISLIKE)
                .select(UserVideoAction::getVideoId);
        //是否收藏
        LambdaQueryWrapper<UserFavoriteVideo> favoriteVideoQueryWrapper = new LambdaQueryWrapper<>();
        favoriteVideoQueryWrapper.eq(UserFavoriteVideo::getUserId, userId)
                .in(UserFavoriteVideo::getVideoId, videos.stream().map(Video::getId).toArray())
                .select(UserFavoriteVideo::getVideoId);
        //查询作者信息
        LambdaQueryWrapper<UserInfo> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(UserInfo::getId, videos.stream().map(Video::getUserId).toArray())
                .select(UserInfo::getId, UserInfo::getNickname);

        //异步编排
        CompletableFuture<List<Long>> likeFuture = CompletableFuture.supplyAsync(() -> {
            return userVideoActionMapper.selectList(likeActionQueryWrapper).stream()
                    .map(UserVideoAction::getVideoId).toList();
        }, asyncServiceExecutor);
        CompletableFuture<List<Long>> dislikeFuture = CompletableFuture.supplyAsync(() -> {
            return userVideoActionMapper.selectList(dislikeActionQueryWrapper).stream()
                    .map(UserVideoAction::getVideoId).toList();
        }, asyncServiceExecutor);
        CompletableFuture<List<Long>> favoriteFuture = CompletableFuture.supplyAsync(() -> {
            return userFavoriteVideoMapper.selectList(favoriteVideoQueryWrapper).stream()
                    .map(UserFavoriteVideo::getVideoId).toList();
        }, asyncServiceExecutor);
        CompletableFuture<List<UserInfo>> userInfoFuture = CompletableFuture.supplyAsync(() -> {
            return userInfoMapper.selectList(userQueryWrapper);
        }, asyncServiceExecutor);

        CompletableFuture.allOf(likeFuture, favoriteFuture, dislikeFuture, userInfoFuture);
        List<Long> likeVideoIds = likeFuture.get();
        List<Long> dislikeVideoIds = dislikeFuture.get();
        List<Long> favoriteVideoIds = favoriteFuture.get();
        List<UserInfo> userInfos = userInfoFuture.get();
        List<UserVideo> userVideos = new ArrayList<>();
        for (int i = 0; i < videos.size(); i++) {
            UserVideo userVideo = new UserVideo();
            int finalI = i;
            userVideo.setVideoId(videos.get(i).getId())
                    .setUserId(videos.get(i).getUserId())
                    .setUrl(videos.get(i).getUrl())
                    .setViews(stringRedisTemplate.opsForHyperLogLog()
                            .size(WebRedisConstants.VIEW_VIDEO_KEY + videos.get(i).getId()) +
                            videos.get(i).getViews())
                    .setTitle(videos.get(i).getTitle())
                    .setLikes(videos.get(i).getLikes())
                    .setDislikes(videos.get(i).getDislikes())
                    .setIsLike(likeVideoIds.contains(videos.get(i).getId()))
                    .setIsDislike(dislikeVideoIds.contains(videos.get(i).getId()))
                    .setIsFavorite(favoriteVideoIds.contains(videos.get(i).getId()))
                    .setNickname(userInfos.stream().filter(userInfo ->
                                    userInfo.getId().equals(videos.get(finalI).getUserId()))
                            .findFirst().orElse(new UserInfo()).getNickname())
                    .setCover(videos.get(i).getCover());
            userVideos.add(userVideo);
        }

        return userVideos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result dislikeVideo(Long videoId, Long userId) {
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

    @Override
    public Result favoriteVideo(Long videoId, Long userId) {
        // 检查是否已收藏
        UserFavoriteVideo existingFavorite = userFavoriteVideoMapper.selectOne(
                new LambdaQueryWrapper<UserFavoriteVideo>()
                        .eq(UserFavoriteVideo::getUserId, userId)
                        .eq(UserFavoriteVideo::getVideoId, videoId)
        );

        if (existingFavorite != null) {
            return Result.failed("视频已被收藏");
        }

        UserFavoriteVideo favorite = new UserFavoriteVideo()
                .setUserId(userId)
                .setVideoId(videoId)
                .setCreateTime(LocalDateTime.now());

        userFavoriteVideoMapper.insert(favorite);

        return Result.success("收藏成功");
    }


    @Override
    public Result unfavoriteVideo(Long videoId, Long userId) {
        int deleted = userFavoriteVideoMapper.delete(
                new LambdaQueryWrapper<UserFavoriteVideo>()
                        .eq(UserFavoriteVideo::getUserId, userId)
                        .eq(UserFavoriteVideo::getVideoId, videoId)
        );

        if (deleted > 0) {
            return Result.success("取消收藏成功");
        } else {
            return Result.failed("收藏记录不存在");
        }
    }

    @Override
    public Result getFavoriteVideoList(Long userId, Integer page, Integer size) {
        Page<UserFavoriteVideo> favoriteVideoPage = new Page<>(page, size);
        Page<UserFavoriteVideo> favoriteVideoList = userFavoriteVideoMapper.selectPage(favoriteVideoPage,
                new LambdaQueryWrapper<UserFavoriteVideo>()
                        .eq(UserFavoriteVideo::getUserId, userId)
        );
        return Result.success(PageInfo.restPage(favoriteVideoList));
    }

    @Override
    public Result getBrowseRecordList(Long userId, Integer page, Integer size) {
        Page<UserWatchRecord> watchRecordPage = new Page<>(page, size);
        Page<UserWatchRecord> watchRecordList = userWatchRecordMapper.selectPage(watchRecordPage,
                new LambdaQueryWrapper<UserWatchRecord>()
                        .eq(UserWatchRecord::getUserId, userId)
        );
        return Result.success(PageInfo.restPage(watchRecordList));
    }
}
