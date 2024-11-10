package com.sky.web.mq;

import com.google.gson.Gson;
import com.sky.pojo.entity.Video;
import com.sky.pojo.mapper.VideoMapper;
import jakarta.annotation.Resource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditVideoToEsMq {

    @Resource
    Gson gson;

    @KafkaListener(topics = "video_audit", groupId = "video_audit_group")
    public void auditVideoToEs(String videoJson) {
        System.out.println("video = " + videoJson);
    }
}
