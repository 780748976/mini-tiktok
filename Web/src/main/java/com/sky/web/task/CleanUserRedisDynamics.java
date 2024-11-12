package com.sky.web.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.pojo.constant.WebRedisConstants;
import com.sky.pojo.entity.User;
import com.sky.pojo.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Objects;

public class CleanUserRedisDynamics {

    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    UserMapper userMapper;

    //每天凌晨4点清理用户动态
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanUserDynamics() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().select(User::getId);
        List<Long> userIdList = userMapper.selectList(wrapper).stream()
                .map(user -> Objects.requireNonNull(user.getId()))
                .toList();
        //一次删除1000个用户的动态
        for (int i = 0; i < userIdList.size(); i += 1000) {
            List<Long> subList = userIdList.subList(i, Math.min(i + 1000, userIdList.size()));
            stringRedisTemplate.delete(subList.stream()
                    .map(userId -> WebRedisConstants.USER_DYNAMICS + userId)
                    .toList());
        }
    }
}
