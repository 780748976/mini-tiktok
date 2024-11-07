package com.sky.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户收藏视频表
 */
@Data
@Accessors(chain = true)
@TableName("user_favorite_video")
@Schema(name = "UserFavoriteVideo", description = "用户收藏视频表")
public class UserFavoriteVideo {

    @Schema(description = "收藏ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "视频ID")
    private Long videoId;

    @Schema(description = "收藏时间")
    private LocalDateTime createTime;
}