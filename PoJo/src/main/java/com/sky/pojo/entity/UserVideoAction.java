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
@TableName("user_video_action")
@Schema(name = "UserVideoAction", description = "用户对视频的点赞/点踩记录")
public class UserVideoAction {
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "视频ID")
    private Long videoId;

    @Schema(description = "操作类型，1表示LIKE，2表示DISLIKE")
    private Integer actionType;

    @Schema(description = "操作时间")
    private LocalDateTime actionTime;
}