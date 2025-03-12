package com.sky.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "评论数据传输对象")
public class CommentParam {
    @Schema(description = "视频ID")
    @NotNull(message = "视频ID不可为空")
    private Long videoId;

    @Schema(description = "父评论ID，回复时填写")
    private Long parentId;

    @Schema(description = "评论内容")
    @NotBlank(message = "评论内容不可为空")
    private String content;

    @Schema(description = "@用户ID列表")
    private List<String> mentionUserIds;
    
    @Schema(description = "回复的二级评论ID")
    private Long replyId;
    
    @Schema(description = "回复的用户ID")
    private Long replyUserId;
}