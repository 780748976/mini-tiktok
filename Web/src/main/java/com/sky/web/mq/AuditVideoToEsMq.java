package com.sky.web.mq;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.google.gson.Gson;
import com.sky.pojo.entity.Video;
import com.sky.web.service.InternalMessageService;
import jakarta.annotation.Resource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuditVideoToEsMq {

    @Resource
    Gson gson;
    @Resource
    ElasticsearchClient elasticsearchClient;

    @KafkaListener(topics = "video_audit", groupId = "video_audit_group")
    public void auditVideoToEs(String videoJson) throws IOException {
        Video video = gson.fromJson(videoJson, Video.class);
        elasticsearchClient.index(i -> i
                .index("video")
                .id(video.getId().toString())
                .document(video)
        );
    }
}
