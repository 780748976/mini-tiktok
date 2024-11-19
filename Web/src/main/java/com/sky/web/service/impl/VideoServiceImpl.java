package com.sky.web.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.sky.common.utils.AliyunOss;
import com.sky.common.utils.ErrorLogUtil;
import com.sky.common.utils.PageInfo;
import com.sky.common.utils.Result;
import com.sky.pojo.bo.HotVideo;
import com.sky.pojo.constant.InternalMessageReceiverType;
import com.sky.pojo.constant.UserChangeProbabilityType;
import com.sky.pojo.constant.VideoStatus;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
    @Resource
    ElasticsearchClient elasticsearchClient;
    @Resource
    TagMapper tagMapper;
    @Resource
    VideoTagMapper videoTagMapper;
    @Resource
    UserFavoriteTagMapper userFavoriteTagMapper;
    @Resource
    Gson gson;

    private static final int LIKE = 1;
    private static final int DISLIKE = 2;

    @Override
    public Result getUserVideoList(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Video::getUserId, userId)
                .eq(Video::getStatus, VideoStatus.NORMAL);
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
        if (!uploadPendingVideoParam.getTags().isEmpty()) {
            if (uploadPendingVideoParam.getTags().size() > 5) {
                return Result.failed("标签数量不能超过5个");
            }
            LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Tag::getName, uploadPendingVideoParam.getTags());
            boolean ifTagExist = tagMapper.exists(queryWrapper);
            if (!ifTagExist) {
                return Result.failed("标签不存在");
            }
        }
        String tags = String.join(" ", uploadPendingVideoParam.getTags());
        PendingVideo pendingVideo = new PendingVideo();
        pendingVideo.setTitle(uploadPendingVideoParam.getTitle())
                .setCover(uploadPendingVideoParam.getCover())
                .setUrl(uploadPendingVideoParam.getUrl())
                .setDescription(uploadPendingVideoParam.getDescription())
                .setUploadTime(LocalDateTime.now())
                .setTags(tags)
                .setUserId(userId);
        Integer id = pendingVideoMapper.insert(pendingVideo);
        stringRedisTemplate.opsForZSet().remove(WebRedisConstants.FILE_UPLOAD_RECORD_KEY,
                uploadPendingVideoParam.getUrl());
        return Result.success(id);
    }

    @Override
    public Result viewVideo(Long videoId, Long userId) throws ExecutionException, InterruptedException {
        LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Video::getId, videoId)
                .eq(Video::getStatus, VideoStatus.NORMAL);
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
                userFavoriteTagService.changeProbability(video.getId(), userId, UserChangeProbabilityType.PLAY);
            } catch (Exception ex) {
                ErrorLogUtil.save(ex);
            }
        });
        stringRedisTemplate.opsForSet().add(WebRedisConstants.VIDEO_APPEND_LIST, videoId.toString());
        return Result.success(userVideo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result likeVideo(Long videoId, Long userId) {
        LambdaQueryWrapper<Video> videoQueryWrapper = new LambdaQueryWrapper<>();
        videoQueryWrapper.eq(Video::getId, videoId)
                .eq(Video::getStatus, VideoStatus.NORMAL);
        Video video = videoMapper.selectOne(videoQueryWrapper);
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
            asyncServiceExecutor.execute(() -> {
                try {
                    userFavoriteTagService.changeProbability(video.getId(), userId,
                            UserChangeProbabilityType.UNLIKE);
                } catch (Exception ex) {
                    ErrorLogUtil.save(ex);
                }
            });
            return Result.failed("取消点赞成功");
        } else if (action != null && action.getActionType() == DISLIKE) {
            // 取消点踩并进行点赞
            videoMapper.cancelDislike(videoId);
            videoMapper.like(videoId); // 增加点赞数
            action.setActionType(LIKE);
            action.setActionTime(LocalDateTime.now());
            userVideoActionMapper.updateById(action);
            asyncServiceExecutor.execute(() -> {
                try {
                    userFavoriteTagService.changeProbability(video.getId(), userId,
                            UserChangeProbabilityType.DISLIKE_TO_LIKE);
                } catch (Exception ex) {
                    ErrorLogUtil.save(ex);
                }
            });
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
                InternalMessageReceiverType.VIDEO, userId);
        stringRedisTemplate.opsForSet().add(WebRedisConstants.VIDEO_APPEND_LIST, videoId.toString());
        asyncServiceExecutor.execute(() -> {
            try {
                userFavoriteTagService.changeProbability(video.getId(), userId, UserChangeProbabilityType.LIKE);
            } catch (Exception ex) {
                ErrorLogUtil.save(ex);
            }
        });
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

        CompletableFuture.allOf(likeFuture, favoriteFuture, dislikeFuture, userInfoFuture).get();

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
                    .setDescription(videos.get(i).getDescription())
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
        LambdaQueryWrapper<Video> videoQueryWrapper = new LambdaQueryWrapper<>();
        videoQueryWrapper.eq(Video::getId, videoId)
                .eq(Video::getStatus, VideoStatus.NORMAL);
        Video video = videoMapper.selectOne(videoQueryWrapper);
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
            asyncServiceExecutor.execute(() -> {
                try {
                    userFavoriteTagService.changeProbability(video.getId(), userId,
                            UserChangeProbabilityType.UNDISLIKE);
                } catch (Exception ex) {
                    ErrorLogUtil.save(ex);
                }
            });
            return Result.success("取消点踩成功");
        } else if (action != null && action.getActionType() == LIKE) {
            //取消点赞 点踩
            videoMapper.cancelLike(videoId);
            videoMapper.dislike(videoId);
            action.setActionType(DISLIKE);
            action.setActionTime(LocalDateTime.now());
            userVideoActionMapper.updateById(action);
            asyncServiceExecutor.execute(() -> {
                try {
                    userFavoriteTagService.changeProbability(video.getId(), userId,
                            UserChangeProbabilityType.LIKE_TO_DISLIKE);
                } catch (Exception ex) {
                    ErrorLogUtil.save(ex);
                }
            });
            return Result.success("点踩成功");
        }

        videoMapper.dislike(videoId);

        action = new UserVideoAction()
                .setUserId(userId)
                .setVideoId(videoId)
                .setActionType(DISLIKE)
                .setActionTime(LocalDateTime.now());
        userVideoActionMapper.insert(action);

        stringRedisTemplate.opsForSet().add(WebRedisConstants.VIDEO_APPEND_LIST, videoId.toString());
        asyncServiceExecutor.execute(() -> {
            try {
                userFavoriteTagService.changeProbability(video.getId(), userId, UserChangeProbabilityType.DISLIKE);
            } catch (Exception ex) {
                ErrorLogUtil.save(ex);
            }
        });
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
        asyncServiceExecutor.execute(() -> {
            try {
                userFavoriteTagService.changeProbability(videoId, userId, UserChangeProbabilityType.FAVORITE);
            } catch (Exception ex) {
                ErrorLogUtil.save(ex);
            }
        });

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
            asyncServiceExecutor.execute(() -> {
                try {
                    userFavoriteTagService.changeProbability(videoId, userId, UserChangeProbabilityType.UNFAVORITE);
                } catch (Exception ex) {
                    ErrorLogUtil.save(ex);
                }
            });
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

    @Override
    public Result searchVideo(String keyword, Integer page, Integer size, String sortType) throws IOException {
        switch (sortType) {
            case "default" -> sortType = null;
            case "like" -> sortType = "likes";
            case "view" -> sortType = "views";
            case "time" -> sortType = "uploadTime";
            default -> {
                return Result.failed("排序类型不存在");
            }
        }
        String finalSortType = sortType;
        if (sortType == null) {
            SearchResponse<Video> videos = elasticsearchClient.search(i -> i
                    .index("video")
                    .query(q -> q
                            .match(m -> m
                                    .field("title")
                                    .query(keyword)
                            )
                    )
                    .from((page - 1) * size)
                    .size(size), Video.class);
            return Result.success(videos.hits().hits());
        }
        SearchResponse<Video> videos = elasticsearchClient.search(i -> i
                .index("video")
                .query(q -> q
                        .match(m -> m
                                .field("title")
                                .query(keyword)
                        )
                )
                .sort(s -> s
                        .field(FieldSort.of(f -> f.field(finalSortType)
                                .order(SortOrder.Desc)))
                )
                .from((page - 1) * size)
                .size(size), Video.class);
        return Result.success(videos.hits().hits());
    }

    @Override
    public Result getUserLikeVideoList(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<UserVideoAction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserVideoAction::getUserId, userId)
                .eq(UserVideoAction::getActionType, LIKE)
                .orderByDesc(UserVideoAction::getActionTime);
        Page<UserVideoAction> userVideoActionList =
                userVideoActionMapper.selectPage(new Page<>(page, size), queryWrapper);
        return Result.success(PageInfo.restPage(userVideoActionList));
    }

    @Override
    public Result getRecommendVideoList(Long userId) throws ExecutionException, InterruptedException {
        List<UserFavoriteTag> userFavoriteTags;
        if (userId != 0) {
            //获取用户喜欢的标签前5
            LambdaQueryWrapper<UserFavoriteTag> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserFavoriteTag::getUserId, userId)
                    .orderByDesc(UserFavoriteTag::getProbability)
                    .last("limit 5");
            userFavoriteTags = userFavoriteTagMapper.selectList(queryWrapper);
        } else {
            userFavoriteTags = new ArrayList<>();
        }
        //随机获取补充
        Random random = new Random();
        LambdaQueryWrapper<VideoTag> videoTagQueryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Tag> randomQueryWrapper = new LambdaQueryWrapper<>();
        // 获取最大Id
        CompletableFuture<Long> maxVideoTagIdFuture = CompletableFuture.supplyAsync(() -> {

            videoTagQueryWrapper.notIn(VideoTag::getTagId, userFavoriteTags.stream()
                            .map(UserFavoriteTag::getTagId).toArray())
                    .orderByDesc(VideoTag::getId).last("limit 1");
            return videoTagMapper.selectOne(videoTagQueryWrapper).getId();
        }, asyncServiceExecutor);

        CompletableFuture<Long> maxTagIdFuture = CompletableFuture.supplyAsync(() -> {

            randomQueryWrapper.orderByDesc(Tag::getId).last("limit 1");
            return tagMapper.selectOne(randomQueryWrapper).getId();
        }, asyncServiceExecutor);

        Long maxVideoTagId = maxVideoTagIdFuture.get();
        Long maxTagId = maxTagIdFuture.get();
        if (userFavoriteTags.size() < 7) {
            //生成随机数
            Long randomId = random.nextLong(maxTagId.intValue());
            randomQueryWrapper.clear();
            randomQueryWrapper.orderByAsc(Tag::getId)
                    .ge(Tag::getId, randomId)
                    .notIn(Tag::getId, userFavoriteTags.stream().map(UserFavoriteTag::getTagId).toArray())
                    .last("limit " + (7 - userFavoriteTags.size()));
            List<Tag> randomTags = tagMapper.selectList(randomQueryWrapper);
            for (Tag tag : randomTags) {
                UserFavoriteTag userFavoriteTag = new UserFavoriteTag();
                userFavoriteTag.setUserId(userId)
                        .setTagId(tag.getId())
                        .setProbability(0);
                userFavoriteTags.add(userFavoriteTag);
            }
        }

        Long randomId = random.nextLong(maxVideoTagId.intValue());
        videoTagQueryWrapper.clear();
        videoTagQueryWrapper.orderByAsc(VideoTag::getId)
                .ge(VideoTag::getId, randomId)
                .in(VideoTag::getTagId, userFavoriteTags.stream().map(UserFavoriteTag::getTagId).toArray())
                .last("limit 10");
        List<VideoTag> videoTags = videoTagMapper.selectList(videoTagQueryWrapper);
        List<Long> videoIds = videoTags.stream().map(VideoTag::getVideoId).toList();
        if (videoIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        LambdaQueryWrapper<Video> videoQueryWrapper = new LambdaQueryWrapper<>();
        videoQueryWrapper.in(Video::getId, videoIds)
                .eq(Video::getStatus, VideoStatus.NORMAL)
                .orderByDesc(Video::getLikes)
                .last("limit 10");
        List<Video> videos = videoMapper.selectList(videoQueryWrapper);

        //查询用户12小时以内的播放记录
        LambdaQueryWrapper<UserWatchRecord> watchRecordQueryWrapper = new LambdaQueryWrapper<>();
        watchRecordQueryWrapper.eq(UserWatchRecord::getUserId, userId)
                .ge(UserWatchRecord::getWatchTime, LocalDateTime.now().minusHours(12))
                .in(UserWatchRecord::getVideoId, videos.stream().map(Video::getId).toArray());
        //完成去重
        List<UserWatchRecord> userWatchRecords = userWatchRecordMapper.selectList(watchRecordQueryWrapper);
        for (UserWatchRecord userWatchRecord : userWatchRecords) {
            videos.removeIf(video -> video.getId().equals(userWatchRecord.getVideoId()));
        }

        //重新获取直到满足10个
        while (userWatchRecords.size() < 10) {
            videoQueryWrapper.clear();
            videoQueryWrapper.in(Video::getId, videoIds)
                    .orderByDesc(Video::getLikes)
                    .last("limit " + (10 - userWatchRecords.size()));
            List<Video> tempVideos = videoMapper.selectList(videoQueryWrapper);
            for (Video video : tempVideos) {
                if (userWatchRecords.stream().noneMatch(record -> record.getVideoId().equals(video.getId()))) {
                    videos.add(video);
                }
            }
            if (videos.size() >= 10) {
                break;
            }
        }

        return Result.success(videos);
    }

    @Override
    public Result getHotVideoList() {
        //分页查询热门视频
        Set<String> hotVideos = stringRedisTemplate.opsForZSet().range(WebRedisConstants.HOT_RANK, 0, -1);
        if (hotVideos == null || hotVideos.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<HotVideo> hotVideoList = new ArrayList<>();
        for (String hotVideo : hotVideos) {
            hotVideoList.add(gson.fromJson(hotVideo, HotVideo.class));
        }
        LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Video::getId, hotVideoList.stream().map(HotVideo::getId).toArray())
                .eq(Video::getStatus, VideoStatus.NORMAL)
                .select(Video::getId, Video::getTitle, Video::getCover);
        List<Video> videos = videoMapper.selectList(queryWrapper);
        return Result.success(videos);
    }

    @Override
    public Result getHotVideoRankList(Integer page, Integer size) {
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DATE);

        final HashMap<String, Integer> map = new HashMap<>();
        // 优先推送今日的
        map.put(WebRedisConstants.HOT_VIDEO + today, 10);
        map.put(WebRedisConstants.HOT_VIDEO + (today - 1), 3);
        map.put(WebRedisConstants.HOT_VIDEO + (today - 2), 2);

        // 获取热门视频的ID
        List<Long> hotVideoIds = stringRedisTemplate.executePipelined((RedisCallback<List<Long>>) connection -> {
                    map.forEach((k, v) -> {
                        connection.sRandMember(k.getBytes(), v);
                    });
                    return null;
                }).stream()
                .filter(Objects::nonNull)
                .flatMap(o -> ((List<byte[]>) o).stream())
                .map(bytes -> Long.parseLong(new String(bytes)))
                .toList();

        LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Video::getId, hotVideoIds)
                .eq(Video::getStatus, VideoStatus.NORMAL)
                .select(Video::getId, Video::getTitle, Video::getCover);
        List<Video> videos = videoMapper.selectList(queryWrapper);
        return Result.success(videos);
    }
}
