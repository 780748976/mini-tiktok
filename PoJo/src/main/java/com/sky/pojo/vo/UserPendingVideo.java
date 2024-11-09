package com.sky.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UserPendingVideo", description = "用户待审核视频信息")
public class UserPendingVideo {

    @Schema(description = "视频ID")
    private Long videoId;

    @Schema(description = "视频标题")
    private String title;

    @Schema(description = "视频封面")
    private String cover;

    @Schema(description = "视频链接")
    private String url;

    @Schema(description = "视频描述")
    private String description;

    @Schema(description = "作者ID")
    private Long userId;

    @Schema(description = "作者名字")
    private String nickname;

    @Schema(description = "上传时间")
    private String uploadTime;
}
