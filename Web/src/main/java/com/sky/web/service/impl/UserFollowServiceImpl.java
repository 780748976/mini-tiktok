package com.sky.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.common.utils.Result;
import com.sky.pojo.entity.UserFollow;
import com.sky.pojo.mapper.UserFollowMapper;
import com.sky.web.service.UserFollowService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class UserFollowServiceImpl implements UserFollowService {

    @Resource
    private UserFollowMapper userFollowMapper;

    @Override
    public Result followUser(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            return Result.failed("不能关注自己");
        }

        LambdaQueryWrapper<UserFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFollow::getFollowerId, userId)
                    .eq(UserFollow::getFollowingId, targetUserId);
        UserFollow existingFollow = userFollowMapper.selectOne(queryWrapper);
        if (existingFollow != null) {
            return Result.failed("已关注该用户");
        }

        UserFollow follow = new UserFollow();
        follow.setFollowerId(userId);
        follow.setFollowingId(targetUserId);
        follow.setCreateTime(LocalDateTime.now());
        userFollowMapper.insert(follow);
        return Result.success("关注成功");
    }

    @Override
    public Result unfollowUser(Long userId, Long targetUserId) {
        LambdaQueryWrapper<UserFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFollow::getFollowerId, userId)
                    .eq(UserFollow::getFollowingId, targetUserId);
        int deleted = userFollowMapper.delete(queryWrapper);
        if (deleted > 0) {
            return Result.success("取消关注成功");
        } else {
            return Result.failed("关注记录不存在");
        }
    }

    @Override
    public Result getFollowers(Long userId, Integer page, Integer pageSize) {
        Page<UserFollow> followPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<UserFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFollow::getFollowingId, userId);
        Page<UserFollow> resultPage = userFollowMapper.selectPage(followPage, queryWrapper);
        return Result.success(resultPage);
    }

    @Override
    public Result getFollowings(Long userId, Integer page, Integer pageSize) {
        Page<UserFollow> followPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<UserFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFollow::getFollowerId, userId);
        Page<UserFollow> resultPage = userFollowMapper.selectPage(followPage, queryWrapper);
        return Result.success(resultPage);
    }
}