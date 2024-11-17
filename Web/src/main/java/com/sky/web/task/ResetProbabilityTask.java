package com.sky.web.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.pojo.constant.WebRedisConstants;
import com.sky.pojo.entity.UserFavoriteTag;
import com.sky.pojo.mapper.UserFavoriteTagMapper;
import com.sky.pojo.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ResetProbabilityTask {

    @Resource
    UserMapper userMapper;
    @Resource
    UserFavoriteTagMapper userFavoriteTagMapper;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    //每天1点执行一次
    @Scheduled(cron = "0 0 1 * * ?")
    public void resetProbability() {
        Set<String> userIdSet = stringRedisTemplate.opsForSet().members(WebRedisConstants.USER_TODAY_SIGN_IN_KEY);
        if (userIdSet == null || userIdSet.isEmpty()) {
            return;
        }
        // 将FavoriteTag表中的probability重置为0，排名前10的probability分别为1-10
        // 分批处理，每次处理100个用户
        List<String> userIdList = userIdSet.stream().toList();
        for (int i = 0; i < userIdList.size(); i += 100) {
            List<String> subUserIdList = userIdList.subList(i, Math.min(i + 100, userIdList.size()));
            // 手动事务管理
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            TransactionStatus status = transactionManager.getTransaction(def);
            try {
                // 选择每一个user排行前10的tag，将probability设置为1-10
                List<UserFavoriteTag> tagsToUpdate = new ArrayList<>();
                for (String userId : subUserIdList) {
                    List<UserFavoriteTag> favoriteTags = userFavoriteTagMapper.selectList(
                            new LambdaQueryWrapper<UserFavoriteTag>()
                                    .eq(UserFavoriteTag::getUserId, userId)
                                    .orderByDesc(UserFavoriteTag::getProbability));
                    for (int j = 0; j < favoriteTags.size(); j++) {
                        UserFavoriteTag tag = favoriteTags.get(j);
                        if (j < 10) {
                            tag.setProbability(j + 1);
                        } else {
                            tag.setProbability(0);
                        }
                        tagsToUpdate.add(tag);
                    }
                }
                // 批量更新
                userFavoriteTagMapper.batchUpdate(tagsToUpdate);
                // 删除 Redis 键
                stringRedisTemplate.delete(WebRedisConstants.USER_TODAY_SIGN_IN_KEY);
                transactionManager.commit(status);
            } catch (Exception e) {
                transactionManager.rollback(status);
                throw e; // 重新抛出异常以便上层处理
            }
        }
    }
}
