package com.sky.web.task;

import com.sky.common.utils.AliyunOss;
import com.sky.pojo.constant.WebRedisConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CleanOssTask {

    @Resource
    AliyunOss aliyunOss;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    //每个1h执行一次
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void cleanOss() {
        long now = System.currentTimeMillis();
        Objects.requireNonNull(stringRedisTemplate.opsForZSet().rangeByScore(WebRedisConstants.FILE_UPLOAD_RECORD_KEY,
                        0, now)).forEach(key -> {
            try{
                aliyunOss.deleteFile(key);
            }
            catch (Exception ignored){
            }
        });
        stringRedisTemplate.opsForZSet().removeRangeByScore(WebRedisConstants.FILE_UPLOAD_RECORD_KEY, 0, now);
    }
}
