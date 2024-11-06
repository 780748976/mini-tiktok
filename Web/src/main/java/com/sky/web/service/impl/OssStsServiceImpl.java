package com.sky.web.service.impl;

import com.aliyuncs.exceptions.ClientException;
import com.sky.common.utils.AliyunOss;
import com.sky.common.utils.Result;
import com.sky.pojo.constant.WebRedisConstants;
import com.sky.pojo.dto.GetOssStsParam;
import com.sky.web.service.OssStsService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class OssStsServiceImpl implements OssStsService {

    @Resource
    AliyunOss aliyunOss;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getOssStsImage(Long userId, GetOssStsParam getOssStsParam) throws ClientException {
        //判断字段是否为type: avatar-头像 background-背景图 dynamicImage-动态图片 chatImage-聊天图片 commentImage-评论图片其中之一
        List<String> validTypes = Arrays.asList("avatar", "background", "dynamicImage", "chatImage", "commentImage");
        if (validTypes.contains(getOssStsParam.getType())) {
            String fileName = aliyunOss.getUrl(getOssStsParam.getSuffix(), getOssStsParam.getType());
            long score = System.currentTimeMillis() + 1000 * 60 * 60;
            stringRedisTemplate.opsForZSet().add(WebRedisConstants.FILE_UPLOAD_RECORD_KEY, fileName, score);
            return Result.success(aliyunOss.getKey(fileName));
        }
        return Result.failed("type参数错误");
    }

    @Override
    public Result getOssStsVideo(Long userId, String suffix) throws ClientException {
        String url = aliyunOss.getUrl(suffix, "video");
        long score = System.currentTimeMillis() + 1000 * 60 * 60;
        stringRedisTemplate.opsForZSet().add(WebRedisConstants.FILE_UPLOAD_RECORD_KEY, url, score);
        return Result.success(aliyunOss.getKey(url));
    }
}
