package com.sky.web.mq;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import com.sky.pojo.constant.InternalMessageTypeConstants;
import com.sky.pojo.constant.WebRedisConstants;
import com.sky.pojo.entity.UserFollow;
import com.sky.pojo.entity.Video;
import com.sky.pojo.mapper.UserFollowMapper;
import com.sky.web.service.InternalMessageService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AuditVideoMq {

    @Resource
    Gson gson;
    @Resource
    ElasticsearchClient elasticsearchClient;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    UserFollowMapper userFollowMapper;
    @Resource
    InternalMessageService internalMessageService;

    @KafkaListener(topics = "video_audit", containerFactory = "kafkaListenerContainerFactory1")
    public void auditVideoToEs(String videoJson) throws IOException {
        Video video = gson.fromJson(videoJson, Video.class);
        elasticsearchClient.index(i -> i
                .index("video")
                .id(video.getId().toString())
                .document(video)
        );
    }

    @KafkaListener(topics = "video_audit", containerFactory = "kafkaListenerContainerFactory2")
    public void sendToUserDynamics(String videoJson) {
        Video video = gson.fromJson(videoJson, Video.class);
        AtomicReference<Long> count = new AtomicReference<>(gson.fromJson(stringRedisTemplate.opsForValue()
                .get(WebRedisConstants.VIDEO_PUBLISH_CURSOR + video.getUserId()), Long.class));
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowingId, video.getUserId()).last("limit 1000")
                .gt(UserFollow::getId, count).orderByDesc(UserFollow::getId);
        List<UserFollow> userFollowList = userFollowMapper.selectList(wrapper);
        try {
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                userFollowList.forEach(userFollow -> {
                    connection.zAdd(
                            (WebRedisConstants.USER_DYNAMICS + userFollow.getFollowerId())
                                    .getBytes(StandardCharsets.UTF_8),
                            (double) System.currentTimeMillis() + 60 * 60 * 24 * 7,
                            videoJson.getBytes(StandardCharsets.UTF_8)
                    );
                    count.set(userFollow.getId());
                });
                return null;
            });
        } catch (Exception ignored) {
        }
        stringRedisTemplate.opsForValue().set(WebRedisConstants.VIDEO_PUBLISH_CURSOR + video.getUserId(),
                count.toString());
        internalMessageService.sendFollowMessage(userFollowList.stream().map(UserFollow::getFollowerId).toList());
    }
}
