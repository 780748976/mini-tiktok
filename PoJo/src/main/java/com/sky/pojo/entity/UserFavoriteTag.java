package com.sky.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("user_favorite_tag")
@Schema(name = "UserFavoriteTag", description = "用户最喜欢的视频标签统计表")
public class UserFavoriteTag {

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "视频标签ID")
    private Integer tagId;

    @Schema(description = "概率分析")
    private Integer probability;
}