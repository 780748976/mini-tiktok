package com.sky.web.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.google.gson.Gson;
import com.sky.pojo.bo.HotVideo;
import com.sky.pojo.bo.TopK;
import com.sky.pojo.constant.VideoStatus;
import com.sky.pojo.entity.Video;
import com.sky.pojo.mapper.VideoMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.sky.pojo.constant.WebRedisConstants;

@Component
public class HotComputeTask {

    @Resource
    VideoMapper videoMapper;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    Gson gson;

    //每小时执行一次
    @Scheduled(cron = "0 0 * * * ?")
    public void computeHotRank() {
        final TopK topK = new TopK(20, new PriorityQueue<HotVideo>(30, Comparator.comparing(HotVideo::getHot)));
        long limit = 1000;
        List<Video> videos = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .select(Video::getId, Video::getViews, Video::getLikes, Video::getDislikes, Video::getUploadTime)
                .orderByDesc(Video::getViews)
                .eq(Video::getStatus, VideoStatus.NORMAL).last("limit " + limit));

        for (Video video : videos) {
            Long views = video.getViews();
            Long likes = video.getLikes();
            Long dislikes = video.getDislikes();
            Date date = new Date();
            long t = date.getTime() - video.getUploadTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            double v = weightRandom();
            double hot = hot(views + likes - dislikes + v, TimeUnit.MILLISECONDS.toDays(t));
            HotVideo hotVideo = new HotVideo();
            hotVideo.setId(video.getId());
            hotVideo.setHot(hot);

            topK.add(hotVideo);
        }
        final byte[] key = WebRedisConstants.HOT_RANK.getBytes();
        final List<HotVideo> hotVideos = topK.get();
        stringRedisTemplate.opsForZSet().remove(WebRedisConstants.HOT_RANK);
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (HotVideo hotVideo : hotVideos) {
                final Double hot = hotVideo.getHot();
                hotVideo.setHot(null);
                connection.zAdd(key, hot, gson.toJson(hotVideo).getBytes());
            }
            return null;
        });
    }

    //每3个小时执行一次
    @Scheduled(cron = "0 0 0/3 * * ?")
    public void computeHotVideo() {
        int limit = 30000;
        //选择3天以内发布的视频
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Video::getId, Video::getViews, Video::getLikes, Video::getDislikes, Video::getUploadTime)
                .orderByDesc(Video::getViews).eq(Video::getStatus, VideoStatus.NORMAL)
                .ge(Video::getUploadTime, LocalDateTime.now().minusDays(3))
                .last("limit " + limit);

        List<Video> videos = videoMapper.selectList(wrapper);
        final Double hotLimit = calculateHotLimit(videos);
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DATE);

        final ArrayList<Long> hotVideos = new ArrayList<>();

        for (Video video : videos) {
            Long views = video.getViews();
            Long likes = video.getLikes();
            Long dislikes = video.getDislikes();
            final Date date = new Date();
            long t = date.getTime() - video.getUploadTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            final double hot = hot(views + likes - dislikes, TimeUnit.MILLISECONDS.toDays(t));

            if (hot > hotLimit) {
                hotVideos.add(video.getId());
            }
        }

        //将热门视频的id存入redis
        String key = WebRedisConstants.HOT_VIDEO + today;
        Set<String> hotVideoIds = hotVideos.stream().map(String::valueOf).collect(Collectors.toSet());
        stringRedisTemplate.opsForSet().add(key, hotVideoIds.toArray(new String[0]));
        stringRedisTemplate.expire(key, 3, TimeUnit.DAYS);
    }

    private Double calculateHotLimit(List<Video> videos) {
        // 计算所有视频的热度分数
        List<Double> hotScores = new ArrayList<>();
        for (Video video : videos) {
            Long views = video.getViews();
            Long likes = video.getLikes();
            Long dislikes = video.getDislikes();
            final Date date = new Date();
            long t = date.getTime() - video.getUploadTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            final double hot = hot(views + likes - dislikes, TimeUnit.MILLISECONDS.toDays(t));
            hotScores.add(hot);
        }

        // 计算热度分数的平均值或中位数作为hotLimit
        Collections.sort(hotScores);
        int middle = hotScores.size() / 2;
        if (hotScores.size() % 2 == 0) {
            return (hotScores.get(middle - 1) + hotScores.get(middle)) / 2.0;
        } else {
            return hotScores.get(middle);
        }
    }


    static double a = 0.011;

    public static double hot(double weight, double t) {
        return weight * Math.exp(-a * t);
    }

    public double weightRandom() {
        int i = (int) ((Math.random() * 9 + 1) * 100000);
        return i / 1000000.0;
    }

}
