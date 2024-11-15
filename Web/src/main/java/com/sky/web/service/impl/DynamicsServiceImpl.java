package com.sky.web.service.impl;

import com.sky.common.utils.Result;
import com.sky.pojo.constant.WebRedisConstants;
import com.sky.web.service.DynamicsService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DynamicsServiceImpl implements DynamicsService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getUserDynamics(Integer page, Integer size, Long userId) {
        String key = WebRedisConstants.USER_DYNAMICS + userId;
        Set<String> dynamics = stringRedisTemplate.opsForZSet().reverseRange(key,
                (long) (page - 1) * size, (long) page * size - 1);
        return Result.success(dynamics);
    }
}