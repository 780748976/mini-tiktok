package com.sky.web.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.pojo.constant.WebRedisConstants;
import com.sky.pojo.entity.User;
import com.sky.pojo.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class CleanUserRedisDynamics {

    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    UserMapper userMapper;

    //每天凌晨4点清理用户动态
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanUserDynamics() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().select(User::getId);
        List<Long> userIdList = userMapper.selectList(wrapper).stream()
                .map(user -> Objects.requireNonNull(user.getId()))
                .toList();

        // 计算7天前的时间戳（毫秒）
        long sevenDaysAgoTimestamp = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;

        // 一次处理1000个用户的动态
        for (int i = 0; i < userIdList.size(); i += 1000) {
            List<Long> subList = userIdList.subList(i, Math.min(i + 1000, userIdList.size()));
            for (Long userId : subList) {
                String key = WebRedisConstants.USER_DYNAMICS + userId;
                // 删除score小于7天前时间戳的所有成员
                stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, sevenDaysAgoTimestamp);
            }
        }
    }
}
