package com.sky.web.task;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.pojo.constant.WebRedisConstants;
import com.sky.pojo.entity.Video;
import com.sky.pojo.mapper.VideoMapper;
import com.sky.web.service.VideoService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class AppendVideoToEsTask {

    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    VideoMapper videoMapper;
    @Resource
    VideoService videoService;

    //每15分钟执行一次
    @Scheduled(cron = "0 0/15 * * * ?")
    public void appendVideoToEs() throws IOException {
        Set<String> videoIdSet = stringRedisTemplate.opsForSet().members(WebRedisConstants.VIDEO_APPEND_LIST);
        if (videoIdSet == null || videoIdSet.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
                .in(Video::getId, videoIdSet);
        List<Video> videos = videoMapper.selectList(wrapper);
        videoService.appendVideoToEsRetry(videos);
        //清空redis中的视频id
        stringRedisTemplate.opsForSet().remove(WebRedisConstants.VIDEO_APPEND_LIST, videoIdSet.toArray());
    }
}
