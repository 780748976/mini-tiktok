package com.sky.web.controller;

import com.sky.common.utils.Result;
import com.sky.web.service.UserFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/userFollow")
@Tag(name = "UserFollow", description = "用户关注接口")
@Validated
public class UserFollowController {

    @Resource
    private UserFollowService userFollowService;

    @PostMapping("/follow")
    @Operation(summary = "关注用户")
    public Result followUser(@RequestParam @NotNull Long targetUserId) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return userFollowService.followUser(userId, targetUserId);
    }

    @PostMapping("/unfollow")
    @Operation(summary = "取消关注用户")
    public Result unfollowUser(@RequestParam @NotNull Long targetUserId) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return userFollowService.unfollowUser(userId, targetUserId);
    }

    @GetMapping("/followers")
    @Operation(summary = "获取关注者列表")
    public Result getFollowers(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return userFollowService.getFollowers(userId, page, pageSize);
    }

    @GetMapping("/followings")
    @Operation(summary = "获取关注列表")
    public Result getFollowings(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return userFollowService.getFollowings(userId, page, pageSize);
    }
}