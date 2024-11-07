package com.sky.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(name = "UserVideo", description = "用户视频信息")
@Data
@Accessors(chain = true)
public class UserVideo {

    @Schema(description = "视频ID")
    private Long videoId;

    @Schema(description = "视频标题")
    private String title;

    @Schema(description = "视频封面")
    private String cover;

    @Schema(description = "视频链接")
    private String url;

    @Schema(description = "作者ID")
    private Long userId;

    @Schema(description = "作者名字")
    private String nickname;

    @Schema(description = "播放次数")
    private Long views;

    @Schema(description = "点赞次数")
    private Long likes;

    @Schema(description = "点踩次数")
    private Long dislikes;

    @Schema(description = "是否点赞")
    private Boolean isLike;

    @Schema(description = "是否点踩")
    private Boolean isDislike;

    @Schema(description = "是否收藏")
    private Boolean isFavorite;
}
