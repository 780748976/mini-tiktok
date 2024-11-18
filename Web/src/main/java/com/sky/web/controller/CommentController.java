package com.sky.web.controller;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.CommentParam;
import com.sky.web.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@Tag(name = "Comment", description = "视频评论控制器")
@Validated
public class CommentController {

    @Resource
    private CommentService commentService;

    @PostMapping("/add")
    @Operation(summary = "在视频下添加评论")
    public Result addComment(@Valid @RequestBody CommentParam commentParam) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return commentService.addComment(commentParam, userId);
    }

    @PostMapping("/like")
    @Operation(summary = "点赞评论")
    public Result likeComment(@RequestParam @Min(1) Long commentId) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return commentService.likeComment(commentId, userId);
    }

    @PostMapping("/dislike")
    @Operation(summary = "点踩评论")
    public Result dislikeComment(@RequestParam @Min(1) Long commentId) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return commentService.dislikeComment(commentId, userId);
    }
}