package com.sky.web.mq;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
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

    @KafkaListener(topics = "video_audit", containerFactory = "kafkaListenerBatchContainerFactory")
    public void auditVideoToEs(List<String> videoJson) throws IOException {
        List<Video> videoList = videoJson.stream().map(json -> gson.fromJson(json, Video.class)).toList();
        appendVideoToEsRetry(videoList);
    }

    void appendVideoToEsRetry(List<Video> videoList) throws IOException {
        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
        for (Video video : videoList) {
            bulkRequestBuilder.operations(op -> op
                    .index(idx -> idx
                            .index("videos")
                            .id(video.getId().toString())
                            .document(video)
                    )
            );
        }
        BulkResponse bulkResponse = elasticsearchClient.bulk(bulkRequestBuilder.build());
        List<Video> failedVideoList = bulkResponse.items().stream()
                .filter(item -> item.error() != null)
                .map(item -> videoList.get(bulkResponse.items().indexOf(item)))
                .toList();
        // 重试
        if (!failedVideoList.isEmpty()) {
            appendVideoToEsRetry(failedVideoList);
        }
    }

    @KafkaListener(topics = "video_audit", containerFactory = "kafkaListenerContainerFactory")
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
        internalMessageService.sendDynamicsMessage(userFollowList.stream().map(UserFollow::getFollowerId).toList());
    }
}
