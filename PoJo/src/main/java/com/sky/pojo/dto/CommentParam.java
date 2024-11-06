package com.sky.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "评论数据传输对象")
public class CommentParam {
    @Schema(description = "视频ID")
    @NotNull(message = "视频ID不可为空")
    private Long videoId;

    @Schema(description = "父评论ID，回复时填写")
    private Long parentId;

    @Schema(description = "用户ID")
    @NotBlank(message = "用户ID不可为空")
    private String userId;

    @Schema(description = "评论内容")
    @NotBlank(message = "评论内容不可为空")
    private String content;

    @Schema(description = "@用户ID")
    private String mentionUserId;
}