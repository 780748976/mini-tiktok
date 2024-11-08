package com.sky.web.controller;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.CommentParam;
import com.sky.web.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
        return commentService.addComment(commentParam);
    }

    @PostMapping("/reply")
    @Operation(summary = "在评论下添加回复")
    public Result replyComment(@Valid @RequestBody CommentParam commentParam) {
        return commentService.replyComment(commentParam);
    }

    @PostMapping("/like")
    @Operation(summary = "点赞评论")
    public Result likeComment(@RequestParam @Min(1) Long commentId) {
        return commentService.likeComment(commentId);
    }

    @PostMapping("/dislike")
    @Operation(summary = "点踩评论")
    public Result dislikeComment(@RequestParam @Min(1) Long commentId) {
        return commentService.dislikeComment(commentId);
    }
}