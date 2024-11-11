package com.sky.web.service;

import com.sky.common.utils.Result;

public interface UserFollowService {
    // 关注用户
    Result followUser(Long userId, Long targetUserId);

    // 取消关注
    Result unfollowUser(Long userId, Long targetUserId);

    // 获取关注者列表
    Result getFollowers(Long userId, Integer page, Integer pageSize);

    // 获取关注列表
    Result getFollowings(Long userId, Integer page, Integer pageSize);
}