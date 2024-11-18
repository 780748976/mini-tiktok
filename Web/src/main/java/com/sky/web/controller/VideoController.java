package com.sky.web.controller;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.UploadPendingVideoParam;
import com.sky.web.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/video")
@Tag(name = "Video", description = "视频接口")
@Validated
public class VideoController {

    @Resource
    VideoService videoService;

    @GetMapping("/get_user_video_list")
    @Operation(summary = "分页获取用户视频列表")
    public Result getUserVideoList(@NotBlank @RequestParam Long userId,
                                   @NotNull @RequestParam Integer page, @NotNull @RequestParam Integer size) {
        return videoService.getUserVideoList(userId, page, size);
    }

    @PostMapping("/upload_pending_video")
    @Operation(summary = "上传视频")
    public Result uploadPendingVideo(@RequestBody @Validated UploadPendingVideoParam uploadPendingVideoParam) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return videoService.uploadPendingVideo(uploadPendingVideoParam, userId);
    }

    @GetMapping("/view_video")
    @Operation(summary = "查看视频")
    public Result viewVideo(@NotNull @RequestParam Long videoId) throws ExecutionException, InterruptedException {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return videoService.viewVideo(videoId, userId);
    }

    @GetMapping("/like")
    @Operation(summary = "点赞视频")
    public Result likeVideo(@RequestParam @NotNull Long videoId) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return videoService.likeVideo(videoId, userId);
    }

    @GetMapping("/dislike")
    @Operation(summary = "点踩视频")
    public Result dislikeVideo(@RequestParam @NotNull Long videoId) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return videoService.dislikeVideo(videoId, userId);
    }

    @PostMapping("/favorite")
    @Operation(summary = "收藏视频")
    public Result favoriteVideo(@RequestParam @NotNull Long videoId) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return videoService.favoriteVideo(videoId, userId);
    }

    @PostMapping("/unfavorite")
    @Operation(summary = "取消收藏视频")
    public Result unfavoriteVideo(@RequestParam @NotNull Long videoId) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return videoService.unfavoriteVideo(videoId, userId);
    }

    @GetMapping("/get_favorite_video_page")
    @Operation(summary = "分页获取用户收藏视频列表")
    public Result getFavoriteVideoList(@RequestParam @NotNull Integer page, @RequestParam @NotNull Integer size) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return videoService.getFavoriteVideoList(userId, page, size);
    }

    @Operation(summary = "分页获取用户浏览记录")
    @GetMapping("/get_browse_record_page")
    public Result getBrowseRecordList(@RequestParam @NotNull Integer page, @RequestParam @NotNull Integer size) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return videoService.getBrowseRecordList(userId, page, size);
    }

    @Operation(summary = "分页搜索视频")
    @GetMapping("/search_video")
    public Result searchVideo(@RequestParam @NotBlank String keyword,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer size,
                              @RequestParam @NotBlank String sortType) throws IOException {
        return videoService.searchVideo(keyword, page, size, sortType);
    }

    @GetMapping("/get_user_like_video_list")
    @Operation(summary = "分页获取用户点赞视频列表")
    public Result getUserLikeVideoList(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return videoService.getUserLikeVideoList(userId, page, size);
    }
}
